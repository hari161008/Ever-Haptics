package com.hapticks.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Intro banner at the top of every feature screen: expressive shaped icon + one-line description. */
@Composable
fun ScreenIconHeader(
    icon: ImageVector,
    featureColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    PetalGroupItem(position = PetalGroupPosition.SINGLE, modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FeatureIcon(
                icon = icon,
                tint = featureColor,
                size = 64.dp,
                iconSize = 30.dp,
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
