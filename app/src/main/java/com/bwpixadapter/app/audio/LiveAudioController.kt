package com.bwpixadapter.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import `object`.p2pipcam.nativecaller.NativeCaller
import java.util.concurrent.atomic.AtomicBoolean

class LiveAudioController(
    private val did: String,
    private val context: Context,
) {
    private val sampleRate = 8000
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // 播放监听相关
    private var audioTrack: AudioTrack? = null
    private val isAudioListening = AtomicBoolean(false)

    // 喊话录音相关
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private val isTalking = AtomicBoolean(false)

    /**
     * 开启/关闭音频监听 (从摄像头喇叭听取现场声音)
     */
    @Synchronized
    fun setListening(enable: Boolean) {
        if (enable) {
            if (isAudioListening.get()) return
            try {
                val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelOut, audioFormat)
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelOut,
                    audioFormat,
                    maxOf(minBufSize * 2, 4096),
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()
                isAudioListening.set(true)
                NativeCaller.PPPPStartAudio(did)
                Log.i("AudioController", "Audio listening started for $did")
            } catch (e: Exception) {
                Log.e("AudioController", "Failed to start audio listening: ${e.message}", e)
            }
        } else {
            if (!isAudioListening.get()) return
            isAudioListening.set(false)
            try {
                NativeCaller.PPPPStopAudio(did)
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                Log.i("AudioController", "Audio listening stopped for $did")
            } catch (e: Exception) {
                Log.e("AudioController", "Failed to stop audio listening: ${e.message}", e)
            }
        }
    }

    /**
     * 处理摄像头下发的 PCM 音频数据
     */
    fun onAudioDataReceived(pcm: ByteArray, len: Int) {
        if (!isAudioListening.get() || audioTrack == null || len <= 0) return
        try {
            audioTrack?.write(pcm, 0, len)
        } catch (e: Exception) {
            Log.e("AudioController", "Error writing PCM to AudioTrack: ${e.message}")
        }
    }

    /**
     * 开启/关闭喊话对讲 (从手机麦克风采集声音发送给摄像头)
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun setTalking(enable: Boolean) {
        if (enable) {
            if (isTalking.get()) return
            isTalking.set(true)
            NativeCaller.PPPPStartTalk(did)

            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelIn, audioFormat)
            val bufferSize = maxOf(minBufSize, 1024)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelIn,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioController", "AudioRecord initialization failed")
                    isTalking.set(false)
                    NativeCaller.PPPPStopTalk(did)
                    return
                }

                audioRecord?.startRecording()
                recordThread = Thread {
                    val buffer = ByteArray(bufferSize)
                    while (isTalking.get()) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            NativeCaller.PPPPTalkAudioData(did, buffer, read)
                        }
                    }
                }.apply {
                    priority = Thread.MAX_PRIORITY
                    start()
                }
                Log.i("AudioController", "Talk intercom started for $did")
            } catch (e: Exception) {
                Log.e("AudioController", "Failed to start talk: ${e.message}", e)
                isTalking.set(false)
                NativeCaller.PPPPStopTalk(did)
            }
        } else {
            if (!isTalking.get()) return
            isTalking.set(false)
            try {
                recordThread?.join(500)
            } catch (_: Exception) {}
            recordThread = null

            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                NativeCaller.PPPPStopTalk(did)
                Log.i("AudioController", "Talk intercom stopped for $did")
            } catch (e: Exception) {
                Log.e("AudioController", "Failed to stop talk: ${e.message}", e)
            }
        }
    }

    fun isListening(): Boolean = isAudioListening.get()
    fun isTalking(): Boolean = isTalking.get()

    /**
     * 页面退出时释放全部音频资源
     */
    @Synchronized
    fun release() {
        setTalking(false)
        setListening(false)
    }
}
