package com.hapticks.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Option model for [PetalChoiceButtonGroup]. */
data class PetalButtonGroupItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

/**
 * Material 3 Expressive connected segmented button group.
 * Connected shapes with active item morphing into pill with spring animation.
 */
@Composable
fun <T> PetalChoiceButtonGroup(
    options: List<PetalButtonGroupItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 48.dp,
    scrollableIfCrowded: Boolean = true,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val shouldScroll = scrollableIfCrowded && (options.size > 4 || options.any { it.label.length > 12 })

    val rowModifier = if (shouldScroll) {
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = rowModifier,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val count = options.size
            options.forEachIndexed { index, option ->
                val isChecked = option.value == selected

                val itemModifier = if (shouldScroll) {
                    Modifier
                        .defaultMinSize(minHeight = minHeight)
                        .semantics { role = Role.RadioButton }
                } else {
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = minHeight)
                        .semantics { role = Role.RadioButton }
                }

                val cornerRadius by animateDpAsState(
                    targetValue = if (isChecked) 20.dp else 12.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "segmented_corner",
                )

                val backgroundColor by animateColorAsState(
                    targetValue = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0f),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "segmented_bg",
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "segmented_text",
                )

                Box(
                    modifier = itemModifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(backgroundColor)
                        .clickable(enabled = enabled && option.enabled) {
                            if (!isChecked) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(option.value)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (option.label.isNotEmpty()) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Index-based convenience overload for simple label (and optional icon) lists. */
@Composable
fun PetalChoiceButtonGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
    minHeight: Dp = 48.dp,
    scrollableIfCrowded: Boolean = true,
    enabled: Boolean = true,
) {
    val options = labels.mapIndexed { index, label ->
        PetalButtonGroupItem(value = index, label = label, icon = icons?.getOrNull(index))
    }
    PetalChoiceButtonGroup(
        options = options,
        selected = selectedIndex,
        onSelect = onSelectIndex,
        modifier = modifier,
        minHeight = minHeight,
        scrollableIfCrowded = scrollableIfCrowded,
        enabled = enabled,
    )
}
