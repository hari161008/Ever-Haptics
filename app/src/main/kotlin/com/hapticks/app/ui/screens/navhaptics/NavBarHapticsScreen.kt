package com.hapticks.app.ui.screens.navhaptics

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
fun NavBarHapticsScreen(
    settings: HapticsSettings,
    onNavBarHapticEnabledChange: (Boolean) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenCustomEditor: () -> Unit,
    onClearCustomSequence: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = stringResource(R.string.navbar_haptics_title), onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = PetalIcons.NavBar,
                tint = FeatureColors.NavBar,
                subtitle = "Feel every navigation gesture - home, back, and window switches.",
                modifier = Modifier.entrance(0),
            )
        }
        item("navbar") {
            PetalSection(title = stringResource(R.string.navbar_haptics_section), modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.NavBar,
                        title = stringResource(R.string.navbar_haptics_toggle_title),
                        subtitle = stringResource(R.string.navbar_haptics_toggle_subtitle),
                        checked = settings.navBarHapticEnabled,
                        onCheckedChange = onNavBarHapticEnabledChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Pentagon,
                    )
                }
            }
        }
        item("feel") {
            PetalSection(title = "Pattern", modifier = Modifier.entrance(2)) {
                hapticTuningRows(
                    enabled = settings.navBarHapticEnabled,
                    pattern = settings.navBarHapticPattern,
                    intensity = settings.navBarHapticIntensity,
                    customSequence = settings.navBarHapticCustomSequence,
                    onPatternSelected = onPatternSelected,
                    onIntensityCommit = onIntensityCommit,
                    onOpenCustomEditor = onOpenCustomEditor,
                    onClearCustomSequence = onClearCustomSequence,
                    testLabel = { stringResource(R.string.navbar_haptics_test) },
                    onTest = onTestHaptic,
                )
            }
        }
        item("reset") { PetalResetFooter(onReset = onResetToDefaults, label = stringResource(R.string.reset_to_defaults), modifier = Modifier.entrance(3)) }
    }
}
