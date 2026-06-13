package com.hapticks.app.ui.screens.autohaptics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.components.ScreenIconHeader
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

enum class AutoHapticsType { CALL, ALARM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoHapticsSettingsScreen(
    type: AutoHapticsType,
    isEnabled: Boolean,
    sensitivity: Float,
    strength: Float,
    onEnabledChange: (Boolean) -> Unit,
    onSensitivityCommit: (Float) -> Unit,
    onStrengthCommit: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var sensitivityDraft by remember(sensitivity) { mutableFloatStateOf(sensitivity) }
    var sensitivityTick by remember { mutableIntStateOf(slider01ToTickIndex(sensitivity)) }
    var strengthDraft by remember(strength) { mutableFloatStateOf(strength) }
    var strengthTick by remember { mutableIntStateOf(slider01ToTickIndex(strength)) }

    val title = if (type == AutoHapticsType.CALL) "Auto Call Haptics" else "Auto Alarm Haptics"
    val subtitle = if (type == AutoHapticsType.CALL)
        "Vibrates in sync with your ringtone beats in real time — works even on silent or vibrate mode."
    else
        "Vibrates in sync with your alarm sound beats in real time using audio analysis."
    val featureColor = MaterialTheme.colorScheme.tertiary

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
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
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("header") {
                ScreenIconHeader(
                    icon = Icons.Rounded.GraphicEq,
                    featureColor = featureColor,
                    subtitle = subtitle,
                )
            }

            item("toggle") {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Enable Auto Beat Haptics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (type == AutoHapticsType.CALL) "Vibrate with ringtone audio regardless of ring mode"
                                else "Vibrate with alarm audio beats in real time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onEnabledChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onTertiary, checkedTrackColor = featureColor),
                        )
                    }
                }
            }

            item("sliders") {
                SectionCard {
                    Column {
                        AutoSlider(
                            title = "Beat Sensitivity",
                            subtitle = "How aggressively beats are detected",
                            value = sensitivityDraft,
                            enabled = isEnabled,
                            startLabel = "Subtle",
                            endLabel = "Reactive",
                            featureColor = featureColor,
                            onValueChange = { v ->
                                sensitivityDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != sensitivityTick) { sensitivityTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onSensitivityCommit(sensitivityDraft) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        AutoSlider(
                            title = "Haptic Strength",
                            subtitle = "Intensity of each vibration pulse",
                            value = strengthDraft,
                            enabled = isEnabled,
                            startLabel = "Gentle",
                            endLabel = "Strong",
                            featureColor = featureColor,
                            onValueChange = { v ->
                                strengthDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != strengthTick) { strengthTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onStrengthCommit(strengthDraft) },
                        )
                    }
                }
            }

            item("info") {
                Surface(
                    color = featureColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = featureColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How Auto Beat Haptics works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Uses Android's audio Visualizer API to read real-time audio output and detect beat patterns. Vibrations are triggered on each beat — even when the device is on silent or vibrate-only mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSlider(
    title: String,
    subtitle: String,
    value: Float,
    enabled: Boolean,
    startLabel: String,
    endLabel: String,
    featureColor: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = featureColor.copy(alpha = 0.18f), shape = CircleShape) {
                Text(
                    "${(value * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = featureColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f, steps = SliderTickStepsDefault, enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = featureColor,
                activeTrackColor = featureColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
