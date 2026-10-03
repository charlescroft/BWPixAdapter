package com.bwpixadapter.app.wifi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Checks whether a camera is actually reachable on the network. This is far more
 * reliable than comparing WiFi SSIDs (which needs location permission + services
 * on modern Android). If the camera IP answers, we're connected to its hotspot.
 */
object CameraProbe {

    suspend fun isReachable(host: String, port: Int, timeoutMs: Int = 1500): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
}
