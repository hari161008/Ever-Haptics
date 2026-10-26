package com.hapticks.app.ui.screens.keyboardhaptics

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
fun KeyboardHapticsScreen(
    settings: HapticsSettings,
    onKeyboardHapticEnabledChange: (Boolean) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenCustomEditor: () -> Unit,
    onClearCustomSequence: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = "Keyboard Haptics", onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = Icons.Rounded.Keyboard,
                tint = FeatureColors.Keyboard,
                subtitle = "Custom haptic feedback on every keystroke for a premium typing feel.",
                modifier = Modifier.entrance(0),
            )
        }
        item("info") {
            PetalGroupItem(
                position = PetalGroupPosition.SINGLE,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.entrance(1),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    PetalShapedIcon(
                        icon = PetalIcons.Info,
                        shape = PetalMaterialShapes.Cookie6Sided,
                        variant = PetalBadgeVariant.TERTIARY,
                        size = 40.dp,
                        iconSize = 20.dp,
                        modifier = Modifier,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("How it works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            "Ever Haptics detects text input changes via the accessibility service to fire a custom haptic on each keystroke. For best results, disable your keyboard's own vibration feedback in your keyboard settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
        item("toggle") {
            PetalSection(title = "Typing", modifier = Modifier.entrance(2)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Keyboard,
                        title = "Keyboard Haptics",
                        subtitle = "Play a haptic pattern on every keystroke",
                        checked = settings.keyboardHapticEnabled,
                        onCheckedChange = onKeyboardHapticEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover8Leaf,
                    )
                }
            }
        }
        item("feel") {
            PetalSection(title = "Pattern", modifier = Modifier.entrance(3)) {
                hapticTuningRows(
                    enabled = settings.keyboardHapticEnabled,
                    pattern = settings.keyboardHapticPattern,
                    intensity = settings.keyboardHapticIntensity,
                    customSequence = settings.keyboardHapticCustomSequence,
                    onPatternSelected = onPatternSelected,
                    onIntensityCommit = onIntensityCommit,
                    onOpenCustomEditor = onOpenCustomEditor,
                    onClearCustomSequence = onClearCustomSequence,
                    testLabel = { "Test Keyboard Haptic" },
                    onTest = onTestHaptic,
                )
            }
        }
        item("reset") { PetalResetFooter(onReset = onResetToDefaults, label = "Reset to defaults", modifier = Modifier.entrance(4)) }
    }
}
