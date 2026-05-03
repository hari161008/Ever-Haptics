package com.hapticks.app.ui.screens.scrollhaptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
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
    onOpenAccessibilitySettings: () -> Unit,
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.scroll_haptics_title), style = MaterialTheme.typography.displaySmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface) } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (!isServiceEnabled) {
                item("service_card") {
                    EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
                }
            }
            item("toggles") {
                SectionCard {
                    HapticToggleRow(stringResource(R.string.scroll_toggle_title), stringResource(R.string.scroll_toggle_subtitle), settings.scrollEnabled, onScrollEnabledChange, leadingIcon = Icons.Rounded.SwipeUp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    HapticToggleRow(stringResource(R.string.scroll_horizontal_toggle_title), stringResource(R.string.scroll_horizontal_toggle_subtitle), settings.scrollHorizontalEnabled, onScrollHorizontalEnabledChange, leadingIcon = Icons.Rounded.Swipe)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    AppExclusionRow(settings.scrollExcludedPackages.size, onOpenAppExclusions)
                }
            }
            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item("cm") {
                SectionCard { HapticEventsInCmControl(settings.scrollHapticEventsPerCm, onScrollHapticEventsPerCmCommit) }
            }
            item("intensity") {
                SectionCard {
                    FeatureToggleHeader(stringResource(R.string.scroll_intensity_title), settings.scrollIntensityEnabled, onIntensityEnabledChange)
                    if (settings.scrollIntensityEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        IntensitySlider(settings.scrollIntensity, onIntensityCommit, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item("vibs") {
                SectionCard {
                    FeatureToggleHeader(stringResource(R.string.scroll_vibs_per_event_title), settings.scrollVibrationsPerEventEnabled, onVibrationsPerEventEnabledChange)
                    if (settings.scrollVibrationsPerEventEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        VibsPerEventControl(settings.scrollVibrationsPerEvent, onVibrationsPerEventCommit)
                    }
                }
            }
            item("speed") {
                SectionCard {
                    FeatureToggleHeader(stringResource(R.string.scroll_speed_vib_scale_title), settings.scrollSpeedVibrationEnabled, onSpeedVibEnabledChange)
                    if (settings.scrollSpeedVibrationEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        SpeedSlider(settings.scrollSpeedVibrationScale, onSpeedVibScaleCommit)
                    }
                }
            }
            item("cutoff") {
                SectionCard {
                    FeatureToggleHeader(stringResource(R.string.scroll_tail_cutoff_title), settings.scrollTailCutoffEnabled, onTailCutoffEnabledChange)
                    if (settings.scrollTailCutoffEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        TailCutoffControl(settings.scrollTailCutoffMs, onTailCutoffMsCommit)
                    }
                }
            }
            item("pattern") { SectionCard { PatternSelector(settings.scrollPattern, onPatternSelected) } }
            item("test") { HapticTestButton(stringResource(R.string.scroll_haptic_screen_test_button), onTestHaptic, enabled = settings.scrollEnabled) }
            item("bottom") { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun FeatureToggleHeader(title: String, enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun HapticEventsInCmControl(eventsPerCm: Float, onCommit: (Float) -> Unit) {
    val focusManager = LocalFocusManager.current
    var text by remember(eventsPerCm) { mutableStateOf(String.format(Locale.US, "%.1f", eventsPerCm)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.scroll_events_per_cm_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.scroll_events_per_cm_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
            suffix = { Text("cm", style = MaterialTheme.typography.bodyMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val parsed = text.toFloatOrNull()
                if (parsed != null) { val c = parsed.coerceIn(HapticsSettings.MIN_SCROLL_EVENTS_PER_CM, HapticsSettings.MAX_SCROLL_EVENTS_PER_CM); text = String.format(Locale.US, "%.1f", c); onCommit(c) } else { text = String.format(Locale.US, "%.1f", eventsPerCm) }
                focusManager.clearFocus()
            }),
            singleLine = true, label = { Text(stringResource(R.string.scroll_events_per_cm_label)) },
        )
    }
}

@Composable
private fun AppExclusionRow(excludedCount: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Rounded.AppBlocking, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_exclusions_row_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(if (excludedCount == 0) stringResource(R.string.app_exclusions_row_subtitle_none) else stringResource(R.string.app_exclusions_row_subtitle_some, excludedCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun IntensitySlider(intensity: Float, onCommit: (Float) -> Unit, color: androidx.compose.ui.graphics.Color, container: androidx.compose.ui.graphics.Color, onContainer: androidx.compose.ui.graphics.Color) {
    val ctx = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.scroll_intensity_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = container, shape = CircleShape) { Text(stringResource(R.string.intensity_value, (draft * 100f).roundToInt()), style = MaterialTheme.typography.labelLarge, color = onContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
        Slider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onCommit(draft) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Composable
private fun VibsPerEventControl(value: Float, onCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTick by remember(value) { mutableIntStateOf(0) }
    val displayCount = draft.roundToInt().coerceIn(1, 3)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.scroll_vibs_per_event_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) { Text(stringResource(R.string.scroll_vibs_per_event_value, displayCount), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
        Slider(value = ((draft - 1f) / 2f).coerceIn(0f, 1f), onValueChange = { draft = 1f + it * 2f; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onCommit(draft) }, valueRange = 0f..1f, steps = 1, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Composable
private fun SpeedSlider(value: Float, onCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    var draft by remember(value) { mutableFloatStateOf(value) }
    var lastTick by remember(value) { mutableIntStateOf(slider01ToTickIndex(value)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.scroll_speed_vib_scale_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) { Text(stringResource(R.string.scroll_speed_vib_scale_value, (draft * 100f).roundToInt()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
        Slider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onCommit(draft) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

@Composable
private fun TailCutoffControl(valueMs: Int, onCommit: (Int) -> Unit) {
    val ctx = LocalContext.current
    val max = HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS.toFloat()
    var draft by remember(valueMs) { mutableFloatStateOf(valueMs.toFloat() / max) }
    var lastTick by remember(valueMs) { mutableIntStateOf(slider01ToTickIndex(valueMs.toFloat() / max)) }
    val draftMs = (draft * max).roundToInt().coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.scroll_tail_cutoff_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) { Text(if (draftMs <= 0) stringResource(R.string.scroll_tail_cutoff_value_off) else stringResource(R.string.scroll_tail_cutoff_value_ms, draftMs), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
        Slider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onCommit(draftMs) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.error, activeTrackColor = MaterialTheme.colorScheme.error, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}
