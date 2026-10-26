package com.hapticks.app.ui.screens.tapHaptics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.PatternSelectorWithCustom
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalContentRow
import com.hapticks.app.ui.components.PetalExclusionsRow
import com.hapticks.app.ui.components.PetalFeatureHero
import com.hapticks.app.ui.components.PetalFeatureScreen
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalIntensityRow
import com.hapticks.app.ui.components.PetalResetFooter
import com.hapticks.app.ui.components.PetalSection
import com.hapticks.app.ui.components.PetalTestRow
import com.hapticks.app.ui.components.entrance
import com.hapticks.app.ui.theme.PetalMaterialShapes

@Composable
fun FeelEveryTapScreen(
    settings: HapticsSettings,
    onTapEnabledChange: (Boolean) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onTapCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenAppExclusions: () -> Unit,
    onOpenCustomEditor: (label: String, sequence: CustomHapticSequence, onSave: (CustomHapticSequence) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = stringResource(R.string.screen_title), onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = Icons.Rounded.TouchApp,
                tint = FeatureColors.FeelEveryTap,
                subtitle = "Tap-accurate haptic feedback on every interaction across the system.",
                modifier = Modifier.entrance(0),
            )
        }
        item("interaction") {
            PetalSection(title = "Tap feedback", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.TouchApp,
                        title = stringResource(R.string.toggle_tap_title),
                        subtitle = stringResource(R.string.toggle_tap_subtitle),
                        checked = settings.tapEnabled,
                        onCheckedChange = onTapEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie12Sided,
                    )
                }
                row { position ->
                    PetalIntensityRow(
                        intensity = settings.intensity,
                        onIntensityCommit = onIntensityCommit,
                        position = position,
                        title = stringResource(R.string.intensity_label),
                        enabled = settings.tapEnabled,
                    )
                }
                row { position ->
                    PetalExclusionsRow(
                        excludedCount = settings.tapExcludedPackages.size,
                        onClick = onOpenAppExclusions,
                        position = position,
                        title = stringResource(R.string.app_exclusions_row_title),
                        subtitleNone = stringResource(R.string.app_exclusions_row_subtitle_none),
                        subtitleSome = stringResource(R.string.app_exclusions_row_subtitle_some, settings.tapExcludedPackages.size),
                    )
                }
            }
        }
        item("pattern") {
            PetalSection(
                title = "Pattern",
                description = "Pick a built-in feel or record your own rhythm",
                modifier = Modifier.entrance(2),
            ) {
                row { position ->
                    PetalContentRow(position = position, enabled = settings.tapEnabled) {
                        PatternSelectorWithCustom(
                            selectedPattern = settings.pattern,
                            intensity = settings.intensity,
                            customSequence = settings.tapHapticCustomSequence,
                            onPatternSelected = onPatternSelected,
                            onIntensityCommit = {},
                            onOpenCustomEditor = { onOpenCustomEditor("tap", settings.tapHapticCustomSequence, onTapCustomSequenceSave) },
                            onClearCustomSequence = { onTapCustomSequenceSave(CustomHapticSequence()) },
                            showIntensity = false,
                        )
                    }
                }
                row { position ->
                    PetalTestRow(label = stringResource(R.string.test_haptic), onClick = onTestHaptic, position = position, enabled = settings.tapEnabled)
                }
            }
        }
        item("reset") {
            PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(3))
        }
    }
}
