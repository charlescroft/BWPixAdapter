package com.bwpixadapter.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bwpixadapter.app.data.DeviceStore
import com.bwpixadapter.app.ui.theme.ThemeMode

object Routes {
    const val DEVICES = "devices"
    const val ADD = "add?deviceId={deviceId}"
    const val LIVE = "live/{deviceId}"
    const val EVENTS = "events/{deviceId}"
    const val SETTINGS = "settings/{deviceId}"
    const val PLAYBACK = "playback/{deviceId}?recordIndex={recordIndex}&fileName={fileName}&localFile={localFile}&fileDate={fileDate}"
    const val TF_EXPLORER = "tf_explorer/{deviceId}"
    const val DIAG = "diag/{deviceId}"

    fun add(deviceId: String = "") = "add?deviceId=$deviceId"
    fun live(deviceId: String) = "live/$deviceId"
    fun events(deviceId: String) = "events/$deviceId"
    fun settings(deviceId: String) = "settings/$deviceId"
    fun playback(deviceId: String, recordIndex: Int = -1, fileName: String = "", localFile: String = "", fileDate: String = "") =
        "playback/$deviceId?recordIndex=$recordIndex&fileName=$fileName&localFile=$localFile&fileDate=$fileDate"
    fun tfExplorer(deviceId: String) = "tf_explorer/$deviceId"
    fun diag(deviceId: String) = "diag/$deviceId"
}

@Composable
fun AppRoot(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { DeviceStore(context.applicationContext) }
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.DEVICES) {
        composable(Routes.DEVICES) {
            DeviceListScreen(
                store = store,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onAdd = { nav.navigate(Routes.add()) },
                onEdit = { nav.navigate(Routes.add(it.id)) },
                onLive = { nav.navigate(Routes.live(it.id)) },
                onEvents = { nav.navigate(Routes.events(it.id)) },
                onPlayback = { nav.navigate(Routes.tfExplorer(it.id)) },
                onSettings = { nav.navigate(Routes.settings(it.id)) },
            )
        }
        composable(
            route = Routes.ADD,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType; defaultValue = "" }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            AddDeviceScreen(
                store = store,
                deviceId = deviceId,
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.LIVE,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            LiveViewScreen(deviceId = deviceId, store = store, onBack = { nav.popBackStack() })
        }
        composable(
            route = Routes.EVENTS,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            EventsScreen(
                deviceId = deviceId,
                store = store,
                onBack = { nav.popBackStack() },
                onDiag = { nav.navigate(Routes.diag(deviceId)) },
                onPlayRecord = { did, recIdx ->
                    nav.navigate(Routes.playback(did, recordIndex = recIdx))
                },
            )
        }
        composable(
            route = Routes.SETTINGS,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            CameraSettingsScreen(
                deviceId = deviceId,
                store = store,
                onBack = { nav.popBackStack() },
                onDiag = { id -> nav.navigate(Routes.diag(id)) },
                onPlayback = { id -> nav.navigate(Routes.tfExplorer(id)) },
            )
        }
        composable(
            route = Routes.TF_EXPLORER,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            TfCardExplorerScreen(
                deviceId = deviceId,
                store = store,
                onBack = { nav.popBackStack() },
                onPlayVideo = { fileName, localPath, fileDate ->
                    nav.navigate(Routes.playback(deviceId, fileName = fileName, localFile = localPath, fileDate = fileDate))
                },
            )
        }
        composable(
            route = Routes.PLAYBACK,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("recordIndex") { type = NavType.IntType; defaultValue = -1 },
                navArgument("fileName") { type = NavType.StringType; defaultValue = "" },
                navArgument("localFile") { type = NavType.StringType; defaultValue = "" },
                navArgument("fileDate") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            val fileName = it.arguments?.getString("fileName").orEmpty()
            val localFile = it.arguments?.getString("localFile").orEmpty()
            val fileDate = it.arguments?.getString("fileDate").orEmpty()
            PlaybackScreen(
                deviceId = deviceId,
                initialFileName = fileName,
                localFilePath = localFile,
                fileDate = fileDate,
                store = store,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.DIAG,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) {
            val deviceId = it.arguments?.getString("deviceId").orEmpty()
            DiagScreen(deviceId = deviceId, store = store, onBack = { nav.popBackStack() })
        }
    }
}
