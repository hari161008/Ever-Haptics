package com.hapticks.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hapticks.app.R
import com.hapticks.app.ui.components.BottomTab
import com.hapticks.app.ui.components.FloatingBottomBar
import com.hapticks.app.ui.components.SlidingBottomTabHost
import com.hapticks.app.ui.screens.AppExclusionsScreen
import com.hapticks.app.ui.screens.HomeScreen
import com.hapticks.app.ui.screens.SettingsScreen
import com.hapticks.app.ui.screens.tapHaptics.FeelEveryTapScreen
import com.hapticks.app.ui.screens.scrollhaptics.ScrollHapticsScreen
import com.hapticks.app.ui.screens.charginghaptics.ChargingHapticsScreen
import com.hapticks.app.ui.screens.buttonhaptics.ButtonHapticsScreen
import com.hapticks.app.ui.screens.navhaptics.NavBarHapticsScreen
import com.hapticks.app.ui.screens.unlockHaptics.UnlockHapticsScreen
import com.hapticks.app.ui.screens.notificationhaptics.NotificationHapticsScreen
import com.hapticks.app.ui.haptics.ProvideHapticksEdgeOverscrollHaptics
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

            HapticksTheme(
                themeMode = settings.themeMode,
                useDynamicColors = settings.useDynamicColors,
                amoledBlack = settings.amoledBlack,
                seedColor = if (settings.useDynamicColors) null else settings.seedColor,
            ) {
                ProvideHapticksEdgeOverscrollHaptics {
                    var route by rememberSaveable { mutableStateOf(Route.HOME) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (route) {
                            Route.FEEL_EVERY_TAP -> {
                                BackHandler { route = Route.HOME }
                                FeelEveryTapScreen(
                                    settings = settings,
                                    onTapEnabledChange = viewModel::setTapEnabled,
                                    onIntensityCommit = viewModel::commitIntensity,
                                    onPatternSelected = viewModel::setPattern,
                                    onTestHaptic = viewModel::testHaptic,
                                    onResetToDefaults = viewModel::resetTapDefaults,
                                    onOpenAppExclusions = { route = Route.TAP_APP_EXCLUSIONS },
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.TAP_APP_EXCLUSIONS -> {
                                BackHandler { route = Route.FEEL_EVERY_TAP }
                                AppExclusionsScreen(
                                    title = getString(R.string.app_exclusions_title),
                                    excludedPackages = settings.tapExcludedPackages,
                                    onExcludedPackagesChange = viewModel::setTapExcludedPackages,
                                    onBack = { route = Route.FEEL_EVERY_TAP },
                                )
                            }
                            Route.HOME, Route.SETTINGS -> {
                                val bottomTab = if (route == Route.HOME) BottomTab.HOME else BottomTab.SETTINGS
                                SlidingBottomTabHost(selectedTab = bottomTab, modifier = Modifier.fillMaxSize()) { tab ->
                                    when (tab) {
                                        BottomTab.HOME -> HomeScreen(
                                            globalEnabled = settings.globalEnabled,
                                            isServiceEnabled = isServiceEnabled,
                                            onGlobalEnabledChange = viewModel::setGlobalEnabled,
                                            onOpenFeelEveryTap = { route = Route.FEEL_EVERY_TAP },
                                            onOpenTactileScrolling = { route = Route.TACTILE_SCROLLING },
                                            onOpenChargingHaptics = { route = Route.CHARGING_HAPTICS },
                                            onOpenButtonHaptics = { route = Route.BUTTON_HAPTICS },
                                            onOpenNavBarHaptics = { route = Route.NAVBAR_HAPTICS },
                                            onOpenUnlockHaptics = { route = Route.UNLOCK_HAPTICS },
                                            onOpenNotificationHaptics = { route = Route.NOTIFICATION_HAPTICS },
                                            onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                        )
                                        BottomTab.SETTINGS -> SettingsScreen(
                                            settings = settings,
                                            onUseDynamicColorsChange = viewModel::setUseDynamicColors,
                                            onThemeModeChange = viewModel::setThemeMode,
                                            onAmoledBlackChange = viewModel::setAmoledBlack,
                                            onSeedColorChange = viewModel::setSeedColor,
                                        )
                                    }
                                }
                            }
                            Route.TACTILE_SCROLLING -> {
                                BackHandler { route = Route.HOME }
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
                                    onOpenAppExclusions = { route = Route.SCROLL_APP_EXCLUSIONS },
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.SCROLL_APP_EXCLUSIONS -> {
                                BackHandler { route = Route.TACTILE_SCROLLING }
                                AppExclusionsScreen(
                                    title = getString(R.string.app_exclusions_title),
                                    excludedPackages = settings.scrollExcludedPackages,
                                    onExcludedPackagesChange = viewModel::setScrollExcludedPackages,
                                    onBack = { route = Route.TACTILE_SCROLLING },
                                )
                            }
                            Route.CHARGING_HAPTICS -> {
                                BackHandler { route = Route.HOME }
                                ChargingHapticsScreen(
                                    settings = settings,
                                    onChargingVibEnabledChange = viewModel::setChargingVibEnabled,
                                    onChargingVibOnConnectChange = viewModel::setChargingVibOnConnect,
                                    onChargingVibOnDisconnectChange = viewModel::setChargingVibOnDisconnect,
                                    onDurationIndexChange = viewModel::setChargingVibDurationIndex,
                                    onIntensityCommit = viewModel::commitChargingVibIntensity,
                                    onTestHaptic = viewModel::testChargingHaptic,
                                    onResetToDefaults = viewModel::resetChargingDefaults,
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.BUTTON_HAPTICS -> {
                                BackHandler { route = Route.HOME }
                                ButtonHapticsScreen(
                                    settings = settings,
                                    onVolumeHapticEnabledChange = viewModel::setVolumeHapticEnabled,
                                    onVolumePatternSelected = viewModel::setVolumeHapticPattern,
                                    onVolumeIntensityCommit = viewModel::commitVolumeHapticIntensity,
                                    onPowerHapticEnabledChange = viewModel::setPowerHapticEnabled,
                                    onPowerPatternSelected = viewModel::setPowerHapticPattern,
                                    onPowerIntensityCommit = viewModel::commitPowerHapticIntensity,
                                    onBrightnessHapticEnabledChange = viewModel::setBrightnessHapticEnabled,
                                    onBrightnessPatternSelected = viewModel::setBrightnessHapticPattern,
                                    onBrightnessIntensityCommit = viewModel::commitBrightnessHapticIntensity,
                                    onTestVolumeHaptic = viewModel::testVolumeHaptic,
                                    onTestPowerHaptic = viewModel::testPowerHaptic,
                                    onTestBrightnessHaptic = viewModel::testBrightnessHaptic,
                                    onResetToDefaults = viewModel::resetButtonHapticsDefaults,
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.NAVBAR_HAPTICS -> {
                                BackHandler { route = Route.HOME }
                                NavBarHapticsScreen(
                                    settings = settings,
                                    onNavBarHapticEnabledChange = viewModel::setNavBarHapticEnabled,
                                    onPatternSelected = viewModel::setNavBarHapticPattern,
                                    onIntensityCommit = viewModel::commitNavBarHapticIntensity,
                                    onTestHaptic = viewModel::testNavBarHaptic,
                                    onResetToDefaults = viewModel::resetButtonHapticsDefaults,
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.UNLOCK_HAPTICS -> {
                                BackHandler { route = Route.HOME }
                                UnlockHapticsScreen(
                                    settings = settings,
                                    onUnlockHapticEnabledChange = viewModel::setUnlockHapticEnabled,
                                    onPatternSelected = viewModel::setUnlockHapticPattern,
                                    onIntensityCommit = viewModel::commitUnlockHapticIntensity,
                                    onTestHaptic = viewModel::testUnlockHaptic,
                                    onResetToDefaults = viewModel::resetButtonHapticsDefaults,
                                    onBack = { route = Route.HOME },
                                )
                            }
                            Route.NOTIFICATION_HAPTICS -> {
                                BackHandler { route = Route.HOME }
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
                                    onResetToDefaults = viewModel::resetNotificationHapticsDefaults,
                                    onBack = { route = Route.HOME },
                                )
                            }
                        }

                        if (route == Route.HOME || route == Route.SETTINGS) {
                            FloatingBottomBar(
                                selectedTab = if (route == Route.HOME) BottomTab.HOME else BottomTab.SETTINGS,
                                onTabSelected = { tab -> route = if (tab == BottomTab.HOME) Route.HOME else Route.SETTINGS },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
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
        NAVBAR_HAPTICS, UNLOCK_HAPTICS, SETTINGS,
        NOTIFICATION_HAPTICS
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}
