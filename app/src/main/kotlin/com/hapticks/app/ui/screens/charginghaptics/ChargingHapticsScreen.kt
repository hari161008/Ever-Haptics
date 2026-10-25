package com.hapticks.app.ui.screens.charginghaptics

import androidx.compose.foundation.layout.*
import com.hapticks.app.ui.components.PetalButtonStyle
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
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
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.ScreenIconHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingHapticsScreen(
    settings: HapticsSettings,
    onChargingVibEnabledChange: (Boolean) -> Unit,
    onChargingVibOnConnectChange: (Boolean) -> Unit,
    onChargingVibOnDisconnectChange: (Boolean) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onCustomSequenceSaved: (CustomHapticSequence) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
    onOpenCustomEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PetalCollapsingTopAppBar(
                title = stringResource(R.string.charging_haptics_title),
                scrollBehavior = scrollBehavior,
                onNavigateBack = onBack,
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
                ScreenIconHeader(icon = Icons.Rounded.ElectricBolt, featureColor = FeatureColors.Charging, subtitle = "Play haptic feedback when charging is connected or disconnected.")
            }
            item("toggles") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(R.string.charging_vib_toggle_title),
                        subtitle = stringResource(R.string.charging_vib_toggle_subtitle),
                        checked = settings.chargingVibEnabled,
                        onCheckedChange = onChargingVibEnabledChange,
                        leadingIcon = Icons.Rounded.ElectricBolt,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Play haptic when", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        PetalMultiChoiceButtonGroup(
                            options = listOf(
                                PetalMultiChoiceItem(
                                    label = "Plugged in",
                                    checked = settings.chargingVibOnConnect,
                                    onCheckedChange = onChargingVibOnConnectChange,
                                    icon = Icons.Rounded.BatteryChargingFull,
                                ),
                                PetalMultiChoiceItem(
                                    label = "Unplugged",
                                    checked = settings.chargingVibOnDisconnect,
                                    onCheckedChange = onChargingVibOnDisconnectChange,
                                    icon = Icons.Rounded.BatteryFull,
                                ),
                            ),
                            enabled = settings.chargingVibEnabled,
                        )
                    }
                }
            }
            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    PetalButton(onClick = onResetToDefaults, style = PetalButtonStyle.TEXT) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item("pattern") {
                SectionCard {
                    PatternSelectorWithCustom(
                        selectedPattern = settings.chargingVibPattern,
                        intensity = settings.chargingVibIntensity,
                        customSequence = settings.chargingVibCustomSequence,
                        onPatternSelected = onPatternSelected,
                        onIntensityCommit = onIntensityCommit,
                        onOpenCustomEditor = onOpenCustomEditor,
                        onClearCustomSequence = { onCustomSequenceSaved(CustomHapticSequence()) },
                    )
                }
            }
            item("test") {
                HapticTestButton(
                    label = stringResource(R.string.charging_haptic_test_button),
                    onClick = onTestHaptic,
                    enabled = settings.chargingVibEnabled,
                )
            }
        }
    }
}
