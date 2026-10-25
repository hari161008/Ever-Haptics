package com.hapticks.app.ui.screens.tapHaptics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.HapticTestButton
import com.hapticks.app.ui.components.PetalScreenScaffold
import com.hapticks.app.ui.components.ScreenIconHeader
import com.hapticks.app.ui.components.rememberPetalHeaderScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    PetalScreenScaffold(
        title = stringResource(R.string.screen_title),
        scrollBehavior = scrollBehavior,
        onNavigateBack = onBack,
        modifier = modifier,
    ) {
        ScreenIconHeader(
            icon = Icons.Rounded.TouchApp,
            featureColor = FeatureColors.FeelEveryTap,
            subtitle = "Tap-accurate haptic feedback on every interaction across the system.",
        )

        FeelEveryTapInteractionSection(
            settings = settings,
            onTapEnabledChange = onTapEnabledChange,
            onIntensityCommit = onIntensityCommit,
            onOpenAppExclusions = onOpenAppExclusions,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onResetToDefaults) {
                Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.reset_to_defaults), style = MaterialTheme.typography.labelLarge)
            }
        }

        FeelEveryTapPatternSection(
            settings = settings,
            onPatternSelected = onPatternSelected,
            onOpenCustomEditor = { onOpenCustomEditor("tap", settings.tapHapticCustomSequence, onTapCustomSequenceSave) },
            onClearCustomSequence = { onTapCustomSequenceSave(CustomHapticSequence()) },
        )

        HapticTestButton(stringResource(R.string.test_haptic), onTestHaptic)
    }
}
