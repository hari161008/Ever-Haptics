package com.hapticks.app.ui.components

import com.hapticks.app.ui.theme.PetalIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import com.hapticks.app.ui.theme.PetalMaterialShapes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hapticks.app.ui.haptics.hapticClickable
import com.hapticks.app.ui.haptics.performHapticClick

@Composable
fun HapticToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val context = LocalContext.current

    val iconBgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(250),
        label = "icon_bg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "icon_tint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (checked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "icon_scale",
    )

    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).hapticClickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(iconScale)
                        .background(color = iconBgColor, shape = (if (checked) PetalMaterialShapes.Cookie12Sided else PetalMaterialShapes.Cookie6Sided).toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(leadingIcon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(
                modifier = Modifier.padding(start = if (leadingIcon != null) 14.dp else 0.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { value -> context.performHapticClick(); onCheckedChange(value) },
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = PetalIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}
