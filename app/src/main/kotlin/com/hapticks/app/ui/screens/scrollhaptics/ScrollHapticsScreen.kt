package com.hapticks.app.ui.screens.scrollhaptics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.EnableServiceCard
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.PatternSelector
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalContentRow
import com.hapticks.app.ui.components.PetalExclusionsRow
import com.hapticks.app.ui.components.PetalFeatureHero
import com.hapticks.app.ui.components.PetalFeatureScreen
import com.hapticks.app.ui.components.PetalGroupPosition
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalIntensityRow
import com.hapticks.app.ui.components.PetalResetFooter
import com.hapticks.app.ui.components.PetalSection
import com.hapticks.app.ui.components.PetalSliderRow
import com.hapticks.app.ui.components.PetalTestRow
import com.hapticks.app.ui.components.entrance
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.theme.PetalMaterialShapes
import kotlin.math.roundToInt

// Linear scale: left = 1cm (frequent), right = 15cm (sparse) - 15 uniform positions
private const val SLIDER_MIN_CM = 1f
private const val SLIDER_MAX_CM = 15f
private const val SLIDER_STEPS = 13   // 13 intermediate ticks -> 15 total positions

private fun distanceCmToSlider(distanceCm: Float): Float =
    ((distanceCm - SLIDER_MIN_CM) / (SLIDER_MAX_CM - SLIDER_MIN_CM)).coerceIn(0f, 1f)

private fun sliderToDistanceCm(s: Float): Float =
    (SLIDER_MIN_CM + s * (SLIDER_MAX_CM - SLIDER_MIN_CM)).coerceIn(SLIDER_MIN_CM, SLIDER_MAX_CM)

private fun eventsPerCmToDistanceCm(v: Float): Float = (1f / v).coerceIn(SLIDER_MIN_CM, SLIDER_MAX_CM)
private fun distanceCmToEventsPerCm(d: Float): Float = (1f / d).coerceIn(0.01f, 10f)

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
    PetalFeatureScreen(title = stringResource(R.string.scroll_haptics_title), onBack = onBack, modifier = modifier) {
        if (!isServiceEnabled) {
            item("service_card") { EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings) }
        }
        item("hero") {
            PetalFeatureHero(
                icon = PetalIcons.Motion,
                tint = FeatureColors.TactileScrolling,
                subtitle = "Feel every scroll tick - tune how often and how strong.",
                modifier = Modifier.entrance(0),
            )
        }
        item("toggles") {
            PetalSection(title = "Scrolling", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.Motion,
                        title = stringResource(R.string.scroll_toggle_title),
                        subtitle = stringResource(R.string.scroll_toggle_subtitle),
                        checked = settings.scrollEnabled,
                        onCheckedChange = onScrollEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                    )
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Swipe,
                        title = stringResource(R.string.scroll_horizontal_toggle_title),
                        subtitle = stringResource(R.string.scroll_horizontal_toggle_subtitle),
                        checked = settings.scrollHorizontalEnabled,
                        onCheckedChange = onScrollHorizontalEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        variant = PetalBadgeVariant.SECONDARY,
                        enabled = settings.scrollEnabled,
                    )
                }
                row { position ->
                    PetalExclusionsRow(
                        excludedCount = settings.scrollExcludedPackages.size,
                        onClick = onOpenAppExclusions,
                        position = position,
                        title = stringResource(R.string.app_exclusions_row_title),
                        subtitleNone = stringResource(R.string.app_exclusions_row_subtitle_none),
                        subtitleSome = stringResource(R.string.app_exclusions_row_subtitle_some, settings.scrollExcludedPackages.size),
                    )
                }
            }
        }
        item("tuning") {
            PetalSection(
                title = "Feel",
                description = "Spacing and strength of every scroll pulse",
                modifier = Modifier.entrance(2),
            ) {
                row { position ->
                    ScrollDistanceRow(settings.scrollHapticEventsPerCm, onScrollHapticEventsPerCmCommit, position, settings.scrollEnabled)
                }
                row { position ->
                    PetalIntensityRow(
                        intensity = settings.scrollIntensity,
                        onIntensityCommit = onIntensityCommit,
                        position = position,
                        title = stringResource(R.string.intensity_label),
                        enabled = settings.scrollEnabled,
                    )
                }
            }
        }
        item("pattern") {
            PetalSection(title = "Pattern", modifier = Modifier.entrance(3)) {
                row { position ->
                    PetalContentRow(position = position, enabled = settings.scrollEnabled) {
                        PatternSelector(settings.scrollPattern, onPatternSelected)
                    }
                }
                row { position ->
                    PetalTestRow(
                        label = stringResource(R.string.scroll_haptic_screen_test_button),
                        onClick = onTestHaptic,
                        position = position,
                        enabled = settings.scrollEnabled,
                    )
                }
            }
        }
        item("reset") {
            PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(4))
        }
    }
}

@Composable
private fun ScrollDistanceRow(
    eventsPerCm: Float,
    onCommit: (Float) -> Unit,
    position: PetalGroupPosition,
    enabled: Boolean,
) {
    val ctx = LocalContext.current
    val distanceCm = eventsPerCmToDistanceCm(eventsPerCm)
    var sliderVal by remember(distanceCm) { mutableFloatStateOf(distanceCmToSlider(distanceCm)) }
    var lastTick by remember { mutableIntStateOf(0) }
    val displayCm = sliderToDistanceCm(sliderVal).roundToInt()
    PetalSliderRow(
        icon = PetalIcons.Motion,
        title = "Haptic interval",
        subtitle = "Distance scrolled between haptic pulses",
        valueLabel = "$displayCm cm",
        value = sliderVal,
        onValueChange = { v ->
            sliderVal = v
            val t = (v * SLIDER_STEPS).toInt()
            if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() }
        },
        onValueChangeFinished = { onCommit(distanceCmToEventsPerCm(sliderToDistanceCm(sliderVal))) },
        position = position,
        steps = SLIDER_STEPS,
        enabled = enabled,
        badgeShape = PetalMaterialShapes.Cookie9Sided,
        variant = PetalBadgeVariant.SECONDARY,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1 cm  (frequent)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("15 cm  (sparse)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
