package com.hapticks.app.ui.screens.buttonhaptics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.ScreenIconHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonHapticsScreen(
    settings: HapticsSettings,
    onVolumeHapticEnabledChange: (Boolean) -> Unit,
    onVolumePatternSelected: (HapticPattern) -> Unit,
    onVolumeIntensityCommit: (Float) -> Unit,
    onVolumeOpenCustomEditor: () -> Unit,
    onPowerHapticEnabledChange: (Boolean) -> Unit,
    onPowerPatternSelected: (HapticPattern) -> Unit,
    onPowerIntensityCommit: (Float) -> Unit,
    onPowerOpenCustomEditor: () -> Unit,
    onBrightnessHapticEnabledChange: (Boolean) -> Unit,
    onBrightnessPatternSelected: (HapticPattern) -> Unit,
    onBrightnessIntensityCommit: (Float) -> Unit,
    onBrightnessOpenCustomEditor: () -> Unit,
    onTestVolumeHaptic: () -> Unit,
    onTestPowerHaptic: () -> Unit,
    onTestBrightnessHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.button_haptics_title), style = MaterialTheme.typography.displaySmall) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("icon_header") {
                ScreenIconHeader(icon = Icons.Rounded.RadioButtonChecked, featureColor = FeatureColors.ButtonHaptics, subtitle = "Custom haptic patterns for volume, power button, and brightness slider interactions.")
            }
            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            // Volume
            item("volume_card") {
                SectionCard(title = stringResource(R.string.button_haptics_volume_section), icon = Icons.Rounded.VolumeUp, iconTint = FeatureColors.ButtonHaptics) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_volume_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_volume_toggle_subtitle),
                        checked = settings.volumeHapticEnabled,
                        onCheckedChange = onVolumeHapticEnabledChange,
                        leadingIcon = Icons.Rounded.VolumeUp,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.volumeHapticPattern,
                        intensity = settings.volumeHapticIntensity,
                        customSequence = settings.volumeHapticCustomSequence,
                        onPatternSelected = onVolumePatternSelected,
                        onIntensityCommit = onVolumeIntensityCommit,
                        onOpenCustomEditor = onVolumeOpenCustomEditor,
                    )
                }
            }
            item("volume_test") {
                HapticTestButton(stringResource(R.string.button_haptics_volume_test), onTestVolumeHaptic, enabled = settings.volumeHapticEnabled)
            }
            // Power
            item("power_card") {
                SectionCard(title = stringResource(R.string.button_haptics_power_section), icon = Icons.Rounded.Power, iconTint = FeatureColors.ButtonHaptics.copy(red = 0.95f)) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_power_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_power_toggle_subtitle),
                        checked = settings.powerHapticEnabled,
                        onCheckedChange = onPowerHapticEnabledChange,
                        leadingIcon = Icons.Rounded.Power,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.powerHapticPattern,
                        intensity = settings.powerHapticIntensity,
                        customSequence = settings.powerHapticCustomSequence,
                        onPatternSelected = onPowerPatternSelected,
                        onIntensityCommit = onPowerIntensityCommit,
                        onOpenCustomEditor = onPowerOpenCustomEditor,
                    )
                }
            }
            item("power_test") {
                HapticTestButton(stringResource(R.string.button_haptics_power_test), onTestPowerHaptic, enabled = settings.powerHapticEnabled)
            }
            // Brightness
            item("brightness_card") {
                SectionCard(title = stringResource(R.string.button_haptics_brightness_section), icon = Icons.Rounded.Brightness6, iconTint = FeatureColors.Charging) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_brightness_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_brightness_toggle_subtitle),
                        checked = settings.brightnessHapticEnabled,
                        onCheckedChange = onBrightnessHapticEnabledChange,
                        leadingIcon = Icons.Rounded.Brightness6,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.brightnessHapticPattern,
                        intensity = settings.brightnessHapticIntensity,
                        customSequence = settings.brightnessHapticCustomSequence,
                        onPatternSelected = onBrightnessPatternSelected,
                        onIntensityCommit = onBrightnessIntensityCommit,
                        onOpenCustomEditor = onBrightnessOpenCustomEditor,
                    )
                }
            }
            item("brightness_test") {
                HapticTestButton(stringResource(R.string.button_haptics_brightness_test), onTestBrightnessHaptic, enabled = settings.brightnessHapticEnabled)
            }
        }
    }
}
