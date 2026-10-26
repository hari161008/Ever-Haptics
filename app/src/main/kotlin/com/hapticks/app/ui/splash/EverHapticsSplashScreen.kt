package com.hapticks.app.ui.splash

import com.hapticks.app.ui.theme.PetalIcons
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.ui.theme.PetalMaterialShapes
import kotlinx.coroutines.delay
import kotlin.random.Random

object EverHapticsSplashState {
    /** Plays the splash once per app process, so rotation / recreation does not replay it. */
    @Volatile
    var hasShownThisSession: Boolean = false
}

private data class HapticGlyph(
    val iconIndex: Int,
    val xFraction: Float,
    val startFraction: Float,
    val speed: Float,
    val sizeDp: Int,
    val alpha: Float,
)

// Haptics themed glyphs: touch, scroll, keys, charging, unlock, music, alerts.
private val hapticGlyphIcons = listOf(
    PetalIcons.Vibration,
    Icons.Rounded.TouchApp,
    Icons.Rounded.Fingerprint,
    Icons.Rounded.GraphicEq,
    Icons.Rounded.Waves,
    Icons.Rounded.MusicNote,
    Icons.Rounded.Notifications,
    Icons.Rounded.Keyboard,
    PetalIcons.Charging,
    Icons.Rounded.LockOpen,
    Icons.Rounded.Smartphone,
    Icons.Rounded.Sensors,
    PetalIcons.Bolt,
    Icons.Rounded.Tune,
    Icons.Rounded.AutoAwesome,
)

@Composable
fun EverHapticsSplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = 1800L,
) {
    LaunchedEffect(Unit) {
        delay(durationMs)
        EverHapticsSplashState.hasShownThisSession = true
        onSplashComplete()
    }
    EverHapticsSplashContent(modifier = modifier)
}

@Composable
private fun HapticGlyphField(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    glyphCount: Int = 90,
    columns: Int = 12,
) {
    val painters = hapticGlyphIcons.map { rememberVectorPainter(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "haptic_glyphs_loop")
    val progressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
        ),
        label = "glyphs_progress",
    )

    val glyphs = remember(glyphCount, columns) {
        val random = Random(42)
        List(glyphCount) { index ->
            val column = index % columns
            HapticGlyph(
                iconIndex = random.nextInt(hapticGlyphIcons.size),
                xFraction = ((column + 0.5f) / columns.toFloat()) + random.nextFloat() * 0.04f - 0.02f,
                startFraction = random.nextFloat() * 1.35f,
                speed = 0.30f + random.nextFloat() * 0.50f,
                sizeDp = 12 + random.nextInt(12),
                alpha = 0.08f + random.nextFloat() * 0.28f,
            )
        }
    }

    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val progress = progressState.value
        val widthPx = size.width
        val heightPx = size.height
        if (widthPx <= 0f || heightPx <= 0f) return@Canvas

        for (i in glyphs.indices) {
            val glyph = glyphs[i]
            val x = widthPx * glyph.xFraction
            val yProgress = (glyph.startFraction + progress * glyph.speed) % 1.35f
            val y = heightPx * yProgress - heightPx * 0.15f
            val sizePx = with(density) { glyph.sizeDp.dp.toPx() }
            val painter = painters[glyph.iconIndex]

            translate(left = x, top = y) {
                with(painter) {
                    draw(
                        size = Size(sizePx, sizePx),
                        alpha = glyph.alpha,
                        colorFilter = ColorFilter.tint(accentColor),
                    )
                }
            }
        }
    }
}

/** Soft concentric "vibration" rings that ripple outward from the centre badge. */
@Composable
private fun VibrationRings(
    modifier: Modifier = Modifier,
    color: Color,
) {
    val transition = rememberInfiniteTransition(label = "vibration_rings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
        ),
        label = "rings_phase",
    )
    Canvas(modifier = modifier) {
        val maxRadius = size.minDimension / 2f
        val ringCount = 3
        for (i in 0 until ringCount) {
            val p = (phase + i / ringCount.toFloat()) % 1f
            val radius = maxRadius * (0.45f + 0.55f * p)
            drawCircle(
                color = color.copy(alpha = 0.22f * (1f - p)),
                radius = radius,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun EverHapticsSplashContent(
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary

    val contentScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splash_scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ever_haptics_splash")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash_pulse",
    )
    val badgeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
        ),
        label = "badge_rotation",
    )
    // The badge "buzzes" a little, like a motor spinning up.
    val buzz by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 70, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "icon_buzz",
    )

    val isDark = (colorScheme.background.red + colorScheme.background.green + colorScheme.background.blue) < 1.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) colorScheme.background else colorScheme.surface,
                        accentColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                        if (isDark) colorScheme.surfaceContainerLowest else colorScheme.surfaceContainerLow,
                    ),
                ),
            ),
    ) {
        HapticGlyphField(modifier = Modifier.fillMaxSize(), accentColor = accentColor)

        // Soft tonal backdrop behind the badge (a flat tint, not a glow).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = (if (isDark) 0.18f else 0.10f) * pulse),
                            Color.Transparent,
                        ),
                        radius = 800f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                VibrationRings(modifier = Modifier.size(220.dp), color = accentColor)
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer { rotationZ = badgeRotation }
                        .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            rotationZ = -badgeRotation + buzz
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PetalIcons.Vibration,
                            contentDescription = "Ever Haptics icon",
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Ever Haptics",
                color = colorScheme.onBackground,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
        }
    }
}
