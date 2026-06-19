package com.hapticks.app.ui.screens.statusbarhaptics

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
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Status Bar Haptics", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
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
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("icon_header") {
                ScreenIconHeader(
                    icon = Icons.Rounded.Notifications,
                    featureColor = FeatureColors.StatusBar,
                    subtitle = "Feel haptic feedback when you pull down or dismiss the status bar.",
                )
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

            // Expand section
            item("expand_card") {
                SectionCard(
                    title = "Status Bar Expanded",
                    icon = Icons.Rounded.KeyboardArrowDown,
                    iconTint = FeatureColors.StatusBar,
                ) {
                    HapticToggleRow(
                        title = "Haptic when status bar is expanded",
                        subtitle = "Vibrate when you pull down the status bar",
                        checked = settings.statusBarExpandEnabled,
                        onCheckedChange = onExpandEnabledChange,
                        leadingIcon = Icons.Rounded.KeyboardArrowDown,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.statusBarExpandPattern,
                        intensity = settings.statusBarExpandIntensity,
                        customSequence = settings.statusBarExpandCustomSequence,
                        onPatternSelected = onExpandPatternSelected,
                        onIntensityCommit = onExpandIntensityCommit,
                        onOpenCustomEditor = onOpenExpandCustomEditor,
                        onClearCustomSequence = onClearExpandCustomSequence,
                    )
                }
            }

            item("test_expand") {
                HapticTestButton("Test Expand Haptic", onTestExpandHaptic, enabled = settings.statusBarExpandEnabled)
            }

            // Collapse section
            item("collapse_card") {
                SectionCard(
                    title = "Status Bar Collapsed",
                    icon = Icons.Rounded.KeyboardArrowUp,
                    iconTint = FeatureColors.StatusBar,
                ) {
                    HapticToggleRow(
                        title = "Haptic when status bar is closed",
                        subtitle = "Vibrate when you dismiss the status bar",
                        checked = settings.statusBarCollapseEnabled,
                        onCheckedChange = onCollapseEnabledChange,
                        leadingIcon = Icons.Rounded.KeyboardArrowUp,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                    PatternSelectorWithCustom(
                        selectedPattern = settings.statusBarCollapsePattern,
                        intensity = settings.statusBarCollapseIntensity,
                        customSequence = settings.statusBarCollapseCustomSequence,
                        onPatternSelected = onCollapsePatternSelected,
                        onIntensityCommit = onCollapseIntensityCommit,
                        onOpenCustomEditor = onOpenCollapseCustomEditor,
                        onClearCustomSequence = onClearCollapseCustomSequence,
                    )
                }
            }

            item("test_collapse") {
                HapticTestButton("Test Collapse Haptic", onTestCollapseHaptic, enabled = settings.statusBarCollapseEnabled)
            }
        }
    }
}
