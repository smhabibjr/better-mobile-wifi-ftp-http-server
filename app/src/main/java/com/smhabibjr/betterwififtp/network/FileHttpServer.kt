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

                val queryString = rawPath.substringAfter('?', "")
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
                    // Single directory → stream as ZIP
                    queryString == "zip" && file.isDirectory ->
                        handleZipDownload(out, file, file.name)

                    // Selected items → combined ZIP
                    queryString.startsWith("zip=") && file.isDirectory -> {
                        val names = URLDecoder.decode(queryString.removePrefix("zip="), "UTF-8").split(",")
                        val items = names.mapNotNull { name ->
                            File(file, name).canonicalFile.takeIf {
                                it.exists() && it.absolutePath.startsWith(file.canonicalPath)
                            }
                        }
                        handleMultiZipDownload(out, items, "Selection")
                    }

                    file.isDirectory -> out.write(directoryListing(file, decodedPath))
                    file.isFile -> {
                        val header = "HTTP/1.0 200 OK\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
                            "Content-Length: ${file.length()}\r\n\r\n"
                        out.write(header.toByteArray())
                        file.inputStream().use { it.copyTo(out) }
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
        sb.append(".row{display:flex;align-items:center;margin-bottom:8px;gap:6px}")
        sb.append(".entry{flex:1;color:#E2E8F0;text-decoration:none;display:flex;align-items:center;gap:10px;padding:10px 14px;border-radius:10px;border:1px solid rgba(148,163,184,0.12);background:#141C32}")
        sb.append(".entry:hover{background:#1A2340;border-color:rgba(148,163,184,0.22)}")
        sb.append(".meta{color:#64748B;font-size:12px;margin-left:auto}")
        sb.append(".cb{width:16px;height:16px;cursor:pointer;accent-color:#06B6D4;flex-shrink:0}")
        sb.append(".toolbar{display:none;align-items:center;gap:12px;padding:10px 14px;background:#141C32;border-radius:10px;margin-bottom:12px;border:1px solid rgba(34,211,238,0.2);color:#E2E8F0;font-size:13px}")
        sb.append(".sel-hdr{display:flex;align-items:center;gap:8px;margin-bottom:8px;color:#94A3B8;font-size:13px;padding:0 2px}")
        sb.append("form{margin-top:24px;padding:16px;border-radius:12px;background:#141C32;border:1px solid rgba(148,163,184,0.12)}")
        sb.append("input[type=file]{color:#94A3B8;margin-bottom:12px;display:block}")
        sb.append("input[type=text]{background:#0B1220;border:1px solid rgba(148,163,184,0.2);color:#E2E8F0;padding:8px 12px;border-radius:8px;margin-bottom:12px;display:block;width:100%;box-sizing:border-box}")
        sb.append("button{background:#06B6D4;color:#031018;border:none;padding:9px 20px;border-radius:8px;font-weight:600;cursor:pointer}")
        sb.append("#ctx-menu{display:none;position:fixed;background:#141C32;border:1px solid rgba(148,163,184,0.2);border-radius:8px;padding:4px;z-index:999;min-width:140px;box-shadow:0 4px 16px rgba(0,0,0,0.4)}")
        sb.append("#ctx-menu div{padding:8px 16px;cursor:pointer;color:#E2E8F0;border-radius:6px;font-size:14px}")
        sb.append("#ctx-menu div:hover{background:#1A2340}")
        sb.append("</style></head><body>")
        sb.append("<h1>📂 ${dir.name}</h1>")

        val parent = urlPath.trimEnd('/')
        val parentUrl = if (parent.contains('/')) parent.substringBeforeLast('/').ifEmpty { "/" } else "/"
        if (urlPath != "/") sb.append("<div class='row'><a class='entry' href='$parentUrl'>⬆ ..</a></div>")

        // Toolbar (shown when ≥1 checkbox checked)
        sb.append("<div class='toolbar' id='toolbar'>")
        sb.append("<span id='sel-count'>0 selected</span>")
        sb.append("<button onclick='downloadSelected()'>⬇ Download</button>")
        sb.append("</div>")

        // Select All header
        sb.append("<div class='sel-hdr'>")
        sb.append("<input type='checkbox' class='cb' id='sel-all' onchange='toggleAll(this)'>")
        sb.append("<label for='sel-all' style='cursor:pointer'>Select all</label>")
        sb.append("</div>")

        // Context menu
        sb.append("<div id='ctx-menu'><div id='ctx-dl'>⬇ Download</div></div>")

        for (entry in entries) {
            val icon = if (entry.isDirectory) "🗂" else "📄"
            val href = (if (parent.isEmpty()) "" else parent) + "/" + entry.name
            val dlHref = if (entry.isDirectory) "$href?zip" else href
            val meta = if (entry.isFile) formatSize(entry.length()) else ""
            sb.append("<div class='row' data-name='${entry.name}' data-href='$dlHref'>")
            sb.append("<input type='checkbox' class='cb'>")
            sb.append("<a class='entry' href='$href'>$icon ${entry.name}<span class='meta'>$meta</span></a>")
            sb.append("</div>")
        }

        if (!readOnly) {
            val action = if (parent.isEmpty()) "/" else parent
            sb.append("<form method='POST' action='$action' enctype='multipart/form-data'>")
            sb.append("<p style='color:#94A3B8;margin:0 0 12px;font-size:13px'>Upload</p>")
            sb.append("<label style='color:#94A3B8;font-size:12px;display:block;margin-bottom:4px'>Multiple files</label>")
            sb.append("<input type='file' name='file' multiple style='color:#94A3B8;margin-bottom:12px;display:block'>")
            sb.append("<label style='color:#94A3B8;font-size:12px;display:block;margin-bottom:4px;margin-top:4px'>Entire folder</label>")
            sb.append("<input type='file' name='file' webkitdirectory multiple style='color:#94A3B8;margin-bottom:12px;display:block'>")
            sb.append("<button type='submit'>Upload</button>")
            sb.append("</form>")
            sb.append("<form method='POST' action='$action?mkdir' enctype='application/x-www-form-urlencoded'>")
            sb.append("<p style='color:#94A3B8;margin:0 0 10px;font-size:13px'>Create folder</p>")
            sb.append("<input type='text' name='name' placeholder='Folder name' required>")
            sb.append("<button type='submit'>Create</button>")
            sb.append("</form>")
        }

        sb.append("<script>")
        sb.append("function toggleAll(cb){document.querySelectorAll('.row .cb').forEach(function(c){c.checked=cb.checked;});updateBar();}")
        sb.append("function updateBar(){")
        sb.append("var n=document.querySelectorAll('.row .cb:checked').length;")
        sb.append("document.getElementById('toolbar').style.display=n>0?'flex':'none';")
        sb.append("document.getElementById('sel-count').textContent=n+' selected';")
        sb.append("}")
        sb.append("document.querySelectorAll('.row .cb').forEach(function(cb){cb.addEventListener('change',updateBar);});")
        sb.append("function downloadSelected(){")
        sb.append("var rows=[].slice.call(document.querySelectorAll('.row .cb:checked')).map(function(cb){return cb.closest('.row');});")
        sb.append("if(rows.length===0)return;")
        sb.append("if(rows.length===1){window.location.href=rows[0].dataset.href;return;}")
        sb.append("var names=rows.map(function(r){return encodeURIComponent(r.dataset.name);}).join(',');")
        sb.append("window.location.href=location.pathname+'?zip='+names;")
        sb.append("}")
        sb.append("var menu=document.getElementById('ctx-menu');")
        sb.append("var ctxDl=document.getElementById('ctx-dl');")
        sb.append("document.querySelectorAll('.row').forEach(function(row){")
        sb.append("row.addEventListener('contextmenu',function(e){")
        sb.append("e.preventDefault();")
        sb.append("menu.style.display='block';")
        sb.append("menu.style.left=e.pageX+'px';")
        sb.append("menu.style.top=e.pageY+'px';")
        sb.append("ctxDl.onclick=function(){window.location.href=row.dataset.href;menu.style.display='none';};")
        sb.append("});")
        sb.append("});")
        sb.append("document.addEventListener('click',function(){menu.style.display='none';});")
        sb.append("</script>")

        sb.append("</body></html>")
        val body = sb.toString().toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.0 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\n\r\n"
        return header.toByteArray() + body
    }

    private fun handleZipDownload(out: OutputStream, dir: File, name: String) {
        val safe = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        out.write(
            ("HTTP/1.0 200 OK\r\nContent-Type: application/zip\r\n" +
            "Content-Disposition: attachment; filename=\"$safe.zip\"\r\n\r\n").toByteArray()
        )
        out.flush()
        val zos = java.util.zip.ZipOutputStream(out)
        try { zipAddDirectory(zos, dir, "") } catch (_: Exception) {}
        zos.finish()
        out.flush()
    }

    private fun handleMultiZipDownload(out: OutputStream, items: List<File>, zipName: String) {
        val safe = zipName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        out.write(
            ("HTTP/1.0 200 OK\r\nContent-Type: application/zip\r\n" +
            "Content-Disposition: attachment; filename=\"$safe.zip\"\r\n\r\n").toByteArray()
        )
        out.flush()
        val zos = java.util.zip.ZipOutputStream(out)
        try {
            items.forEach { item ->
                if (item.isDirectory) zipAddDirectory(zos, item, item.name)
                else zipAddFile(zos, item, item.name)
            }
        } catch (_: Exception) {}
        zos.finish()
        out.flush()
    }

    private fun zipAddDirectory(zos: java.util.zip.ZipOutputStream, dir: File, prefix: String) {
        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { child ->
            val entry = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            if (child.isDirectory) zipAddDirectory(zos, child, entry)
            else zipAddFile(zos, child, entry)
        }
    }

    private fun zipAddFile(zos: java.util.zip.ZipOutputStream, file: File, entryName: String) {
        try {
            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        } catch (_: Exception) {}
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

            val endMarker = "\r\n--$boundary".toByteArray(Charsets.ISO_8859_1)

            // Skip the opening --boundary line
            readStreamLine(inStream) ?: return

            while (true) {
                // Read part headers, extract filename (may contain relative path for folder uploads)
                var filename: String? = null
                while (true) {
                    val line = readStreamLine(inStream) ?: break
                    if (line.isEmpty()) break
                    if (line.lowercase().startsWith("content-disposition:"))
                        Regex("filename=\"([^\"]+)\"").find(line)?.let { filename = it.groupValues[1] }
                }

                if (!filename.isNullOrEmpty()) {
                    val safePath = sanitizeUploadPath(filename)
                    val destFile = File(dir, safePath)
                    if (destFile.canonicalPath.startsWith(dir.canonicalPath)) {
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().buffered().use { fos ->
                            streamUntilBoundary(inStream, fos, endMarker)
                        }
                    } else {
                        streamUntilBoundary(inStream, OutputStream.nullOutputStream(), endMarker)
                    }
                } else {
                    // No filename (non-file field or empty input) — consume and discard
                    streamUntilBoundary(inStream, OutputStream.nullOutputStream(), endMarker)
                }

                // After boundary: "--" = end of body, "\r\n" = more parts follow
                val b1 = inStream.read()
                val b2 = inStream.read()
                if (b1 == '-'.code && b2 == '-'.code) break
            }

            val location = currentUrlPath.ifEmpty { "/" }
            socket.getOutputStream().write("HTTP/1.0 303 See Other\r\nLocation: $location\r\n\r\n".toByteArray())
        } catch (_: Exception) {}
    }

    private fun sanitizeUploadPath(rawFilename: String): String {
        val parts = rawFilename.replace('\\', '/').split('/')
            .filter { it.isNotEmpty() && it != "." && it != ".." }
        return parts.joinToString("/")
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
