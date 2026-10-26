package com.hapticks.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val TrackHeight = 46.dp
private val ThumbSize = 30.dp
private val ThumbInset = 8.dp

/**
 * ImageToolbox-style expressive slider: a chunky pill track with a filled
 * section and a scalloped cookie thumb that rides *inside* the pill. The thumb
 * physically rolls — its rotation is derived from the distance travelled, so
 * the scallops turn like a wheel as the value changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier.height(TrackHeight),
        // The track draws the thumb too, so it can be positioned inside the pill
        thumb = {
            // Empty thumb to avoid default huge circle thumb drawing over custom track
            Spacer(Modifier.size(0.dp))
        },
        track = { state ->
            val rangeSpan = state.valueRange.endInclusive - state.valueRange.start
            val target = if (rangeSpan > 0f) {
                ((state.value - state.valueRange.start) / rangeSpan).coerceIn(0f, 1f)
            } else 0f

            // Springy follow: snappy enough to track a drag, loose enough to
            // settle with a little life when a preset jumps the value.
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 900f
                ),
                label = "sliderFraction"
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TrackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val travel = maxWidth - ThumbSize - ThumbInset * 2
                val thumbStart = ThumbInset + travel * fraction

                // Rolling: one full turn per circumference travelled
                val circumference = ThumbSize.value * Math.PI.toFloat()
                val rollDegrees = (travel.value * fraction / circumference) * 360f

                // Deep filled section — its rounded cap sits just past the thumb
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(thumbStart + ThumbSize + ThumbInset)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                // Scalloped cookie thumb, rolling as it travels
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbStart)
                        .size(ThumbSize)
                        .graphicsLayer { rotationZ = rollDegrees }
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                )
            }
        }
    )
}
