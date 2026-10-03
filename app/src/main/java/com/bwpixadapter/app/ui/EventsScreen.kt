package com.bwpixadapter.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bwpixadapter.app.data.DeviceStore
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import com.easyview.table.EventTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import `object`.p2pipcam.bean.EventDetailBean
import `object`.p2pipcam.utils.Pub
import java.io.File

// 全局 20MB 缩略图内存 LRU 缓存，避免滑动数百条事件列表时主线程重复解码卡顿
private val thumbnailCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    deviceId: String,
    store: DeviceStore,
    onBack: () -> Unit,
    onDiag: () -> Unit,
    onPlayRecord: (String, Int) -> Unit = { _, _ -> },
) {
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val did = device.uid.ifBlank { device.id }

    var events by remember { mutableStateOf<List<EventDetailBean>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selectedBean by remember { mutableStateOf<EventDetailBean?>(null) }
    var selectedBmp by remember { mutableStateOf<Bitmap?>(null) }
    var downloadingIdx by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 多选删除状态
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }

    // 保留天数设置 (默认 30 天, 0 代表不限)
    var reserveDays by remember { mutableIntStateOf(Pub.get_event_reserve_days(context)) }
    var showDaysMenu by remember { mutableStateOf(false) }

    val app = context.applicationContext as CameraApplication
    val cam = app.cameraList.getCamera(did) as? PPCSCamera

    fun cleanExpiredEvents(days: Int) {
        if (days > 0) {
            val nowSec = (System.currentTimeMillis() / 1000).toInt()
            val cutoff = nowSec - (days * 24 * 3600)
            EventTable.getInstance(context).deleteOldEvents(did, cutoff)
        }
    }

    fun loadLocalEvents() {
        cleanExpiredEvents(reserveDays)
        events = EventTable.getAllEvent(context, did) ?: emptyList()
    }

    LaunchedEffect(deviceId) {
        loading = true
        loadLocalEvents()
        loading = false
    }

    // 多选模式下按返回键仅退出多选状态，不退出页面
    BackHandler(enabled = isSelectMode) {
        isSelectMode = false
        selectedIds.clear()
    }

    // 按需下载单张照片，绝不在后台自动遍历批量下载，避免阻塞网络和滥用空间
    fun downloadSinglePhoto(bean: EventDetailBean) {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线，请先连上摄像头热点", Toast.LENGTH_SHORT).show()
            return
        }
        downloadingIdx = bean.eventIndex
        Toast.makeText(context, "正在从摄像头下载该抓拍照片…", Toast.LENGTH_SHORT).show()
        cam.queryEvent(bean.eventIndex, null, null)
        scope.launch(Dispatchers.IO) {
            var found = false
            val photoDir = Pub.photoSavePathFile()
            val didNum = Pub.getDIDNum(did)
            for (i in 0 until 18) {
                delay(800)
                var p = bean.getPicturePath(true)
                if (p == null && photoDir != null && photoDir.exists()) {
                    val files = photoDir.listFiles { _, name ->
                        name.startsWith(didNum) && name.endsWith(".jpg")
                    }
                    if (files != null && files.isNotEmpty()) {
                        val matched = files.firstOrNull { it.name.contains("${bean.eventTime}") }
                            ?: files.maxByOrNull { it.lastModified() }
                        if (matched != null && matched.length() > 0) {
                            p = matched.absolutePath
                            EventTable.getInstance(context).Update(did, bean.fileID, bean.eventType, bean.eventTime, p)
                        }
                    }
                }

                if (p != null) {
                    found = true
                    thumbnailCache.remove(p)
                    withContext(Dispatchers.Main) {
                        loadLocalEvents()
                        downloadingIdx = null
                        Toast.makeText(context, "抓拍照片下载成功", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
            if (!found) {
                withContext(Dispatchers.Main) {
                    downloadingIdx = null
                    Toast.makeText(context, "下载超时，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 删除选中的多项记录
    fun deleteSelectedEvents() {
        val toDelete = events.filter { it.id in selectedIds }
        if (toDelete.isEmpty()) return

        // 1. 从 SQLite 数据库删除
        EventTable.getInstance(context).delEventsByIds(toDelete.map { it.id })

        // 2. 从摄像头 TF 卡同步清理
        if (cam != null && cam.isOnline) {
            val indices = toDelete.map { it.eventIndex }.toIntArray()
            if (indices.isNotEmpty()) {
                cam.delEvents(indices, null)
            }
        }

        // 3. 删除本地图片缓存
        toDelete.forEach { bean ->
            bean.picturePath?.let {
                try {
                    File(it).delete()
                    thumbnailCache.remove(it)
                } catch (_: Exception) {}
            }
        }

        val count = toDelete.size
        isSelectMode = false
        selectedIds.clear()
        loadLocalEvents()
        Toast.makeText(context, "已删除 $count 项记录", Toast.LENGTH_SHORT).show()
    }

    // 清空全部事件
    fun deleteAll() {
        val indices = events.map { it.eventIndex }.toIntArray()
        if (cam != null && cam.isOnline && indices.isNotEmpty()) {
            cam.delEvents(indices, null)
        }
        EventTable.getInstance(context).delEvent(did)
        thumbnailCache.evictAll()
        events = emptyList()
        Toast.makeText(context, "已清空全部记录", Toast.LENGTH_SHORT).show()
    }

    fun exportToGallery(bean: EventDetailBean) {
        val path = bean.picturePath
        if (path == null) {
            Toast.makeText(context, "图片尚未下载", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        val success = Pub.saveImageToGallery(context, file, "BWPix_${bean.eventTime}")
        if (success) {
            Toast.makeText(context, "已成功保存到系统相册", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "保存相册失败", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectMode) "已选择 ${selectedIds.size} 项"
                        else "事件记录 (${events.size})"
                    )
                },
                navigationIcon = {
                    if (isSelectMode) {
                        IconButton(onClick = {
                            isSelectMode = false
                            selectedIds.clear()
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
                        // 多选模式：全选 / 全不选按钮
                        val isAllSelected = events.isNotEmpty() && selectedIds.size == events.size
                        IconButton(onClick = {
                            if (isAllSelected) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(events.map { it.id })
                            }
                        }) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (isAllSelected) "取消全选" else "全选",
                            )
                        }

                        // 复用右上角删除按钮：删除选中的项
                        IconButton(
                            onClick = { if (selectedIds.isNotEmpty()) showDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除选中",
                                tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    } else {
                        // 正常浏览模式
                        // 保留天数设置按钮
                        Box {
                            IconButton(onClick = { showDaysMenu = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "保留天数")
                            }
                            DropdownMenu(
                                expanded = showDaysMenu,
                                onDismissRequest = { showDaysMenu = false },
                            ) {
                                val daysList = listOf(7 to "保留 7 天", 15 to "保留 15 天", 30 to "保留 30 天", 0 to "全部保留")
                                for ((days, label) in daysList) {
                                    DropdownMenuItem(
                                        text = { Text(if (reserveDays == days) "✓ $label" else "  $label") },
                                        onClick = {
                                            showDaysMenu = false
                                            reserveDays = days
                                            Pub.set_event_reserve_days(context, days)
                                            loadLocalEvents()
                                            Toast.makeText(context, "已设置事件$label", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }

                        if (events.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "清空全部", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        IconButton(onClick = { loadLocalEvents() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
            )
        },
    ) { pad ->
        when {
            loading && events.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            events.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(pad).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                Text("暂无事件记录", style = MaterialTheme.typography.titleMedium)
                Text(
                    "摄像头检测到移动或报警时会自动产生抓拍记录。\n设置了「保留天数」后，超期事件会自动清理以保障流畅运行。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events, key = { if (it.id > 0) it.id else "${it.eventIndex}_${it.eventTime}_${it.hashCode()}" }) { bean ->
                    val isSelected = selectedIds.contains(bean.id)
                    EventCardAsync(
                        bean = bean,
                        isSelectMode = isSelectMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (selectedIds.contains(bean.id)) {
                                selectedIds.remove(bean.id)
                            } else {
                                selectedIds.add(bean.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectMode) {
                                isSelectMode = true
                                selectedIds.add(bean.id)
                            }
                        },
                        isDownloading = downloadingIdx == bean.eventIndex,
                        onDownloadClick = { downloadSinglePhoto(bean) },
                        onPhotoClick = { bmp ->
                            selectedBean = bean
                            selectedBmp = bmp
                        },
                        onPlayRecordClick = {
                            onPlayRecord(did, bean.recordIndex)
                        },
                    )
                }
            }
        }
    }

    // 大图预览弹窗：极简设计，无标题与冗余说明，仅呈现圆角抓拍图与底部的「关闭」和「保存」按钮
    if (selectedBmp != null && selectedBean != null) {
        val currentBean = selectedBean!!
        AlertDialog(
            onDismissRequest = {
                selectedBmp = null
                selectedBean = null
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = { exportToGallery(currentBean) },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        selectedBmp = null
                        selectedBean = null
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("关闭")
                }
            },
            text = {
                Image(
                    bitmap = selectedBmp!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            },
        )
    }

    // 删除确认弹窗（自动复用并区分：多选删除 vs 全部清空）
    if (showDeleteConfirm) {
        val isBatchDelete = isSelectMode && selectedIds.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isBatchDelete) "删除选中的 ${selectedIds.size} 项？" else "清空全部事件？") },
            text = {
                Text(
                    if (isBatchDelete) "将从手机中删除选中的记录，并同步清理摄像头 TF 卡上的对应文件。"
                    else "将删除手机本地记录并同步清空摄像头 TF 卡上的全部抓拍文件，避免卡内事件过多引起卡顿。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        if (isBatchDelete) {
                            deleteSelectedEvents()
                        } else {
                            deleteAll()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(if (isBatchDelete) "确认删除" else "确认清空")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 带有长按多选、后台异步解码与 LRU 内存缓存的事件条目组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCardAsync(
    bean: EventDetailBean,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onPhotoClick: (Bitmap) -> Unit,
    onPlayRecordClick: () -> Unit = {},
) {
    val picPath = bean.picturePath

    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = picPath) {
        if (picPath.isNullOrEmpty()) {
            value = null
            return@produceState
        }
        val cached = thumbnailCache.get(picPath)
        if (cached != null && !cached.isRecycled) {
            value = cached
            return@produceState
        }
        val bmp = withContext(Dispatchers.IO) {
            try {
                val f = File(picPath)
                if (f.exists() && f.length() > 0) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val decoded = BitmapFactory.decodeFile(picPath, opts)
                    if (decoded != null) {
                        thumbnailCache.put(picPath, decoded)
                    }
                    decoded
                } else null
            } catch (e: Exception) {
                null
            }
        }
        value = bmp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectMode) {
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    onLongClick()
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
            // 多选勾选框
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (isSelectMode) {
                            onToggleSelect()
                        } else {
                            if (thumbnailBitmap != null) {
                                val fullBmp = try {
                                    BitmapFactory.decodeFile(picPath)
                                } catch (_: Exception) { null } ?: thumbnailBitmap
                                if (fullBmp != null) onPhotoClick(fullBmp)
                            } else if (!isDownloading) {
                                onDownloadClick()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    thumbnailBitmap != null -> {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    isDownloading -> {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    else -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("点此下载", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    bean.eventText ?: "报警事件 #${bean.eventIndex}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bean.timeText ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (bean.recordIndex >= 0 && !isSelectMode) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onPlayRecordClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "回放录像",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}
