package com.bwpixadapter.app.wifi

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

object WifiHelper {

    fun currentSsid(context: Context): String? {
        val app = context.applicationContext
        val wm = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val ssid = wm.connectionInfo?.ssid?.trim('"')
            ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } catch (e: Exception) {
            null
        }
    }

    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
