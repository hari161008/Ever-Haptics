package com.hapticks.app.ui.screens.keyboardhaptics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.ScreenIconHeader

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Keyboard Haptics", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } },
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
                ScreenIconHeader(icon = Icons.Rounded.Keyboard, featureColor = FeatureColors.Keyboard, subtitle = "Custom haptic feedback on every keystroke for a premium typing feel.")
            }
            item("info") {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How it works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Ever Haptics detects text input changes via the accessibility service to fire a custom haptic on each keystroke. For best results, disable your keyboard's own vibration feedback in your keyboard settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp))
                        Text("Reset to defaults", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item("toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = "Keyboard Haptics",
                        subtitle = "Play a haptic pattern on every keystroke",
                        checked = settings.keyboardHapticEnabled,
                        onCheckedChange = onKeyboardHapticEnabledChange,
                    )
                }
            }
            item("pattern") {
                SectionCard {
                    PatternSelectorWithCustom(
                        selectedPattern = settings.keyboardHapticPattern,
                        intensity = settings.keyboardHapticIntensity,
                        customSequence = settings.keyboardHapticCustomSequence,
                        onPatternSelected = onPatternSelected,
                        onIntensityCommit = onIntensityCommit,
                        onOpenCustomEditor = onOpenCustomEditor,
                        onClearCustomSequence = onClearCustomSequence,
                        accentColor = MaterialTheme.colorScheme.primary,
                        accentContainer = MaterialTheme.colorScheme.primaryContainer,
                        accentOnContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            item("test") {
                HapticTestButton("Test Keyboard Haptic", onTestHaptic, enabled = settings.keyboardHapticEnabled)
            }
            item("bottom") { Spacer(Modifier.size(4.dp)) }
        }
    }
}
