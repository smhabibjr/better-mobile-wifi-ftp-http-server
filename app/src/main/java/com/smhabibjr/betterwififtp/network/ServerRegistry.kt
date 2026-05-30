package com.smhabibjr.betterwififtp.network

import kotlinx.coroutines.flow.MutableStateFlow

object ServerRegistry {
    val isRunning     = MutableStateFlow(false)
    val ftpPort       = MutableStateFlow(0)
    val httpPort      = MutableStateFlow(0)
    val username      = MutableStateFlow("")
    val password      = MutableStateFlow("")
    val activeClients = MutableStateFlow(0)

    internal var httpServer: FileHttpServer? = null
    internal var ftpServer: FtpServer? = null

    fun stopAll() {
        httpServer?.stop(); httpServer = null
        ftpServer?.stop();  ftpServer  = null
        isRunning.value     = false
        activeClients.value = 0
    }
}
