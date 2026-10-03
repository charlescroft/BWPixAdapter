package com.bwpixadapter.app.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.data.HiCamClient
import com.bwpixadapter.app.data.SdCardInfo
import com.bwpixadapter.app.data.TfFileItem
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import com.easyview.table.EventTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import `object`.p2pipcam.utils.Pub

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TfCardExplorerScreen(
    deviceId: String,
    store: DeviceStore,
    onBack: () -> Unit,
    onPlayVideo: (String, String, String) -> Unit,
) {
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember(device) { HiCamClient(device) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 录像视频, 1: 抓拍照片, 2: 存储卡管理
    var sdInfo by remember { mutableStateOf<SdCardInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val videoList = remember { mutableStateListOf<TfFileItem>() }
    val photoList = remember { mutableStateListOf<TfFileItem>() }

    var selectedPreviewPhoto by remember { mutableStateOf<TfFileItem?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDownloadingPhoto by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }

    // 多选删除状态：照片与视频共用
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<TfFileItem>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 正在下载的录像文件名（同一时刻仅允许一个下载，防重复点击）
    var downloadingName by remember { mutableStateOf<String?>(null) }

    // 已下载到本地的录像文件名集合
    val downloadedNames = remember { mutableStateListOf<String>() }

    fun refreshDownloaded() {
        val names = mutableSetOf<String>()
        val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (privateDir != null && privateDir.exists()) {
            privateDir.listFiles()?.forEach { names.add(it.name) }
        }
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "BWPixAdapter")
        if (publicDir.exists()) {
            publicDir.listFiles()?.forEach { names.add(it.name) }
        }
        downloadedNames.clear()
        downloadedNames.addAll(names)
    }

    fun isDownloaded(item: TfFileItem): Boolean {
        val base = item.name.removeSuffix(".avi")
        return downloadedNames.any {
            it == item.name || it.removeSuffix(".avi") == base || it.removeSuffix(".avi").startsWith("$base ")
        }
    }

    fun localFileFor(item: TfFileItem): File? {
        val private = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), item.name)
        if (private.exists()) return private
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "BWPixAdapter")
        if (publicDir.exists()) {
            val base = item.name.removeSuffix(".avi")
            publicDir.listFiles()?.firstOrNull {
                it.name.removeSuffix(".avi").startsWith(base) || it.name == item.name
            }?.let { return it }
        }
        return null
    }

    val app = context.applicationContext as CameraApplication
    val cam = app.cameraList.getCamera(device.uid.ifBlank { device.id }) as? PPCSCamera

    fun refreshAll() {
        scope.launch {
            isLoading = true
            refreshDownloaded()
            sdInfo = client.getSdCardInfo()

            // 1. 递归检索最近所有录像文件
            val vFiles = mutableListOf<TfFileItem>()
            val videoDateDirs = client.getDirectoryListing("Videos/")
            for (dir in videoDateDirs.filter { it.isDirectory }) {
                val files = client.getDirectoryListing(dir.relPath)
                vFiles.addAll(files.filter { it.name.endsWith(".avi", ignoreCase = true) })
            }
            videoList.clear()
            videoList.addAll(vFiles.sortedByDescending { it.name })

            // 2. 递归检索抓拍图片
            val pFiles = mutableListOf<TfFileItem>()
            val photoDateDirs = client.getDirectoryListing("Images/")
            for (dir in photoDateDirs.filter { it.isDirectory }) {
                val files = client.getDirectoryListing(dir.relPath)
                pFiles.addAll(files.filter { it.name.endsWith(".jpg", ignoreCase = true) })
            }
            photoList.clear()
            photoList.addAll(pFiles.sortedByDescending { it.name })

            isLoading = false
        }
    }

    // 从 TF 卡文件相对路径解析出事件时间戳（unix 秒），用于匹配本地事件索引
    fun fileToEventTime(item: TfFileItem): Int? {
        return try {
            val m = Regex("""(\d{8})/(\d{6})\.\w+$""").find(item.relPath) ?: return null
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            val date = sdf.parse(m.groupValues[1] + m.groupValues[2]) ?: return null
            (date.time / 1000L).toInt()
        } catch (_: Exception) {
            null
        }
    }

    // 通过 PPPP 协议拉取摄像头当前完整事件列表（覆盖照片与录像所有事件类型），返回 event_time -> event.index 映射
    suspend fun resolveCameraTimeToIndex(): Map<Int, Int> {
        val c = cam ?: return emptyMap()
        if (!c.isOnline) return emptyMap()
        return try {
            c.ClearEvents()
            val total = suspendCancellableCoroutine<Int> { cont ->
                c.queryEventList(0, 9999, object : com.easyview.basecamera.ICamera.IRespondListener {
                    override fun OnRespondResult(cc: com.easyview.basecamera.ICamera, cmd: Int, result: Int) {
                        if (cont.isActive) cont.resume(result) {}
                    }
                })
                cont.invokeOnCancellation { }
            }
            if (total <= 0) return emptyMap()
            val map = HashMap<Int, Int>()
            for (e in c.GetEvents()) {
                if (e.index > 0) map[e.event_time] = e.index
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // 通过本地事件表把文件时间戳映射为 PPPP 事件索引，再走 DEL_EVENTS 协议从 TF 卡删除对应事件文件
    fun deleteSelectedFiles() {
        val toDelete = selectedItems.toList()
        if (toDelete.isEmpty()) return

        val didStr = device.uid.ifBlank { device.id }
        val eventTable = EventTable.getInstance(context)

        val targetTimes = toDelete.mapNotNull { fileToEventTime(it) }.toSet()

        // 优先使用本地事件表映射；未匹配部分再通过 PPPP 查询摄像头权威事件列表（覆盖录像事件 513/514）
        val localTimeToIndex = (EventTable.getAllEvent(context, didStr) ?: emptyList())
            .associate { it.eventTime to it.eventIndex }

        val matchedIndices = LinkedHashSet<Int>()
        scope.launch {
            // 权威映射：直接从 TF 卡 events.inx 索引解析（覆盖照片与录像），其次是 PPPP 事件列表，最后是本地库
            val sdIndexMap = client.resolveSdEventIndexMap()
            val cameraMap = if (sdIndexMap.isEmpty()) resolveCameraTimeToIndex() else emptyMap()

            // 采用 ±300 秒容差的最近时间匹配：录像文件名是分段封盘时刻，事件索引时间可能是录像起始时刻
            fun fuzzyLookup(target: Int, map: Map<Int, Int>): Int? {
                val exact = map[target]
                if (exact != null) return exact
                var best: Pair<Int, Int>? = null
                for ((t, idx) in map) {
                    val diff = Math.abs(t - target)
                    if (diff <= 300) {
                        if (best == null || diff < best.second) {
                            best = t to idx
                        }
                    }
                }
                return best?.second
            }

            var resolved = 0
            for (item in toDelete) {
                val ts = fileToEventTime(item)
                if (ts != null) {
                    val idx = fuzzyLookup(ts, sdIndexMap)
                        ?: fuzzyLookup(ts, cameraMap)
                        ?: localTimeToIndex[ts]
                    if (idx != null && idx > 0) {
                        matchedIndices.add(idx)
                        resolved++
                    }
                    // 同步清理本地数据库记录
                    try {
                        eventTable.delEvent(didStr, 1, ts)
                    } catch (_: Exception) {}
                }
            }

            // 通过 PPPP 协议通知摄像头删除 TF 卡上的对应事件文件
            android.util.Log.i("TFDelete", "matched indices=${matchedIndices.toList()} cam=${cam != null} online=${cam?.isOnline}")
            if (matchedIndices.isNotEmpty() && cam != null && cam.isOnline) {
                cam.delEvents(matchedIndices.toIntArray(), null)
                android.util.Log.i("TFDelete", "delEvents sent: ${matchedIndices.toList()}")
            }

            // 本地已缓存的照片文件同步删除
            toDelete.forEach { item ->
                try {
                    val base = context.getExternalFilesDir("Photos")
                    if (base != null) {
                        val cached = File(base, item.name)
                        if (cached.exists()) cached.delete()
                    }
                } catch (_: Exception) {}
            }

            isSelectMode = false
            selectedItems.clear()
            val msg = if (resolved > 0) "已删除 ${toDelete.size} 项（本地记录已清理）"
            else "已删除本地 ${toDelete.size} 项（TF 卡事件未匹配）"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            refreshAll()
        }
    }

    fun toggleSelect(item: TfFileItem) {
        val existing = selectedItems.indexOfFirst { it.relPath == item.relPath }
        if (existing >= 0) selectedItems.removeAt(existing)
        else selectedItems.add(item)
    }

    BackHandler(enabled = isSelectMode) {
        isSelectMode = false
        selectedItems.clear()
    }

    LaunchedEffect(deviceId) {
        refreshAll()
    }

    fun downloadVideoFile(item: TfFileItem) {
        // 同一时刻只允许一个下载，避免重复点击造成并发拉流
        if (downloadingName != null) {
            Toast.makeText(context, "已有文件正在下载，请稍候…", Toast.LENGTH_SHORT).show()
            return
        }
        downloadingName = item.name
        Toast.makeText(context, "开始下载 ${item.name}…", Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            try {
                val ok = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()
                val req = Request.Builder().url(item.fullUrl).header("Authorization", client.basicAuth()).build()
                val resp = ok.newCall(req).execute()
                if (resp.isSuccessful) {
                    // 1. 先下载到应用私有目录（保证可写）
                    val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: context.filesDir
                    if (!privateDir.exists()) privateDir.mkdirs()
                    val tmpFile = File(privateDir, item.name)
                    val out = FileOutputStream(tmpFile)
                    resp.body?.byteStream()?.copyTo(out)
                    out.close()

                    // 2. 通过 MediaStore 导出到公共 Movies/BWPixAdapter（兼容分区存储）
                    val exported = `object`.p2pipcam.utils.Pub.saveVideoToMovies(context, tmpFile)

                    withContext(Dispatchers.Main) {
                        if (exported) {
                            Toast.makeText(context, "录像已保存至 Movies/BWPixAdapter/${item.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "已下载至应用私有目录 ${tmpFile.absolutePath}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "下载失败 (HTTP ${resp.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    downloadingName = null
                    refreshDownloaded()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectMode) "已选择 ${selectedItems.size} 项" else "TF卡文件浏览")
                },
                navigationIcon = {
                    if (isSelectMode) {
                        IconButton(onClick = {
                            isSelectMode = false
                            selectedItems.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (isSelectMode) {
                        val currentList = when (selectedTab) {
                            0 -> videoList
                            1 -> photoList
                            else -> emptyList()
                        }
                        val allSelected = currentList.isNotEmpty() && currentList.all { cur ->
                            selectedItems.any { it.relPath == cur.relPath }
                        }
                        IconButton(onClick = {
                            if (allSelected) {
                                selectedItems.clear()
                            } else {
                                currentList.forEach { item ->
                                    if (selectedItems.none { it.relPath == item.relPath }) {
                                        selectedItems.add(item)
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "取消全选" else "全选",
                            )
                        }
                        IconButton(
                            onClick = { if (selectedItems.isNotEmpty()) showDeleteConfirm = true },
                            enabled = selectedItems.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除选中",
                                tint = if (selectedItems.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    } else {
                        IconButton(onClick = { refreshAll() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad),
        ) {
            // 顶部容量指示看板
            sdInfo?.let { info ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SdCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "TF 卡状态: ${if (info.status.equals("Ready", true)) "正常" else "异常"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (info.status.equals("Ready", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(
                                "可用 ${(info.freeMb / 1024f).let { "%.1f".format(it) }} GB / 共 ${(info.totalMb / 1024f).let { "%.1f".format(it) }} GB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { info.usedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }

            // 分栏标签
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("录像视频 (${videoList.size})") },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("抓拍照片 (${photoList.size})") },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("卡管理") },
                    icon = { Icon(Icons.Default.SdCard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("正在读取 TF 卡文件系统…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // 1. 录像视频列表
                        if (videoList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("未检索到录像文件，或未检测到 TF 卡", color = MaterialTheme.colorScheme.secondary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(videoList, key = { it.relPath }) { item ->
                                    val isSelected = selectedItems.any { it.relPath == item.relPath }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (isSelectMode) toggleSelect(item)
                                                },
                                                onLongClick = {
                                                    if (!isSelectMode) {
                                                        isSelectMode = true
                                                        toggleSelect(item)
                                                    }
                                                },
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (isSelectMode) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { toggleSelect(item) },
                                                    modifier = Modifier.padding(end = 6.dp),
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "大小: ${(item.sizeKb / 1024f).let { "%.1f".format(it) }} MB · ${item.dateStr}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                            }
                                            if (!isSelectMode) {
                                                val isDownloadingThis = downloadingName == item.name
                                                val isLocallyDownloaded = isDownloaded(item)
                                                if (isLocallyDownloaded) {
                                                    // 已下载：显示 ✅，不再提供下载按钮
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "已下载",
                                                        tint = Color(0xFF2E7D32),
                                                        modifier = Modifier.padding(horizontal = 12.dp).size(22.dp),
                                                    )
                                                } else if (isDownloadingThis) {
                                                    // 下载中：显示旋转进度，禁用该按钮，避免重复点击
                                                    CircularProgressIndicator(
                                                        modifier = Modifier
                                                            .padding(horizontal = 10.dp)
                                                            .size(22.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                } else {
                                                    IconButton(
                                                        onClick = { downloadVideoFile(item) },
                                                        enabled = downloadingName == null,
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Download,
                                                            contentDescription = "下载到手机",
                                                            tint = if (downloadingName == null) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        )
                                                    }
                                                }
                                                FilledTonalButton(onClick = {
                                                    val local = localFileFor(item)
                                                    if (local != null) {
                                                        onPlayVideo(item.name, local.absolutePath, item.dateStr)
                                                    } else {
                                                        onPlayVideo(item.name, "", item.dateStr)
                                                    }
                                                }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("回放")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 2. 抓拍照片九宫格网格
                        if (photoList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("未检测到抓拍照片", color = MaterialTheme.colorScheme.secondary)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(photoList, key = { it.relPath }) { item ->
                                    val isSelected = selectedItems.any { it.relPath == item.relPath }
                                    TfPhotoThumbnail(
                                        item = item,
                                        client = client,
                                        isSelectMode = isSelectMode,
                                        isSelected = isSelected,
                                        onToggleSelect = { toggleSelect(item) },
                                        onLongClick = {
                                            if (!isSelectMode) {
                                                isSelectMode = true
                                                toggleSelect(item)
                                            }
                                        },
                                        onClick = {
                                            if (isSelectMode) toggleSelect(item)
                                            else selectedPreviewPhoto = item
                                        },
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // 3. 存储卡管理面板
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("存储卡状态", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "卡状态: ${if (sdInfo?.status.equals("Ready", true)) "正常" else "异常"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (sdInfo?.status.equals("Ready", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                    Text("总容量: ${((sdInfo?.totalMb ?: 0) / 1024f).let { "%.2f".format(it) }} GB", style = MaterialTheme.typography.bodyMedium)
                                    Text("已使用: ${((sdInfo?.usedMb ?: 0) / 1024f).let { "%.2f".format(it) }} GB", style = MaterialTheme.typography.bodyMedium)
                                    Text("剩余空间: ${((sdInfo?.freeMb ?: 0) / 1024f).let { "%.2f".format(it) }} GB", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("格式化存储卡", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                    Text("清空 TF 卡中的所有抓拍照片与视频录像，重新初始化文件系统。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = { showFormatDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("格式化 TF 卡")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 多选删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除选中的 ${selectedItems.size} 项？") },
            text = {
                Text(
                    "将发送删除指令并清理手机本地记录。\n" +
                    "注意：部分固件版本对删除指令仅回复 ACK 而不物理移除 TF 卡文件；若删除后文件仍在，可到「卡管理」中格式化存储卡彻底清理。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        deleteSelectedFiles()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }

    // 格式化确认弹窗
    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("确认格式化 TF 卡？") },
            text = { Text("格式化将永久清除卡内所有录像与抓拍数据，操作不可逆。确认继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        showFormatDialog = false
                        scope.launch {
                            val ok = client.formatSdCard()
                            if (ok) {
                                Toast.makeText(context, "已发送格式化指令，TF 卡正在初始化…", Toast.LENGTH_LONG).show()
                                refreshAll()
                            } else {
                                Toast.makeText(context, "格式化指令发送失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("确认格式化")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFormatDialog = false }) { Text("取消") }
            },
        )
    }

    // 单张抓拍预览弹窗
    if (selectedPreviewPhoto != null) {
        val item = selectedPreviewPhoto!!
        val bmp by produceState<Bitmap?>(initialValue = null, key1 = item.fullUrl) {
            value = withContext(Dispatchers.IO) {
                try {
                    val ok = OkHttpClient.Builder().connectTimeout(6, TimeUnit.SECONDS).build()
                    val req = Request.Builder().url(item.fullUrl).header("Authorization", client.basicAuth()).build()
                    ok.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedPreviewPhoto = null },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val ok = OkHttpClient.Builder().build()
                                val req = Request.Builder().url(item.fullUrl).header("Authorization", client.basicAuth()).build()
                                val bytes = ok.newCall(req).execute().use { it.body?.bytes() }
                                if (bytes != null) {
                                    val picDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BWPixAdapter")
                                    if (!picDir.exists()) picDir.mkdirs()
                                    val f = File(picDir, item.name)
                                    f.writeBytes(bytes)
                                    android.media.MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), arrayOf("image/jpeg"), null)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "已保存到系统相册", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedPreviewPhoto = null }, shape = RoundedCornerShape(8.dp)) {
                    Text("关闭")
                }
            },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TfPhotoThumbnail(
    item: TfFileItem,
    client: HiCamClient,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val bmp by produceState<Bitmap?>(initialValue = null, key1 = item.fullUrl) {
        value = withContext(Dispatchers.IO) {
            try {
                val ok = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).build()
                val req = Request.Builder().url(item.fullUrl).header("Authorization", client.basicAuth()).build()
                ok.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null) {
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        } else null
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp),
            )
        }

        // 多选模式下的勾选角标与选中高亮遮罩
        if (isSelectMode) {
            if (isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            )
        }
    }
}
