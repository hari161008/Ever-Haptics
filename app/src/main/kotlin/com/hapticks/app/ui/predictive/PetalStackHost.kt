package com.hapticks.app.ui.predictive

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Predictive back + depth blur for Ever Haptics, ported from Petal Browser's
 * PetalPredictiveJunction / PetalScreenWrapper / PetalTransitions.
 *
 * Every screen on the back stack stays composed (scroll positions and state survive), and the one
 * directly under the top screen is what you see, blurred and dimmed, while you swipe back. Push,
 * pop and the back gesture all drive ONE continuous "depth" value, so they look identical and the
 * hand-off after a committed gesture is seamless.
 */

val PetalEmphasizedEasing = CubicBezierEasing(0.08f, 0.82f, 0.17f, 1.0f)
private val PetalExitSlideEasing = CubicBezierEasing(0.2f, 0f, 0.15f, 1.0f)

private const val TRANSITION_MS = 620
private const val MAX_BLUR_DP = 24f
private const val CORNER_DP = 32f
private const val MAX_FOREGROUND_SHRINK = 0.10f

@Composable
fun <R : Any> PetalStackHost(
    stack: List<R>,
    onBack: () -> Unit,
    predictiveEnabled: Boolean,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (R) -> Unit,
) {
    var displayed by remember { mutableStateOf(stack) }
    val depth = remember { Animatable((stack.size - 1).toFloat()) }

    // Live gesture state (read in draw / layer phase only).
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    var gestureActive by remember { mutableStateOf(false) }
    var gestureEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    // True from the moment a committed gesture pops the stack until the new state is applied.
    var committed by remember { mutableStateOf(false) }

    LaunchedEffect(stack) {
        val target = (stack.size - 1).toFloat()
        val previous = displayed
        when {
            committed -> {
                // The gesture already animated the departure: apply the new stack without replaying it.
                displayed = stack
                depth.snapTo(target)
                gestureProgress = 0f
                gestureActive = false
                committed = false
            }
            stack == previous -> Unit
            stack.size < previous.size && previous.take(stack.size) == stack -> {
                // Pop: keep the leaving screen composed while the depth animates down.
                depth.animateTo(target, tween(TRANSITION_MS, easing = PetalEmphasizedEasing))
                displayed = stack
            }
            else -> {
                // Push (or replace): the new top starts fully off-screen and slides in.
                displayed = stack
                if (stack.size == previous.size) depth.snapTo(target)
                else depth.animateTo(target, tween(TRANSITION_MS, easing = PetalEmphasizedEasing))
            }
        }
    }
    // Safety net: never leave the committed flag stuck if the stack did not actually change.
    LaunchedEffect(committed) {
        if (committed) {
            delay(1000)
            if (committed) {
                committed = false
                gestureProgress = 0f
                gestureActive = false
            }
        }
    }

    if (stack.size > 1) {
        if (predictiveEnabled) {
            PredictiveBackHandler(enabled = true) { progressFlow ->
                try {
                    progressFlow.collect { event ->
                        gestureEdge = event.swipeEdge
                        gestureActive = true
                        gestureProgress = event.progress
                    }
                    // Committed: finish the departure with a velvety low-stiffness glide, then actually pop.
                    animate(
                        initialValue = gestureProgress,
                        targetValue = 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
                    ) { value, _ -> gestureProgress = value }
                    gestureProgress = 1f
                    committed = true
                    onBack()
                } catch (e: CancellationException) {
                    withContext(NonCancellable) {
                        animate(
                            initialValue = gestureProgress,
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
                        ) { value, _ -> gestureProgress = value }
                        gestureProgress = 0f
                        gestureActive = false
                    }
                }
            }
        } else {
            BackHandler(enabled = true, onBack = onBack)
        }
    }

    // Effective depth: the animated depth minus whatever the back gesture has already "popped".
    val depthProvider: () -> Float = {
        depth.value - PetalEmphasizedEasing.transform(gestureProgress.coerceIn(0f, 1f))
    }
    val isGestureActive: () -> Boolean = { gestureActive }
    val foregroundSign: () -> Float = {
        if (gestureActive && gestureEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        displayed.forEachIndexed { index, route ->
            key(route) {
                StackLayer(
                    index = index,
                    isTarget = index == stack.lastIndex && route == stack.last(),
                    depth = depthProvider,
                    gestureActive = isGestureActive,
                    foregroundSign = foregroundSign,
                    blurEnabled = blurEnabled,
                ) {
                    content(route)
                }
            }
        }
    }
}

@Composable
private fun StackLayer(
    index: Int,
    isTarget: Boolean,
    depth: () -> Float,
    gestureActive: () -> Boolean,
    foregroundSign: () -> Float,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    // Only recomposes while this layer is actually behind another one and blurring.
    val blurRadius by remember(index, blurEnabled) {
        derivedStateOf {
            val r = depth() - index
            if (blurEnabled && r > 0.01f && (r < 0.995f || gestureActive()) && r <= 1f) (MAX_BLUR_DP * r.coerceIn(0f, 1f)).dp else 0.dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .graphicsLayer {
                val r = depth() - index
                val sign = foregroundSign()
                when {
                    r > 0f -> {
                        // Behind a newer screen: pushed back, shifted a little and hidden once covered.
                        val b = r.coerceIn(0f, 1f)
                        val scale = 1f - 0.06f * b
                        scaleX = scale
                        scaleY = scale
                        translationX = -sign * size.width / 6f * b
                        alpha = if (r > 1f || (r >= 0.995f && !gestureActive())) 0f else 1f
                        val corner = CORNER_DP * b
                        if (corner > 0.5f) {
                            shape = RoundedCornerShape(corner.dp)
                            clip = true
                        }
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    r < 0f -> {
                        // Entering (push) or leaving (pop / back gesture): same motion in both directions.
                        val p = (-r).coerceIn(0f, 1f)
                        val eased = PetalEmphasizedEasing.transform(p)
                        val slide = PetalExitSlideEasing.transform(p)
                        val scale = 1f - MAX_FOREGROUND_SHRINK * eased
                        scaleX = scale
                        scaleY = scale
                        translationX = sign * size.width * slide
                        val corner = CORNER_DP * eased
                        alpha = 1f - 0.20f * eased
                        if (corner > 0.5f) {
                            shape = RoundedCornerShape(corner.dp)
                            clip = true
                            shadowElevation = (16f * eased).dp.toPx()
                        }
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                }
            }
            .background(MaterialTheme.colorScheme.background)
            // Layers are opaque: never let touches fall through to the screen below.
            .pointerInput(Unit) { detectTapGestures { } }
            .then(if (isTarget) Modifier else Modifier.clearAndSetSemantics { }),
    ) {
        content()

        // Depth dim while this layer sits behind another one.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val b = (depth() - index).coerceIn(0f, 1f)
                    alpha = (if (blurEnabled) 0.40f else 0.75f) * b
                }
                .background(Color.Black),
        )
    }
}
