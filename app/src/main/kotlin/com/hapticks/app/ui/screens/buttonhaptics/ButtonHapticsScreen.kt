package com.hapticks.app.ui.screens.buttonhaptics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.theme.PetalMaterialShapes

@Composable
fun ButtonHapticsScreen(
    settings: HapticsSettings,
    onVolumeHapticEnabledChange: (Boolean) -> Unit,
    onVolumePatternSelected: (HapticPattern) -> Unit,
    onVolumeIntensityCommit: (Float) -> Unit,
    onVolumeOpenCustomEditor: () -> Unit,
    onVolumeClearCustomSequence: () -> Unit = {},
    onPowerHapticEnabledChange: (Boolean) -> Unit,
    onPowerPatternSelected: (HapticPattern) -> Unit,
    onPowerIntensityCommit: (Float) -> Unit,
    onPowerOpenCustomEditor: () -> Unit,
    onPowerClearCustomSequence: () -> Unit = {},
    onBrightnessHapticEnabledChange: (Boolean) -> Unit,
    onBrightnessPatternSelected: (HapticPattern) -> Unit,
    onBrightnessIntensityCommit: (Float) -> Unit,
    onBrightnessOpenCustomEditor: () -> Unit,
    onBrightnessClearCustomSequence: () -> Unit = {},
    onTestVolumeHaptic: () -> Unit,
    onTestPowerHaptic: () -> Unit,
    onTestBrightnessHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = stringResource(R.string.button_haptics_title), onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = Icons.Rounded.RadioButtonChecked,
                tint = FeatureColors.ButtonHaptics,
                subtitle = "Custom haptic patterns for volume, power button, and brightness slider interactions.",
                modifier = Modifier.entrance(0),
            )
        }
        item("volume") {
            PetalSection(title = stringResource(R.string.button_haptics_volume_section), modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = stringResource(R.string.button_haptics_volume_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_volume_toggle_subtitle),
                        checked = settings.volumeHapticEnabled,
                        onCheckedChange = onVolumeHapticEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie6Sided,
                        variant = PetalBadgeVariant.PRIMARY,
                    )
                }
                hapticTuningRows(
                    enabled = settings.volumeHapticEnabled,
                    pattern = settings.volumeHapticPattern,
                    intensity = settings.volumeHapticIntensity,
                    customSequence = settings.volumeHapticCustomSequence,
                    onPatternSelected = onVolumePatternSelected,
                    onIntensityCommit = onVolumeIntensityCommit,
                    onOpenCustomEditor = onVolumeOpenCustomEditor,
                    onClearCustomSequence = onVolumeClearCustomSequence,
                    testLabel = { stringResource(R.string.button_haptics_volume_test) },
                    onTest = onTestVolumeHaptic,
                )
            }
        }
        item("power") {
            PetalSection(title = stringResource(R.string.button_haptics_power_section), modifier = Modifier.entrance(2)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Power,
                        title = stringResource(R.string.button_haptics_power_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_power_toggle_subtitle),
                        checked = settings.powerHapticEnabled,
                        onCheckedChange = onPowerHapticEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        variant = PetalBadgeVariant.SECONDARY,
                    )
                }
                hapticTuningRows(
                    enabled = settings.powerHapticEnabled,
                    pattern = settings.powerHapticPattern,
                    intensity = settings.powerHapticIntensity,
                    customSequence = settings.powerHapticCustomSequence,
                    onPatternSelected = onPowerPatternSelected,
                    onIntensityCommit = onPowerIntensityCommit,
                    onOpenCustomEditor = onPowerOpenCustomEditor,
                    onClearCustomSequence = onPowerClearCustomSequence,
                    testLabel = { stringResource(R.string.button_haptics_power_test) },
                    onTest = onTestPowerHaptic,
                )
            }
        }
        item("brightness") {
            PetalSection(title = stringResource(R.string.button_haptics_brightness_section), modifier = Modifier.entrance(3)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.Brightness,
                        title = stringResource(R.string.button_haptics_brightness_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_brightness_toggle_subtitle),
                        checked = settings.brightnessHapticEnabled,
                        onCheckedChange = onBrightnessHapticEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                        variant = PetalBadgeVariant.TERTIARY,
                    )
                }
                hapticTuningRows(
                    enabled = settings.brightnessHapticEnabled,
                    pattern = settings.brightnessHapticPattern,
                    intensity = settings.brightnessHapticIntensity,
                    customSequence = settings.brightnessHapticCustomSequence,
                    onPatternSelected = onBrightnessPatternSelected,
                    onIntensityCommit = onBrightnessIntensityCommit,
                    onOpenCustomEditor = onBrightnessOpenCustomEditor,
                    onClearCustomSequence = onBrightnessClearCustomSequence,
                    testLabel = { stringResource(R.string.button_haptics_brightness_test) },
                    onTest = onTestBrightnessHaptic,
                )
            }
        }
        item("reset") { PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(4)) }
    }
}
