package com.bwpixadapter.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bwpixadapter.app.data.CameraDevice
import com.bwpixadapter.app.data.DeviceStore
import com.easyview.bean.WifiBean
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import com.easyview.table.EventTable
import `object`.p2pipcam.nativecaller.NativeCaller
import `object`.p2pwificam.clientActivity.BridgeService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScannedWifi(
    val ssid: String,
    val mac: String,
    val security: Int,
    val dbm: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsScreen(
    deviceId: String,
    store: DeviceStore,
    onBack: () -> Unit,
    onDiag: (String) -> Unit,
    onPlayback: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }

    val did = device.uid.ifBlank { device.id }
    val app = context.applicationContext as CameraApplication
    val cam = app.cameraList.getCamera(did) as? PPCSCamera

    var isScanning by remember { mutableStateOf(false) }
    val scannedList = remember { mutableStateListOf<ScannedWifi>() }
    var selectedWifi by remember { mutableStateOf<ScannedWifi?>(null) }
    var wifiPassword by remember { mutableStateOf("") }
    var showWifiDialog by remember { mutableStateOf(false) }

    var showClearEventsDialog by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var eventCount by remember { mutableIntStateOf(0) }

    // 报警自动录像开关（控制实况观看期间收到移动报警时是否自动录制到手机）
    val settingsPrefs = remember(did) { context.getSharedPreferences("bwpix_settings", Context.MODE_PRIVATE) }
    var autoRecordOnAlarm by remember {
        mutableStateOf(settingsPrefs.getBoolean("auto_record_on_alarm", false))
    }

    fun refreshEventCount() {
        eventCount = EventTable.getEventCount(did, context)
    }

    LaunchedEffect(deviceId) {
        refreshEventCount()
        if (cam != null && !cam.isOnline) {
            cam.Start()
        }
    }

    DisposableEffect(deviceId) {
        BridgeService.setWifiInterface(object : BridgeService.WifiInterface {
            override fun callBackPPPPMsgNotifyData(d: String?, type: Int, param: Int) {}
            override fun callBackSetSystemParamsResult(d: String?, p1: Int, p2: Int) {
                Toast.makeText(context, "WiFi 设置已下发，摄像头正在连接…", Toast.LENGTH_SHORT).show()
            }

            override fun callBackWifiParams(
                d: String?, enable: Int, ssid: String?, channel: Int, mode: Int,
                authtype: Int, encryp: Int, keyformat: Int, defkey: Int,
                k1: String?, k2: String?, k3: String?, k4: String?,
                kb1: Int, kb2: Int, kb3: Int, kb4: Int, psk: String?
            ) {}

            override fun callBackWifiScanResult(
                d: String?, ssid: String?, mac: String?,
                security: Int, dbm0: Int, dbm1: Int, mode: Int, channel: Int, bEnd: Int
            ) {
                if (ssid != null && ssid.isNotBlank()) {
                    val cleanSsid = ssid.trim()
                    if (scannedList.none { it.ssid == cleanSsid }) {
                        scannedList.add(ScannedWifi(cleanSsid, mac ?: "", security, dbm0))
                    }
                }
                if (bEnd == 1) {
                    isScanning = false
                }
            }
        })

        onDispose {
            BridgeService.setWifiInterface(null)
        }
    }

    fun startWifiScan() {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线，无法扫描 WiFi", Toast.LENGTH_SHORT).show()
            return
        }
        scannedList.clear()
        isScanning = true
        cam.wifiScan(null)
    }

    fun applyWifiSetting(targetSsid: String, targetPwd: String, security: Int) {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线", Toast.LENGTH_SHORT).show()
            return
        }
        val bean = WifiBean().apply {
            enable = 1
            ssid = targetSsid
            wpa_psk = targetPwd
            authtype = if (security <= 0) 0 else 3 // WPA/WPA2
            encryp = 3 // AES
            channel = 0
            mode = 0
        }
        cam.setWifiParam(bean, null)
        Toast.makeText(context, "正在连接到 $targetSsid…", Toast.LENGTH_LONG).show()
    }

    fun syncTime() {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线", Toast.LENGTH_SHORT).show()
            return
        }
        cam.timing()
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        Toast.makeText(context, "已将手机时间 ($nowStr) 同步到摄像头", Toast.LENGTH_SHORT).show()
    }

    fun clearTfEvents() {
        if (cam != null && cam.isOnline) {
            val allEvents = EventTable.getAllEvent(context, did) ?: emptyList()
            val indices = allEvents.map { it.eventIndex }.toIntArray()
            if (indices.isNotEmpty()) {
                cam.delEvents(indices, null)
            }
        }
        EventTable.getInstance(context).delEvent(did)
        refreshEventCount()
        Toast.makeText(context, "已清空抓拍记录", Toast.LENGTH_SHORT).show()
    }

    fun rebootCamera() {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线", Toast.LENGTH_SHORT).show()
            return
        }
        NativeCaller.PPPPRebootDevice(did)
        Toast.makeText(context, "已发送重启指令，设备正在重启…", Toast.LENGTH_LONG).show()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("摄像头设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 设备基本状态
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (cam?.isOnline == true) "设备状态：在线" else "设备状态：未连接 (请连接灯泡热点)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (cam?.isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // 2. WiFi 网络配置
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("WiFi 连接设置", style = MaterialTheme.typography.titleSmall)
                            }
                            if (isScanning) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                FilledTonalButton(onClick = { startWifiScan() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("扫描 WiFi")
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "让灯泡摄像头连接到你家里的 WiFi 路由器。扫描完成后，点击目标网络并输入密码即可。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )

                        if (scannedList.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Text("可用网络列表 (${scannedList.size}):", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (wifi in scannedList) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedWifi = wifi
                                                wifiPassword = ""
                                                showWifiDialog = true
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (wifi.security > 0) Icons.Default.WifiLock else Icons.Default.Wifi,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.secondary,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(wifi.ssid, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("连接", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. 时间校准
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("时间校准", style = MaterialTheme.typography.titleSmall)
                                Text("同步手机系统时间到摄像头", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        OutlinedButton(onClick = { syncTime() }) {
                            Text("一键同步")
                        }
                    }
                }
            }

            // 4. 报警自动录像开关
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("报警时自动录像", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "实况观看期间检测到移动报警，自动开始/结束录像到手机本地",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        Switch(
                            checked = autoRecordOnAlarm,
                            onCheckedChange = { checked ->
                                autoRecordOnAlarm = checked
                                settingsPrefs.edit().putBoolean("auto_record_on_alarm", checked).apply()
                                Toast.makeText(
                                    context,
                                    if (checked) "已开启：报警时自动录像" else "已关闭：报警时自动录像",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                }
            }

            // 5. TF 卡录像文件回放
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("TF 卡录像文件回放", style = MaterialTheme.typography.titleSmall)
                                Text("检索并回放摄像头存储卡中的视频录像", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        FilledTonalButton(onClick = { onPlayback(deviceId) }) {
                            Text("浏览录像")
                        }
                    }
                }
            }

            // 5. TF 卡与存储抓拍管理
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SdCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("抓拍与存储记录", style = MaterialTheme.typography.titleSmall)
                                Text("已记录 $eventCount 条报警事件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        OutlinedButton(
                            onClick = { showClearEventsDialog = true },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("清空记录")
                        }
                    }
                }
            }

            // 5. 设备维护：重启
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("重启摄像头", style = MaterialTheme.typography.titleSmall)
                                Text("软重启摄像头固件系统", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        OutlinedButton(onClick = { showRebootDialog = true }) {
                            Text("重启")
                        }
                    }
                }
            }

            // 6. 底部高级选项：网络诊断
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onDiag(deviceId) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("高级网络排障诊断", style = MaterialTheme.typography.titleSmall)
                                Text("查看详细网络连通性、Ping 与原始报文", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Text("进入 ›", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    // WiFi 输入密码弹窗
    if (showWifiDialog && selectedWifi != null) {
        val wifi = selectedWifi!!
        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            title = { Text("连接到 ${wifi.ssid}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请输入该 WiFi 的访问密码：", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("WiFi 密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showWifiDialog = false
                    applyWifiSetting(wifi.ssid, wifiPassword, wifi.security)
                }) {
                    Text("确认连接")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWifiDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // 清空事件确认弹窗
    if (showClearEventsDialog) {
        AlertDialog(
            onDismissRequest = { showClearEventsDialog = false },
            title = { Text("确认清空记录？") },
            text = { Text("将删除本地抓拍及摄像头 TF 卡中的所有事件，避免卡内事件过多导致固件卡顿。") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearEventsDialog = false
                        clearTfEvents()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearEventsDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // 重启确认弹窗
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("确认重启摄像头？") },
            text = { Text("重启过程约需 30 秒，期间视频将断开，稍后重新连接即可。") },
            confirmButton = {
                Button(onClick = {
                    showRebootDialog = false
                    rebootCamera()
                }) {
                    Text("立即重启")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRebootDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}
