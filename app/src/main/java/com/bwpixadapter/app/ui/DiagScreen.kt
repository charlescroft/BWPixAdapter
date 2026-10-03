package com.bwpixadapter.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.data.HiCamClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagScreen(
    deviceId: String,
    store: DeviceStore,
    onBack: () -> Unit,
) {
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }
    val client = remember { HiCamClient(device) }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun run(label: String, path: String) {
        busy = true
        output = "$label\n正在请求…\n"
        val text = client.rawGet(path)
        output = "$label\n$text"
        busy = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("摄像头诊断") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { scope.launch { run("getall", "/web/cgi-bin/hi3510/param.cgi?cmd=getall") } },
                    modifier = Modifier.fillMaxWidth()) { Text("getall (全部参数)") }
                Button(onClick = { scope.launch { run("geteventinfo", "/web/cgi-bin/hi3510/param.cgi?cmd=geteventinfo") } },
                    modifier = Modifier.fillMaxWidth()) { Text("geteventinfo (事件概要)") }
                Button(onClick = { scope.launch { run("geteventlist", "/web/cgi-bin/hi3510/param.cgi?cmd=geteventlist&index=1&count=10") } },
                    modifier = Modifier.fillMaxWidth()) { Text("geteventlist (事件列表)") }
                Button(onClick = { scope.launch { run("getevent", "/web/cgi-bin/hi3510/param.cgi?cmd=getevent&index=1") } },
                    modifier = Modifier.fillMaxWidth()) { Text("getevent (单事件照片)") }
                Button(onClick = { scope.launch { run("searchrecord", "/web/cgi-bin/hi3510/param.cgi?cmd=searchrecord") } },
                    modifier = Modifier.fillMaxWidth()) { Text("searchrecord (录像列表)") }
                Button(onClick = { scope.launch { run("snap", "/web/cgi-bin/hi3510/snap.cgi") } },
                    modifier = Modifier.fillMaxWidth()) { Text("snap.cgi (快照)") }
            }
            OutlinedTextField(
                value = output,
                onValueChange = { output = it },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                label = { Text("原始返回") },
            )
        }
    }
}
