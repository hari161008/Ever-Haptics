package com.hapticks.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.haptics.HapticPattern

@Composable
fun PatternSelector(
    selected: HapticPattern,
    onPatternSelected: (HapticPattern) -> Unit,
    modifier: Modifier = Modifier,
) {
    val patterns = remember { HapticPattern.entries }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        patterns.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),  
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { pattern ->
                    PatternCard(
                        pattern = pattern,
                        isSelected = pattern == selected,
                        onClick = {
                            if (pattern != selected) {
                                onPatternSelected(pattern)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),   
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun PatternCard(
    pattern: HapticPattern,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(stiffness = 300f),
        label = "pattern-container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = spring(stiffness = 300f),
        label = "pattern-border",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 1.dp,
        label = "pattern-border-width",
    )
    val titleColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val descriptionColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val stateDescription = stringResource(
        id = if (isSelected) R.string.pattern_selected else R.string.pattern_not_selected
    )

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
            )
            .semantics {
                this.stateDescription = stateDescription
            },
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()  
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PatternIconBadge(icon = pattern.icon, isSelected = isSelected)
                SelectionDot(isSelected = isSelected)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = pattern.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Text(
                text = stringResource(id = pattern.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = descriptionColor,
            )
        }
    }
}

@Composable
private fun PatternIconBadge(icon: ImageVector, isSelected: Boolean) {
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(stiffness = 300f),
        label = "badge-bg",
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = spring(stiffness = 300f),
        label = "badge-tint",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = background, shape = RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,   
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = 600f),
        label = "selection-dot",
    )
    Box(
        modifier = Modifier
            .size(22.dp)
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.pattern_selected),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}