package com.hapticks.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
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
    onClearCustomSequence: () -> Unit = {},
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    accentOnContainer: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val isCustomActive = !customSequence.isEmpty
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HapticPattern.entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { p ->
                        PatternCard(
                            pattern = p,
                            isSelected = !isCustomActive && p == selectedPattern,
                            onClick = { onPatternSelected(p); onClearCustomSequence() },
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                            accentOnContainer = accentOnContainer,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (rowItems.size == 1) Box(Modifier.weight(1f).fillMaxHeight())
                }
            }
            CustomPatternCard(
                isSelected = isCustomActive,
                beatCount = customSequence.beats.size,
                durationMs = customSequence.durationMs,
                onClick = onOpenCustomEditor,
                accentColor = accentColor,
                accentContainer = accentContainer,
                accentOnContainer = accentOnContainer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        IntensityRow(intensity, onIntensityCommit, accentColor, accentContainer, accentOnContainer)
    }
}

@Composable
private fun PatternCard(
    pattern: HapticPattern,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    accentContainer: Color,
    accentOnContainer: Color,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) accentContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = 300f), label = "pc",
    )
    val borderColor by animateColorAsState(
        if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        spring(stiffness = 300f), label = "pb",
    )
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "pbw")
    val titleColor = if (isSelected) accentOnContainer else MaterialTheme.colorScheme.onSurface
    val descColor = if (isSelected) accentOnContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .heightIn(min = 132.dp)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
            ),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        pattern.icon, null,
                        tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                SelectionDot(isSelected, accentColor)
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(pattern.labelRes), style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(stringResource(pattern.descriptionRes), style = MaterialTheme.typography.bodySmall, color = descColor)
        }
    }
}

@Composable
private fun CustomPatternCard(
    isSelected: Boolean,
    beatCount: Int,
    durationMs: Long,
    onClick: () -> Unit,
    accentColor: Color,
    accentContainer: Color,
    accentOnContainer: Color,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) accentContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = 300f), label = "cc",
    )
    val borderColor by animateColorAsState(
        if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        spring(stiffness = 300f), label = "cb",
    )
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "cbw")
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            onClick = onClick,
            role = Role.RadioButton,
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
        ),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Draw, null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Custom",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) accentOnContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (isSelected && beatCount > 0) "$beatCount beats · ${durationMs}ms" else "Record your own haptic rhythm",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) accentOnContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean, accentColor: Color) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0f, spring(stiffness = 600f), label = "sd")
    Box(
        modifier = Modifier.size(22.dp).scale(scale).background(accentColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
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
            Text("Intensity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = accentContainer, shape = CircleShape) {
                Text(
                    "${(draft * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentOnContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = draft,
            onValueChange = {
                draft = it
                val t = slider01ToTickIndex(it)
                if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}
