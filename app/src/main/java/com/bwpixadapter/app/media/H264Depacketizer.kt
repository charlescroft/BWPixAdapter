package com.bwpixadapter.app.media

/**
 * RFC 6184 H.264 RTP depacketizer: turns RTP payloads into complete NAL units
 * (single NAL, STAP-A, FU-A). Feeds the MediaCodec decoder.
 */
object H264Depacketizer {

    const val NAL_TYPE_SPS = 7
    const val NAL_TYPE_PPS = 8

    data class Nal(val data: ByteArray, val isKeyFrame: Boolean, val timestampUs: Long = 0L) {
        val type: Int get() = data[0].toInt() and 0x1f
    }

    class Depacketizer(
        private val onNal: (Nal) -> Unit,
    ) {
        private var fuStart = false
        private var fuNalType = 0
        private var fuIndicator = 0
        private var fuBuffer = ByteArray(0)
        private var fuSize = 0
        private var fuTsUs = 0L

        /** Feed one RTP packet (full packet including 12-byte RTP header). */
        fun pushRtp(rtp: ByteArray) {
            if (rtp.size <= 12) return
            val ts = rtpTimestampUs(rtp)
            val payload = rtp
            var offset = 12
            if (payload.size <= offset) return
            val nalType = payload[offset].toInt() and 0x1f

            when (nalType) {
                28 -> processFuA(payload, offset, ts)         // FU-A
                24 -> processStapA(payload, offset, ts)       // STAP-A
                in 1..23 -> {                                  // single NAL
                    onNal(Nal(payload.copyOfRange(offset, payload.size), nalType == 5, ts))
                }
            }
        }

        private fun rtpTimestampUs(rtp: ByteArray): Long {
            val t = ((rtp[4].toLong() and 0xff) shl 24) or ((rtp[5].toLong() and 0xff) shl 16) or
                ((rtp[6].toLong() and 0xff) shl 8) or (rtp[7].toLong() and 0xff)
            // H.264 RTP clock = 90 kHz
            return t * 1_000_000L / 90_000L
        }

        private fun processStapA(p: ByteArray, start: Int, ts: Long) {
            var i = start + 1
            while (i + 2 <= p.size) {
                val len = ((p[i].toInt() and 0xff) shl 8) or (p[i + 1].toInt() and 0xff)
                i += 2
                if (i + len > p.size) break
                val nal = p.copyOfRange(i, i + len)
                val t = nal[0].toInt() and 0x1f
                if (t in 1..23) onNal(Nal(nal, t == 5, ts))
                i += len
            }
        }

        private fun processFuA(p: ByteArray, start: Int, ts: Long) {
            if (p.size < start + 2) return
            val fuIndicatorIn = p[start].toInt()
            val fuHeader = p[start + 1].toInt()
            val sBit = (fuHeader and 0x80) != 0
            val eBit = (fuHeader and 0x40) != 0
            val nalType = fuHeader and 0x1f
            val dataStart = start + 2
            val dataLen = p.size - dataStart

            if (sBit) {
                fuIndicator = (fuIndicatorIn and 0xe0) or nalType
                fuNalType = nalType
                fuTsUs = ts
                ensureFu(dataLen + 2)
                fuBuffer[0] = fuIndicator.toByte()
                fuSize = 1
                System.arraycopy(p, dataStart, fuBuffer, 1, dataLen)
                fuSize += dataLen
                fuStart = true
            } else if (fuStart) {
                ensureFu(fuSize + dataLen)
                System.arraycopy(p, dataStart, fuBuffer, fuSize, dataLen)
                fuSize += dataLen
            }

            if (eBit && fuStart) {
                val nal = fuBuffer.copyOf(fuSize)
                fuStart = false
                fuSize = 0
                onNal(Nal(nal, fuNalType == 5, fuTsUs))
            }
        }

        private fun ensureFu(needed: Int) {
            if (fuBuffer.size < needed) {
                val next = maxOf(needed, fuBuffer.size * 2, 64 * 1024)
                fuBuffer = fuBuffer.copyOf(next)
            }
        }
    }
}
