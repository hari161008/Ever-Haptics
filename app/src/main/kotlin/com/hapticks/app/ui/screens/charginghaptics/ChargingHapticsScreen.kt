package com.hapticks.app.ui.screens.charginghaptics

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
    PetalFeatureScreen(title = stringResource(R.string.charging_haptics_title), onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = PetalIcons.Charging,
                tint = FeatureColors.Charging,
                subtitle = "Play haptic feedback when charging is connected or disconnected.",
                modifier = Modifier.entrance(0),
            )
        }
        item("events") {
            PetalSection(title = "Charging events", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.Charging,
                        title = stringResource(R.string.charging_vib_toggle_title),
                        subtitle = stringResource(R.string.charging_vib_toggle_subtitle),
                        checked = settings.chargingVibEnabled,
                        onCheckedChange = onChargingVibEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie12Sided,
                    )
                }
                row { position ->
                    PetalGroupControlRow(
                        icon = PetalIcons.Bolt,
                        title = "Play haptic when",
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        variant = PetalBadgeVariant.SECONDARY,
                    ) {
                        PetalMultiChoiceButtonGroup(
                            options = listOf(
                                PetalMultiChoiceItem(
                                    label = "Plugged in",
                                    checked = settings.chargingVibOnConnect,
                                    onCheckedChange = onChargingVibOnConnectChange,
                                    icon = PetalIcons.Charging,
                                ),
                                PetalMultiChoiceItem(
                                    label = "Unplugged",
                                    checked = settings.chargingVibOnDisconnect,
                                    onCheckedChange = onChargingVibOnDisconnectChange,
                                    icon = PetalIcons.BatteryFull,
                                ),
                            ),
                            enabled = settings.chargingVibEnabled,
                        )
                    }
                }
            }
        }
        item("feel") {
            PetalSection(title = "Pattern", modifier = Modifier.entrance(2)) {
                hapticTuningRows(
                    enabled = settings.chargingVibEnabled,
                    pattern = settings.chargingVibPattern,
                    intensity = settings.chargingVibIntensity,
                    customSequence = settings.chargingVibCustomSequence,
                    onPatternSelected = onPatternSelected,
                    onIntensityCommit = onIntensityCommit,
                    onOpenCustomEditor = onOpenCustomEditor,
                    onClearCustomSequence = { onCustomSequenceSaved(CustomHapticSequence()) },
                    testLabel = { stringResource(R.string.charging_haptic_test_button) },
                    onTest = onTestHaptic,
                )
            }
        }
        item("reset") { PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(3)) }
    }
}
