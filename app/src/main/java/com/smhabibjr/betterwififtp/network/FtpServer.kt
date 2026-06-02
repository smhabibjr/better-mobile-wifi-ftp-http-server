package com.smhabibjr.betterwififtp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FtpServer(
    private val rootPath: String,
    private val readOnly: Boolean,
    private val serverIp: String,
    private val username: String = "",
    private val password: String = "",
) {
    private val _activeClients = MutableStateFlow(0)
    val activeClients: StateFlow<Int> = _activeClients

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null

    /** Returns the actual port bound (tries [preferredPort] then 2122). */
    suspend fun start(preferredPort: Int = 2121): Int = withContext(Dispatchers.IO) {
        val ss = try { ServerSocket(preferredPort) } catch (_: Exception) { ServerSocket(2122) }
        serverSocket = ss
        val s = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = s
        s.launch {
            while (!ss.isClosed) {
                val client = try { ss.accept() } catch (_: Exception) { break }
                s.launch { handleSession(client) }  // child of scope, not of accept-loop coroutine
            }
        }
        ss.localPort
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        scope?.cancel()
        scope = null
        _activeClients.value = 0
    }

    private suspend fun handleSession(socket: Socket) = withContext(Dispatchers.IO) {
        _activeClients.value++
        try {
            socket.use { ctrl ->
                val w = PrintWriter(OutputStreamWriter(ctrl.getOutputStream(), Charsets.UTF_8), true)
                val r = BufferedReader(InputStreamReader(ctrl.getInputStream(), Charsets.UTF_8))

                var cwd = File(rootPath).canonicalFile
                var dataServerSocket: ServerSocket? = null
                var renameFrom: File? = null
                var pendingUser = ""
                var loggedIn = false

                fun send(code: Int, msg: String) = w.println("$code $msg")

                fun sendMultiLine(code: Int, lines: List<String>) {
                    lines.forEachIndexed { i, line ->
                        if (i < lines.lastIndex) w.println("$code-$line") else w.println("$code $line")
                    }
                }

                fun openData(): Socket? = try {
                    val s = dataServerSocket?.accept()
                    dataServerSocket?.close()
                    dataServerSocket = null
                    s
                } catch (_: Exception) { null }

                fun resolve(arg: String): File? {
                    val f = if (arg.startsWith('/')) File(rootPath, arg).canonicalFile
                             else File(cwd, arg).canonicalFile
                    return if (f.absolutePath.startsWith(File(rootPath).canonicalPath)) f else null
                }

                fun listEntry(f: File): String {
                    val perms = if (f.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                    val size = if (f.isDirectory) 0L else f.length()
                    val cal = Calendar.getInstance().apply { timeInMillis = f.lastModified() }
                    val month = SimpleDateFormat("MMM", Locale.US).format(cal.time)
                    val day = "%2d".format(cal.get(Calendar.DAY_OF_MONTH))
                    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
                    val timeOrYear = if (cal.get(Calendar.YEAR) == thisYear)
                        "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    else cal.get(Calendar.YEAR).toString()
                    return "$perms 1 owner group $size $month $day $timeOrYear ${f.name}"
                }

                send(220, "BetterWiFiFTP Server ready")

                var line: String?
                outer@ while (r.readLine().also { line = it } != null) {
                    val raw = line!!.trim()
                    val upperCmd = raw.substringBefore(' ').uppercase()
                    val arg = if (' ' in raw) raw.substringAfter(' ').trim() else ""

                    when (upperCmd) {
                        // ── Pre-auth commands (always allowed) ────────────
                        "USER" -> { pendingUser = arg; send(331, "Password required") }
                        "PASS" -> {
                            val userOk = username.isEmpty() || pendingUser == username
                            val passOk = password.isEmpty() || arg == password
                            if (userOk && passOk) {
                                loggedIn = true
                                send(230, "User logged in")
                            } else {
                                send(530, "Login incorrect")
                            }
                        }
                        "QUIT" -> { send(221, "Goodbye"); break@outer }
                        "AUTH" -> send(502, "Security extensions not supported")
                        "SYST" -> send(215, "UNIX Type: L8")
                        "FEAT" -> sendMultiLine(211, listOf("Features:", " PASV", " EPSV", " SIZE", " MDTM", " UTF8", "End"))
                        "NOOP" -> send(200, "OK")
                        "ACCT", "TYPE", "MODE", "STRU", "OPTS" -> send(200, "OK")

                        // ── Authenticated commands ────────────────────────
                        else -> if (!loggedIn) {
                            send(530, "Not logged in")
                        } else when (upperCmd) {
                        "PWD", "XPWD" -> {
                            val rel = "/" + cwd.relativeTo(File(rootPath).canonicalFile).path
                                .replace('\\', '/').trimStart('/')
                            send(257, "\"${rel.ifEmpty { "/" }}\" is current directory")
                        }

                        "CWD", "XCWD" -> {
                            val target = if (arg == "..") cwd.parentFile ?: cwd
                                         else resolve(arg)
                            if (target != null && target.isDirectory) {
                                cwd = target; send(250, "Directory changed")
                            } else send(550, "No such directory")
                        }

                        "CDUP", "XCUP" -> {
                            val parent = cwd.parentFile
                            if (parent != null && parent.absolutePath.startsWith(File(rootPath).canonicalPath)) {
                                cwd = parent; send(250, "Directory changed")
                            } else send(250, "Directory unchanged")  // must be 2xx; 550 breaks FileZilla
                        }

                        "PASV" -> {
                            dataServerSocket?.close()
                            val ds = ServerSocket(0).also { it.soTimeout = 15_000 }
                            dataServerSocket = ds
                            val p = ds.localPort
                            val parts = serverIp.split('.')
                            if (parts.size == 4) {
                                send(227, "Entering Passive Mode (${parts[0]},${parts[1]},${parts[2]},${parts[3]},${p / 256},${p % 256})")
                            } else {
                                send(227, "Entering Passive Mode (127,0,0,1,${p / 256},${p % 256})")
                            }
                        }

                        "EPSV" -> {
                            dataServerSocket?.close()
                            val ds = ServerSocket(0).also { it.soTimeout = 15_000 }
                            dataServerSocket = ds
                            send(229, "Entering Extended Passive Mode (|||${ds.localPort}|)")
                        }

                        "LIST", "NLST" -> {
                            val conn = openData()
                            if (conn == null) { send(425, "Can't open data connection") }
                            else {
                                send(150, "Opening data connection")
                                try {
                                    conn.use { dc ->
                                        val out = PrintWriter(OutputStreamWriter(dc.getOutputStream(), Charsets.UTF_8), true)
                                        val entries = cwd.listFiles()
                                            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                                            ?: emptyList()
                                        entries.forEach { f ->
                                            out.println(if (upperCmd == "NLST") f.name else listEntry(f))
                                        }
                                        out.flush()
                                    }
                                    send(226, "Transfer complete")
                                } catch (_: Exception) { send(426, "Transfer aborted") }
                            }
                        }

                        "MLSD" -> {
                            val conn = openData()
                            if (conn == null) { send(425, "Can't open data connection") }
                            else {
                                send(150, "Opening data connection")
                                try {
                                    conn.use { dc ->
                                        val out = PrintWriter(OutputStreamWriter(dc.getOutputStream(), Charsets.UTF_8), true)
                                        val fmt = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                                        cwd.listFiles()
                                            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                                            ?.forEach { f ->
                                                val type = if (f.isDirectory) "dir" else "file"
                                                out.println("Type=$type;Size=${f.length()};Modify=${fmt.format(f.lastModified())}; ${f.name}")
                                            }
                                        out.flush()
                                    }
                                    send(226, "Transfer complete")
                                } catch (_: Exception) { send(426, "Transfer aborted") }
                            }
                        }

                        "SIZE" -> {
                            val f = resolve(arg)
                            if (f != null && f.isFile) send(213, f.length().toString())
                            else send(550, "File not found")
                        }

                        "MDTM" -> {
                            val f = resolve(arg)
                            if (f != null && f.exists()) {
                                val fmt = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                                send(213, fmt.format(f.lastModified()))
                            } else send(550, "File not found")
                        }

                        "RETR" -> {
                            val f = resolve(arg)
                            if (f == null || !f.isFile) {
                                send(550, "File not found")
                            } else {
                                val conn = openData()
                                if (conn == null) { send(425, "Can't open data connection") }
                                else {
                                    send(150, "Opening data connection for ${f.name}")
                                    try {
                                        conn.use { dc ->
                                            f.inputStream().use { it.copyTo(dc.getOutputStream()) }
                                            dc.getOutputStream().flush()
                                        }
                                        send(226, "Transfer complete")
                                    } catch (_: Exception) { send(426, "Transfer aborted") }
                                }
                            }
                        }

                        "STOR", "STOU", "APPE" -> {
                            if (readOnly) { send(550, "Server is read-only") }
                            else {
                                val f = resolve(arg) ?: File(cwd, arg)
                                val conn = openData()
                                if (conn == null) { send(425, "Can't open data connection") }
                                else {
                                    send(150, "Opening data connection")
                                    try {
                                        conn.use { dc ->
                                            val append = upperCmd == "APPE"
                                            java.io.FileOutputStream(f, append).use { dc.inputStream.copyTo(it) }
                                        }
                                        send(226, "Transfer complete")
                                    } catch (_: Exception) { send(426, "Transfer aborted") }
                                }
                            }
                        }

                        "MKD", "XMKD" -> {
                            if (readOnly) { send(550, "Server is read-only") }
                            else {
                                val f = resolve(arg) ?: File(cwd, arg)
                                if (f.mkdirs()) send(257, "\"${f.name}\" created")
                                else send(550, "Could not create directory")
                            }
                        }

                        "RMD", "XRMD" -> {
                            if (readOnly) { send(550, "Server is read-only") }
                            else {
                                val f = resolve(arg)
                                if (f != null && f.isDirectory && f.deleteRecursively()) send(250, "Directory removed")
                                else send(550, "Could not remove directory")
                            }
                        }

                        "DELE" -> {
                            if (readOnly) { send(550, "Server is read-only") }
                            else {
                                val f = resolve(arg)
                                if (f != null && f.isFile && f.delete()) send(250, "File deleted")
                                else send(550, "Could not delete")
                            }
                        }

                        "RNFR" -> {
                            renameFrom = resolve(arg)
                            if (renameFrom != null) send(350, "Ready for destination name")
                            else send(550, "File not found")
                        }

                        "RNTO" -> {
                            if (readOnly) { send(550, "Server is read-only") }
                            else {
                                val src = renameFrom
                                if (src == null) { send(503, "RNFR required first") }
                                else {
                                    val dst = resolve(arg) ?: File(cwd, arg)
                                    renameFrom = null
                                    if (src.renameTo(dst)) send(250, "Rename successful")
                                    else send(550, "Rename failed")
                                }
                            }
                        }

                        else -> send(502, "Command not implemented: $upperCmd")
                        } // end inner when (authenticated)
                    } // end outer when
                }
                dataServerSocket?.close()
            }
        } finally {
            _activeClients.value = (_activeClients.value - 1).coerceAtLeast(0)
        }
    }
}
