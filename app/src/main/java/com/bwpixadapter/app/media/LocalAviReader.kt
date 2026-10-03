package com.bwpixadapter.app.media

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

/**
 * 轻量级 AVI(H.264) 容器读取器：直接按 RIFF 分块解析 movi 中的视频帧（Annex-B）。
 * 比原生 Avi.readFrame 快得多，可支持实时帧率播放与关键帧定位。
 */
class LocalAviReader(private val file: File) {

    var microSecPerFrame: Long = 40000L
        private set
    var totalFrames: Int = 0
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    private var stream: BufferedInputStream? = null
    private var pos = 0L
    private var moviDataStart = 0L
    private var eof = false

    val fps: Int
        get() = if (microSecPerFrame > 0) (1_000_000L / microSecPerFrame).toInt().coerceAtLeast(1) else 25

    fun open(): Boolean {
        return try {
            val s = BufferedInputStream(FileInputStream(file), 256 * 1024)
            stream = s
            pos = 0
            if (!parseHeader(s)) {
                s.close()
                return false
            }
            seekToFrame(0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        try { stream?.close() } catch (_: Exception) {}
        stream = null
    }

    /** 定位到指定视频帧号（重新打开文件并跳到目标帧） */
    fun seekToFrame(target: Int) {
        try { stream?.close() } catch (_: Exception) {}
        val ns = BufferedInputStream(FileInputStream(file), 256 * 1024)
        stream = ns
        pos = 0
        eof = false
        moviDataStart = 0
        if (!parseHeader(ns)) return
        var skipped = 0
        while (skipped < target) {
            val f = readNextChunk()
            if (f == null) break
            if (f.type == AviChunkType.VIDEO) skipped++
        }
    }

    /** 读取下一音/视频块（Annex-B H264 访问单元 或 PCM 音频）；结尾返回 null */
    fun readNextChunk(): AviChunk? {
        if (eof) return null
        while (true) {
            val tag = readBytes(4) ?: run { eof = true; return null }
            val sizeB = readBytes(4) ?: run { eof = true; return null }
            val size = u32(sizeB, 0)
            val tagStr = String(tag, Charsets.US_ASCII)
            when (tagStr) {
                "idx1" -> { eof = true; return null }
                "LIST" -> {
                    val sub = readBytes(4) ?: run { eof = true; return null }
                    val subStr = String(sub, Charsets.US_ASCII)
                    if (subStr == "movi") continue
                    else skip(size - 4 + (size and 1L))
                }
                "00dc", "00db", "00DC", "00DB" -> {
                    val data = readBytes(size.toInt()) ?: run { eof = true; return null }
                    if (size and 1L == 1L) skip(1)
                    return AviChunk(AviChunkType.VIDEO, data)
                }
                "01wb", "01wv", "00wb", "00wv", "01WB", "01WV" -> {
                    val data = readBytes(size.toInt()) ?: run { eof = true; return null }
                    if (size and 1L == 1L) skip(1)
                    return AviChunk(AviChunkType.AUDIO, data)
                }
                else -> skip(size + (size and 1L))
            }
        }
    }

    enum class AviChunkType { VIDEO, AUDIO }

    data class AviChunk(val type: AviChunkType, val data: ByteArray)

    private fun parseHeader(s: BufferedInputStream): Boolean {
        val head = readBytes(12)
        if (head == null || head.size < 12 || String(head, 0, 4, Charsets.US_ASCII) != "RIFF") return false
        while (true) {
            val tag = readBytes(4) ?: break
            val sizeB = readBytes(4) ?: break
            val size = u32(sizeB, 0)
            val tagStr = String(tag, Charsets.US_ASCII)
            if (tagStr == "LIST") {
                val sub = readBytes(4) ?: break
                val subStr = String(sub, Charsets.US_ASCII)
                if (subStr == "hdrl") {
                    scanHdrl(size - 4)
                } else if (subStr == "movi") {
                    moviDataStart = pos
                    return true
                } else {
                    skip(size - 4 + (size and 1L))
                }
            } else if (tagStr == "movi") {
                moviDataStart = pos
                return true
            } else {
                skip(size + (size and 1L))
            }
            if (pos > file.length()) break
        }
        return moviDataStart > 0
    }

    private fun scanHdrl(bodySize: Long) {
        var remaining = bodySize
        while (remaining > 0) {
            val tag = readBytes(4) ?: break
            val sizeB = readBytes(4) ?: break
            val size = u32(sizeB, 0)
            val tagStr = String(tag, Charsets.US_ASCII)
            if (tagStr == "avih") {
                val d = readBytes(size.toInt())
                if (d != null && d.size >= 40) {
                    microSecPerFrame = u32(d, 0)
                    totalFrames = u32(d, 16).toInt()
                    width = u32(d, 32).toInt()
                    height = u32(d, 36).toInt()
                }
                remaining -= 8 + size + (size and 1L)
            } else {
                skip(size + (size and 1L))
                remaining -= 8 + size + (size and 1L)
            }
        }
    }

    private fun readBytes(n: Int): ByteArray? {
        val s = stream ?: return null
        val b = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = s.read(b, off, n - off)
            if (r < 0) break
            off += r
        }
        pos += off
        return if (off == 0) null else b
    }

    private fun skip(n: Long) {
        val s = stream ?: return
        var remaining = n
        while (remaining > 0) {
            val r = s.skip(remaining)
            if (r <= 0) break
            remaining -= r
            pos += r
        }
    }

    private fun u32(b: ByteArray, off: Int): Long {
        return (b[off].toLong() and 0xff) or
            ((b[off + 1].toLong() and 0xff) shl 8) or
            ((b[off + 2].toLong() and 0xff) shl 16) or
            ((b[off + 3].toLong() and 0xff) shl 24)
    }
}
