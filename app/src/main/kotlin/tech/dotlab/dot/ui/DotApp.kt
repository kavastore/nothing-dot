package tech.dotlab.dot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.device.DeviceRegistry
import tech.dotlab.dot.device.DeviceSupport
import tech.dotlab.dot.feature.editor.ui.EditorScreen
import tech.dotlab.dot.feature.editor.ui.GalleryScreen
import tech.dotlab.dot.feature.game.ui.GameInfoScreen
import tech.dotlab.dot.feature.key.ui.ActionPickerScreen
import tech.dotlab.dot.feature.key.ui.AdbHelperScreen
import tech.dotlab.dot.feature.key.ui.InAppAdbScreen
import tech.dotlab.dot.feature.key.ui.KeyStatusScreen
import tech.dotlab.dot.feature.key.ui.RecordTriggerScreen
import tech.dotlab.dot.feature.key.ui.UnlockMethodScreen
import tech.dotlab.dot.feature.key.ui.UnlockWizardScreen

private object Routes {
    const val INTRO = "intro"
    const val DEVICE = "device"
    const val HOME = "home"
    const val GALLERY = "gallery"
    const val EDITOR = "editor"
    const val IMAGE = "image"
    const val PLAY = "play"
    const val SETTINGS = "settings"
    const val KEY = "key"
    const val KEY_ACTIONS = "key/actions"
    const val KEY_RECORD = "key/record"
    const val KEY_ADB = "key/adb"
    const val KEY_UNLOCK = "key/unlock"
    const val KEY_UNLOCK_HUB = "key/unlock-hub"
    const val KEY_INAPP_ADB = "key/inapp-adb"
}

@Composable
fun DotApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val support = remember { DeviceRegistry.resolveCurrent() }
    val prefs = remember { OnboardingPrefs(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val seen by produceState<Boolean?>(initialValue = null) { value = prefs.hasSeen() }
    val start = seen ?: return // wait until we know, avoids flashing the wrong screen

    NavHost(navController = navController, startDestination = if (start) Routes.HOME else Routes.INTRO) {
        composable(Routes.INTRO) {
            OnboardingScreen(
                onFinish = {
                    scope.launch { prefs.markSeen() }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.DEVICE) {
            DeviceCheckScreen(
                support = support,
                onContinue = { navController.popBackStack() },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                support = support,
                onDraw = { navController.navigate(Routes.GALLERY) },
                onKey = { navController.navigate(Routes.KEY) },
                onImage = { navController.navigate(Routes.IMAGE) },
                onPlay = { navController.navigate(Routes.PLAY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onDeviceInfo = { navController.navigate(Routes.DEVICE) },
            )
        }

        composable(Routes.IMAGE) {
            ImagePickerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onReplayOnboarding = {
                    navController.navigate(Routes.INTRO) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.PLAY) {
            val profile = (support as? DeviceSupport.Supported)?.profile
            GameInfoScreen(
                hasGlyphButton = profile?.hasGlyphTouch == true,
                matrixSize = profile?.matrixSize ?: 25,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                onOpen = { id -> navController.navigate("${Routes.EDITOR}/$id") },
                onNew = { navController.navigate("${Routes.EDITOR}/0") },
            )
        }

        composable(
            route = "${Routes.EDITOR}/{artId}",
            arguments = listOf(navArgument("artId") { type = NavType.LongType }),
        ) { entry ->
            val artId = entry.arguments?.getLong("artId") ?: 0L
            EditorScreen(
                artId = artId.takeIf { it > 0 },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.KEY) {
            KeyStatusScreen(
                onPickAction = { gesture -> navController.navigate("${Routes.KEY_ACTIONS}/${gesture.name}") },
                onRecordTrigger = { navController.navigate(Routes.KEY_RECORD) },
                onUnlockHub = { navController.navigate(Routes.KEY_UNLOCK_HUB) },
                onAdbHelper = { navController.navigate(Routes.KEY_ADB) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.KEY_UNLOCK_HUB) {
            UnlockMethodScreen(
                onInAppAdb = { navController.navigate(Routes.KEY_INAPP_ADB) },
                onShizuku = { navController.navigate(Routes.KEY_UNLOCK) },
                onAdbPc = { navController.navigate(Routes.KEY_ADB) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.KEY_INAPP_ADB) {
            InAppAdbScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.KEY_UNLOCK) {
            UnlockWizardScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.KEY_ADB) {
            AdbHelperScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "${Routes.KEY_ACTIONS}/{gesture}",
            arguments = listOf(navArgument("gesture") { type = NavType.StringType }),
        ) { entry ->
            val gesture = entry.arguments?.getString("gesture")
                ?.let { runCatching { Gesture.valueOf(it) }.getOrNull() }
                ?: Gesture.DOUBLE
            ActionPickerScreen(
                gesture = gesture,
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.KEY_RECORD) {
            RecordTriggerScreen(
                onRecorded = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
