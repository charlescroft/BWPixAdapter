package com.bwpixadapter.app.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bwpixadapter.app.data.CameraDevice
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Custom H.264 player: RtspSession (TCP interleaved RTSP) + MediaCodec hardware
 * decode rendered onto a SurfaceView. Bypasses ExoPlayer which can't negotiate
 * this camera's finicky RTSP transport.
 */
class RtspPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs) {

    private val TAG = "RtspPlayerView"
    private var session: RtspSession? = null
    private var codec: MediaCodec? = null
    private var surfaceReady = false
    private var codecReady = false
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private var width = 1280
    private var height = 720
    private val pending = LinkedBlockingQueue<H264Depacketizer.Nal>(256)
    private var pumpThread: Thread? = null
    private var running = false
    private var device: CameraDevice? = null

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

    fun start(device: CameraDevice, channel: String = "11") {
        this.device = device
        session?.stop()
        session = null
        releaseCodec()
        running = true
        pending.clear()
        sps = null
        pps = null
        codecReady = false

        val s = RtspSession(device, channel)
        s.onError = { msg -> post { onError?.invoke(msg) } }
        s.onNal = { nal -> handleNal(nal) }
        s.onReady = { info ->
            width = info.width
            height = info.height
            if (info.sps != null && info.pps != null) {
                sps = info.sps
                pps = info.pps
                post { maybeConfigure() }
            }
        }
        session = s
        s.start()
    }

    fun stop() {
        running = false
        session?.stop()
        session = null
        releaseCodec()
        pending.clear()
    }

    private fun handleNal(nal: H264Depacketizer.Nal) {
        if (!codecReady) {
            when (nal.type) {
                H264Depacketizer.NAL_TYPE_SPS -> { sps = nal.data; return }
                H264Depacketizer.NAL_TYPE_PPS -> { pps = nal.data; return }
            }
        }
        if (codecReady) {
            pending.offer(nal)
        } else {
            maybeConfigure()
        }
    }

    @Synchronized
    private fun maybeConfigure() {
        if (!running || !surfaceReady || codecReady) return
        val sps = sps ?: return
        val pps = pps ?: return
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
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
        pumpThread = thread(name = "rtsp-decode") {
            val mc = codec ?: return@thread
            val info = MediaCodec.BufferInfo()
            var fallbackPts = 0L
            while (running && codecReady) {
                // feed
                val nal = pending.poll(200, TimeUnit.MILLISECONDS)
                if (nal != null) {
                    try {
                        val inIdx = mc.dequeueInputBuffer(20_000)
                        if (inIdx >= 0) {
                            val buf = mc.getInputBuffer(inIdx) ?: continue
                            buf.clear()
                            buf.put(nal.data)
                            val flags = if (nal.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            // use RTP timestamp; fall back to monotonic 33ms cadence
                            val pts = if (nal.timestampUs > 0) nal.timestampUs
                            else { fallbackPts += 33_000; fallbackPts }
                            mc.queueInputBuffer(inIdx, 0, nal.data.size, pts, flags)
                        }
                    } catch (e: IllegalStateException) {
                        break
                    }
                }
                // drain
                try {
                    while (running && codecReady) {
                        val outIdx = mc.dequeueOutputBuffer(info, 0)
                        if (outIdx >= 0) {
                            mc.releaseOutputBuffer(outIdx, true)
                        } else {
                            break
                        }
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
}
