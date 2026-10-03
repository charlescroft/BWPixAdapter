package com.bwpixadapter.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.media.H264DecoderView
import com.bwpixadapter.app.media.LocalAviReader
import com.easyview.camera.CameraApplication
import com.easyview.ppcs.PPCSCamera
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import `object`.p2pipcam.nativecaller.NativeCaller
import `object`.p2pwificam.clientActivity.BridgeService
import `object`.p2pwificam.clientActivity.MyRender

data class RecordFileItem(
    val fileName: String,
    val fileSize: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    deviceId: String,
    initialFileName: String = "",
    localFilePath: String = "",
    fileDate: String = "",
    store: DeviceStore,
    onBack: () -> Unit,
) {
    val device = store.devices.value.firstOrNull { it.id == deviceId }
    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("设备不存在") }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    val did = device.uid.ifBlank { device.id }

    val app = context.applicationContext as CameraApplication
    val cam = app.cameraList.getCamera(did) as? PPCSCamera

    var myRender by remember { mutableStateOf<MyRender?>(null) }
    var glView by remember { mutableStateOf<GLSurfaceView?>(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayingFile by remember { mutableStateOf(initialFileName) }
    var frameCount by remember { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    val recordFileList = remember { mutableStateListOf<RecordFileItem>() }
    var isFullscreen by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var isContentReady by remember { mutableStateOf(false) }

    // 本地文件播放模式：从手机已下载的 AVI 直接读取播放
    val localAviFile = remember { if (localFilePath.isNotBlank()) File(localFilePath) else null }
    val isLocalMode = remember { localAviFile?.exists() == true }
    var decoderView by remember { mutableStateOf<H264DecoderView?>(null) }
    var localPaused by remember { mutableStateOf(false) }
    var localDone by remember { mutableStateOf(false) }
    var localStarted by remember { mutableStateOf(false) }
    var localFrameCount by remember { mutableIntStateOf(0) }
    var localTotalFrames by remember { mutableIntStateOf(0) }
    var localDurationMs by remember { mutableIntStateOf(0) }
    var localPositionMs by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    val localStopFlag = remember { AtomicBoolean(false) }
    val localPauseFlag = remember { AtomicBoolean(false) }
    val localStartFlag = remember { AtomicBoolean(false) }
    val seekTargetPts = remember { AtomicInteger(-1) }
    val readThreadStarted = remember { AtomicBoolean(false) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun containsSpsLocal(d: ByteArray): Boolean {
        for (i in 0 until d.size - 5) {
            if (d[i] == 0.toByte() && d[i + 1] == 0.toByte() && d[i + 2] == 0.toByte() &&
                d[i + 3] == 1.toByte() && (d[i + 4].toInt() and 0x1f) == 7
            ) return true
        }
        return false
    }

    fun startLocalPlayback() {
        if (localDone) {
            // 重播：重置状态
            localDone = false
            localFrameCount = 0
            localPositionMs = 0
            localStopFlag.set(false)
            localStartFlag.set(false)
            readThreadStarted.set(false)
            seekTargetPts.set(-1)
        }
        localStarted = true
        localPaused = false
        localPauseFlag.set(false)
        localStartFlag.set(true)
        isPlaying = true
    }

    fun toggleLocalPlayback() {
        if (!localStarted) {
            startLocalPlayback()
        } else if (localPaused) {
            localPaused = false
            localPauseFlag.set(false)
        } else {
            localPaused = true
            localPauseFlag.set(true)
        }
    }

    fun seekLocal(ms: Int) {
        val clamped = ms.coerceIn(0, localDurationMs.coerceAtLeast(0))
        localPositionMs = clamped
        if (localTotalFrames > 0 && localDurationMs > 0) {
            seekTargetPts.set((clamped.toLong() * localTotalFrames / localDurationMs).toInt())
        }
    }

    LaunchedEffect(Unit) {
        delay(60)
        isContentReady = true
    }

    fun stopPlayback() {
        if (isPlaying) {
            cam?.recordStop()
            isPlaying = false
        }
    }

    fun safeExit() {
        if (isExiting) return
        isExiting = true
        stopPlayback()
        glView?.visibility = View.GONE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBack()
    }

    fun startPlayback(fileName: String) {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线，无法回放", Toast.LENGTH_SHORT).show()
            return
        }
        stopPlayback()
        currentPlayingFile = fileName
        frameCount = 0
        isPlaying = true
        cam.recordPlay(fileName)
    }

    fun searchRecordFiles() {
        if (cam == null || !cam.isOnline) {
            Toast.makeText(context, "摄像头未在线", Toast.LENGTH_SHORT).show()
            return
        }
        recordFileList.clear()
        isSearching = true
        val nowSec = (System.currentTimeMillis() / 1000).toInt()
        val startSec = nowSec - 30 * 24 * 3600
        NativeCaller.PPPPGetSDCardRecordFileList(did, startSec, nowSec)

        // 5 秒超时保护，彻底避免网络或固件未返回 bEnd 导致界面一直转圈卡死在搜索中
        scope.launch {
            delay(5000)
            if (isSearching) {
                isSearching = false
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    BackHandler(enabled = !isFullscreen) {
        safeExit()
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
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(deviceId) {
        BridgeService.setPlayBackInterface(object : BridgeService.PlayBackInterface {
            override fun callBackPlaybackVideoData(videobuf: ByteArray?, h264Data: Int, len: Int, width: Int, height: Int, time: Int) {
                if (videobuf != null && width > 0 && height > 0 && len > 0 && !isExiting) {
                    myRender?.writeSample(videobuf, width, height)
                    frameCount++
                }
            }

            override fun callBackAudioData(bArr: ByteArray?, i: Int) {}
            override fun callBackMessageNotify(d: String?, msgType: Int, param: Int) {
                if (msgType == 2 && param == 0) {
                    // 回放结束
                    isPlaying = false
                }
            }
        })

        BridgeService.setPlayBackTFInterface { d, filename, nFileSize, bEnd ->
            if (filename != null && filename.isNotBlank()) {
                val clean = filename.trim()
                if (recordFileList.none { it.fileName == clean }) {
                    recordFileList.add(RecordFileItem(clean, nFileSize))
                }
            }
            if (bEnd == 1) {
                isSearching = false
            }
        }

        onDispose {
            isExiting = true
            localStopFlag.set(true)
            decoderView?.stop()
            decoderView = null
            stopPlayback()
            BridgeService.setPlayBackInterface(null)
            BridgeService.setPlayBackTFInterface(null)
            myRender = null
            glView = null
        }
    }

    LaunchedEffect(deviceId) {
        if (isLocalMode) return@LaunchedEffect
        if (initialFileName.isNotBlank()) {
            startPlayback(initialFileName)
        }
        searchRecordFiles()
    }

    // 本地文件播放：后台线程用轻量 AVI 解复用器读取 → 逐帧 Annex-B H264 喂给 MediaCodec 解码渲染。
    // 1) 按真实帧率墙钟推进，保证实时播放速度；
    // 2) 默认不自动播放，等用户点击控制栏播放按钮后再读取；
    // 3) 支持拖动进度（定位到目标帧）。
    LaunchedEffect(deviceId) {
        if (!isLocalMode) return@LaunchedEffect
        val file = localAviFile ?: return@LaunchedEffect
        localStopFlag.set(false)
        localPauseFlag.set(false)
        localStartFlag.set(false)
        localDone = false
        localStarted = false
        localFrameCount = 0
        localPositionMs = 0
        localDurationMs = 0
        localTotalFrames = 0
        seekTargetPts.set(-1)
        isPlaying = false

        thread(name = "local-avi") {
            val reader = LocalAviReader(file)
            if (!reader.open()) {
                mainHandler.post {
                    localDone = true
                    Toast.makeText(context, "本地文件打开失败", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val totalFrames = reader.totalFrames.coerceAtLeast(0)
            val microsPerFrame = reader.microSecPerFrame.coerceAtLeast(1)
            val durationMs = if (totalFrames > 0) (totalFrames.toLong() * microsPerFrame / 1000L).toInt() else 0
            android.util.Log.i("LocalAvi", "opened frames=$totalFrames micros=$microsPerFrame dur=$durationMs ${reader.width}x${reader.height}")

            fun posMs(frame: Int): Int =
                if (totalFrames > 0) (frame.toLong() * durationMs / totalFrames).toInt() else 0

            mainHandler.post {
                localTotalFrames = totalFrames
                localDurationMs = durationMs
            }

            // 等待用户点击播放
            while (!localStopFlag.get() && !localStartFlag.get()) {
                Thread.sleep(20)
            }

            var n = 0
            var nextDueMs = 0L

            // PCM 音频播放（8000Hz 单声道 16bit，与原版通话音频一致）
            val audioTrack = try {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    8000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4, 4096),
                    AudioTrack.MODE_STREAM
                )
            } catch (_: Exception) {
                null
            }
            var audioPaused = false

            try {
                while (!localStopFlag.get()) {
                    if (localPauseFlag.get() || !localStartFlag.get()) {
                        if (!audioPaused && audioTrack != null) {
                            try { audioTrack.pause() } catch (_: Exception) {}
                            audioPaused = true
                        }
                        Thread.sleep(40)
                        continue
                    }
                    if (audioPaused && audioTrack != null) {
                        try { audioTrack.play() } catch (_: Exception) {}
                        audioPaused = false
                    }
                    // 拖动进度：定位到目标帧
                    val seek = seekTargetPts.getAndSet(-1)
                    if (seek >= 0) {
                        reader.seekToFrame(seek)
                        n = seek
                        if (audioTrack != null) {
                            try { audioTrack.pause(); audioTrack.flush(); audioTrack.play() } catch (_: Exception) {}
                            audioPaused = false
                        }
                        mainHandler.post {
                            localFrameCount = n
                            localPositionMs = posMs(n)
                        }
                        nextDueMs = 0L
                    }

                    val chunk = reader.readNextChunk() ?: break
                    when (chunk.type) {
                        com.bwpixadapter.app.media.LocalAviReader.AviChunkType.AUDIO -> {
                            try { audioTrack?.write(chunk.data, 0, chunk.data.size) } catch (_: Exception) {}
                        }
                        else -> {
                            decoderView?.feed(chunk.data, containsSpsLocal(chunk.data))
                            n++
                            mainHandler.post {
                                localFrameCount = n
                                localPositionMs = posMs(n)
                            }
                            // 墙钟节流：按真实帧率推进，保证实时速度
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (nextDueMs == 0L) {
                                nextDueMs = now + (microsPerFrame / 1000L)
                            } else {
                                nextDueMs += microsPerFrame / 1000L
                            }
                            val sleep = nextDueMs - now
                            if (sleep > 0) Thread.sleep(sleep)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { audioTrack?.pause(); audioTrack?.flush(); audioTrack?.release() } catch (_: Exception) {}
                reader.close()
                mainHandler.post {
                    localDone = true
                    localStarted = false
                    localStartFlag.set(false)
                    isPlaying = false
                }
            }
        }

        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            localStopFlag.set(true)
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text("TF卡录像回放") },
                    navigationIcon = {
                        IconButton(onClick = { safeExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (!isLocalMode) {
                            IconButton(onClick = { searchRecordFiles() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新录像列表")
                            }
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
            // 播放器区域：严格 16:9
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
                if (isLocalMode) {
                    AndroidView(
                        factory = { ctx ->
                            H264DecoderView(ctx).apply {
                                decoderView = this
                                start()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (isContentReady) {
                    AndroidView(
                        factory = { ctx ->
                            GLSurfaceView(ctx).apply {
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

                if (isLocalMode) {
                    // 本地播放：点击视频区域切换控制栏显隐（屏幕中央不放任何按钮）
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { showControls = !showControls },
                    )

                    // 底部现代控制栏：播放/暂停 - 可拖动进度条 - 全屏
                    if (showControls) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { toggleLocalPlayback() },
                            ) {
                                Icon(
                                    if (!localStarted || localPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (!localStarted) "播放" else if (localPaused) "继续播放" else "暂停",
                                    tint = Color.White,
                                )
                            }
                            androidx.compose.material3.Slider(
                                value = localPositionMs.toFloat().coerceIn(0f, localDurationMs.coerceAtLeast(1).toFloat()),
                                onValueChange = { localPositionMs = it.toInt() },
                                onValueChangeFinished = { seekLocal(localPositionMs) },
                                valueRange = 0f..localDurationMs.coerceAtLeast(1).toFloat(),
                                enabled = localTotalFrames > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                            )
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                } else {
                    if (isPlaying && frameCount == 0) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text("正在拉取录像流…", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (!isPlaying) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Text(
                                if (currentPlayingFile.isNotBlank()) "录像已暂停 / 播放完毕" else "请在下方列表中选择录像进行播放",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    // 右上角全屏按钮（非本地模式）
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "全屏切换",
                            tint = Color.White,
                        )
                    }
                }
            }

            if (!isFullscreen) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 控制播放与暂停状态条（本地模式控制栏已叠加在播放器上，此处不重复显示）
                    if (isLocalMode) {
                        // 本地文件信息面板
                        val durationText = if (localDurationMs > 0) {
                            val s = localDurationMs / 1000
                            String.format("%02d:%02d", s / 60, s % 60)
                        } else "加载中…"
                        val sizeText = localAviFile?.let {
                            String.format("%.1f MB", it.length() / 1048576.0)
                        } ?: "-"
                        val dateText = fileDate.ifBlank {
                            localAviFile?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date(it.lastModified()))
                            } ?: "-"
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    localAviFile?.name ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                InfoRow("创建时间", dateText)
                                InfoRow("时长", durationText)
                                InfoRow("大小", sizeText)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    } else if (currentPlayingFile.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "当前录像：${currentPlayingFile.substringAfterLast("/")}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        if (isPlaying) "正在流畅回放中" else "已暂停",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    )
                                }

                                FilledTonalButton(onClick = {
                                    if (isPlaying) {
                                        stopPlayback()
                                    } else {
                                        startPlayback(currentPlayingFile)
                                    }
                                }) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPlaying) "暂停" else "播放")
                                }
                            }
                        }
                    }

                    if (isLocalMode) {
                        Spacer(Modifier.weight(1f))
                    } else {
                    // TF 卡录像文件清单
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("TF 卡录像文件列表 (${recordFileList.size})", style = MaterialTheme.typography.titleSmall)
                        if (isSearching) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }

                    if (recordFileList.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (isSearching) "正在检索 TF 卡录像…" else "暂无录像记录，或请确保已插入 TF 卡",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(recordFileList, key = { it.fileName }) { item ->
                                val isCur = item.fileName == currentPlayingFile && isPlaying
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { startPlayback(item.fileName) },
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCur) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = if (isCur) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.fileName.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                "大小：${item.fileSize / 1024} KB",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }

                                        Surface(
                                            color = if (isCur) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                        ) {
                                            Text(
                                                if (isCur) "正在播放" else "点击播放",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCur) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } // end else (非本地模式：TF 卡列表)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
