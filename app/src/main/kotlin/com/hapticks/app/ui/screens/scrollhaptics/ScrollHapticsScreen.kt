package com.hapticks.app.ui.screens.scrollhaptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.ScreenIconHeader
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

// Linear scale: left = 1cm (frequent), right = 15cm (sparse) — 15 uniform positions
private const val SLIDER_MIN_CM = 1f
private const val SLIDER_MAX_CM = 15f
private const val SLIDER_STEPS = 13   // 13 intermediate ticks → 15 total positions

private fun distanceCmToSlider(distanceCm: Float): Float =
    ((distanceCm - SLIDER_MIN_CM) / (SLIDER_MAX_CM - SLIDER_MIN_CM)).coerceIn(0f, 1f)

private fun sliderToDistanceCm(s: Float): Float =
    (SLIDER_MIN_CM + s * (SLIDER_MAX_CM - SLIDER_MIN_CM)).coerceIn(SLIDER_MIN_CM, SLIDER_MAX_CM)

private fun eventsPerCmToDistanceCm(v: Float): Float = (1f / v).coerceIn(SLIDER_MIN_CM, SLIDER_MAX_CM)
private fun distanceCmToEventsPerCm(d: Float): Float = (1f / d).coerceIn(0.01f, 10f)

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
                item("service_card") { EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings) }
            }
            item("icon_header") {
                ScreenIconHeader(icon = Icons.Rounded.SwipeUp, featureColor = FeatureColors.TactileScrolling, subtitle = "Feel every scroll tick — tune how often and how strong.")
            }
            item("toggles") {
                SectionCard {
                    HapticToggleRow(stringResource(R.string.scroll_toggle_title), stringResource(R.string.scroll_toggle_subtitle), settings.scrollEnabled, onScrollEnabledChange)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    HapticToggleRow(stringResource(R.string.scroll_horizontal_toggle_title), stringResource(R.string.scroll_horizontal_toggle_subtitle), settings.scrollHorizontalEnabled, onScrollHorizontalEnabledChange)
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
            item("distance") {
                SectionCard { ScrollDistanceControl(settings.scrollHapticEventsPerCm, onScrollHapticEventsPerCmCommit) }
            }
            item("intensity") {
                SectionCard { ScrollIntensityControl(settings.scrollIntensity, onIntensityCommit) }
            }
            item("pattern") {
                SectionCard { PatternSelector(settings.scrollPattern, onPatternSelected) }
            }
            item("test") {
                HapticTestButton(stringResource(R.string.scroll_haptic_screen_test_button), onTestHaptic, enabled = settings.scrollEnabled)
            }
            item("bottom") { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun ScrollDistanceControl(eventsPerCm: Float, onCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    val distanceCm = eventsPerCmToDistanceCm(eventsPerCm)
    var sliderVal by remember(distanceCm) { mutableFloatStateOf(distanceCmToSlider(distanceCm)) }
    var lastTick by remember { mutableIntStateOf(0) }
    val displayCm = sliderToDistanceCm(sliderVal).roundToInt()
    val label = "$displayCm cm"
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Haptic interval", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Distance scrolled between haptic pulses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = sliderVal,
            onValueChange = { v ->
                sliderVal = v
                val t = (v * SLIDER_STEPS).toInt()
                if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() }
            },
            onValueChangeFinished = { onCommit(distanceCmToEventsPerCm(sliderToDistanceCm(sliderVal))) },
            valueRange = 0f..1f,
            steps = SLIDER_STEPS,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1 cm  (frequent)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("15 cm  (sparse)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScrollIntensityControl(intensity: Float, onCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.intensity_label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(stringResource(R.string.intensity_value, (draft * 100f).roundToInt()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onCommit(draft) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
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
