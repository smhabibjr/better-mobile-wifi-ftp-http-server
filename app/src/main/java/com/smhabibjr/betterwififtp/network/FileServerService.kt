package com.smhabibjr.betterwififtp.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smhabibjr.betterwififtp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FileServerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val rootPath = intent?.getStringExtra(KEY_ROOT)            ?: return START_NOT_STICKY
        val readOnly = intent.getBooleanExtra(KEY_READONLY, true)
        val ip       = intent.getStringExtra(KEY_IP)               ?: ""
        val user     = intent.getStringExtra(KEY_USER)             ?: ""
        val pass     = intent.getStringExtra(KEY_PASS)             ?: ""

        createChannel()
        startForeground(NOTIF_ID, buildNotification("Starting…"))

        scope.launch {
            val http     = FileHttpServer(rootPath, readOnly, user, pass)
            val httpPort = http.start(8888)
            val ftp      = FtpServer(rootPath, readOnly, ip, user, pass)
            val ftpPort  = ftp.start(2121)

            ServerRegistry.httpServer      = http
            ServerRegistry.ftpServer       = ftp
            ServerRegistry.httpPort.value  = httpPort
            ServerRegistry.ftpPort.value   = ftpPort
            ServerRegistry.username.value  = user
            ServerRegistry.password.value  = pass
            ServerRegistry.isRunning.value = true

            notifyManager().notify(NOTIF_ID, buildNotification("FTP :$ftpPort  HTTP :$httpPort"))

            combine(http.activeClients, ftp.activeClients) { h, f -> h + f }
                .collect { ServerRegistry.activeClients.value = it }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        ServerRegistry.stopAll()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(status: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi FTP/HTTP Server")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Server Status", NotificationManager.IMPORTANCE_LOW
            )
            notifyManager().createNotificationChannel(ch)
        }
    }

    private fun notifyManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ACTION_STOP      = "com.smhabibjr.betterwififtp.STOP_SERVER"
        private const val NOTIF_ID    = 1001
        private const val CHANNEL_ID  = "ftp_server"
        private const val KEY_ROOT    = "rootPath"
        private const val KEY_READONLY = "readOnly"
        private const val KEY_IP      = "ip"
        private const val KEY_USER    = "username"
        private const val KEY_PASS    = "password"

        fun startIntent(
            ctx: Context, rootPath: String, readOnly: Boolean,
            ip: String, user: String, pass: String,
        ) = Intent(ctx, FileServerService::class.java).apply {
            putExtra(KEY_ROOT, rootPath)
            putExtra(KEY_READONLY, readOnly)
            putExtra(KEY_IP, ip)
            putExtra(KEY_USER, user)
            putExtra(KEY_PASS, pass)
        }

        fun stopIntent(ctx: Context) =
            Intent(ctx, FileServerService::class.java).apply { action = ACTION_STOP }
    }
}
