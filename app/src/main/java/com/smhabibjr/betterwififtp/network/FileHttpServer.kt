package com.smhabibjr.betterwififtp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class FileHttpServer(
    private val rootPath: String,
    private val readOnly: Boolean,
    private val username: String = "",
    private val password: String = "",
) {

    private val _activeClients = MutableStateFlow(0)
    val activeClients: StateFlow<Int> = _activeClients

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null

    /** Starts the server. Tries [preferredPort] then 8889. Returns the actual port used. */
    suspend fun start(preferredPort: Int = 8888): Int = withContext(Dispatchers.IO) {
        val ss = try {
            ServerSocket(preferredPort)
        } catch (_: Exception) {
            ServerSocket(8889)
        }
        serverSocket = ss
        val actualPort = ss.localPort
        val s = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = s
        s.launch {
            while (!ss.isClosed) {
                val client = try { ss.accept() } catch (_: Exception) { break }
                s.launch { handleClient(client) }
            }
        }
        actualPort
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        scope?.cancel()
        scope = null
        _activeClients.value = 0
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        _activeClients.value++
        try {
            socket.use { s ->
                // Use BufferedInputStream so headers and body share one stream — no byte-stealing.
                val inStream = s.getInputStream().buffered()

                fun readLine(): String? {
                    val sb = StringBuilder()
                    var prev = -1
                    while (true) {
                        val b = inStream.read()
                        if (b < 0) return if (sb.isEmpty()) null else sb.toString()
                        if (b == '\n'.code) {
                            if (prev == '\r'.code && sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
                            return sb.toString()
                        }
                        sb.append(b.toChar())
                        prev = b
                    }
                }

                val requestLine = readLine() ?: return@use
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@use
                val method = parts[0]
                val rawPath = parts[1]

                // Drain headers
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine() ?: break
                    if (line.isEmpty()) break
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        headers[line.substring(0, colonIdx).trim().lowercase()] =
                            line.substring(colonIdx + 1).trim()
                    }
                }

                if (!isAuthorized(headers)) {
                    s.getOutputStream().write(response401())
                    s.getOutputStream().flush()
                    return@use
                }

                val decodedPath = URLDecoder.decode(rawPath.substringBefore('?'), "UTF-8")
                val file = File(rootPath, decodedPath).canonicalFile

                // Prevent path traversal
                if (!file.absolutePath.startsWith(File(rootPath).canonicalPath)) {
                    s.getOutputStream().write(response403())
                    return@use
                }

                val out = s.getOutputStream()

                if (method == "POST" && !readOnly && file.isDirectory) {
                    if (rawPath.contains("?mkdir")) {
                        handleMkdir(headers, file, decodedPath, s, inStream)
                    } else {
                        handleUpload(headers, file, s, decodedPath, inStream)
                    }
                    return@use
                }

                when {
                    file.isDirectory -> out.write(directoryListing(file, decodedPath))
                    file.isFile -> {
                        val bytes = file.readBytes()
                        val header = "HTTP/1.0 200 OK\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                            "Content-Length: ${bytes.size}\r\n\r\n"
                        out.write(header.toByteArray())
                        out.write(bytes)
                    }
                    else -> out.write(response404(decodedPath))
                }
                out.flush()
            }
        } finally {
            _activeClients.value = (_activeClients.value - 1).coerceAtLeast(0)
        }
    }

    private fun directoryListing(dir: File, urlPath: String): ByteArray {
        val entries = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
        sb.append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
        sb.append("<title>${dir.name}</title>")
        sb.append("<style>")
        sb.append("body{font-family:system-ui,sans-serif;background:#0B1220;color:#E2E8F0;margin:0;padding:20px}")
        sb.append("h1{font-size:18px;color:#22D3EE;margin-bottom:16px}")
        sb.append("a{color:#E2E8F0;text-decoration:none;display:flex;align-items:center;gap:10px;padding:10px 14px;border-radius:10px;border:1px solid rgba(148,163,184,0.12);margin-bottom:8px;background:#141C32}")
        sb.append("a:hover{background:#1A2340;border-color:rgba(148,163,184,0.22)}")
        sb.append(".meta{color:#64748B;font-size:12px;margin-left:auto}")
        sb.append("form{margin-top:24px;padding:16px;border-radius:12px;background:#141C32;border:1px solid rgba(148,163,184,0.12)}")
        sb.append("input[type=file]{color:#94A3B8;margin-bottom:12px;display:block}")
        sb.append("input[type=text]{background:#0B1220;border:1px solid rgba(148,163,184,0.2);color:#E2E8F0;padding:8px 12px;border-radius:8px;margin-bottom:12px;display:block;width:100%;box-sizing:border-box}")
        sb.append("button{background:#06B6D4;color:#031018;border:none;padding:9px 20px;border-radius:8px;font-weight:600;cursor:pointer}")
        sb.append("</style></head><body>")
        sb.append("<h1>📂 ${dir.name}</h1>")

        val parent = urlPath.trimEnd('/')
        val parentUrl = if (parent.contains('/')) parent.substringBeforeLast('/').ifEmpty { "/" } else "/"
        if (urlPath != "/") sb.append("<a href='$parentUrl'>⬆ ..</a>")

        for (entry in entries) {
            val icon = if (entry.isDirectory) "🗂" else "📄"
            val href = (if (parent.isEmpty()) "" else parent) + "/" + entry.name
            val meta = if (entry.isFile) formatSize(entry.length()) else ""
            sb.append("<a href='$href'>$icon ${entry.name}<span class='meta'>$meta</span></a>")
        }

        if (!readOnly) {
            val action = if (parent.isEmpty()) "/" else parent
            sb.append("<form method='POST' action='$action' enctype='multipart/form-data'>")
            sb.append("<p style='color:#94A3B8;margin:0 0 10px;font-size:13px'>Upload file</p>")
            sb.append("<input type='file' name='file'><button type='submit'>Upload</button>")
            sb.append("</form>")
            sb.append("<form method='POST' action='$action?mkdir' enctype='application/x-www-form-urlencoded'>")
            sb.append("<p style='color:#94A3B8;margin:0 0 10px;font-size:13px'>Create folder</p>")
            sb.append("<input type='text' name='name' placeholder='Folder name' required>")
            sb.append("<button type='submit'>Create</button>")
            sb.append("</form>")
        }

        sb.append("</body></html>")
        val body = sb.toString().toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.0 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\n\r\n"
        return header.toByteArray() + body
    }

    private fun handleUpload(
        headers: Map<String, String>,
        dir: File,
        socket: Socket,
        currentUrlPath: String = "/",
        inStream: InputStream,
    ) {
        try {
            val contentType = headers["content-type"] ?: return
            val boundary = contentType.split(";")
                .firstOrNull { it.trim().startsWith("boundary=") }
                ?.substringAfter("boundary=")?.trim() ?: return
            if (boundary.isEmpty()) return

            // Skip opening --boundary line
            readStreamLine(inStream) ?: return

            // Read part headers, extract filename
            var filename = "upload"
            while (true) {
                val line = readStreamLine(inStream) ?: return
                if (line.isEmpty()) break
                if (line.lowercase().startsWith("content-disposition:"))
                    Regex("filename=\"([^\"]+)\"").find(line)?.let { filename = it.groupValues[1] }
            }

            // Stream directly to disk — no full-file memory allocation
            val endMarker = "\r\n--$boundary".toByteArray(Charsets.ISO_8859_1)
            File(dir, filename).outputStream().buffered().use { fos ->
                streamUntilBoundary(inStream, fos, endMarker)
            }

            val location = currentUrlPath.ifEmpty { "/" }
            socket.getOutputStream().write("HTTP/1.0 303 See Other\r\nLocation: $location\r\n\r\n".toByteArray())
        } catch (_: Exception) {}
    }

    private fun readStreamLine(inStream: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = inStream.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) {
                if (sb.endsWith('\r')) sb.deleteCharAt(sb.length - 1)
                return sb.toString()
            }
            sb.append(b.toChar())
        }
    }

    private fun streamUntilBoundary(inStream: InputStream, out: OutputStream, endMarker: ByteArray) {
        val window = ByteArray(endMarker.size)
        var filled = 0
        // Pre-fill sliding window
        while (filled < endMarker.size) {
            val b = inStream.read()
            if (b < 0) { out.write(window, 0, filled); return }
            window[filled++] = b.toByte()
        }
        val writeBuf = ByteArray(8192)
        var wPos = 0
        fun flush() { if (wPos > 0) { out.write(writeBuf, 0, wPos); wPos = 0 } }
        while (true) {
            if (window.contentEquals(endMarker)) break
            writeBuf[wPos++] = window[0]
            if (wPos == writeBuf.size) flush()
            System.arraycopy(window, 1, window, 0, endMarker.size - 1)
            val b = inStream.read()
            if (b < 0) { flush(); out.write(window, 0, endMarker.size - 1); return }
            window[endMarker.size - 1] = b.toByte()
        }
        flush()
    }

    private fun handleMkdir(
        headers: Map<String, String>,
        dir: File,
        currentUrlPath: String,
        socket: Socket,
        inStream: InputStream,
    ) {
        try {
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val raw = if (contentLength > 0) {
                val buf = ByteArray(contentLength)
                inStream.read(buf, 0, contentLength)
                String(buf, Charsets.UTF_8)
            } else ""
            val name = raw.split("&")
                .firstOrNull { it.startsWith("name=") }
                ?.removePrefix("name=")
                ?.let { URLDecoder.decode(it, "UTF-8") }
                ?.trim()
                ?.replace(Regex("[/\\\\]"), "")
            if (!name.isNullOrEmpty()) {
                File(dir, name).mkdirs()
            }
            val location = currentUrlPath.ifEmpty { "/" }
            socket.getOutputStream().write("HTTP/1.0 303 See Other\r\nLocation: $location\r\n\r\n".toByteArray())
        } catch (_: Exception) {}
    }

    private fun isAuthorized(headers: Map<String, String>): Boolean {
        if (username.isEmpty() && password.isEmpty()) return true
        val authHeader = headers["authorization"] ?: return false
        if (!authHeader.startsWith("Basic ")) return false
        val decoded = try {
            String(
                android.util.Base64.decode(authHeader.removePrefix("Basic ").trim(), android.util.Base64.DEFAULT),
                Charsets.UTF_8,
            )
        } catch (_: Exception) { return false }
        val colonIdx = decoded.indexOf(':')
        if (colonIdx < 0) return false
        val suppliedUser = decoded.substring(0, colonIdx)
        val suppliedPass = decoded.substring(colonIdx + 1)
        val userOk = username.isEmpty() || suppliedUser == username
        val passOk = password.isEmpty() || suppliedPass == password
        return userOk && passOk
    }

    private fun response401() =
        "HTTP/1.0 401 Unauthorized\r\nWWW-Authenticate: Basic realm=\"BetterWiFiFTP\"\r\nContent-Length: 0\r\n\r\n"
            .toByteArray()

    private fun response403() =
        "HTTP/1.0 403 Forbidden\r\nContent-Type: text/plain\r\n\r\nForbidden".toByteArray()

    private fun response404(path: String) =
        "HTTP/1.0 404 Not Found\r\nContent-Type: text/html\r\n\r\n<h2>404 — Not found: $path</h2>".toByteArray()

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
        bytes >= 1_048_576L -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1_024L -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}
