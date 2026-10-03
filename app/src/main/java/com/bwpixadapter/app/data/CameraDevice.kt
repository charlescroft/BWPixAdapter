package com.bwpixadapter.app.data

import org.json.JSONObject

data class CameraDevice(
    val id: String,
    val name: String,
    val wifiSsid: String,
    val host: String,
    val rtspPort: Int,
    val httpPort: Int,
    val user: String,
    val password: String,
    val uid: String = "",
    val forceRtpTcp: Boolean = true,
) {
    fun rtspUrl(): String = buildString {
        append("rtsp://")
        append(user.urlEncode())
        append(':')
        append(password.urlEncode())
        append('@')
        append(host)
        append(':').append(rtspPort).append('/')
    }

    fun httpBase(): String = "http://$host:$httpPort"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("wifiSsid", wifiSsid)
        put("host", host)
        put("rtspPort", rtspPort)
        put("httpPort", httpPort)
        put("user", user)
        put("password", password)
        put("uid", uid)
        put("forceRtpTcp", forceRtpTcp)
    }

    companion object {
        fun fromJson(o: JSONObject): CameraDevice = CameraDevice(
            id = o.optString("id", java.util.UUID.randomUUID().toString()),
            name = o.optString("name"),
            wifiSsid = o.optString("wifiSsid"),
            host = o.optString("host", DEFAULT_HOST),
            rtspPort = o.optInt("rtspPort", 554),
            httpPort = o.optInt("httpPort", 80),
            user = o.optString("user"),
            password = o.optString("password"),
            uid = o.optString("uid"),
            forceRtpTcp = o.optBoolean("forceRtpTcp", true),
        )

        const val DEFAULT_HOST = "192.168.234.1"
    }
}

private fun String.urlEncode(): String =
    java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20")
