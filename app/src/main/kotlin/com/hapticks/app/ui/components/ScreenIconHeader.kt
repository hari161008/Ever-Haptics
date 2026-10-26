package com.hapticks.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Intro banner at the top of every feature screen; delegates to the Petal expressive hero. */
@Composable
fun ScreenIconHeader(
    icon: ImageVector,
    featureColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    PetalFeatureHero(icon = icon, tint = featureColor, subtitle = subtitle, modifier = modifier)
}
