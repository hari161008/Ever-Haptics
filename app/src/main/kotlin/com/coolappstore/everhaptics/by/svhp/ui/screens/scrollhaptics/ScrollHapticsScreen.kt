package com.coolappstore.everhaptics.by.svhp.ui.screens.scrollhaptics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticTestButton
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticToggleRow
import com.coolappstore.everhaptics.by.svhp.ui.components.PatternSelector
import com.coolappstore.everhaptics.by.svhp.ui.components.SectionCard
import com.coolappstore.everhaptics.by.svhp.ui.haptics.SliderTickStepsDefault
import com.coolappstore.everhaptics.by.svhp.ui.haptics.performHapticSliderTick
import com.coolappstore.everhaptics.by.svhp.ui.haptics.slider01ToTickIndex
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollHapticsScreen(
    settings: HapticsSettings,
    isServiceEnabled: Boolean,
    onScrollEnabledChange: (Boolean) -> Unit,
    onScrollHorizontalEnabledChange: (Boolean) -> Unit,
    onScrollHapticEventsPerCmCommit: (Float) -> Unit,
    onIntensityEnabledChange: (Boolean) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onVibrationsPerEventEnabledChange: (Boolean) -> Unit,
    onVibrationsPerEventCommit: (Float) -> Unit,
    onSpeedVibEnabledChange: (Boolean) -> Unit,
    onSpeedVibScaleCommit: (Float) -> Unit,
    onTailCutoffEnabledChange: (Boolean) -> Unit,
    onTailCutoffMsCommit: (Int) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenAppExclusions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.scroll_haptics_title), style = MaterialTheme.typography.displaySmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onSurface) } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "scroll_toggle_section") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(id = R.string.scroll_toggle_title),
                        subtitle = stringResource(id = R.string.scroll_toggle_subtitle),
                        checked = settings.scrollEnabled,
                        onCheckedChange = onScrollEnabledChange,
                        leadingIcon = Icons.Rounded.SwipeVertical,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    HapticToggleRow(
                        title = stringResource(id = R.string.scroll_horizontal_toggle_title),
                        subtitle = stringResource(id = R.string.scroll_horizontal_toggle_subtitle),
                        checked = settings.scrollHorizontalEnabled,
                        onCheckedChange = onScrollHorizontalEnabledChange,
                        leadingIcon = Icons.Rounded.Swipe,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    AppExclusionRow(excludedCount = settings.scrollExcludedPackages.size, onClick = onOpenAppExclusions)
                }
            }

            item(key = "scroll_sliders_reset") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Haptic Events in Centimeters (text input, no toggle)
            item(key = "scroll_cm_section") {
                SectionCard {
                    HapticEventsInCmControl(eventsPerCm = settings.scrollHapticEventsPerCm, onCommit = onScrollHapticEventsPerCmCommit)
                }
            }

            // Pulse Strength with toggle
            item(key = "scroll_intensity_section") {
                SectionCard {
                    FeatureToggleHeader(
                        title = stringResource(id = R.string.scroll_intensity_title),
                        enabled = settings.scrollIntensityEnabled,
                        onEnabledChange = onIntensityEnabledChange,
                    )
                    if (settings.scrollIntensityEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        IntensityControl(intensity = settings.scrollIntensity, onIntensityCommit = onIntensityCommit)
                    }
                }
            }

            // Vibrations per scroll with toggle
            item(key = "scroll_vibs_section") {
                SectionCard {
                    FeatureToggleHeader(
                        title = stringResource(id = R.string.scroll_vibs_per_event_title),
                        enabled = settings.scrollVibrationsPerEventEnabled,
                        onEnabledChange = onVibrationsPerEventEnabledChange,
                    )
                    if (settings.scrollVibrationsPerEventEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        VibsPerEventControl(value = settings.scrollVibrationsPerEvent, onCommit = onVibrationsPerEventCommit)
                    }
                }
            }

            // Speed-based vibrations with toggle
            item(key = "scroll_speed_section") {
                SectionCard {
                    FeatureToggleHeader(
                        title = stringResource(id = R.string.scroll_speed_vib_scale_title),
                        enabled = settings.scrollSpeedVibrationEnabled,
                        onEnabledChange = onSpeedVibEnabledChange,
                    )
                    if (settings.scrollSpeedVibrationEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        SpeedVibScaleControl(value = settings.scrollSpeedVibrationScale, onCommit = onSpeedVibScaleCommit)
                    }
                }
            }

            // Cut off after scroll ends with toggle
            item(key = "scroll_tail_cutoff_section") {
                SectionCard {
                    FeatureToggleHeader(
                        title = stringResource(id = R.string.scroll_tail_cutoff_title),
                        enabled = settings.scrollTailCutoffEnabled,
                        onEnabledChange = onTailCutoffEnabledChange,
                    )
                    if (settings.scrollTailCutoffEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        TailCutoffControl(valueMs = settings.scrollTailCutoffMs, onCommit = onTailCutoffMsCommit)
                    }
                }
            }

            item(key = "scroll_pattern_section") {
                SectionCard {
                    PatternSelector(selected = settings.scrollPattern, onPatternSelected = onPatternSelected)
                }
            }

            item(key = "scroll_test") {
                HapticTestButton(
                    label = stringResource(id = R.string.scroll_haptic_screen_test_button),
                    enabled = settings.scrollEnabled,
                    onClick = onTestHaptic,
                )
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun FeatureToggleHeader(title: String, enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun HapticEventsInCmControl(eventsPerCm: Float, onCommit: (Float) -> Unit) {
    val focusManager = LocalFocusManager.current
    var text by remember(eventsPerCm) { mutableStateOf(String.format(Locale.US, "%.1f", eventsPerCm)) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(id = R.string.scroll_events_per_cm_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = stringResource(id = R.string.scroll_events_per_cm_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text(text = "cm", style = MaterialTheme.typography.bodyMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val parsed = text.toFloatOrNull()
                if (parsed != null) {
                    val clamped = parsed.coerceIn(HapticsSettings.MIN_SCROLL_EVENTS_PER_CM, HapticsSettings.MAX_SCROLL_EVENTS_PER_CM)
                    text = String.format(Locale.US, "%.1f", clamped)
                    onCommit(clamped)
                } else {
                    text = String.format(Locale.US, "%.1f", eventsPerCm)
                }
                focusManager.clearFocus()
            }),
            singleLine = true,
            label = { Text(text = stringResource(R.string.scroll_events_per_cm_label)) },
        )
    }
}

@Composable
private fun AppExclusionRow(excludedCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = Icons.Rounded.AppBlocking, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.app_exclusions_row_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (excludedCount == 0) stringResource(R.string.app_exclusions_row_subtitle_none)
                else stringResource(R.string.app_exclusions_row_subtitle_some, excludedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IntensityControl(intensity: Float, onIntensityCommit: (Float) -> Unit) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTickIndex by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    val percent = (draft * 100f).roundToInt()
    val sliderColors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.scroll_intensity_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IntensityBadge(percent = percent)
        }
        Slider(
            value = draft,
            onValueChange = { newValue ->
                draft = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) { lastTickIndex = tickIndex; context.performHapticSliderTick() }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = sliderColors,
        )
    }
}

@Composable
private fun VibsPerEventControl(value: Float, onCommit: (Float) -> Unit) {
    val context = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTickIndex by remember(value) { mutableIntStateOf(slider01ToTickIndex(vibsPerEventToSlider(value))) }
    val displayCount = draft.roundToInt().coerceIn(1, 3)
    val sliderColors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.scroll_vibs_per_event_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                Text(text = stringResource(id = R.string.scroll_vibs_per_event_value, displayCount), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = vibsPerEventToSlider(draft),
            onValueChange = { sliderVal ->
                val newValue = sliderToVibsPerEvent(sliderVal)
                draft = newValue
                val tickIndex = slider01ToTickIndex(sliderVal)
                if (tickIndex != lastTickIndex) { lastTickIndex = tickIndex; context.performHapticSliderTick() }
            },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = 0f..1f, steps = 1, colors = sliderColors,
        )
    }
}

@Composable
private fun SpeedVibScaleControl(value: Float, onCommit: (Float) -> Unit) {
    val context = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTickIndex by remember(value) { mutableIntStateOf(slider01ToTickIndex(value)) }
    val percent = (draft * 100f).roundToInt()
    val sliderColors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.scroll_speed_vib_scale_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                Text(text = stringResource(id = R.string.scroll_speed_vib_scale_value, percent), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = draft,
            onValueChange = { newValue ->
                draft = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) { lastTickIndex = tickIndex; context.performHapticSliderTick() }
            },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = sliderColors,
        )
    }
}

@Composable
private fun TailCutoffControl(valueMs: Int, onCommit: (Int) -> Unit) {
    val context = LocalContext.current
    var draftSlider by remember(valueMs) { mutableFloatStateOf(tailCutoffMsToSlider(valueMs)) }
    var lastTickIndex by remember(valueMs) { mutableIntStateOf(slider01ToTickIndex(tailCutoffMsToSlider(valueMs))) }
    val draftMs = sliderToTailCutoffMs(draftSlider)
    val sliderColors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.error, activeTrackColor = MaterialTheme.colorScheme.error, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.scroll_tail_cutoff_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) {
                val labelText = if (draftMs <= 0) stringResource(id = R.string.scroll_tail_cutoff_value_off) else stringResource(id = R.string.scroll_tail_cutoff_value_ms, draftMs)
                Text(text = labelText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = draftSlider,
            onValueChange = { newValue ->
                draftSlider = newValue
                val tickIndex = slider01ToTickIndex(newValue)
                if (tickIndex != lastTickIndex) { lastTickIndex = tickIndex; context.performHapticSliderTick() }
            },
            onValueChangeFinished = { onCommit(sliderToTailCutoffMs(draftSlider)) },
            valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = sliderColors,
        )
    }
}

private fun vibsPerEventToSlider(value: Float): Float {
    val clamped = value.coerceIn(1f, 3f)
    return ((clamped - 1f) / 2f).coerceIn(0f, 1f)
}

private fun sliderToVibsPerEvent(slider: Float): Float = 1f + slider.coerceIn(0f, 1f) * 2f

private fun tailCutoffMsToSlider(ms: Int): Float {
    val clamped = ms.coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS)
    return (clamped.toFloat() / HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS.toFloat()).coerceIn(0f, 1f)
}

private fun sliderToTailCutoffMs(slider: Float): Int =
    (slider.coerceIn(0f, 1f) * HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS).roundToInt()
        .coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS)

@Composable
private fun IntensityBadge(percent: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
        Text(text = stringResource(id = R.string.intensity_value, percent), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}
