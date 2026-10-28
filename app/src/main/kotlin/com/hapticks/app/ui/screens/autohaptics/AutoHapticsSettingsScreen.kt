package com.hapticks.app.ui.screens.autohaptics

import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.components.PetalFeatureScreen
import androidx.compose.foundation.layout.*
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
import com.hapticks.app.ui.components.PetalSlider
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
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalGroup
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalSliderRow
import com.hapticks.app.ui.theme.PetalMaterialShapes
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

    PetalFeatureScreen(title = title, onBack = onBack, modifier = modifier) {
            item("header") {
                ScreenIconHeader(
                    icon = Icons.Rounded.GraphicEq,
                    featureColor = featureColor,
                    subtitle = subtitle,
                )
            }

            item("toggle") {
                PetalGroup {
                    row { position ->
                        PetalGroupToggleRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Enable Auto Beat Haptics",
                            subtitle = if (type == AutoHapticsType.CALL) "Vibrate with ringtone audio regardless of ring mode"
                            else "Vibrate with alarm audio beats in real time",
                            checked = isEnabled,
                            onCheckedChange = onEnabledChange,
                            position = position,
                            badgeShape = PetalMaterialShapes.Cookie12Sided,
                            variant = PetalBadgeVariant.TERTIARY,
                        )
                    }
                }
            }

            item("sliders") {
                PetalGroup {
                    row { position ->
                        PetalSliderRow(
                            icon = PetalIcons.Speed,
                            title = "Beat Sensitivity",
                            subtitle = "How aggressively beats are detected",
                            valueLabel = "${(sensitivityDraft * 100f).roundToInt()}%",
                            value = sensitivityDraft,
                            onValueChange = { v ->
                                sensitivityDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != sensitivityTick) { sensitivityTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onSensitivityCommit(sensitivityDraft) },
                            position = position,
                            steps = SliderTickStepsDefault,
                            enabled = isEnabled,
                            badgeShape = PetalMaterialShapes.Sunny,
                            variant = PetalBadgeVariant.TERTIARY,
                            footer = { SliderEndLabels("Subtle", "Reactive") },
                        )
                    }
                    row { position ->
                        PetalSliderRow(
                            icon = PetalIcons.Bolt,
                            title = "Haptic Strength",
                            subtitle = "Intensity of each vibration pulse",
                            valueLabel = "${(strengthDraft * 100f).roundToInt()}%",
                            value = strengthDraft,
                            onValueChange = { v ->
                                strengthDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != strengthTick) { strengthTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onStrengthCommit(strengthDraft) },
                            position = position,
                            steps = SliderTickStepsDefault,
                            enabled = isEnabled,
                            badgeShape = PetalMaterialShapes.Cookie9Sided,
                            variant = PetalBadgeVariant.TERTIARY,
                            footer = { SliderEndLabels("Gentle", "Strong") },
                        )
                    }
                }
            }

            item("info") {
                Surface(
                    color = featureColor.copy(alpha = 0.12f),
                    shape = com.hapticks.app.ui.components.PetalContainmentDefaults.shape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(PetalIcons.Info, null, tint = featureColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
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

@Composable
private fun SliderEndLabels(startLabel: String, endLabel: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
