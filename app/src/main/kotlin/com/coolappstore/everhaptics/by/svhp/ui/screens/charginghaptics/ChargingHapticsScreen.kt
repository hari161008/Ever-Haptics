package com.coolappstore.everhaptics.by.svhp.ui.screens.charginghaptics

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticTestButton
import com.coolappstore.everhaptics.by.svhp.ui.components.HapticToggleRow
import com.coolappstore.everhaptics.by.svhp.ui.components.SectionCard
import com.coolappstore.everhaptics.by.svhp.ui.haptics.SliderTickStepsDefault
import com.coolappstore.everhaptics.by.svhp.ui.haptics.performHapticSliderTick
import com.coolappstore.everhaptics.by.svhp.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingHapticsScreen(
    settings: HapticsSettings,
    onChargingVibEnabledChange: (Boolean) -> Unit,
    onChargingVibOnConnectChange: (Boolean) -> Unit,
    onChargingVibOnDisconnectChange: (Boolean) -> Unit,
    onDurationIndexChange: (Int) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onTestHaptic: () -> Unit,
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
                title = { Text(text = stringResource(R.string.charging_haptics_title), style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
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
            item(key = "toggle_section") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(R.string.charging_vib_toggle_title),
                        subtitle = stringResource(R.string.charging_vib_toggle_subtitle),
                        checked = settings.chargingVibEnabled,
                        onCheckedChange = onChargingVibEnabledChange,
                        leadingIcon = Icons.Rounded.ElectricBolt,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    HapticToggleRow(
                        title = stringResource(R.string.charging_vib_on_connect_title),
                        subtitle = stringResource(R.string.charging_vib_on_connect_subtitle),
                        checked = settings.chargingVibOnConnect,
                        onCheckedChange = onChargingVibOnConnectChange,
                        leadingIcon = Icons.Rounded.BatteryChargingFull,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    HapticToggleRow(
                        title = stringResource(R.string.charging_vib_on_disconnect_title),
                        subtitle = stringResource(R.string.charging_vib_on_disconnect_subtitle),
                        checked = settings.chargingVibOnDisconnect,
                        onCheckedChange = onChargingVibOnDisconnectChange,
                        leadingIcon = Icons.Rounded.BatteryFull,
                    )
                }
            }

            item(key = "reset_row") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item(key = "duration_section") {
                SectionCard {
                    DurationSelector(selectedIndex = settings.chargingVibDurationIndex, onIndexChange = onDurationIndexChange)
                }
            }

            item(key = "intensity_section") {
                SectionCard {
                    ChargingIntensityControl(intensity = settings.chargingVibIntensity, onIntensityCommit = onIntensityCommit)
                }
            }

            item(key = "test_haptic") {
                HapticTestButton(label = stringResource(R.string.charging_haptic_test_button), enabled = settings.chargingVibEnabled, onClick = onTestHaptic)
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.size(4.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationSelector(selectedIndex: Int, onIndexChange: (Int) -> Unit) {
    val durations = listOf(
        stringResource(R.string.charging_duration_short),
        stringResource(R.string.charging_duration_medium),
        stringResource(R.string.charging_duration_long),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(text = stringResource(R.string.charging_duration_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = stringResource(R.string.charging_duration_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            durations.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onIndexChange(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = durations.size),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun ChargingIntensityControl(intensity: Float, onIntensityCommit: (Float) -> Unit) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTickIndex by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    val percent = (draft * 100f).roundToInt()
    val sliderColors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.intensity_label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(text = stringResource(R.string.intensity_value, percent), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
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
