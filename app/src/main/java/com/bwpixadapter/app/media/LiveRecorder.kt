package com.bwpixadapter.app.media

import android.content.Context
import android.util.Log
import `object`.p2pipcam.nativecaller.NativeCaller
import `object`.p2pipcam.utils.Pub
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 实况 H264 流本地 AVI 录制器。
 *
 * 封装 libavi_utils.so 提供的原生写入接口：
 *  - NativeCaller.OpenAvi(path, "h264", width, height, fps)  打开 AVI 写入上下文
 *  - NativeCaller.WriteData(h264, len, frameType)            逐帧写入（frameType = 1 - 回调 type，与原版一致）
 *  - NativeCaller.CloseAvi()                                 关闭并最终化 AVI 索引
 *
 * 原生库内部只有单一 AVI 全局上下文，因此所有操作必须互斥。
 */
class LiveRecorder(private val context: Context) {

    private val lock = Any()
    private var active = false
    private var recordFile: File? = null

    val isRecording: Boolean
        get() = synchronized(lock) { active }

    /**
     * 开始录制：以当前视频宽高创建一个新的 AVI 文件。
     * @return 录制的文件绝对路径；失败返回 null
     */
    fun start(did: String, width: Int, height: Int, fps: Int): String? {
        synchronized(lock) {
            if (active) return recordFile?.absolutePath

            val div = Pub.recordPathFile(context)
            if (div != null && !div.exists()) {
                div.mkdirs()
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeDid = did.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(div, "${safeDid}_$stamp.avi")

            val w = if (width > 0) width else 640
            val h = if (height > 0) height else 360
            // 注意：native 签名是 OpenAvi(path, forcc, HEIGHT, WIDTH, fps)
            val ret = try {
                NativeCaller.OpenAvi(file.absolutePath, "h264", h, w, fps)
            } catch (e: Throwable) {
                Log.e("LiveRecorder", "OpenAvi failed: ${e.message}")
                -1
            }
            if (ret >= 0) {
                active = true
                recordFile = file
                Log.i("LiveRecorder", "started: ${file.absolutePath} ${w}x$h @${fps}fps (ret=$ret)")
                return file.absolutePath
            }
            Log.e("LiveRecorder", "OpenAvi returned $ret")
            return null
        }
    }

    /**
     * 写入一帧 H264 数据。frameType 映射与原版 PlayActivity 一致（1 - 回调 type）。
     * 仅在实际录制状态下写入，否则丢弃。
     */
    fun writeFrame(h264: ByteArray, len: Int, type: Int) {
        synchronized(lock) {
            if (!active || len <= 0 || h264 == null) return
            try {
                NativeCaller.WriteData(h264, len, 1 - type)
            } catch (e: Throwable) {
                Log.w("LiveRecorder", "WriteData failed: ${e.message}")
            }
        }
    }

    /**
     * 结束录制并最终化 AVI。
     * @return 已保存的文件路径；未处于录制状态返回 null
     */
    fun stop(): String? {
        synchronized(lock) {
            if (!active) return null
            active = false
            try {
                NativeCaller.CloseAvi()
            } catch (e: Throwable) {
                Log.w("LiveRecorder", "CloseAvi failed: ${e.message}")
            }
            val path = recordFile?.absolutePath
            recordFile = null
            Log.i("LiveRecorder", "stopped, saved: $path")
            return path
        }
    }
}
