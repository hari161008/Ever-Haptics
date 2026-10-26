package com.hapticks.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Staggered spring rise-in used for the first appearance of list items. Because the screens stay
 * composed on the Petal back stack, this only plays once per screen, not every time you come back.
 */
@Composable
fun Modifier.entrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 45L).coerceAtMost(500L))
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 36.dp.toPx()
    }
}
