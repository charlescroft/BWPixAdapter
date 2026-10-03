package com.bwpixadapter.app.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * MediaCodec hardware decoder that renders raw H.264 NAL units (Annex-B) onto a
 * SurfaceView. Fed from the PPPP native callback (CallBack_H264Data).
 */
class H264DecoderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs) {

    private var codec: MediaCodec? = null
    private var surfaceReady = false
    private var codecReady = false
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private val pending = LinkedBlockingQueue<Frame>(256)
    private var pumpThread: Thread? = null
    private var running = false
    private var nFrames = 0
    private var lastLog = 0L

    data class Frame(val data: ByteArray, val isKey: Boolean)

    var onState: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
                maybeConfigure()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
                releaseCodec()
            }
        })
    }

    fun start() {
        running = true
        pending.clear()
        sps = null
        pps = null
        codecReady = false
    }

    fun stop() {
        running = false
        releaseCodec()
        pending.clear()
    }

    /**
     * Feed one access unit. [data] is an H.264 frame (Annex-B start codes).
     * [isKey] true for IDR frames.
     */
    fun feed(data: ByteArray, isKey: Boolean) {
        if (!running) return
        nFrames++
        val now = System.currentTimeMillis()
        if (now - lastLog > 5000) {
            lastLog = now
            Log.i("H264DecoderView", "feed nFrames=$nFrames codecReady=$codecReady")
            onState?.invoke("已连接 (${nFrames}帧)")
        }
        if (!codecReady) {
            // harvest SPS/PPS from the stream before configuring the decoder
            val pair = findSpsPps(data)
            if (pair.first != null) sps = pair.first
            if (pair.second != null) pps = pair.second
            maybeConfigure()
            if (!codecReady) return
        }
        pending.offer(Frame(data, isKey))
    }

    @Synchronized
    private fun maybeConfigure() {
        if (!running || !surfaceReady || codecReady) return
        val sps = sps ?: return
        val pps = pps ?: return
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720)
            format.setByteBuffer("csd-0", ByteBuffer.wrap(sps))
            format.setByteBuffer("csd-1", ByteBuffer.wrap(pps))
            val mc = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mc.configure(format, holder.surface, null, 0)
            mc.start()
            codec = mc
            codecReady = true
            onState?.invoke("已连接")
            startPump()
        } catch (e: Exception) {
            onError?.invoke("解码器初始化失败: ${e.message}")
        }
    }

    private fun startPump() {
        pumpThread = thread(name = "pppp-decode") {
            val mc = codec ?: return@thread
            val info = MediaCodec.BufferInfo()
            var pts = 0L
            while (running && codecReady) {
                val frame = pending.poll(200, TimeUnit.MILLISECONDS)
                if (frame != null) {
                    try {
                        val inIdx = mc.dequeueInputBuffer(20_000)
                        if (inIdx >= 0) {
                            val buf = mc.getInputBuffer(inIdx) ?: continue
                            buf.clear()
                            buf.put(frame.data)
                            val flags = if (frame.isKey) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            pts += 33_000
                            mc.queueInputBuffer(inIdx, 0, frame.data.size, pts, flags)
                        }
                    } catch (e: IllegalStateException) {
                        break
                    }
                }
                try {
                    while (running && codecReady) {
                        val outIdx = mc.dequeueOutputBuffer(info, 0)
                        if (outIdx >= 0) mc.releaseOutputBuffer(outIdx, true)
                        else break
                    }
                } catch (e: IllegalStateException) {
                    break
                }
            }
        }
    }

    private fun releaseCodec() {
        codecReady = false
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Exception) {
        }
        codec = null
    }

    /** Find SPS/PPS NAL units inside an Annex-B access unit. */
    private fun findSpsPps(data: ByteArray): Pair<ByteArray?, ByteArray?> {
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        var i = 0
        val n = data.size
        while (i + 3 < n) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) {
                var end = i + 4
                while (end + 3 < n) {
                    if (data[end] == 0.toByte() && data[end + 1] == 0.toByte() && data[end + 2] == 0.toByte() && data[end + 3] == 1.toByte()) break
                    if (data[end] == 0.toByte() && data[end + 1] == 0.toByte() && data[end + 2] == 1.toByte()) break
                    end++
                }
                if (end + 4 <= n) {
                    val nalType = data[i + 4].toInt() and 0x1f
                    val nal = data.copyOfRange(i, end)
                    when (nalType) {
                        7 -> sps = nal
                        8 -> pps = nal
                    }
                }
                i = end
            } else {
                i++
            }
        }
        return sps to pps
    }

    companion object {
        private val H264_START = byteArrayOf(0, 0, 0, 1)

        fun normalizeH264(data: ByteArray): ByteArray {
            if (data.size >= 4 && data[0] == 0.toByte() && data[1] == 0.toByte() &&
                data[2] == 0.toByte() && data[3] == 1.toByte()
            ) return data
            if (data.size >= 3 && data[0] == 0.toByte() && data[1] == 0.toByte() && data[2] == 1.toByte()) {
                val out = ByteArray(data.size + 1)
                out[0] = 0
                System.arraycopy(data, 0, out, 1, data.size)
                return out
            }
            val out = ByteArray(data.size + H264_START.size)
            System.arraycopy(H264_START, 0, out, 0, H264_START.size)
            System.arraycopy(data, 0, out, H264_START.size, data.size)
            return out
        }
    }
}
