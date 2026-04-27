package com.hapticks.app.ui.screens.scrollhaptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import com.hapticks.app.ui.components.EnableServiceCard
import com.hapticks.app.ui.components.HapticTestButton
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.PatternSelector
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollHapticsScreen(
    settings: HapticsSettings,
    isServiceEnabled: Boolean,
    onScrollEnabledChange: (Boolean) -> Unit,
    onScrollHapticDensityCommit: (Float) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onVibrationsPerEventCommit: (Float) -> Unit,
    onSpeedVibScaleCommit: (Float) -> Unit,
    onTailCutoffMsCommit: (Int) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppExclusions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.scroll_haptics_title),
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
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (!isServiceEnabled) {
                item(key = "enable_service") {
                    EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
                }
            }

            item(key = "scroll_toggle_section") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(id = R.string.scroll_toggle_title),
                        subtitle = stringResource(id = R.string.scroll_toggle_subtitle),
                        checked = settings.scrollEnabled,
                        onCheckedChange = onScrollEnabledChange,
                        leadingIcon = Icons.Rounded.SwipeVertical,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    AppExclusionRow(
                        excludedCount = settings.scrollExcludedPackages.size,
                        onClick = onOpenAppExclusions,
                    )
                }
            }

            item(key = "scroll_sliders_reset") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.reset_to_defaults),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            item(key = "scroll_vib_count_section") {
                SectionCard {
                    ScrollPulseDensityControl(
                        eventsPerHundredPx = settings.scrollHapticEventsPerHundredPx,
                        onCommit = onScrollHapticDensityCommit,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    IntensityControl(
                        intensity = settings.scrollIntensity,
                        onIntensityCommit = onIntensityCommit,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    VibsPerEventControl(
                        value = settings.scrollVibrationsPerEvent,
                        onCommit = onVibrationsPerEventCommit,
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    SpeedVibScaleControl(
                        value = settings.scrollSpeedVibrationScale,
                        onCommit = onSpeedVibScaleCommit,
                    )
                }
            }

            item(key = "scroll_tail_cutoff_section") {
                SectionCard {
                    TailCutoffControl(
                        valueMs = settings.scrollTailCutoffMs,
                        onCommit = onTailCutoffMsCommit,
                    )
                }
            }

            item(key = "scroll_pattern_section") {
                SectionCard {
                    PatternSelector(
                        selected = settings.scrollPattern,
                        onPatternSelected = onPatternSelected,
                    )
                }
            }

            item(key = "scroll_test") {
                HapticTestButton(
                    label = stringResource(id = R.string.scroll_haptic_screen_test_button),
                    enabled = settings.scrollEnabled,
                    onClick = onTestHaptic,
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AppExclusionRow(excludedCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AppBlocking,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_exclusions_row_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (excludedCount == 0) {
                    stringResource(R.string.app_exclusions_row_subtitle_none)
                } else {
                    stringResource(R.string.app_exclusions_row_subtitle_some, excludedCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
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

private fun scrollDensitySliderToEvents(slider01: Float): Float {
    val t = slider01.coerceIn(0f, 1f)
    return HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX +
        t * (HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX - HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX)
}

private fun eventsToScrollDensitySlider(eventsPerHundredPx: Float): Float {
    val e = eventsPerHundredPx.coerceIn(
        HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX,
        HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX,
    )
    return ((e - HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX) /
        (HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX - HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX))
        .coerceIn(0f, 1f)
}

@Composable
private fun ScrollPulseDensityControl(
    eventsPerHundredPx: Float,
    onCommit: (Float) -> Unit,
) {
    val context = LocalContext.current
    val initialSlider = eventsToScrollDensitySlider(eventsPerHundredPx)
    var draftSlider by remember(eventsPerHundredPx) {
        mutableFloatStateOf(initialSlider)
    }
    var lastTickIndex by remember(eventsPerHundredPx) {
        mutableIntStateOf(slider01ToTickIndex(initialSlider))
    }
    val draftEvents = scrollDensitySliderToEvents(draftSlider)
    val eventsLabel = String.format(Locale.US, "%.2f", draftEvents)

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.secondary,
        activeTrackColor = MaterialTheme.colorScheme.secondary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.secondary,
        inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.scroll_events_per_unit_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = R.string.scroll_events_per_unit_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(id = R.string.scroll_events_per_unit_value, eventsLabel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = draftSlider,
            onValueChange = { newValue ->
                draftSlider = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) {
                    lastTickIndex = tickIndex
                    context.performHapticSliderTick()
                }
            },
            onValueChangeFinished = { onCommit(scrollDensitySliderToEvents(draftSlider)) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = sliderColors,
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.scroll_intensity_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IntensityBadge(percent = percent)
        }
        Text(
            text = stringResource(id = R.string.scroll_intensity_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun VibsPerEventControl(
    value: Float,
    onCommit: (Float) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTickIndex by remember(value) {
        mutableIntStateOf(slider01ToTickIndex(vibsPerEventToSlider(value)))
    }
    val displayCount = draft.roundToInt().coerceIn(
        HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT.toInt(),
        HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT.toInt(),
    )

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.tertiary,
        activeTrackColor = MaterialTheme.colorScheme.tertiary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.tertiary,
        inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.scroll_vibs_per_event_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(id = R.string.scroll_vibs_per_event_value, displayCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.scroll_vibs_per_event_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = vibsPerEventToSlider(draft),
            onValueChange = { sliderVal ->
                val newValue = sliderToVibsPerEvent(sliderVal)
                draft = newValue
                val tickIndex = slider01ToTickIndex(sliderVal)
                if (tickIndex != lastTickIndex) {
                    lastTickIndex = tickIndex
                    context.performHapticSliderTick()
                }
            },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = sliderColors,
        )
    }
}

@Composable
private fun SpeedVibScaleControl(
    value: Float,
    onCommit: (Float) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTickIndex by remember(value) {
        mutableIntStateOf(slider01ToTickIndex(value))
    }
    val percent = (draft * 100f).roundToInt()

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.secondary,
        activeTrackColor = MaterialTheme.colorScheme.secondary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.secondary,
        inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.scroll_speed_vib_scale_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = stringResource(id = R.string.scroll_speed_vib_scale_value, percent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.scroll_speed_vib_scale_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            onValueChangeFinished = { onCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = sliderColors,
        )
    }
}

@Composable
private fun TailCutoffControl(
    valueMs: Int,
    onCommit: (Int) -> Unit,
) {
    val context = LocalContext.current
    var draftSlider by remember(valueMs) {
        mutableFloatStateOf(tailCutoffMsToSlider(valueMs))
    }
    var lastTickIndex by remember(valueMs) {
        mutableIntStateOf(slider01ToTickIndex(tailCutoffMsToSlider(valueMs)))
    }
    val draftMs = sliderToTailCutoffMs(draftSlider)

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.error,
        activeTrackColor = MaterialTheme.colorScheme.error,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.error,
        inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.scroll_tail_cutoff_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ) {
                val labelText = if (draftMs <= 0) {
                    stringResource(id = R.string.scroll_tail_cutoff_value_off)
                } else {
                    stringResource(id = R.string.scroll_tail_cutoff_value_ms, draftMs)
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.scroll_tail_cutoff_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = draftSlider,
            onValueChange = { newValue ->
                draftSlider = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) {
                    lastTickIndex = tickIndex
                    context.performHapticSliderTick()
                }
            },
            onValueChangeFinished = { onCommit(sliderToTailCutoffMs(draftSlider)) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = sliderColors,
        )
    }
}

// --- Conversion helpers ---

private fun vibsPerEventToSlider(value: Float): Float {
    val clamped = value.coerceIn(
        HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
        HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
    )
    return ((clamped - HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT) /
        (HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT - HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT))
        .coerceIn(0f, 1f)
}

private fun sliderToVibsPerEvent(slider: Float): Float {
    return HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT +
        slider.coerceIn(0f, 1f) *
        (HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT - HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT)
}

private fun tailCutoffMsToSlider(ms: Int): Float {
    val clamped = ms.coerceIn(
        HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS,
        HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS,
    )
    return (clamped.toFloat() / HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS.toFloat()).coerceIn(0f, 1f)
}

private fun sliderToTailCutoffMs(slider: Float): Int {
    return (slider.coerceIn(0f, 1f) * HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS).roundToInt()
        .coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS)
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
