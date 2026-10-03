package com.bwpixadapter.app.media

import com.bwpixadapter.app.data.CameraDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import kotlin.concurrent.thread

/**
 * Minimal RTSP client for the HiIpcam/HiSilicon RTSP variant used by BW Pix
 * cameras. Verified working recipe:
 *   DESCRIBE rtsp://host:554/11
 *   SETUP   rtsp://host:554/11/trackID=0  Transport: RTP/AVP/TCP;unicast;interleaved=0-1
 *   PLAY    rtsp://host:554/11
 * The server is finicky about auth on SETUP, so this tries without auth first
 * and falls back to Basic auth. RTP is received TCP-interleaved.
 */
class RtspSession(
    private val device: CameraDevice,
    private val channel: String = "11",
) {
    data class StreamInfo(val width: Int, val height: Int, val sps: ByteArray?, val pps: ByteArray?)

    var onNal: (H264Depacketizer.Nal) -> Unit = {}
    var onError: (String) -> Unit = {}
    var onReady: (StreamInfo) -> Unit = {}

    @Volatile
    private var running = false
    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null
    private var sessionId: String = ""
    private var readThread: Thread? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var csdInfo: StreamInfo? = null

    fun start() {
        if (running) return
        running = true
        thread(name = "rtsp-connect") { runSession() }
    }

    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        scope.cancel()
    }

    private fun runSession() {
        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(device.host, device.rtspPort), 6000)
            sock.tcpNoDelay = true
            socket = sock
            input = BufferedInputStream(sock.getInputStream())
            output = BufferedOutputStream(sock.getOutputStream())
            handshake()
        } catch (e: Exception) {
            if (running) onError(e.message ?: e.javaClass.simpleName)
            stop()
        }
    }

    private fun handshake() {
        val base = "rtsp://${device.host}:${device.rtspPort}/$channel"
        val track = "$base/trackID=0"

        // 1) DESCRIBE (try with auth first, then without)
        var d = sendCommand("DESCRIBE $base RTSP/1.0", mapOf("Accept" to "application/sdp"), withAuth = true)
        if (d?.line1?.startsWith("RTSP/1.0 200") != true) {
            d = sendCommand("DESCRIBE $base RTSP/1.0", mapOf("Accept" to "application/sdp"), withAuth = false)
        }
        if (d?.line1?.startsWith("RTSP/1.0 200") != true) {
            error("DESCRIBE failed: ${d?.line1 ?: "no response"}")
        }
        parseSdp(d?.body ?: "")

        // 2) SETUP trackID=0 over TCP interleaved (server rejects auth here sometimes)
        var s = sendCommand(
            "SETUP $track RTSP/1.0",
            mapOf("Transport" to "RTP/AVP/TCP;unicast;interleaved=0-1"),
            withAuth = false,
        )
        if (s?.line1?.startsWith("RTSP/1.0 200") != true) {
            s = sendCommand(
                "SETUP $track RTSP/1.0",
                mapOf("Transport" to "RTP/AVP/TCP;unicast;interleaved=0-1"),
                withAuth = true,
            )
        }
        if (s?.line1?.startsWith("RTSP/1.0 200") != true) {
            error("SETUP failed: ${s?.line1 ?: "no response"}")
        }
        sessionId = s?.headers?.get("session") ?: ""
        csdInfo?.let { onReady(it) }

        // 3) PLAY
        val p = sendCommand("PLAY $base RTSP/1.0", mapOf("Session" to sessionId), withAuth = false)
        if (p?.line1?.startsWith("RTSP/1.0 200") != true) {
            error("PLAY failed: ${p?.line1 ?: "no response"}")
        }

        readThread = thread(name = "rtsp-read") { readLoop() }
        keepAlive()
    }

    private fun keepAlive() {
        scope.launch {
            while (isActive && running) {
                delay(30_000)
                try {
                    sendCommand("OPTIONS rtsp://${device.host}:${device.rtspPort}/ RTSP/1.0", emptyMap(), false)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun readLoop() {
        val depack = H264Depacketizer.Depacketizer { nal -> onNal(nal) }
        val acc = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        try {
            while (running) {
                val n = input!!.read(buf)
                if (n < 0) break
                acc.write(buf, 0, n)
                val arr = acc.toByteArray()
                var consumed = 0
                while (consumed + 4 <= arr.size && arr[consumed].toInt() == 0x24) {
                    val ch = arr[consumed + 1].toInt()
                    val flen = ((arr[consumed + 2].toInt() and 0xff) shl 8) or
                        (arr[consumed + 3].toInt() and 0xff)
                    if (consumed + 4 + flen > arr.size) break  // partial frame, keep remainder
                    if (ch == 0) {
                        depack.pushRtp(arr.copyOfRange(consumed + 4, consumed + 4 + flen))
                    }
                    consumed += 4 + flen
                }
                if (consumed > 0) {
                    val rest = arr.size - consumed
                    acc.reset()
                    if (rest > 0) acc.write(arr, consumed, rest)
                } else if (arr.size > 4 && arr[0].toInt() != 0x24) {
                    // lost sync: scan forward to the next '$' marker
                    var next = -1
                    for (i in 1 until arr.size - 3) {
                        if (arr[i].toInt() == 0x24) { next = i; break }
                    }
                    acc.reset()
                    if (next > 0) acc.write(arr, next, arr.size - next)
                    else acc.write(arr, 0, arr.size)
                }
            }
        } catch (e: java.io.IOException) {
            if (running) onError("流已断开: ${e.message}")
        } catch (e: Exception) {
            if (running) onError("解码错误: ${e.message}")
        } finally {
            stop()
        }
    }

    private fun sendCommand(cmd: String, extra: Map<String, String>, withAuth: Boolean): Response? {
        val out = output ?: return null
        val sb = StringBuilder()
        sb.append(cmd).append("\r\n")
        sb.append("CSeq: ").append(nextCseq()).append("\r\n")
        sb.append("User-Agent: BWpix/1.0\r\n")
        if (withAuth) {
            val cred = Base64.getEncoder().encodeToString("${device.user}:${device.password}".toByteArray())
            sb.append("Authorization: Basic ").append(cred).append("\r\n")
        }
        for ((k, v) in extra) {
            sb.append(k).append(": ").append(v).append("\r\n")
        }
        sb.append("\r\n")
        synchronized(out) {
            out.write(sb.toString().toByteArray())
            out.flush()
        }
        return readResponse()
    }

    private var cseq = 0

    @Synchronized
    private fun nextCseq(): Int = ++cseq

    private fun readResponse(): Response? {
        val input = input ?: return null
        val headerBuf = ByteArrayOutputStream2()
        var c = -1
        var prev = -1
        // read until \r\n\r\n
        var state = 0
        while (state < 4) {
            c = input.read()
            if (c < 0) return null
            headerBuf.writeByte(c)
            state = when {
                state == 0 && c == '\r'.code -> 1
                state == 1 && c == '\n'.code -> 2
                state == 2 && c == '\r'.code -> 3
                state == 3 && c == '\n'.code -> 4
                else -> 0
            }
        }
        val headerText = String(headerBuf.toByteArray(), Charsets.ISO_8859_1)
        val lines = headerText.split("\r\n")
        val line1 = lines.firstOrNull() ?: ""
        val headers = LinkedHashMap<String, String>()
        for (l in lines.drop(1)) {
            val idx = l.indexOf(':')
            if (idx > 0) {
                headers[l.substring(0, idx).trim().lowercase()] = l.substring(idx + 1).trim()
            }
        }
        var body: String? = null
        val clen = headers["content-length"]?.toIntOrNull()
        if (clen != null && clen > 0 && clen < 100_000) {
            val bodyBytes = ByteArray(clen)
            var read = 0
            while (read < clen) {
                val r = input.read(bodyBytes, read, clen - read)
                if (r < 0) break
                read += r
            }
            body = String(bodyBytes, Charsets.ISO_8859_1)
        }
        return Response(line1, headers, body)
    }

    private fun parseSdp(sdp: String) {
        var width = 1280
        var height = 720
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        Regex("a=framesize:96\\s+(\\d+)-(\\d+)").find(sdp)?.let {
            width = it.groupValues[1].toInt()
            height = it.groupValues[2].toInt()
        }
        Regex("sprop-parameter-sets=([^;,\\s]+),([^;,\\s]+)").find(sdp)?.let {
            try {
                sps = Base64.getDecoder().decode(it.groupValues[1])
                pps = Base64.getDecoder().decode(it.groupValues[2])
            } catch (_: Exception) {
            }
        }
        csdInfo = StreamInfo(width, height, sps, pps)
    }

    private fun error(msg: String) {
        if (running) onError(msg)
    }

    private class Response(val line1: String, val headers: Map<String, String>, val body: String?)

    private class ByteArrayOutputStream2 {
        private var buf = ByteArray(1024)
        private var size = 0

        fun writeByte(b: Int) {
            if (size >= buf.size) buf = buf.copyOf(buf.size * 2)
            buf[size++] = b.toByte()
        }

        fun toByteArray(): ByteArray = buf.copyOf(size)
    }
}
