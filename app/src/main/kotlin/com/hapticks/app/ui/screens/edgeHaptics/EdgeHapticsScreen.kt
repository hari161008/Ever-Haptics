package com.hapticks.app.ui.screens.edgehaptics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.HapticTestButton
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.PatternSelector
import com.hapticks.app.ui.components.EnableServiceCard
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import com.hapticks.app.viewmodel.EdgeHapticsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeHapticsScreen(
    settings: HapticsSettings,
    testEvent: EdgeHapticsViewModel.TestEvent?,
    isServiceEnabled: Boolean,
    isLsposedXposedBridgeActive: Boolean,
    onA11yScrollBoundEdgeChange: (Boolean) -> Unit,
    onEdgeLsposedLibxposedPathChange: (Boolean) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onTestEdgeHaptic: () -> Unit,
    onTestEventConsumed: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val listState = rememberLazyListState()

    TestEventSnackbar(
        testEvent = testEvent,
        snackbarHostState = snackbarHostState,
        onConsumed = onTestEventConsumed,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.edge_screen_title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                },
                navigationIcon = { BackPill(onBack = onBack) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!isServiceEnabled) {
                item(key = "enable_service") {
                    EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
                }
            }

            if (!settings.a11yScrollBoundEdge && settings.edgeLsposedLibxposedPath) {
                item(key = "lsposed_runtime_status") {
                    LsposedRuntimeStatusCard(isActive = isLsposedXposedBridgeActive)
                }
            }

            item(key = "edge_toggles_section") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(id = R.string.edge_a11y_scroll_bound_title),
                        subtitle = stringResource(id = R.string.edge_a11y_scroll_bound_subtitle),
                        checked = settings.a11yScrollBoundEdge,
                        onCheckedChange = onA11yScrollBoundEdgeChange,
                        leadingIcon = Icons.Rounded.SwipeVertical,
                    )
                    HapticToggleRow(
                        title = stringResource(id = R.string.edge_lsposed_title),
                        subtitle = stringResource(id = R.string.edge_lsposed_subtitle),
                        checked = settings.edgeLsposedLibxposedPath,
                        onCheckedChange = onEdgeLsposedLibxposedPathChange,
                        leadingIcon = Icons.Rounded.Extension,
                    )
                    if (settings.edgeLsposedLibxposedPath) {
                        LsposedLibxposedSetupBlock(isLsposedXposedBridgeActive = isLsposedXposedBridgeActive)
                    } else if (settings.a11yScrollBoundEdge) {
                        A11yScrollBoundEdgeGuideBlock()
                    }
                    IntensityControl(
                        intensity = settings.edgeIntensity,
                        onIntensityCommit = onIntensityCommit,
                    )
                }
            }

            item(key = "edge_pattern_section") {
                SectionCard {
                    PatternSelector(
                        selected = settings.edgePattern,
                        onPatternSelected = onPatternSelected,
                    )
                }
            }

            item(key = "edge_test") {
                HapticTestButton(
                    label = stringResource(id = R.string.edge_test_button),
                    enabled = true,
                    onClick = onTestEdgeHaptic,
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun A11yScrollBoundEdgeGuideBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.edge_a11y_guide_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = R.string.edge_a11y_guide_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LsposedLibxposedSetupBlock(isLsposedXposedBridgeActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LsposedGuideSection(
            title = stringResource(id = R.string.edge_lsposed_what_title),
            body = stringResource(id = R.string.edge_lsposed_what_body),
        )
        LsposedGuideSection(
            title = stringResource(id = R.string.edge_lsposed_setup_title),
            body = stringResource(id = R.string.edge_lsposed_setup_body),
        )
    }
}

@Composable
private fun LsposedRuntimeStatusCard(isActive: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (isActive) {
        scheme.primaryContainer
    } else {
        scheme.surfaceContainerHigh
    }
    val iconBg = if (isActive) scheme.primary else scheme.tertiaryContainer
    val iconTint = if (isActive) scheme.onPrimary else scheme.onTertiaryContainer
    val onContainerMuted = if (isActive) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val headlineColor = if (isActive) scheme.onPrimaryContainer else scheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = iconBg,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.SentimentSatisfied else Icons.Filled.SentimentDissatisfied,
                        contentDescription = stringResource(
                            id = if (isActive) {
                                R.string.edge_lsposed_status_icon_active_cd
                            } else {
                                R.string.edge_lsposed_status_icon_inactive_cd
                            },
                        ),
                        modifier = Modifier.size(36.dp),
                        tint = iconTint,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.edge_lsposed_status_overline),
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainerMuted,
                )
                Text(
                    text = stringResource(
                        id = if (isActive) R.string.edge_lsposed_status_active else R.string.edge_lsposed_status_inactive,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = headlineColor,
                )
                Text(
                    text = stringResource(
                        id = if (isActive) {
                            R.string.edge_lsposed_status_active_body
                        } else {
                            R.string.edge_lsposed_status_inactive_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainerMuted,
                )
            }
        }
    }
}

@Composable
private fun LsposedGuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntensityControl(
    intensity: Float,
    onIntensityCommit: (Float) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTickIndex by remember(intensity) {
        mutableIntStateOf(slider01ToTickIndex(intensity))
    }
    val percent = (draft * 100f).roundToInt()

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.primary,
        inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.intensity_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IntensityBadge(percent = percent)
        }
        Slider(
            value = draft,
            onValueChange = { newValue ->
                draft = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) {
                    lastTickIndex = tickIndex
                    context.performHapticSliderTick()
                }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = sliderColors,
        )
    }
}

@Composable
private fun IntensityBadge(percent: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
    ) {
        Text(
            text = stringResource(id = R.string.intensity_value, percent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun TestEventSnackbar(
    testEvent: EdgeHapticsViewModel.TestEvent?,
    snackbarHostState: SnackbarHostState,
    onConsumed: () -> Unit,
) {
    val noVibratorLabel = stringResource(id = R.string.edge_test_no_vibrator)

    LaunchedEffect(testEvent) {
        when (testEvent) {
            null -> return@LaunchedEffect
            EdgeHapticsViewModel.TestEvent.NoVibrator -> snackbarHostState.showSnackbar(noVibratorLabel)
            EdgeHapticsViewModel.TestEvent.Fired -> Unit
        }
        onConsumed()
    }
}

@Composable
private fun BackPill(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.back),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
