package com.bwpixadapter.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bwpixadapter.app.audio.LiveAudioController
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.data.HiCamClient
import com.bwpixadapter.app.media.LiveRecorder
import com.bwpixadapter.app.wifi.WifiHelper
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import `object`.p2pipcam.nativecaller.NativeCaller
import `object`.p2pwificam.clientActivity.BridgeService
import `object`.p2pwificam.clientActivity.MyRender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveViewScreen(
    deviceId: String,
    store: DeviceStore,
    onBack: () -> Unit,
) {
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val did = device.uid.ifBlank { device.id }

    var stateText by remember { mutableStateOf("连接中…") }
    var error by remember { mutableStateOf<String?>(null) }
    var snapshotting by remember { mutableStateOf(false) }
    var snapBmp by remember { mutableStateOf<Bitmap?>(null) }
    var reachable by remember { mutableStateOf(true) }

    var myRender by remember { mutableStateOf<MyRender?>(null) }
    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }
    var frameCount by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<PPCSCamera?>(null) }

    // 解决退出与进入时视图加载时差导致的闪烁问题
    var isExiting by remember { mutableStateOf(false) }
    var isContentReady by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60)
        isContentReady = true
    }

    // 1. 分辨率调节状态: 2=720P 超清, 1=VGA 高清, 0=QVGA 标清
    var currentResolution by remember { mutableIntStateOf(1) }
    var showResolutionMenu by remember { mutableStateOf(false) }

    // 2. 音频监听与对讲控制
    val audioController = remember(did) { LiveAudioController(did, context) }
    var isListeningAudio by remember { mutableStateOf(false) }
    var isTalkingIntercom by remember { mutableStateOf(false) }

    // 3. 本地录像：手动录制 + 报警自动录制（受设置开关控制）
    val recorder = remember(did) { LiveRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    val autoRecordOnAlarm = remember(did) {
        context.getSharedPreferences("bwpix_settings", Context.MODE_PRIVATE)
            .getBoolean("auto_record_on_alarm", false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioController.setTalking(true)
            isTalkingIntercom = true
            Toast.makeText(context, "正在喊话对讲中…", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要麦克风权限才能使用对讲喊话", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. 智能灯泡灯光与色温控制（集成持久化记忆，每次进入自动恢复上次的设置）
    val prefs = remember(did) { context.getSharedPreferences("bwpix_light_$did", Context.MODE_PRIVATE) }
    var lightPower by remember { mutableStateOf(prefs.getBoolean("light_power", true)) }
    var whiteBrightness by remember { mutableIntStateOf(prefs.getInt("light_white", 60)) }
    var yellowBrightness by remember { mutableIntStateOf(prefs.getInt("light_yellow", 60)) }

    fun applyLightSettings(power: Boolean, white: Int, yellow: Int) {
        val cam = camera ?: return
        if (!cam.isOnline) return
        val w = if (power) white else 0
        val y = if (power) yellow else 0
        cam.setLightValue(w, y, 1, null)
        prefs.edit()
            .putBoolean("light_power", power)
            .putInt("light_white", white)
            .putInt("light_yellow", yellow)
            .apply()
    }

    fun applyResolution(res: Int) {
        currentResolution = res
        NativeCaller.PPPPCameraControl(did, 0, res)
        val resName = when (res) {
            2 -> "720P 超清"
            1 -> "VGA 高清"
            else -> "QVGA 标清"
        }
        Toast.makeText(context, "已切换为 $resName", Toast.LENGTH_SHORT).show()
    }

    fun startLocalRecording() {
        if (isRecording) return
        val path = recorder.start(did, videoWidth, videoHeight, 15)
        if (path != null) {
            isRecording = true
            Toast.makeText(context, "正在录像到手机…", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "录像启动失败，请检查存储空间", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopLocalRecording(auto: Boolean = false) {
        if (!isRecording) return
        val path = recorder.stop()
        isRecording = false
        val name = path?.substringAfterLast('/')?.substringAfterLast('\\') ?: ""
        Toast.makeText(
            context,
            if (auto) "报警结束，录像已保存 ($name)" else "录像已保存 ($name)",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun safeExit() {
        if (isExiting) return
        isExiting = true
        if (isRecording) recorder.stop()
        glView?.visibility = View.GONE
        audioController.release()
        camera?.StopVideo()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBack()
    }

    // 拦截物理/手势返回键：若处于全屏则退出全屏，否则执行 safeExit() 退出
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            safeExit()
        }
    }

    DisposableEffect(deviceId) {
        onDispose {
            // 关键：onDispose 仅清理底层资源与监听器，绝不能再次调用 onBack() 或 popBackStack()
            if (isRecording) recorder.stop()
            audioController.release()
            camera?.StopVideo()
            BridgeService.setPlayInterface(null)
            myRender = null
            glView = null
        }
    }

    // 报警自动录制：设置开关开启时，收到 513(录像开始)/514(录像结束) 自动启停本地录像
    DisposableEffect(deviceId) {
        val alarmListener = object : BridgeService.AlarmNotifyListener {
            override fun onAlarmNotify(cameraDid: String, alarmType: Int, beginTime: Int, endTime: Int, index: Int) {
                if (cameraDid != did || !autoRecordOnAlarm) return
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                mainHandler.post {
                    when (alarmType) {
                        513 -> if (!isRecording) startLocalRecording()
                        514 -> if (isRecording) stopLocalRecording(auto = true)
                    }
                }
            }
        }
        BridgeService.addAlarmNotifyListener(alarmListener)
        onDispose {
            BridgeService.removeAlarmNotifyListener(alarmListener)
        }
    }

    LaunchedEffect(deviceId) {
        error = null
        stateText = "连接中…"
        reachable = com.bwpixadapter.app.wifi.CameraProbe.isReachable(device.host, device.httpPort)
        if (!reachable) {
            stateText = "无法访问 ${device.host}"
            return@LaunchedEffect
        }
        if (did.isBlank()) {
            error = "缺少设备 ID (UID)。"
            return@LaunchedEffect
        }

        val app = context.applicationContext as CameraApplication
        val cameraList = app.cameraList

        var cam = cameraList.getCamera(did) as? PPCSCamera
        if (cam == null) {
            cameraList.Add(device.name, did, device.user, device.password)
            cam = cameraList.getCamera(did) as? PPCSCamera
        }
        camera = cam

        BridgeService.setPlayInterface(object : BridgeService.PlayInterface {
            override fun callBaceVideoData(videobuf: ByteArray, h264Data: Int, len: Int, width: Int, height: Int) {
                if (width > 0 && height > 0 && videobuf.isNotEmpty() && !isExiting) {
                    if (videoWidth != width || videoHeight != height) {
                        videoWidth = width
                        videoHeight = height
                    }
                    myRender?.writeSample(videobuf, width, height)
                    frameCount++
                }
            }

            override fun callBackH264Data(h264: ByteArray, type: Int, size: Int) {
                // 录制状态下把原始 H264 帧写入 AVI
                if (isRecording && !isExiting) {
                    recorder.writeFrame(h264, size, type)
                }
            }

            override fun callBackAudioData(pcm: ByteArray, len: Int) {
                if (!isExiting) audioController.onAudioDataReceived(pcm, len)
            }

            override fun callBackCameraParamNotify(did: String, resolution: Int, brightness: Int, contrast: Int, hue: Int, saturation: Int, flip: Int) {
                currentResolution = resolution
            }

            override fun callBackMessageNotify(did: String, msgType: Int, param: Int) {
                if (msgType == 2) stateText = "已连接"
            }
        })

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        // 注册连接状态回调（使用主线程调度与多播监听器）
        val statusListener = object : BridgeService.IpcamClientInterface {
            override fun BSMsgNotifyData(cameraDid: String, type: Int, param: Int) {
                if (cameraDid == did && type == 0) {
                    mainHandler.post {
                        when (param) {
                            2 -> {
                                stateText = "设备在线，正在拉取视频流…"
                                cam?.StartVideo(null)
                                // 恢复上次记忆的灯光设置
                                applyLightSettings(lightPower, whiteBrightness, yellowBrightness)
                            }
                            3, 4, 6 -> {
                                stateText = "摄像头未在线"
                                error = "摄像头连接失败，请确认手机已连接到摄像头的 WiFi 热点"
                            }
                        }
                    }
                }
            }

            override fun BSSnapshotNotify(did: String, data: ByteArray, len: Int) {}
            override fun callBackUserParams(did: String, user1: String, pwd1: String, user2: String, pwd2: String, user3: String, pwd3: String) {}
        }

        BridgeService.addIpcamClientInterface(statusListener)

        stateText = "正在连接摄像头…"
        if (cam?.isOnline == true) {
            stateText = "设备在线，正在拉取视频流…"
            cam.StartVideo(null)
            applyLightSettings(lightPower, whiteBrightness, yellowBrightness)
        } else {
            cam?.Start()
        }

        delay(12000)
        if (error == null && stateText.startsWith("正在连接")) {
            stateText = "仍在尝试连接摄像头…"
        }
    }

    // 全屏模式与返回键拦截管理：全屏时按返回键仅退出全屏，绝不直接退出页面
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                // 退出全屏时严格恢复绝对竖屏，彻底避免横屏残留 bug
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            // 无论以何种方式离开，均强制重置为绝对竖屏并显示系统栏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(device.name) },
                    navigationIcon = {
                        IconButton(onClick = { safeExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else pad)
        ) {
            if (!reachable && !isFullscreen) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Column(Modifier.weight(1f)) {
                            Text("未连接到摄像头的 WiFi 热点")
                            Text("请先连接摄像头的 WiFi (IP ${device.host})",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { WifiHelper.openWifiSettings(context) }) {
                            Text("去连接")
                        }
                    }
                }
            }

            // 视频播放器区域：竖屏时 16:9 比例居中；全屏时满屏呈现
            val videoBoxModifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }

            Box(videoBoxModifier) {
                if (isContentReady) {
                    AndroidView(
                        factory = { ctx ->
                            GLSurfaceView(ctx).apply {
                                // 关键优化：加入 MediaOverlay 参与视图层级合成，消除退出黑洞闪烁
                                setZOrderMediaOverlay(true)
                                glView = this
                                val r = MyRender(this)
                                myRender = r
                                setRenderer(r)
                                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (error == null && frameCount == 0) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(stateText, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 播放器右上角：全屏切换图标
                IconButton(
                    onClick = { isFullscreen = !isFullscreen },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "进入全屏",
                        tint = Color.White,
                    )
                }

                // 错误提示蒙层
                error?.let {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("连接中断", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                error = null
                                stateText = "正在重新连接…"
                                camera?.Start()
                            }) {
                                Text("重试连接")
                            }
                        }
                    }
                }
            }

            if (!isFullscreen) {
                // 竖屏控制与丰富功能面板（支持滚动）
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. 状态与清晰度切换栏（去除冗余帧数显示，只呈现直观流畅度）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("画面状态", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (frameCount > 0) "实时监控中 · 传输流畅" else stateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (frameCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                )
                            }

                            // 分辨率选择下拉框
                            Box {
                                val currentResLabel = when (currentResolution) {
                                    2 -> "720P 超清"
                                    1 -> "VGA 高清"
                                    else -> "QVGA 标清"
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { showResolutionMenu = true },
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            currentResLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showResolutionMenu,
                                    onDismissRequest = { showResolutionMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("720P 超清 (推荐)") },
                                        onClick = {
                                            showResolutionMenu = false
                                            applyResolution(2)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("VGA 高清 (640×360)") },
                                        onClick = {
                                            showResolutionMenu = false
                                            applyResolution(1)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("QVGA 标清 (极速低码率)") },
                                        onClick = {
                                            showResolutionMenu = false
                                            applyResolution(0)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 2. 核心操作按键面板（全新智能家居大圆形按钮设计，2字精简标签，永不换行挤占）
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 录像 / 停止
                            LiveActionButton(
                                icon = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                label = if (isRecording) "停止" else "录像",
                                isActive = isRecording,
                                activeColor = Color(0xFFD32F2F),
                                onClick = {
                                    if (isRecording) {
                                        stopLocalRecording()
                                    } else {
                                        startLocalRecording()
                                    }
                                },
                            )

                            // 抓拍
                            LiveActionButton(
                                icon = Icons.Default.CameraAlt,
                                label = if (snapshotting) "拍照…" else "抓拍",
                                isActive = snapshotting,
                                activeColor = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    if (!snapshotting) {
                                        snapshotting = true
                                        scope.launch(Dispatchers.IO) {
                                            val bytes = HiCamClient(device).snapshotBytes()
                                            val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                            withContext(Dispatchers.Main) {
                                                snapshotting = false
                                                if (bmp != null) snapBmp = bmp
                                            }
                                        }
                                    }
                                },
                            )

                            // 监听声音
                            LiveActionButton(
                                icon = if (isListeningAudio) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                label = if (isListeningAudio) "静音" else "监听",
                                isActive = isListeningAudio,
                                activeColor = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    val newState = !isListeningAudio
                                    audioController.setListening(newState)
                                    isListeningAudio = newState
                                    Toast.makeText(context, if (newState) "已开启声音监听" else "已关闭监听", Toast.LENGTH_SHORT).show()
                                },
                            )

                            // 喊话对讲
                            LiveActionButton(
                                icon = if (isTalkingIntercom) Icons.Default.Mic else Icons.Default.MicOff,
                                label = if (isTalkingIntercom) "对讲中" else "喊话",
                                isActive = isTalkingIntercom,
                                activeColor = MaterialTheme.colorScheme.error,
                                onClick = {
                                    if (isTalkingIntercom) {
                                        audioController.setTalking(false)
                                        isTalkingIntercom = false
                                        Toast.makeText(context, "已结束对讲", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            audioController.setTalking(true)
                                            isTalkingIntercom = true
                                            Toast.makeText(context, "正在喊话，对着手机说话即可", Toast.LENGTH_SHORT).show()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                            )
                        }
                    }

                    // 3. 智能灯泡照明与色温控制面板（具备持久化记忆）
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("灯泡灯光与色温", style = MaterialTheme.typography.titleMedium)
                                        Text("自动记忆上次调优的冷暖光配比", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Switch(
                                    checked = lightPower,
                                    onCheckedChange = { checked ->
                                        lightPower = checked
                                        applyLightSettings(checked, whiteBrightness, yellowBrightness)
                                    },
                                )
                            }

                            if (lightPower) {
                                // 白光 / 冷光亮度调节
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WbIncandescent, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("冷白光亮度", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("$whiteBrightness%", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Slider(
                                        value = whiteBrightness.toFloat(),
                                        onValueChange = { whiteBrightness = it.toInt() },
                                        onValueChangeFinished = { applyLightSettings(true, whiteBrightness, yellowBrightness) },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                // 黄光 / 暖光亮度调节 (色温关键)
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF57F17))
                                            Spacer(Modifier.width(6.dp))
                                            Text("暖黄光色温", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("$yellowBrightness%", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Slider(
                                        value = yellowBrightness.toFloat(),
                                        onValueChange = { yellowBrightness = it.toInt() },
                                        onValueChangeFinished = { applyLightSettings(true, whiteBrightness, yellowBrightness) },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    snapBmp?.let { bmp ->
        AlertDialog(
            onDismissRequest = { snapBmp = null },
            confirmButton = { Button(onClick = { snapBmp = null }) { Text("关闭") } },
            title = { Text("实时快照") },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "快照",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

/**
 * 现代智能家居圆形操作按钮，图标与 2 字标签垂直居中对齐，绝不产生文字换行
 */
@Composable
private fun LiveActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}
