package com.smhabibjr.betterwififtp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.NetworkInterface

object WifiInfoHelper {

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getWifiSsid(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val raw = wm.connectionInfo?.ssid ?: return "Connected WiFi"
        val stripped = raw.removePrefix("\"").removeSuffix("\"")
        return if (stripped.isBlank() || stripped == "<unknown ssid>") "Connected WiFi" else stripped
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
            for (iface in interfaces.asSequence()) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses.asSequence()) {
                    if (addr.isLoopbackAddress) continue
                    val ip = addr.hostAddress ?: continue
                    if (!ip.contains(':')) return ip  // IPv4 only
                }
            }
        } catch (_: Exception) {}
        return ""
    }
}
