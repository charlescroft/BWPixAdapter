package com.bwpixadapter.app.wifi

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Auto-detects a BW Pix camera when the phone is connected to its WiFi hotspot
 * (AP mode). In AP mode the camera IS the network gateway (192.168.234.1), so we
 * probe the gateway for the HiIpcam RTSP server + thttpd web server fingerprint.
 */
object CameraDetector {

    data class Detection(
        val host: String,
        val rtspPort: Int = 554,
        val httpPort: Int = 80,
        val method: String,
    )

    data class NetworkInfo(
        val ssid: String?,
        val gateway: String?,
    )

    fun currentNetwork(context: Context): NetworkInfo {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val info = wm.connectionInfo
            val ssid = info?.ssid?.trim('"')?.takeIf {
                it.isNotBlank() && it != "<unknown ssid>"
            }
            val gateway = wm.dhcpInfo?.takeIf { it.gateway != 0 }?.let { intToIp(it.gateway) }
            NetworkInfo(ssid, gateway)
        } catch (e: Exception) {
            NetworkInfo(null, null)
        }
    }

    /**
     * Detects the camera on the connected network. Tries the gateway first
     * (the camera in AP mode), then a small scan of the subnet.
     */
    suspend fun detect(context: Context): Detection? = withContext(Dispatchers.IO) {
        val info = currentNetwork(context)
        val gateway = info.gateway

        gateway?.let { gw ->
            if (isCamera(gw)) {
                return@withContext Detection(host = gw, method = "gateway")
            }
        }

        // Fallback: scan a small address range of the connected subnet.
        scanSubnet(context)?.let { return@withContext it }
        null
    }

    private fun scanSubnet(context: Context): Detection? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wm.dhcpInfo ?: return null
        val ip = dhcp.ipAddress
        val mask = dhcp.netmask
        val net = (ip.toLong() and mask.toLong())
        val range = 1..40
        val hosts = range.map { off -> intToIp((net or off.toLong()).toInt()) }
        for (h in hosts) {
            if (isCamera(h, quick = true)) {
                return Detection(host = h, method = "scan")
            }
        }
        return null
    }

    /** True if the host exposes the HiIpcam RTSP server or thttpd web server. */
    private fun isCamera(host: String, quick: Boolean = false): Boolean {
        val timeout = if (quick) 350 else 700
        val rtspOk = try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, 554), timeout)
                s.soTimeout = timeout
                val out = BufferedOutputStream(s.getOutputStream())
                out.write("OPTIONS rtsp://$host:554/ RTSP/1.0\r\nCSeq: 1\r\n\r\n".toByteArray())
                out.flush()
                val buf = ByteArray(256)
                val n = BufferedInputStream(s.getInputStream()).read(buf)
                val head = String(buf, 0, maxOf(n, 0), Charsets.ISO_8859_1)
                head.contains("HiIpcam") || head.startsWith("RTSP/1.0 200")
            }
        } catch (e: Exception) {
            false
        }

        val httpOk = try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, 80), timeout)
                s.soTimeout = timeout
                val out = BufferedOutputStream(s.getOutputStream())
                out.write("GET / HTTP/1.0\r\nHost: $host\r\n\r\n".toByteArray())
                out.flush()
                val buf = ByteArray(512)
                val n = BufferedInputStream(s.getInputStream()).read(buf)
                val head = String(buf, 0, maxOf(n, 0), Charsets.ISO_8859_1)
                head.contains("thttpd") || head.contains("HiIpcam")
            }
        } catch (e: Exception) {
            false
        }

        return rtspOk || httpOk
    }

    private fun intToIp(value: Int): String {
        val v = value.toLong() and 0xFFFFFFFFL
        return "${(v and 0xff)}.${(v shr 8) and 0xff}.${(v shr 16) and 0xff}.${(v shr 24) and 0xff}"
    }
}
