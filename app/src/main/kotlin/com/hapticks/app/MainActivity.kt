package com.hapticks.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hapticks.app.ui.components.BottomTab
import com.hapticks.app.ui.components.FloatingBottomBar
import com.hapticks.app.ui.components.SlidingBottomTabHost
import com.hapticks.app.ui.screens.everytap.FeelEveryTapScreen
import com.hapticks.app.ui.screens.HomeScreen
import com.hapticks.app.ui.screens.edgehaptics.EdgeHapticsScreen
import com.hapticks.app.ui.screens.scrollhaptics.ScrollHapticsScreen
import com.hapticks.app.ui.screens.SettingsScreen
import com.hapticks.app.ui.haptics.ProvideHapticksEdgeOverscrollHaptics
import com.hapticks.app.ui.theme.HapticksTheme
import com.hapticks.app.viewmodel.FeelEveryTapViewModel
import com.hapticks.app.viewmodel.EdgeHapticsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FeelEveryTapViewModel by viewModels {
        FeelEveryTapViewModel.factory(application)
    }

    private val edgeViewModel: EdgeHapticsViewModel by viewModels {
        EdgeHapticsViewModel.factory(application)
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
                seedColor = settings.seedColor,
            ) {
                ProvideHapticksEdgeOverscrollHaptics {
                    var route by rememberSaveable { mutableStateOf(Route.HOME) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (route) {
                        Route.FEEL_EVERY_TAP -> {
                            BackHandler { route = Route.HOME }
                            FeelEveryTapScreen(
                                settings = settings,
                                isServiceEnabled = isServiceEnabled,
                                onTapEnabledChange = viewModel::setTapEnabled,
                                onIntensityCommit = viewModel::commitIntensity,
                                onPatternSelected = viewModel::setPattern,
                                onTestHaptic = viewModel::testHaptic,
                                onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                onBack = { route = Route.HOME },
                            )
                        }
                        Route.EDGE_HAPTICS -> {
                            BackHandler { route = Route.HOME }
                            EdgeHapticsFlowHost(
                                edgeViewModel = edgeViewModel,
                                isServiceEnabled = isServiceEnabled,
                                onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                onBack = { route = Route.HOME },
                            )
                        }
                        Route.HOME, Route.SETTINGS -> {
                            val bottomTab =
                                if (route == Route.HOME) BottomTab.HOME else BottomTab.SETTINGS
                            SlidingBottomTabHost(
                                selectedTab = bottomTab,
                                modifier = Modifier.fillMaxSize(),
                            ) { tab ->
                                when (tab) {
                                    BottomTab.HOME -> HomeScreen(
                                        onOpenFeelEveryTap = { route = Route.FEEL_EVERY_TAP },
                                        onOpenEdgeHaptics = { route = Route.EDGE_HAPTICS },
                                        onOpenTactileScrolling = { route = Route.TACTILE_SCROLLING },
                                    )
                                    BottomTab.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        onUseDynamicColorsChange = viewModel::setUseDynamicColors,
                                        onThemeModeChange = viewModel::setThemeMode,
                                        onAmoledBlackChange = viewModel::setAmoledBlack,
                                    )
                                }
                            }
                        }
                        Route.TACTILE_SCROLLING -> {
                            BackHandler { route = Route.HOME }
                            ScrollHapticsScreen(
                                settings = settings,
                                isServiceEnabled = isServiceEnabled,
                                onScrollEnabledChange = viewModel::setScrollEnabled,
                                onScrollHapticDensityCommit = viewModel::commitScrollHapticDensity,
                                onIntensityCommit = viewModel::commitScrollIntensity,
                                onPatternSelected = viewModel::setScrollPattern,
                                onVibrationsPerEventCommit = viewModel::commitScrollVibrationsPerEvent,
                                onSpeedVibScaleCommit = viewModel::commitScrollSpeedVibScale,
                                onTailCutoffMsCommit = viewModel::commitScrollTailCutoffMs,
                                onTestHaptic = viewModel::testScrollHaptic,
                                onOpenAccessibilitySettings = ::openAccessibilitySettings,
                                onBack = { route = Route.HOME },
                            )
                        }
                    }

                    if ((route == Route.HOME) || (route == Route.SETTINGS)) {
                        FloatingBottomBar(
                            selectedTab = if (route == Route.HOME) BottomTab.HOME else BottomTab.SETTINGS,
                            onTabSelected = { tab ->
                                route = if (tab == BottomTab.HOME) Route.HOME else Route.SETTINGS
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
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

    private enum class Route { HOME, FEEL_EVERY_TAP, EDGE_HAPTICS, TACTILE_SCROLLING, SETTINGS }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}

@Composable
private fun EdgeHapticsFlowHost(
    edgeViewModel: EdgeHapticsViewModel,
    isServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onBack: () -> Unit,
) {
    val edgeSettings by edgeViewModel.settings.collectAsStateWithLifecycle()
    val edgeTestEvent by edgeViewModel.testEvent.collectAsStateWithLifecycle()
    val isLsposedXposedBridgeActive by edgeViewModel.isLsposedXposedBridgeActive.collectAsStateWithLifecycle()
    EdgeHapticsScreen(
        settings = edgeSettings,
        testEvent = edgeTestEvent,
        isServiceEnabled = isServiceEnabled,
        isLsposedXposedBridgeActive = isLsposedXposedBridgeActive,
        onA11yScrollBoundEdgeChange = edgeViewModel::setA11yScrollBoundEdge,
        onEdgeLsposedLibxposedPathChange = edgeViewModel::setEdgeLsposedLibxposedPath,
        onPatternSelected = edgeViewModel::setEdgePattern,
        onIntensityCommit = edgeViewModel::setEdgeIntensity,
        onTestEdgeHaptic = edgeViewModel::testEdgeHaptic,
        onTestEventConsumed = edgeViewModel::consumeTestEvent,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onBack = onBack,
    )
}
