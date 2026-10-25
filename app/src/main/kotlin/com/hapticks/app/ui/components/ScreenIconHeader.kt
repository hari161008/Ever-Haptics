package com.hapticks.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hapticks.app.ui.theme.PetalMaterialShapes
import com.hapticks.app.ui.theme.toShape

@Composable
fun ScreenIconHeader(
    icon: ImageVector,
    featureColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    PetalContainment(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = PetalContainmentDefaults.shape,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(PetalMaterialShapes.Cookie7Sided.toShape())
                    .background(featureColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = featureColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
