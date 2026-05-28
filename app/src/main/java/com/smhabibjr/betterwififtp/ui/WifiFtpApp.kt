package com.smhabibjr.betterwififtp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smhabibjr.betterwififtp.network.FileHttpServer
import com.smhabibjr.betterwififtp.network.FtpServer
import com.smhabibjr.betterwififtp.network.WifiInfoHelper
import com.smhabibjr.betterwififtp.storage.StorageHelper
import com.smhabibjr.betterwififtp.ui.components.BannerOverlay
import com.smhabibjr.betterwififtp.ui.components.PermissionModal
import com.smhabibjr.betterwififtp.ui.components.SimulateSheet
import com.smhabibjr.betterwififtp.ui.components.ToastOverlay
import com.smhabibjr.betterwififtp.ui.screens.HomeScreen
import com.smhabibjr.betterwififtp.ui.screens.ServerScreen
import com.smhabibjr.betterwififtp.ui.theme.AppBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ── App state models ──────────────────────────────────────────

enum class Screen { Home, Server }
enum class AccessMode { ReadOnly, ReadWrite }

data class WifiState(val connected: Boolean, val ssid: String, val ip: String)

data class ServerState(
    val clients: Int = 0,
    val ftpPort: Int = 2121,
    val httpPort: Int = 8080,
    val username: String = "",
    val password: String = "",
)

private fun generateFtpPassword(): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

private const val PREFS_NAME = "ftp_creds"
private const val KEY_USER = "ftp_username"
private const val KEY_PASS = "ftp_password"

private fun loadCreds(ctx: Context): Pair<String, String> {
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val user = prefs.getString(KEY_USER, null) ?: "user"
    val pass = prefs.getString(KEY_PASS, null) ?: generateFtpPassword().also { generated ->
        prefs.edit { putString(KEY_PASS, generated) }
    }
    return user to pass
}

private fun saveCreds(ctx: Context, user: String, pass: String) {
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit { putString(KEY_USER, user); putString(KEY_PASS, pass) }
}

data class BannerData(val tone: String, val title: String, val message: String)
data class ToastData(val tone: String, val message: String)

data class FolderInfo(val path: String, val name: String, val meta: String)

data class VolumeInfo(
    val id: String,
    val label: String,
    val short: String,
    val sub: String,
    val totalGb: Int,
    val usedGb: Int,
    val folders: List<FolderInfo>,
)

// ── Root composable ───────────────────────────────────────────

@Composable
fun WifiFtpApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Runtime storage permissions — needed for File.listFiles() to return media files
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    fun allGranted() = storagePermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var storageGranted by remember { mutableStateOf(allGranted()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { storageGranted = it.values.all { v -> v } }

    LaunchedEffect(Unit) {
        if (!storageGranted) permLauncher.launch(storagePermissions)
    }

    var screen by remember { mutableStateOf(Screen.Home) }
    var wifi by remember { mutableStateOf(WifiState(connected = false, ssid = "", ip = "")) }
    var folder by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AccessMode.ReadOnly) }
    var server by remember { mutableStateOf(ServerState()) }
    var banner by remember { mutableStateOf<BannerData?>(null) }
    var toast by remember { mutableStateOf<ToastData?>(null) }
    var showPermModal by remember { mutableStateOf(false) }
    var showSimSheet by remember { mutableStateOf(false) }
    var volumes by remember { mutableStateOf<List<VolumeInfo>>(emptyList()) }
    var httpServer by remember { mutableStateOf<FileHttpServer?>(null) }
    var ftpServer by remember { mutableStateOf<FtpServer?>(null) }
    var ftpUsername by remember { mutableStateOf("") }
    var ftpPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val (u, p) = withContext(Dispatchers.IO) { loadCreds(context) }
        ftpUsername = u
        ftpPassword = p
    }

    // Poll real WiFi info every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            wifi = withContext(Dispatchers.IO) {
                val connected = WifiInfoHelper.isWifiConnected(context)
                val ssid = if (connected) WifiInfoHelper.getWifiSsid(context) else ""
                val ip = if (connected) WifiInfoHelper.getLocalIpAddress() else ""
                WifiState(connected = connected, ssid = ssid, ip = ip)
            }
            delay(5_000)
        }
    }

    // Load real volumes; re-runs after permission is granted
    LaunchedEffect(storageGranted) {
        volumes = withContext(Dispatchers.IO) {
            StorageHelper.getAvailableVolumes(context)
        }
    }

    // Start/stop HTTP + FTP servers with screen
    LaunchedEffect(screen) {
        if (screen == Screen.Server) {
            val path = folder
            val writable = mode == AccessMode.ReadWrite
            val ip = wifi.ip

            val http = FileHttpServer(path, writable)
            val httpPort = http.start(8080)
            httpServer = http

            val ftp = FtpServer(path, !writable, ip, username = ftpUsername, password = ftpPassword)
            val ftpPort = ftp.start(2121)
            ftpServer = ftp

            server = server.copy(httpPort = httpPort, ftpPort = ftpPort, username = ftpUsername, password = ftpPassword)
        } else {
            httpServer?.stop(); httpServer = null
            ftpServer?.stop(); ftpServer = null
        }
    }

    // Collect real client count (HTTP + FTP combined)
    val httpClients by produceState(0, httpServer) {
        httpServer?.activeClients?.collect { value = it }
    }
    val ftpClients by produceState(0, ftpServer) {
        ftpServer?.activeClients?.collect { value = it }
    }
    val activeClients = httpClients + ftpClients

    LaunchedEffect(banner) {
        banner ?: return@LaunchedEffect
        delay(4200)
        banner = null
    }
    LaunchedEffect(toast) {
        toast ?: return@LaunchedEffect
        delay(3500)
        toast = null
    }
    // WiFi lost while server running → auto-stop + banner
    LaunchedEffect(wifi.connected, screen) {
        if (screen == Screen.Server && !wifi.connected) {
            banner = BannerData("red", "WiFi Lost — Server Stopped", "Reconnect to a network to share files again.")
            screen = Screen.Home
        }
    }

    Box(modifier = modifier.background(AppBg)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState == Screen.Server) {
                    (slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 6 } + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally(tween(220)) { -it / 6 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(tween(200)) { it / 6 } + fadeOut(tween(150)))
                }
            },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.Home -> HomeScreen(
                    wifi = wifi,
                    folder = folder,
                    mode = mode,
                    volumes = volumes,
                    ftpUsername = ftpUsername,
                    ftpPassword = ftpPassword,
                    onToggleWifi = { wifi = wifi.copy(connected = !wifi.connected) },
                    onSetFolder = { folder = it },
                    onSetMode = { mode = it },
                    onSetUsername = { ftpUsername = it; saveCreds(context, it, ftpPassword) },
                    onSetPassword = { ftpPassword = it; saveCreds(context, ftpUsername, it) },
                    onGeneratePassword = {
                        val p = generateFtpPassword()
                        ftpPassword = p
                        saveCreds(context, ftpUsername, p)
                    },
                    onStartServer = { server = ServerState(); screen = Screen.Server },
                    onSimulate = { showSimSheet = true },
                )
                Screen.Server -> ServerScreen(
                    wifi = wifi,
                    folder = folder,
                    mode = mode,
                    server = server.copy(clients = activeClients),
                    onStopServer = { screen = Screen.Home },
                    onSimulate = { showSimSheet = true },
                )
            }
        }

        // Banner (slides from top)
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically(tween(280)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
        ) {
            banner?.let { BannerOverlay(it, onClose = { banner = null }) }
        }

        // Toast (slides from bottom)
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        ) {
            toast?.let { ToastOverlay(it) }
        }

        if (showPermModal) {
            PermissionModal(onClose = { showPermModal = false })
        }

        if (showSimSheet) {
            SimulateSheet(
                screen = screen,
                wifiOn = wifi.connected,
                onPick = { id ->
                    showSimSheet = false
                    when (id) {
                        "wifiLost" -> if (wifi.connected) wifi = wifi.copy(connected = false)
                        "permission" -> showPermModal = true
                        "portBusy" -> {
                            server = server.copy(ftpPort = 2121)
                            toast = ToastData("amber", "Port 21 busy — switching to 2121")
                        }
                        "toggleWifi" -> wifi = wifi.copy(connected = !wifi.connected)
                    }
                },
                onClose = { showSimSheet = false },
            )
        }
    }
}
