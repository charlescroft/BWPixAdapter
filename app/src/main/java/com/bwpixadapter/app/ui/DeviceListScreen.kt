package com.bwpixadapter.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bwpixadapter.app.data.CameraDevice
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.ui.theme.ThemeMode
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import com.easyview.table.EventTable
import `object`.p2pwificam.clientActivity.BridgeService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    store: DeviceStore,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAdd: () -> Unit,
    onEdit: (CameraDevice) -> Unit,
    onLive: (CameraDevice) -> Unit,
    onEvents: (CameraDevice) -> Unit,
    onPlayback: (CameraDevice) -> Unit,
    onSettings: (CameraDevice) -> Unit,
) {
    val devices by store.devices.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as CameraApplication
    val cameraList = app.cameraList

    val connectionStatus = remember { mutableStateMapOf<String, Int>() }
    val eventCounts = remember { mutableStateMapOf<String, Int>() }

    var showThemeMenu by remember { mutableStateOf(false) }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        val listener = object : BridgeService.IpcamClientInterface {
            override fun BSMsgNotifyData(did: String, type: Int, param: Int) {
                if (type == 0) {
                    mainHandler.post {
                        connectionStatus[did] = param
                    }
                }
            }

            override fun BSSnapshotNotify(did: String, data: ByteArray, len: Int) {}
            override fun callBackUserParams(did: String, user1: String, pwd1: String, user2: String, pwd2: String, user3: String, pwd3: String) {}
        }

        BridgeService.addIpcamClientInterface(listener)

        onDispose {
            BridgeService.removeIpcamClientInterface(listener)
        }
    }

    LaunchedEffect(devices) {
        for (dev in devices) {
            val did = dev.uid.ifBlank { dev.id }
            eventCounts[did] = EventTable.getEventCount(did, context)

            var cam = cameraList.getCamera(did) as? PPCSCamera
            if (cam == null) {
                cameraList.Add(dev.name, did, dev.user, dev.password)
                cam = cameraList.getCamera(did) as? PPCSCamera
            }
            if (cam != null) {
                if (cam.isOnline) {
                    connectionStatus[did] = 2
                } else {
                    if (!connectionStatus.containsKey(did)) {
                        connectionStatus[did] = 0
                    }
                    cam.Start()
                }
            }
        }
    }

    val isDarkSystem = isSystemInDarkTheme()
    val isCurrentlyDark = when (themeMode) {
        ThemeMode.SYSTEM -> isDarkSystem
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的设备") },
                actions = {
                    // 暗黑模式切换按钮：默认跟随系统，点击切换或长按选择
                    Box {
                        IconButton(
                            onClick = {
                                val nextMode = if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
                                onThemeModeChange(nextMode)
                                Toast.makeText(
                                    context,
                                    if (nextMode == ThemeMode.DARK) "已切换为深色模式" else "已切换为浅色模式",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "暗黑模式切换",
                            )
                        }

                        // 下拉菜单提供「跟随系统」还原选项
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (themeMode == ThemeMode.SYSTEM) "✓ 跟随系统" else "  跟随系统") },
                                onClick = {
                                    showThemeMenu = false
                                    onThemeModeChange(ThemeMode.SYSTEM)
                                    Toast.makeText(context, "已恢复跟随系统主题", Toast.LENGTH_SHORT).show()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (themeMode == ThemeMode.LIGHT) "✓ 浅色模式" else "  浅色模式") },
                                onClick = {
                                    showThemeMenu = false
                                    onThemeModeChange(ThemeMode.LIGHT)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (themeMode == ThemeMode.DARK) "✓ 深色模式" else "  深色模式") },
                                onClick = {
                                    showThemeMenu = false
                                    onThemeModeChange(ThemeMode.DARK)
                                },
                            )
                        }
                    }

                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.BrightnessAuto, contentDescription = "主题设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "添加设备") }
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (devices.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("还没有绑定设备", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "点击右下角 + 添加你的 BWPixAdapter 灯泡摄像头。\n添加后手机连上灯泡自带的 WiFi 热点即可直接查看画面。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            items(devices, key = { it.id }) { device ->
                val did = device.uid.ifBlank { device.id }
                val status = connectionStatus[did]
                val count = eventCounts[did] ?: 0

                DeviceCard(
                    device = device,
                    status = status,
                    eventCount = count,
                    onPlay = { onLive(device) },
                    onEvents = { onEvents(device) },
                    onPlayback = { onPlayback(device) },
                    onSettings = { onSettings(device) },
                    onEdit = { onEdit(device) },
                    onDelete = {
                        val cam = cameraList.getCamera(did)
                        cam?.Stop()
                        cameraList.Del(did)
                        store.remove(device.id)
                    },
                )
            }

            if (devices.isNotEmpty()) {
                item {
                    Text(
                        "提示：请确保手机已连接到灯泡摄像头的 WiFi 热点，或灯泡已配置连接到同一路由器。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: CameraDevice,
    status: Int?,
    eventCount: Int,
    onPlay: () -> Unit,
    onEvents: () -> Unit,
    onPlayback: () -> Unit,
    onSettings: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOnline = status == 2
    val isConnecting = status == null || status == 0 || status == 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            // 顶部：设备图标、设备名称、连接状态徽标、编辑与删除
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOnline) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        device.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            isOnline -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "在线",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF2E7D32),
                                )
                            }
                            isConnecting -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "连接中…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "未连接",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 中部：抓拍事件统计条
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (eventCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (eventCount > 0) "已记录 $eventCount 条报警抓拍" else "暂无抓拍事件",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eventCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 底部主操作：查看实况
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("查看实况")
            }

            Spacer(Modifier.height(10.dp))

            // 次级操作行：仅保留两个 50%/50% 宽度的按钮，空间极为宽裕，绝不换行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onEvents,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(if (eventCount > 0) "事件抓拍 ($eventCount)" else "事件抓拍")
                }

                OutlinedButton(
                    onClick = onPlayback,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("TF卡录像")
                }
            }
        }
    }
}
