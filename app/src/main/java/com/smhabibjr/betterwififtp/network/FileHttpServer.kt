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
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

class FileHttpServer(private val rootPath: String, private val readOnly: Boolean) {

    private val _activeClients = MutableStateFlow(0)
    val activeClients: StateFlow<Int> = _activeClients

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null

    /** Starts the server. Tries [preferredPort] then 8181. Returns the actual port used. */
    suspend fun start(preferredPort: Int = 8080): Int = withContext(Dispatchers.IO) {
        val ss = try {
            ServerSocket(preferredPort)
        } catch (_: Exception) {
            ServerSocket(8181)
        }
        serverSocket = ss
        val actualPort = ss.localPort
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope!!.launch {
            while (!ss.isClosed) {
                val client = try { ss.accept() } catch (_: Exception) { break }
                launch { handleClient(client) }
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
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val requestLine = reader.readLine() ?: return@use
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@use
                val method = parts[0]
                val rawPath = parts[1]

                // Drain headers
                val headers = mutableMapOf<String, String>()
                var line: String?
                while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    val colonIdx = line!!.indexOf(':')
                    if (colonIdx > 0) {
                        headers[line!!.substring(0, colonIdx).trim().lowercase()] =
                            line!!.substring(colonIdx + 1).trim()
                    }
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
                    handleUpload(reader, headers, file, s)
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
        sb.append("button{background:#06B6D4;color:#031018;border:none;padding:9px 20px;border-radius:8px;font-weight:600;cursor:pointer}")
        sb.append("</style></head><body>")
        sb.append("<h1>📂 ${dir.name}</h1>")

        val parent = urlPath.trimEnd('/')
        val parentUrl = if (parent.contains('/')) parent.substringBeforeLast('/').ifEmpty { "/" } else "/"
        if (urlPath != "/") sb.append("<a href='$parentUrl'>⬆ ..</a>")

        for (entry in entries) {
            val icon = if (entry.isDirectory) "🗂" else "📄"
            val href = (if (parent == "/") "" else parent) + "/" + entry.name
            val meta = if (entry.isFile) formatSize(entry.length()) else ""
            sb.append("<a href='$href'>$icon ${entry.name}<span class='meta'>$meta</span></a>")
        }

        if (!readOnly) {
            val action = if (parent.isEmpty()) "/" else parent
            sb.append("<form method='POST' action='$action' enctype='multipart/form-data'>")
            sb.append("<p style='color:#94A3B8;margin:0 0 10px;font-size:13px'>Upload file</p>")
            sb.append("<input type='file' name='file'><button type='submit'>Upload</button>")
            sb.append("</form>")
        }

        sb.append("</body></html>")
        val body = sb.toString().toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.0 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\n\r\n"
        return header.toByteArray() + body
    }

    private fun handleUpload(
        reader: BufferedReader,
        headers: Map<String, String>,
        dir: File,
        socket: Socket,
    ) {
        try {
            val contentLength = headers["content-length"]?.toLongOrNull() ?: return
            val contentType = headers["content-type"] ?: return
            val boundary = contentType.substringAfter("boundary=", "").trim()
            if (boundary.isEmpty()) return

            val raw = ByteArray(contentLength.toInt())
            val inStream = socket.getInputStream()
            var read = 0
            while (read < raw.size) {
                val r = inStream.read(raw, read, raw.size - read)
                if (r < 0) break
                read += r
            }

            val body = String(raw, Charsets.ISO_8859_1)
            val dispMarker = "Content-Disposition: form-data;"
            val dispIdx = body.indexOf(dispMarker)
            if (dispIdx < 0) return
            val dispLine = body.substring(dispIdx, body.indexOf('\n', dispIdx))
            val filenameMatch = Regex("filename=\"([^\"]+)\"").find(dispLine)
            val filename = filenameMatch?.groupValues?.get(1) ?: "upload"
            val headerEnd = body.indexOf("\r\n\r\n", dispIdx) + 4
            val endBoundary = "\r\n--$boundary"
            val fileEnd = body.indexOf(endBoundary, headerEnd)
            if (fileEnd < 0) return
            val fileBytes = raw.copyOfRange(headerEnd, fileEnd)
            File(dir, filename).writeBytes(fileBytes)

            val response = "HTTP/1.0 303 See Other\r\nLocation: /\r\n\r\n"
            socket.getOutputStream().write(response.toByteArray())
        } catch (_: Exception) {}
    }

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
