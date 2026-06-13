package com.hapticks.app.ui.screens.unlockHaptics

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
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.ScreenIconHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockHapticsScreen(
    settings: HapticsSettings,
    onUnlockHapticEnabledChange: (Boolean) -> Unit,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onTestHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenCustomEditor: () -> Unit,
    onClearCustomSequence: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.unlock_haptics_title), style = MaterialTheme.typography.displaySmall) },
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
                ScreenIconHeader(icon = Icons.Rounded.LockOpen, featureColor = FeatureColors.Unlock, subtitle = "Play a satisfying haptic pattern every time the device is unlocked.")
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
            item("unlock_card") {
                SectionCard(title = stringResource(R.string.unlock_haptics_section), icon = Icons.Rounded.LockOpen, iconTint = FeatureColors.Unlock) {
                    HapticToggleRow(
                        title = stringResource(R.string.unlock_haptics_toggle_title),
                        subtitle = stringResource(R.string.unlock_haptics_toggle_subtitle),
                        checked = settings.unlockHapticEnabled,
                        onCheckedChange = onUnlockHapticEnabledChange,
                        leadingIcon = Icons.Rounded.LockOpen,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.unlockHapticPattern,
                        intensity = settings.unlockHapticIntensity,
                        customSequence = settings.unlockHapticCustomSequence,
                        onPatternSelected = onPatternSelected,
                        onIntensityCommit = onIntensityCommit,
                        onOpenCustomEditor = onOpenCustomEditor,
                        onClearCustomSequence = onClearCustomSequence,
                    )
                }
            }
            item("test") {
                HapticTestButton(stringResource(R.string.unlock_haptics_test), onTestHaptic, enabled = settings.unlockHapticEnabled)
            }
        }
    }
}
