package com.coolappstore.everhaptics.by.svhp.ui.screens.buttonhaptics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VolumeUp
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
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticTestButton
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticToggleRow
import com.coolappstore.everhaptics.by.svhp.ui.components.PatternSelector
import com.coolappstore.everhaptics.by.svhp.ui.components.SectionCard
import com.coolappstore.everhaptics.by.svhp.ui.haptics.SliderTickStepsDefault
import com.coolappstore.everhaptics.by.svhp.ui.haptics.performHapticSliderTick
import com.coolappstore.everhaptics.by.svhp.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonHapticsScreen(
    settings: HapticsSettings,
    onVolumeHapticEnabledChange: (Boolean) -> Unit,
    onVolumePatternSelected: (HapticPattern) -> Unit,
    onVolumeIntensityCommit: (Float) -> Unit,
    onPowerHapticEnabledChange: (Boolean) -> Unit,
    onPowerPatternSelected: (HapticPattern) -> Unit,
    onPowerIntensityCommit: (Float) -> Unit,
    onBrightnessHapticEnabledChange: (Boolean) -> Unit,
    onBrightnessPatternSelected: (HapticPattern) -> Unit,
    onBrightnessIntensityCommit: (Float) -> Unit,
    onTestVolumeHaptic: () -> Unit,
    onTestPowerHaptic: () -> Unit,
    onTestBrightnessHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
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
                title = {
                    Text(
                        text = stringResource(R.string.button_haptics_title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "reset_row") {
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
                        Text(text = stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Volume
            item(key = "volume_section") {
                SectionCard(title = stringResource(R.string.button_haptics_volume_section), icon = Icons.Rounded.VolumeUp) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_volume_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_volume_toggle_subtitle),
                        checked = settings.volumeHapticEnabled,
                        onCheckedChange = onVolumeHapticEnabledChange,
                        leadingIcon = Icons.Rounded.VolumeUp,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    IntensityControl(
                        intensity = settings.volumeHapticIntensity,
                        onIntensityCommit = onVolumeIntensityCommit,
                        color = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelector(selected = settings.volumeHapticPattern, onPatternSelected = onVolumePatternSelected)
                }
            }

            item(key = "volume_test") {
                HapticTestButton(
                    label = stringResource(R.string.button_haptics_volume_test),
                    enabled = settings.volumeHapticEnabled,
                    onClick = onTestVolumeHaptic,
                )
            }

            // Power
            item(key = "power_section") {
                SectionCard(title = stringResource(R.string.button_haptics_power_section), icon = Icons.Rounded.Power) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_power_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_power_toggle_subtitle),
                        checked = settings.powerHapticEnabled,
                        onCheckedChange = onPowerHapticEnabledChange,
                        leadingIcon = Icons.Rounded.Power,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    IntensityControl(
                        intensity = settings.powerHapticIntensity,
                        onIntensityCommit = onPowerIntensityCommit,
                        color = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelector(selected = settings.powerHapticPattern, onPatternSelected = onPowerPatternSelected)
                }
            }

            item(key = "power_test") {
                HapticTestButton(
                    label = stringResource(R.string.button_haptics_power_test),
                    enabled = settings.powerHapticEnabled,
                    onClick = onTestPowerHaptic,
                )
            }

            // Brightness
            item(key = "brightness_section") {
                SectionCard(title = stringResource(R.string.button_haptics_brightness_section), icon = Icons.Rounded.Brightness6) {
                    HapticToggleRow(
                        title = stringResource(R.string.button_haptics_brightness_toggle_title),
                        subtitle = stringResource(R.string.button_haptics_brightness_toggle_subtitle),
                        checked = settings.brightnessHapticEnabled,
                        onCheckedChange = onBrightnessHapticEnabledChange,
                        leadingIcon = Icons.Rounded.Brightness6,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    IntensityControl(
                        intensity = settings.brightnessHapticIntensity,
                        onIntensityCommit = onBrightnessIntensityCommit,
                        color = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelector(selected = settings.brightnessHapticPattern, onPatternSelected = onBrightnessPatternSelected)
                }
            }

            item(key = "brightness_test") {
                HapticTestButton(
                    label = stringResource(R.string.button_haptics_brightness_test),
                    enabled = settings.brightnessHapticEnabled,
                    onClick = onTestBrightnessHaptic,
                )
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.size(4.dp)) }
        }
    }
}

@Composable
private fun IntensityControl(
    intensity: Float,
    onIntensityCommit: (Float) -> Unit,
    color: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color,
) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTickIndex by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    val percent = (draft * 100f).roundToInt()

    val sliderColors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.intensity_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(color = containerColor, shape = CircleShape) {
                Text(
                    text = stringResource(R.string.intensity_value, percent),
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainerColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
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
