package com.bwpixadapter.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class SdCardInfo(
    val status: String,
    val freeKb: Long,
    val totalKb: Long,
) {
    val freeMb: Long get() = freeKb / 1024
    val totalMb: Long get() = totalKb / 1024
    val usedMb: Long get() = (totalKb - freeKb).coerceAtLeast(0) / 1024
    val usedRatio: Float get() = if (totalKb > 0) (totalKb - freeKb).toFloat() / totalKb else 0f
}

data class TfFileItem(
    val name: String,
    val fullUrl: String,
    val relPath: String,
    val sizeKb: Long,
    val dateStr: String,
    val isDirectory: Boolean,
)

/**
 * Talks to the HiSilicon (HiIpcam) HTTP CGI + RTSP interface of a BW Pix camera.
 * All requests go straight to the camera's AP-mode IP - no cloud involved.
 */
class HiCamClient(private val device: CameraDevice) {

    private val ok = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun basicAuth() = Credentials.basic(device.user, device.password)

    /** 获取 TF 卡容量与就绪状态 */
    suspend fun getSdCardInfo(): SdCardInfo? = withContext(Dispatchers.IO) {
        try {
            val body = rawGet("/web/cgi-bin/hi3510/param.cgi?cmd=getsdcareInfo") ?: return@withContext null
            val status = Regex("""sdstatus="([^"]*)"""").find(body)?.groupValues?.get(1)?.trim() ?: "Ready"
            val free = Regex("""sdfreespace="([0-9\s]+)"""").find(body)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
            val total = Regex("""sdtotalspace="([0-9\s]+)"""").find(body)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
            SdCardInfo(status, free, total)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 直接解析 TF 卡根目录的 events.inx 二进制事件索引文件，构建 event_time -> event_index 权威映射。
     * 该索引同时覆盖抓拍照片与录像视频（文件名时间与索引时间精确一致），是删除 TF 卡文件的最可靠依据。
     * 记录格式：20 字节/条，偏移 8 处为 unix 时间戳(uint32 LE)，偏移 16 处为事件索引(uint32 LE)。
     */
    suspend fun resolveSdEventIndexMap(): Map<Int, Int> = withContext(Dispatchers.IO) {
        val map = HashMap<Int, Int>()
        try {
            val req = Request.Builder().url(device.httpBase() + "/sd/events.inx")
                .header("Authorization", basicAuth()).build()
            ok.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.size >= 16) {
                        parseSdIndex(bytes, map)
                        Log.d("SDIndex", "events.inx bytes=${bytes.size} parsed=${map.size}")
                    } else {
                        Log.w("SDIndex", "events.inx download empty, code=${resp.code}")
                    }
                } else {
                    Log.w("SDIndex", "events.inx http code=${resp.code}")
                }
            }
        } catch (e: Exception) {
            Log.w("SDIndex", "events.inx error: ${e.message}")
        }
        map
    }

    private fun parseSdIndex(bytes: ByteArray, map: MutableMap<Int, Int>) {
        val recSize = ((bytes[3].toInt() and 0xff) shl 8) or (bytes[2].toInt() and 0xff)
        if (recSize < 12) return
        var off = 16
        while (off + recSize <= bytes.size) {
            val time = le32(bytes, off + 8)
            val index = le32(bytes, off + 16)
            if (index > 0 && time > 0) {
                map[time] = index
            }
            off += recSize
        }
    }

    private fun le32(b: ByteArray, off: Int): Int {
        return (b[off].toInt() and 0xff) or
            ((b[off + 1].toInt() and 0xff) shl 8) or
            ((b[off + 2].toInt() and 0xff) shl 16) or
            ((b[off + 3].toInt() and 0xff) shl 24)
    }

    /** 格式化 TF 卡 */
    suspend fun formatSdCard(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = device.httpBase() + "/cgi-bin/hi3510/sdfrmt.cgi?&-checkname=${device.user}&-checkpasswd=${device.password}&"
            val req = Request.Builder().url(url).header("Authorization", basicAuth()).build()
            ok.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从 thttpd HTTP 目录索引中直接解析 TF 卡中的文件与子目录列表
     * 例：relPath = "Videos/" 或 "Images/20261003/"
     */
    suspend fun getDirectoryListing(relPath: String): List<TfFileItem> = withContext(Dispatchers.IO) {
        val cleanRel = relPath.trim().removePrefix("/").removePrefix("sd/").removePrefix("/")
        val targetUrl = device.httpBase() + "/sd/$cleanRel"
        val html = rawGet("/sd/$cleanRel") ?: return@withContext emptyList()

        val results = mutableListOf<TfFileItem>()
        // 匹配目录索引条目: <A HREF="/sd/..." ...>name</A>  size  date
        val itemRegex = Regex("""<A\s+HREF="([^"]+)"[^>]*>([^<]+)</A>\s*([0-9]+KB)?\s*([0-9\-:\s]+)?""", RegexOption.IGNORE_CASE)

        for (match in itemRegex.findAll(html)) {
            val href = match.groupValues[1].trim()
            val rawName = match.groupValues[2].trim()
            val sizeStr = match.groupValues[3].trim().removeSuffix("KB").trim()
            val dateStr = match.groupValues[4].trim()

            if (rawName == "Parent Folder" || href.endsWith("/..")) continue

            val isDir = href.endsWith("/") || (!rawName.contains(".") && sizeStr.isEmpty())
            val sizeKb = sizeStr.toLongOrNull() ?: 0L
            val itemUrl = if (href.startsWith("http")) href else (device.httpBase() + href)

            results.add(
                TfFileItem(
                    name = rawName,
                    fullUrl = itemUrl,
                    relPath = href.removePrefix("/sd/").removePrefix("/"),
                    sizeKb = sizeKb,
                    dateStr = dateStr,
                    isDirectory = isDir,
                )
            )
        }
        results
    }

    /** Raw text GET for diagnostics / simple CGIs. */
    suspend fun rawGet(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(device.httpBase() + path)
                .header("Authorization", basicAuth()).build()
            ok.newCall(req).execute().use { it.body?.string() }
        } catch (e: Exception) {
            null
        }
    }

    /** Grab a live JPEG via snap.cgi + the returned /tmpfs path (verified working). */
    suspend fun snapshotBytes(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(device.httpBase() + SNAP_CGI)
                .header("Authorization", basicAuth()).build()
            val body = ok.newCall(req).execute().use { it.body?.string() } ?: return@withContext null
            val m = Regex("\"([^\"]+)\"").find(body) ?: return@withContext null
            val path = m.groupValues[1]
            val img = Request.Builder().url(device.httpBase() + path)
                .header("Authorization", basicAuth()).build()
            ok.newCall(img).execute().use { it.body?.bytes() }
        } catch (e: Exception) {
            null
        }
    }

    /** Event list via the HiSilicon param.cgi; tolerant parser for the JS-var format. */
    suspend fun getEventList(index: Int, count: Int): List<EventItem> = withContext(Dispatchers.IO) {
        val body = rawGet("$PARAM_CGI?cmd=geteventlist&index=$index&count=$count") ?: return@withContext emptyList()
        parseEventList(body)
    }

    suspend fun getEventPhoto(index: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(device.httpBase() + "$PARAM_CGI?cmd=getevent&index=$index")
                .header("Authorization", basicAuth()).build()
            val body = ok.newCall(req).execute().use { it.body?.string() } ?: return@withContext null
            val m = Regex("\"([^\"]+\\.jpg)\"").find(body) ?: return@withContext null
            val img = Request.Builder().url(device.httpBase() + m.groupValues[1])
                .header("Authorization", basicAuth()).build()
            ok.newCall(img).execute().use { it.body?.bytes() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchRecord(start: Long, end: Long): List<RecordItem> = withContext(Dispatchers.IO) {
        val body = rawGet("$PARAM_CGI?cmd=searchrecord&starttime=$start&endtime=$end")
            ?: return@withContext emptyList()
        parseRecords(body)
    }

    /**
     * HiSilicon firmware answers in JavaScript variable assignments, e.g.:
     *   var event="100";var path="/tmpfs/...jpg"; ...
     * Parser accepts `var <name>="value"` lines (order tolerant).
     */
    private fun parseEventList(body: String): List<EventItem> {
        val vars = parseVars(body)
        val total = vars["event"]?.toIntOrNull() ?: 0
        val list = mutableListOf<EventItem>()
        for (i in 0 until total.coerceAtMost(100)) {
            val idx = vars["index$i"]?.toIntOrNull() ?: vars["event$i"]?.toIntOrNull() ?: i
            val type = vars["type$i"]?.toIntOrNull() ?: 0
            val time = vars["time$i"]?.toLongOrNull() ?: 0L
            val path = vars["path$i"]
            list += EventItem(idx, type, time, path)
        }
        return list
    }

    private fun parseRecords(body: String): List<RecordItem> {
        val vars = parseVars(body)
        val count = vars["recordcount"]?.toIntOrNull() ?: 0
        val list = mutableListOf<RecordItem>()
        for (i in 0 until count.coerceAtMost(100)) {
            list += RecordItem(
                name = vars["name$i"] ?: "",
                size = vars["size$i"]?.toLongOrNull() ?: 0L,
                startTime = vars["starttime$i"]?.toLongOrNull() ?: 0L,
                endTime = vars["endtime$i"]?.toLongOrNull() ?: 0L,
            )
        }
        return list
    }

    private fun parseVars(body: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        Regex("""var\s+(\w+)\s*=\s*"([^"]*)";?""", RegexOption.IGNORE_CASE)
            .findAll(body)
            .forEach { out[it.groupValues[1]] = it.groupValues[2] }
        return out
    }

    companion object {
        const val SNAP_CGI = "/web/cgi-bin/hi3510/snap.cgi"
        const val PARAM_CGI = "/web/cgi-bin/hi3510/param.cgi"
    }
}
