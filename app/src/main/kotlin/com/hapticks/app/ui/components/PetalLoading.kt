package com.hapticks.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Petal's Material 3 Expressive contained loading indicator (ported from Petal Browser's
 * `ContainedLoadingIndicator.kt`) and the single loading visual used across the whole app.
 *
 * The morphing shape sits on a rounded container that follows the theme (primary / onPrimary by
 * default), so it tracks light/dark, dynamic colour and the app palette automatically.
 *
 * - [progress] == null  -> indeterminate: the shape keeps morphing while work is in flight.
 * - [progress] != null  -> determinate: the shape morphs in step with the reported 0f..1f value.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    progress: (() -> Float)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val animatedContainer by animateColorAsState(
        targetValue = containerColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PetalContainedLoadingContainer",
    )
    val animatedIndicator by animateColorAsState(
        targetValue = indicatorColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PetalContainedLoadingIndicator",
    )
    val semanticsModifier = modifier
        .requiredSize(size)
        .semantics { stateDescription = "Loading" }

    if (progress != null) {
        ContainedLoadingIndicator(
            progress = progress,
            modifier = semanticsModifier,
            containerColor = animatedContainer,
            indicatorColor = animatedIndicator,
        )
    } else {
        ContainedLoadingIndicator(
            modifier = semanticsModifier,
            containerColor = animatedContainer,
            indicatorColor = animatedIndicator,
        )
    }
}

/**
 * Container-less variant of the same expressive loading indicator, for places that already sit on
 * a coloured surface (inside buttons, inside tinted badges). [color] defaults to the surrounding
 * content colour so it stays readable on filled, tonal and outlined buttons alike.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified,
) {
    val tint = if (color == Color.Unspecified) LocalContentColor.current else color
    LoadingIndicator(
        modifier = modifier
            .requiredSize(size)
            .semantics { stateDescription = "Loading" },
        color = tint,
    )
}

/**
 * Pops a [PetalContainedLoadingIndicator] in and out with a bouncy scale. Used in headers and
 * cards to signal background work (SIM scan, mode switch, update check).
 */
@Composable
fun PetalLoadingBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    progress: (() -> Float)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = 0.4f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = 0.4f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
        ) + fadeOut(),
    ) {
        PetalContainedLoadingIndicator(size = size, progress = progress)
    }
}
