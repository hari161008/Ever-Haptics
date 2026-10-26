package com.hapticks.app.ui.screens.statusbarhaptics

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
fun StatusBarHapticsScreen(
    settings: HapticsSettings,
    onStatusBarHapticEnabledChange: (Boolean) -> Unit,
    onExpandEnabledChange: (Boolean) -> Unit,
    onExpandPatternSelected: (HapticPattern) -> Unit,
    onExpandIntensityCommit: (Float) -> Unit,
    onExpandCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestExpandHaptic: () -> Unit,
    onCollapseEnabledChange: (Boolean) -> Unit,
    onCollapsePatternSelected: (HapticPattern) -> Unit,
    onCollapseIntensityCommit: (Float) -> Unit,
    onCollapseCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestCollapseHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenExpandCustomEditor: () -> Unit,
    onClearExpandCustomSequence: () -> Unit,
    onOpenCollapseCustomEditor: () -> Unit,
    onClearCollapseCustomSequence: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = "Status Bar Haptics", onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = PetalIcons.StatusBar,
                tint = FeatureColors.StatusBar,
                subtitle = "Feel haptic feedback when you pull down or dismiss the status bar.",
                modifier = Modifier.entrance(0),
            )
        }
        item("expand") {
            PetalSection(title = "Status bar expanded", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        title = "Haptic when status bar is expanded",
                        subtitle = "Vibrate when you pull down the status bar",
                        checked = settings.statusBarExpandEnabled,
                        onCheckedChange = onExpandEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                        variant = PetalBadgeVariant.PRIMARY,
                    )
                }
                hapticTuningRows(
                    enabled = settings.statusBarExpandEnabled,
                    pattern = settings.statusBarExpandPattern,
                    intensity = settings.statusBarExpandIntensity,
                    customSequence = settings.statusBarExpandCustomSequence,
                    onPatternSelected = onExpandPatternSelected,
                    onIntensityCommit = onExpandIntensityCommit,
                    onOpenCustomEditor = onOpenExpandCustomEditor,
                    onClearCustomSequence = onClearExpandCustomSequence,
                    testLabel = { "Test Expand Haptic" },
                    onTest = onTestExpandHaptic,
                )
            }
        }
        item("collapse") {
            PetalSection(title = "Status bar collapsed", modifier = Modifier.entrance(2)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.KeyboardArrowUp,
                        title = "Haptic when status bar is closed",
                        subtitle = "Vibrate when you dismiss the status bar",
                        checked = settings.statusBarCollapseEnabled,
                        onCheckedChange = onCollapseEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie12Sided,
                        variant = PetalBadgeVariant.SECONDARY,
                    )
                }
                hapticTuningRows(
                    enabled = settings.statusBarCollapseEnabled,
                    pattern = settings.statusBarCollapsePattern,
                    intensity = settings.statusBarCollapseIntensity,
                    customSequence = settings.statusBarCollapseCustomSequence,
                    onPatternSelected = onCollapsePatternSelected,
                    onIntensityCommit = onCollapseIntensityCommit,
                    onOpenCustomEditor = onOpenCollapseCustomEditor,
                    onClearCustomSequence = onClearCollapseCustomSequence,
                    testLabel = { "Test Collapse Haptic" },
                    onTest = onTestCollapseHaptic,
                )
            }
        }
        item("reset") { PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(3)) }
    }
}
