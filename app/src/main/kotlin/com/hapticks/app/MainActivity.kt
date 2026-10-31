package com.hapticks.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.ui.components.BottomTab
import com.hapticks.app.ui.components.FloatingBottomBar
import com.hapticks.app.ui.components.PetalAlertDialog
import com.hapticks.app.ui.predictive.PetalStackHost
import com.hapticks.app.ui.theme.PetalMaterialShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import com.hapticks.app.ui.haptics.ProvideHapticksEdgeOverscrollHaptics
import com.hapticks.app.ui.screens.AppExclusionsScreen
import com.hapticks.app.ui.screens.HomeScreen
import com.hapticks.app.ui.screens.ExploreAppsWebViewScreen
import com.hapticks.app.ui.screens.RateReviewWebViewScreen
import com.hapticks.app.ui.screens.ReviewsWebViewScreen
import com.hapticks.app.ui.screens.SettingsScreen
import com.hapticks.app.ui.screens.WelcomeDialog
import com.hapticks.app.ui.screens.autohaptics.AutoHapticsSettingsScreen
import com.hapticks.app.ui.screens.autohaptics.AutoHapticsType
import com.hapticks.app.ui.screens.buttonhaptics.ButtonHapticsScreen
import com.hapticks.app.ui.screens.charginghaptics.ChargingHapticsScreen
import com.hapticks.app.ui.screens.keyboardhaptics.KeyboardHapticsScreen
import com.hapticks.app.ui.screens.navhaptics.NavBarHapticsScreen
import com.hapticks.app.ui.screens.statusbarhaptics.StatusBarHapticsScreen
import com.hapticks.app.ui.screens.notificationhaptics.CustomHapticEditorScreen
import com.hapticks.app.ui.screens.notificationhaptics.NotificationHapticsScreen
import com.hapticks.app.ui.screens.scrollhaptics.ScrollHapticsScreen
import com.hapticks.app.ui.screens.tapHaptics.FeelEveryTapScreen
import com.hapticks.app.ui.screens.musichaptics.MusicHapticsScreen
import com.hapticks.app.ui.screens.unlockHaptics.UnlockHapticsScreen
import com.hapticks.app.ui.splash.EverHapticsSplashScreen
import com.hapticks.app.ui.splash.EverHapticsSplashState
import com.hapticks.app.util.AppVersion
import com.hapticks.app.util.UpdateChecker
import com.hapticks.app.ui.theme.HapticksTheme
import com.hapticks.app.viewmodel.FeelEveryTapViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FeelEveryTapViewModel by viewModels {
        FeelEveryTapViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isServiceEnabled by viewModel.isServiceEnabled.collectAsStateWithLifecycle()
            val isBatterySaverActive by viewModel.isBatterySaverActive.collectAsStateWithLifecycle()
            val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsStateWithLifecycle()

            HapticksTheme(
                themeMode = settings.themeMode,
                useDynamicColors = settings.useDynamicColors,
                amoledBlack = settings.amoledBlack,
                seedColor = if (settings.useDynamicColors) null else settings.seedColor,
            ) {
                var isSplashFinished by remember { mutableStateOf(EverHapticsSplashState.hasShownThisSession) }
                AnimatedContent(
                    targetState = isSplashFinished,
                    transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) },
                    label = "splash_to_main",
                ) { splashDone ->
                if (!splashDone) {
                EverHapticsSplashScreen(onSplashComplete = { isSplashFinished = true })
                } else {
                ProvideHapticksEdgeOverscrollHaptics {
                    // Back stack of route names (saveable). HOME is always the root; SETTINGS sits on top of it.
                    var stack by rememberSaveable { mutableStateOf(listOf(Route.HOME.name)) }
                    val routes = stack.map { Route.valueOf(it) }
                    val currentRoute = routes.last()
                    fun push(target: Route) { if (target !in routes) stack = stack + target.name }
                    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
                    fun selectTab(tab: BottomTab) {
                        stack = if (tab == BottomTab.HOME) listOf(Route.HOME.name) else listOf(Route.HOME.name, Route.SETTINGS.name)
                    }
                    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

                    // Hoisted scroll state — persists when navigating into and back from features
                    val homeScrollState = rememberScrollState()

                    // Auto-check for updates on launch
                    var hasAutoChecked by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(settings.autoCheckUpdatesEnabled) {
                        if (settings.autoCheckUpdatesEnabled && !hasAutoChecked) {
                            hasAutoChecked = true
                            UpdateChecker.checkForUpdate(AppVersion.get(this@MainActivity)).onSuccess { info ->
                                if (info.isUpdateAvailable) pendingUpdate = info
                            }
                        }
                    }

                    // Welcome dialog — only after DataStore has emitted once (prevents false show from default value)
                    if (isSettingsLoaded && !settings.hasSeenWelcome) {
                        WelcomeDialog(onDismiss = { viewModel.setHasSeenWelcome(true) })
                    }

                    var customEditorLabel by remember { mutableStateOf("") }
                    var customEditorSequence by remember { mutableStateOf(CustomHapticSequence()) }
                    var customEditorOnSave by remember { mutableStateOf<(CustomHapticSequence) -> Unit>({}) }

                    fun openCustomEditor(label: String, sequence: CustomHapticSequence, onSave: (CustomHapticSequence) -> Unit) {
                        customEditorLabel = label
                        customEditorSequence = sequence
                        customEditorOnSave = onSave
                        push(Route.CUSTOM_HAPTIC_EDITOR)
                    }

                    pendingUpdate?.let { info ->
                        PetalAlertDialog(
                            onDismissRequest = { pendingUpdate = null },
                            title = "Update available",
                            message = "Ever Haptics v${info.latestVersion} is ready. You can download and install it from Settings.",
                            icon = Icons.Rounded.SystemUpdate,
                            badgeShape = PetalMaterialShapes.Cookie9Sided,
                            confirmText = "Open Settings",
                            onConfirm = { pendingUpdate = null; selectTab(BottomTab.SETTINGS) },
                            dismissText = "Later",
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PetalStackHost(
                            stack = routes,
                            onBack = { pop() },
                            predictiveEnabled = settings.predictiveBackEnabled,
                            blurEnabled = settings.depthBlurEnabled,
                            modifier = Modifier.fillMaxSize(),
                        ) { currentRoute ->
                            when (currentRoute) {
                                Route.HOME ->
                                            HomeScreen(
                                                globalEnabled = settings.globalEnabled,
                                                isServiceEnabled = isServiceEnabled,
                                                isBatterySaverActive = isBatterySaverActive,
                                                batterySaverDetectionEnabled = settings.batterySaverDetectionEnabled,
                                                scrollState = homeScrollState,
                                                onGlobalEnabledChange = viewModel::setGlobalEnabled,
                                                onOpenFeelEveryTap = { push(Route.FEEL_EVERY_TAP) },
                                                onOpenTactileScrolling = { push(Route.TACTILE_SCROLLING) },
                                                onOpenChargingHaptics = { push(Route.CHARGING_HAPTICS) },
                                                onOpenButtonHaptics = { push(Route.BUTTON_HAPTICS) },
                                                onOpenNavBarHaptics = { push(Route.NAVBAR_HAPTICS) },
                                                onOpenStatusBarHaptics = { push(Route.STATUSBAR_HAPTICS) },
                                                onOpenUnlockHaptics = { push(Route.UNLOCK_HAPTICS) },
                                                onOpenNotificationHaptics = { push(Route.NOTIFICATION_HAPTICS) },
                                                onOpenKeyboardHaptics = { push(Route.KEYBOARD_HAPTICS) },
                                                onOpenMusicHaptics = { push(Route.MUSIC_HAPTICS) },
                                                onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                            )
                                Route.SETTINGS ->
                                            SettingsScreen(
                                                settings = settings,
                                                onUseDynamicColorsChange = viewModel::setUseDynamicColors,
                                                onThemeModeChange = viewModel::setThemeMode,
                                                onAmoledBlackChange = viewModel::setAmoledBlack,
                                                onSeedColorChange = viewModel::setSeedColor,
                                                onBatterySaverDetectionChange = viewModel::setBatterySaverDetectionEnabled,
                                                onAutoCheckUpdatesChange = viewModel::setAutoCheckUpdatesEnabled,
                                                onPredictiveBackChange = viewModel::setPredictiveBackEnabled,
                                                onDepthBlurChange = viewModel::setDepthBlurEnabled,
                                                onOpenRateReview = { push(Route.RATE_REVIEW_WEBVIEW) },
                                                onOpenReviews = { push(Route.REVIEWS_WEBVIEW) },
                                                onOpenExploreApps = { push(Route.EXPLORE_APPS_WEBVIEW) },
                                            )

                                Route.FEEL_EVERY_TAP -> {
                                    FeelEveryTapScreen(
                                        settings = settings,
                                        onTapEnabledChange = viewModel::setTapEnabled,
                                        onIntensityCommit = viewModel::commitIntensity,
                                        onPatternSelected = viewModel::setPattern,
                                        onTapCustomSequenceSave = viewModel::setTapHapticCustomSequence,
                                        onTestHaptic = viewModel::testHaptic,
                                        onResetToDefaults = viewModel::resetTapDefaults,
                                        onOpenAppExclusions = { push(Route.TAP_APP_EXCLUSIONS) },
                                        onOpenCustomEditor = { label, seq, onSave -> openCustomEditor(label, seq, onSave) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.TAP_APP_EXCLUSIONS -> {
                                    AppExclusionsScreen(
                                        title = getString(R.string.app_exclusions_title),
                                        excludedPackages = settings.tapExcludedPackages,
                                        onExcludedPackagesChange = viewModel::setTapExcludedPackages,
                                        onBack = { pop() },
                                    )
                                }

                                Route.TACTILE_SCROLLING -> {
                                    ScrollHapticsScreen(
                                        settings = settings,
                                        isServiceEnabled = isServiceEnabled,
                                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                        onScrollEnabledChange = viewModel::setScrollEnabled,
                                        onScrollHorizontalEnabledChange = viewModel::setScrollHorizontalEnabled,
                                        onScrollHapticEventsPerCmCommit = viewModel::commitScrollHapticEventsPerCm,
                                        onIntensityEnabledChange = viewModel::setScrollIntensityEnabled,
                                        onIntensityCommit = viewModel::commitScrollIntensity,
                                        onPatternSelected = viewModel::setScrollPattern,
                                        onVibrationsPerEventEnabledChange = viewModel::setScrollVibrationsPerEventEnabled,
                                        onVibrationsPerEventCommit = viewModel::commitScrollVibrationsPerEvent,
                                        onSpeedVibEnabledChange = viewModel::setScrollSpeedVibrationEnabled,
                                        onSpeedVibScaleCommit = viewModel::commitScrollSpeedVibScale,
                                        onTailCutoffEnabledChange = viewModel::setScrollTailCutoffEnabled,
                                        onTailCutoffMsCommit = viewModel::commitScrollTailCutoffMs,
                                        onTestHaptic = viewModel::testScrollHaptic,
                                        onResetToDefaults = viewModel::resetScrollDefaults,
                                        onOpenAppExclusions = { push(Route.SCROLL_APP_EXCLUSIONS) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.SCROLL_APP_EXCLUSIONS -> {
                                    AppExclusionsScreen(
                                        title = getString(R.string.app_exclusions_title),
                                        excludedPackages = settings.scrollExcludedPackages,
                                        onExcludedPackagesChange = viewModel::setScrollExcludedPackages,
                                        onBack = { pop() },
                                    )
                                }

                                Route.CHARGING_HAPTICS -> {
                                    ChargingHapticsScreen(
                                        settings = settings,
                                        onChargingVibEnabledChange = viewModel::setChargingVibEnabled,
                                        onChargingVibOnConnectChange = viewModel::setChargingVibOnConnect,
                                        onChargingVibOnDisconnectChange = viewModel::setChargingVibOnDisconnect,
                                        onPatternSelected = viewModel::setChargingVibPattern,
                                        onIntensityCommit = viewModel::commitChargingVibIntensity,
                                        onCustomSequenceSaved = viewModel::setChargingVibCustomSequence,
                                        onTestHaptic = viewModel::testChargingHaptic,
                                        onResetToDefaults = viewModel::resetChargingDefaults,
                                        onOpenCustomEditor = { openCustomEditor("charging", settings.chargingVibCustomSequence, viewModel::setChargingVibCustomSequence) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.BUTTON_HAPTICS -> {
                                    ButtonHapticsScreen(
                                        settings = settings,
                                        onVolumeHapticEnabledChange = viewModel::setVolumeHapticEnabled,
                                        onVolumePatternSelected = viewModel::setVolumeHapticPattern,
                                        onVolumeIntensityCommit = viewModel::commitVolumeHapticIntensity,
                                        onVolumeOpenCustomEditor = { openCustomEditor("volume", settings.volumeHapticCustomSequence, viewModel::setVolumeHapticCustomSequence) },
                                        onVolumeClearCustomSequence = { viewModel.setVolumeHapticCustomSequence(CustomHapticSequence()) },
                                        onPowerHapticEnabledChange = viewModel::setPowerHapticEnabled,
                                        onPowerPatternSelected = viewModel::setPowerHapticPattern,
                                        onPowerIntensityCommit = viewModel::commitPowerHapticIntensity,
                                        onPowerOpenCustomEditor = { openCustomEditor("power", settings.powerHapticCustomSequence, viewModel::setPowerHapticCustomSequence) },
                                        onPowerClearCustomSequence = { viewModel.setPowerHapticCustomSequence(CustomHapticSequence()) },
                                        onBrightnessHapticEnabledChange = viewModel::setBrightnessHapticEnabled,
                                        onBrightnessPatternSelected = viewModel::setBrightnessHapticPattern,
                                        onBrightnessIntensityCommit = viewModel::commitBrightnessHapticIntensity,
                                        onBrightnessOpenCustomEditor = { openCustomEditor("brightness", settings.brightnessHapticCustomSequence, viewModel::setBrightnessHapticCustomSequence) },
                                        onBrightnessClearCustomSequence = { viewModel.setBrightnessHapticCustomSequence(CustomHapticSequence()) },
                                        onTestVolumeHaptic = viewModel::testVolumeHaptic,
                                        onTestPowerHaptic = viewModel::testPowerHaptic,
                                        onTestBrightnessHaptic = viewModel::testBrightnessHaptic,
                                        onResetToDefaults = viewModel::resetButtonHapticsDefaults,
                                        onBack = { pop() },
                                    )
                                }

                                Route.NAVBAR_HAPTICS -> {
                                    NavBarHapticsScreen(
                                        settings = settings,
                                        onNavBarHapticEnabledChange = viewModel::setNavBarHapticEnabled,
                                        onPatternSelected = viewModel::setNavBarHapticPattern,
                                        onIntensityCommit = viewModel::commitNavBarHapticIntensity,
                                        onTestHaptic = viewModel::testNavBarHaptic,
                                        onResetToDefaults = viewModel::resetNavBarDefaults,
                                        onOpenCustomEditor = { openCustomEditor("navbar", settings.navBarHapticCustomSequence, viewModel::setNavBarHapticCustomSequence) },
                                        onClearCustomSequence = { viewModel.setNavBarHapticCustomSequence(CustomHapticSequence()) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.STATUSBAR_HAPTICS -> {
                                    StatusBarHapticsScreen(
                                        settings = settings,
                                        onStatusBarHapticEnabledChange = viewModel::setStatusBarHapticEnabled,
                                        onExpandEnabledChange = viewModel::setStatusBarExpandEnabled,
                                        onExpandPatternSelected = viewModel::setStatusBarExpandPattern,
                                        onExpandIntensityCommit = viewModel::commitStatusBarExpandIntensity,
                                        onExpandCustomSequenceSave = viewModel::setStatusBarExpandCustomSequence,
                                        onTestExpandHaptic = viewModel::testStatusBarExpandHaptic,
                                        onCollapseEnabledChange = viewModel::setStatusBarCollapseEnabled,
                                        onCollapsePatternSelected = viewModel::setStatusBarCollapsePattern,
                                        onCollapseIntensityCommit = viewModel::commitStatusBarCollapseIntensity,
                                        onCollapseCustomSequenceSave = viewModel::setStatusBarCollapseCustomSequence,
                                        onTestCollapseHaptic = viewModel::testStatusBarCollapseHaptic,
                                        onResetToDefaults = viewModel::resetStatusBarDefaults,
                                        onOpenExpandCustomEditor = { openCustomEditor("statusbar_expand", settings.statusBarExpandCustomSequence, viewModel::setStatusBarExpandCustomSequence) },
                                        onClearExpandCustomSequence = { viewModel.setStatusBarExpandCustomSequence(CustomHapticSequence()) },
                                        onOpenCollapseCustomEditor = { openCustomEditor("statusbar_collapse", settings.statusBarCollapseCustomSequence, viewModel::setStatusBarCollapseCustomSequence) },
                                        onClearCollapseCustomSequence = { viewModel.setStatusBarCollapseCustomSequence(CustomHapticSequence()) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.UNLOCK_HAPTICS -> {
                                    UnlockHapticsScreen(
                                        settings = settings,
                                        onUnlockHapticEnabledChange = viewModel::setUnlockHapticEnabled,
                                        onPatternSelected = viewModel::setUnlockHapticPattern,
                                        onIntensityCommit = viewModel::commitUnlockHapticIntensity,
                                        onTestHaptic = viewModel::testUnlockHaptic,
                                        onResetToDefaults = viewModel::resetUnlockDefaults,
                                        onOpenCustomEditor = { openCustomEditor("unlock", settings.unlockHapticCustomSequence, viewModel::setUnlockHapticCustomSequence) },
                                        onClearCustomSequence = { viewModel.setUnlockHapticCustomSequence(CustomHapticSequence()) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.KEYBOARD_HAPTICS -> {
                                    KeyboardHapticsScreen(
                                        settings = settings,
                                        onKeyboardHapticEnabledChange = viewModel::setKeyboardHapticEnabled,
                                        onPatternSelected = viewModel::setKeyboardHapticPattern,
                                        onIntensityCommit = viewModel::commitKeyboardHapticIntensity,
                                        onTestHaptic = viewModel::testKeyboardHaptic,
                                        onResetToDefaults = viewModel::resetKeyboardDefaults,
                                        onOpenCustomEditor = { openCustomEditor("keyboard", settings.keyboardHapticCustomSequence, viewModel::setKeyboardHapticCustomSequence) },
                                        onClearCustomSequence = { viewModel.setKeyboardHapticCustomSequence(CustomHapticSequence()) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.NOTIFICATION_HAPTICS -> {
                                    NotificationHapticsScreen(
                                        callHapticEnabled = settings.callHapticEnabled,
                                        callPattern = settings.callHapticPattern,
                                        callIntensity = settings.callHapticIntensity,
                                        callCustomSequence = settings.callHapticCustomSequence,
                                        onCallEnabledChange = viewModel::setCallHapticEnabled,
                                        onCallPatternSelected = viewModel::setCallHapticPattern,
                                        onCallIntensityCommit = viewModel::commitCallHapticIntensity,
                                        onCallCustomSequenceSave = viewModel::setCallHapticCustomSequence,
                                        onTestCallHaptic = viewModel::testCallHaptic,
                                        callHapticAuto = settings.callHapticAuto,
                                        onOpenCallAutoSettings = { viewModel.setCallHapticAuto(true); viewModel.setCallHapticCustomSequence(CustomHapticSequence()); push(Route.CALL_AUTO_HAPTICS) },
                                        onCallAutoChange = viewModel::setCallHapticAuto,
                                        notifHapticEnabled = settings.notifHapticEnabled,
                                        notifPattern = settings.notifHapticPattern,
                                        notifIntensity = settings.notifHapticIntensity,
                                        notifCustomSequence = settings.notifHapticCustomSequence,
                                        onNotifEnabledChange = viewModel::setNotifHapticEnabled,
                                        onNotifPatternSelected = viewModel::setNotifHapticPattern,
                                        onNotifIntensityCommit = viewModel::commitNotifHapticIntensity,
                                        onNotifCustomSequenceSave = viewModel::setNotifHapticCustomSequence,
                                        onTestNotifHaptic = viewModel::testNotifHaptic,
                                        alarmHapticEnabled = settings.alarmHapticEnabled,
                                        alarmPattern = settings.alarmHapticPattern,
                                        alarmIntensity = settings.alarmHapticIntensity,
                                        alarmCustomSequence = settings.alarmHapticCustomSequence,
                                        onAlarmEnabledChange = viewModel::setAlarmHapticEnabled,
                                        onAlarmPatternSelected = viewModel::setAlarmHapticPattern,
                                        onAlarmIntensityCommit = viewModel::commitAlarmHapticIntensity,
                                        onAlarmCustomSequenceSave = viewModel::setAlarmHapticCustomSequence,
                                        onTestAlarmHaptic = viewModel::testAlarmHaptic,
                                        alarmHapticAuto = settings.alarmHapticAuto,
                                        onOpenAlarmAutoSettings = { viewModel.setAlarmHapticAuto(true); viewModel.setAlarmHapticCustomSequence(CustomHapticSequence()); push(Route.ALARM_AUTO_HAPTICS) },
                                        onAlarmAutoChange = viewModel::setAlarmHapticAuto,
                                        onResetToDefaults = viewModel::resetNotificationHapticsDefaults,
                                        onOpenCustomEditor = { label, sequence, onSave -> openCustomEditor(label, sequence, onSave) },
                                        onBack = { pop() },
                                    )
                                }

                                Route.MUSIC_HAPTICS -> {
                                    MusicHapticsScreen(
                                        settings = settings,
                                        onMusicHapticsEnabledChange = viewModel::setMusicHapticsEnabled,
                                        onMusicHapticsSourceChange = viewModel::setMusicHapticsSource,
                                        onSensitivityCommit = viewModel::commitMusicHapticsSensitivity,
                                        onStrengthCommit = viewModel::commitMusicHapticsStrength,
                                        onBack = { pop() },
                                    )
                                }

                                Route.CALL_AUTO_HAPTICS -> {
                                    AutoHapticsSettingsScreen(
                                        type = AutoHapticsType.CALL,
                                        isEnabled = settings.callHapticAuto,
                                        sensitivity = settings.callAutoSensitivity,
                                        strength = settings.callAutoStrength,
                                        onEnabledChange = viewModel::setCallHapticAuto,
                                        onSensitivityCommit = viewModel::commitCallAutoSensitivity,
                                        onStrengthCommit = viewModel::commitCallAutoStrength,
                                        onBack = { pop() },
                                    )
                                }

                                Route.ALARM_AUTO_HAPTICS -> {
                                    AutoHapticsSettingsScreen(
                                        type = AutoHapticsType.ALARM,
                                        isEnabled = settings.alarmHapticAuto,
                                        sensitivity = settings.alarmAutoSensitivity,
                                        strength = settings.alarmAutoStrength,
                                        onEnabledChange = viewModel::setAlarmHapticAuto,
                                        onSensitivityCommit = viewModel::commitAlarmAutoSensitivity,
                                        onStrengthCommit = viewModel::commitAlarmAutoStrength,
                                        onBack = { pop() },
                                    )
                                }

                                Route.RATE_REVIEW_WEBVIEW -> {
                                    RateReviewWebViewScreen(
                                        onBack = { pop() },
                                    )
                                }

                                Route.REVIEWS_WEBVIEW -> {
                                    ReviewsWebViewScreen(
                                        onBack = { pop() },
                                    )
                                }

                                Route.EXPLORE_APPS_WEBVIEW -> {
                                    ExploreAppsWebViewScreen(
                                        onBack = { pop() },
                                    )
                                }

                                Route.CUSTOM_HAPTIC_EDITOR -> {
                                    CustomHapticEditorScreen(
                                        label = customEditorLabel,
                                        initialSequence = customEditorSequence,
                                        onSave = { seq -> customEditorOnSave(seq) },
                                        onBack = { pop() },
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = currentRoute == Route.HOME || currentRoute == Route.SETTINGS,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 2 },
                            exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 2 },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            FloatingBottomBar(
                                selectedTab = if (currentRoute == Route.SETTINGS) BottomTab.SETTINGS else BottomTab.HOME,
                                onTabSelected = { tab -> selectTab(tab) },
                            )
                        }
                    }
                }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceState()
    }

    private enum class Route {
        HOME, FEEL_EVERY_TAP, TAP_APP_EXCLUSIONS,
        TACTILE_SCROLLING, SCROLL_APP_EXCLUSIONS,
        CHARGING_HAPTICS, BUTTON_HAPTICS,
        NAVBAR_HAPTICS, STATUSBAR_HAPTICS, UNLOCK_HAPTICS, KEYBOARD_HAPTICS,
        MUSIC_HAPTICS,
        SETTINGS, NOTIFICATION_HAPTICS, CUSTOM_HAPTIC_EDITOR,
        CALL_AUTO_HAPTICS, ALARM_AUTO_HAPTICS,
        RATE_REVIEW_WEBVIEW,
        REVIEWS_WEBVIEW,
        EXPLORE_APPS_WEBVIEW
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}
