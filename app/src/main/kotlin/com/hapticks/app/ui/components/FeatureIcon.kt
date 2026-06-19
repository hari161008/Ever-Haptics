package com.hapticks.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Per-feature brand colours ──────────────────────────────────────────────
object FeatureColors {
    val FeelEveryTap      = Color(0xFF3B82F6)   // vivid blue
    val TactileScrolling  = Color(0xFF8B5CF6)   // violet
    val Charging          = Color(0xFFF59E0B)   // amber
    val ButtonHaptics     = Color(0xFFF43F5E)   // rose
    val NavBar            = Color(0xFF10B981)   // emerald
    val Unlock            = Color(0xFF06B6D4)   // cyan
    val Keyboard          = Color(0xFF6366F1)   // indigo
    val Notifications     = Color(0xFFF97316)   // orange
    val MusicHaptics      = Color(0xFFEC4899)   // pink
    val Settings          = Color(0xFF64748B)   // slate
    val Updates           = Color(0xFF22C55E)   // green
    val StatusBar         = Color(0xFF0EA5E9)   // sky blue
}

/**
 * Circular/rounded icon with a translucent tinted background.
 * Used on home cards and inside each feature screen header.
 */
@Composable
fun FeatureIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    cornerRadius: Dp = 18.dp,
    backgroundAlpha: Float = 0.15f,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
