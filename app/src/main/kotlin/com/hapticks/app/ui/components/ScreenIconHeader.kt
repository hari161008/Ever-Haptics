package com.hapticks.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Colourful icon + title row shown at the top of feature screens,
 * sits just below the LargeTopAppBar title area.
 */
@Composable
fun ScreenIconHeader(
    icon: ImageVector,
    featureColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(featureColor.copy(alpha = 0.08f))
            .border(1.dp, featureColor.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        // radial glow behind icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
                .background(Brush.radialGradient(listOf(featureColor.copy(alpha = 0.14f), Color.Transparent))),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureIcon(
                icon = icon,
                tint = featureColor,
                size = 52.dp,
                iconSize = 26.dp,
                cornerRadius = 17.dp,
                backgroundAlpha = 0.18f,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
