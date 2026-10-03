package com.bwpixadapter.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bwpixadapter.app.data.CameraDevice
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.wifi.CameraDetector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    store: DeviceStore,
    deviceId: String,
    onDone: () -> Unit,
) {
    val editing = if (deviceId.isBlank()) null else store.devices.value.firstOrNull { it.id == deviceId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editing?.name ?: "") }
    var ssid by remember { mutableStateOf(editing?.wifiSsid ?: "") }
    var host by remember { mutableStateOf(editing?.host ?: CameraDevice.DEFAULT_HOST) }
    var rtspPort by remember { mutableStateOf((editing?.rtspPort ?: 554).toString()) }
    var httpPort by remember { mutableStateOf((editing?.httpPort ?: 80).toString()) }
    var user by remember { mutableStateOf(editing?.user ?: "") }
    var pwd by remember { mutableStateOf(editing?.password ?: "") }
    var uid by remember { mutableStateOf(editing?.uid ?: "") }
    var forceTcp by remember { mutableStateOf(editing?.forceRtpTcp ?: true) }
    var detecting by remember { mutableStateOf(false) }
    var detectResult by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Request the permissions needed to read the current WiFi SSID (Android 8+).
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ -> /* effect keyed on granted below re-runs detection */ }
    var permsOk by remember { mutableStateOf(false) }

    // On entry for a NEW device: prefill SSID + name from current WiFi, and
    // remember the credentials used for the previous camera.
    LaunchedEffect(deviceId, permsOk) {
        if (editing != null) return@LaunchedEffect
        val net = CameraDetector.currentNetwork(context)
        if (ssid.isBlank()) ssid = net.ssid.orEmpty()
        if (name.isBlank()) name = net.ssid ?: ""
        val (lu, lp) = store.lastCredentials()
        if (user.isBlank()) user = lu
        if (pwd.isBlank()) pwd = lp
    }

    LaunchedEffect(Unit) {
        val needed = if (Build.VERSION.SDK_INT >= 33) {
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) permsOk = true
        else permLauncher.launch(missing.toTypedArray())
    }

    suspend fun runDetect() {
        detecting = true
        detectResult = "正在检测…"
        val found = CameraDetector.detect(context)
        if (found != null) {
            host = found.host
            rtspPort = found.rtspPort.toString()
            httpPort = found.httpPort.toString()
            detectResult = "已识别摄像头: ${found.host} (${found.method})"
        } else {
            detectResult = "未识别到摄像头。请确认手机已连上灯泡的 WiFi 热点。"
        }
        detecting = false
    }

    // Auto-detect once if we are already on the bulb's WiFi.
    LaunchedEffect(Unit) {
        if (editing == null && host == CameraDevice.DEFAULT_HOST) {
            runDetect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "添加摄像头" else "编辑摄像头") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (editing == null) {
                Button(
                    onClick = { scope.launch { runDetect() } },
                    enabled = !detecting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (detecting) {
                        CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("从当前 WiFi 自动识别")
                }
                detectResult?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("名称") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如:客厅灯泡") },
            )
            OutlinedTextField(
                value = ssid, onValueChange = { ssid = it }, label = { Text("灯泡 WiFi 热点名 (SSID)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("自动识别后会自动填入") },
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it }, label = { Text("用户名") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("默认 Bellandwyson") },
            )
            OutlinedTextField(
                value = pwd, onValueChange = { pwd = it }, label = { Text("密码") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("默认 Bwpix+") },
            )
            OutlinedTextField(
                value = uid, onValueChange = { uid = it }, label = { Text("设备 ID (UID)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("机身标签上的 BWLED-XXXXXX,如 BWLED-009433-LFPFT") },
            )

            Row(
                Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("高级设置 (一般无需改动)", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null)
            }
            if (showAdvanced) {
                OutlinedTextField(
                    value = host, onValueChange = { host = it }, label = { Text("摄像头 IP") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = rtspPort, onValueChange = { rtspPort = it }, label = { Text("RTSP 端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = httpPort, onValueChange = { httpPort = it }, label = { Text("HTTP 端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = forceTcp, onCheckedChange = { forceTcp = it })
                    Spacer(Modifier.width(8.dp))
                    Text("使用 TCP 传输 RTSP (兼容性更好,推荐)", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val canSave = name.isNotBlank() && host.isNotBlank()
            Button(
                onClick = {
                    val device = CameraDevice(
                        id = editing?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name,
                        wifiSsid = ssid,
                        host = host,
                        rtspPort = rtspPort.toIntOrNull() ?: 554,
                        httpPort = httpPort.toIntOrNull() ?: 80,
                        user = user,
                        password = pwd,
                        uid = uid,
                        forceRtpTcp = forceTcp,
                    )
                    store.upsert(device)
                    store.saveCredentials(user, pwd)
                    onDone()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
            Text(
                "使用方式:手机连上灯泡的 WiFi 热点,点「从当前 WiFi 自动识别」,只需填名称和账号密码。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
