package com.hapticks.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

@Composable
fun PatternSelectorWithCustom(
    selectedPattern: HapticPattern,
    intensity: Float,
    customSequence: CustomHapticSequence,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onOpenCustomEditor: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    accentOnContainer: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val isCustomActive = !customSequence.isEmpty
    Column(modifier = modifier.fillMaxWidth()) {
        IntensityRow(intensity, onIntensityCommit, accentColor, accentContainer, accentOnContainer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        PatternGridWithCustom(selectedPattern, isCustomActive, customSequence, onPatternSelected, onOpenCustomEditor, accentColor, accentContainer, accentOnContainer)
    }
}

@Composable
private fun IntensityRow(
    intensity: Float,
    onIntensityCommit: (Float) -> Unit,
    accentColor: Color,
    accentContainer: Color,
    accentOnContainer: Color,
) {
    val ctx = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Intensity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = accentContainer, shape = CircleShape) {
                Text("${(draft * 100f).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = accentOnContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = draft,
            onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
private fun PatternGridWithCustom(
    selectedPattern: HapticPattern,
    isCustomActive: Boolean,
    customSequence: CustomHapticSequence,
    onPatternSelected: (HapticPattern) -> Unit,
    onOpenCustomEditor: () -> Unit,
    accentColor: Color,
    accentContainer: Color,
    accentOnContainer: Color,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text("Pattern", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        HapticPattern.entries.chunked(3).forEach { rowPatterns ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPatterns.forEach { pattern ->
                    PatternChip(
                        label = pattern.displayName,
                        isSelected = !isCustomActive && pattern == selectedPattern,
                        onClick = { onPatternSelected(pattern) },
                        accentColor = accentColor,
                        accentContainer = accentContainer,
                        accentOnContainer = accentOnContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowPatterns.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // Custom haptic card
        val customShape = RoundedCornerShape(12.dp)
        val customBg = if (isCustomActive) accentContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        val customBorder = if (isCustomActive) accentColor else MaterialTheme.colorScheme.outlineVariant
        val customTextColor = if (isCustomActive) accentOnContainer else MaterialTheme.colorScheme.onSurface
        Row(
            modifier = Modifier.fillMaxWidth().clip(customShape).background(customBg).border(1.dp, customBorder, customShape).clickable(onClick = onOpenCustomEditor).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Edit, null, tint = if (isCustomActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Custom Pattern", style = MaterialTheme.typography.titleSmall, color = customTextColor)
                Text(
                    text = if (isCustomActive) "${customSequence.beats.size} beats recorded" else "Record your own haptic rhythm",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCustomActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun PatternChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    accentContainer: Color,
    accentOnContainer: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) accentContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) accentOnContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier.clip(shape).background(bg).border(if (isSelected) 1.5.dp else 1.dp, border, shape).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1)
    }
}

private val HapticPattern.displayName: String
    get() = when (this) {
        HapticPattern.CLICK -> "Click"
        HapticPattern.TICK -> "Tick"
        HapticPattern.HEAVY_CLICK -> "Heavy"
        HapticPattern.DOUBLE_CLICK -> "Double"
        HapticPattern.SOFT_BUMP -> "Soft"
        HapticPattern.DOUBLE_TICK -> "D. Tick"
        else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
