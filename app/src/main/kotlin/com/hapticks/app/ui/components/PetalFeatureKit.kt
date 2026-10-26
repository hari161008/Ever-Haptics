package com.hapticks.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import com.hapticks.app.ui.theme.ExpressiveShapeHolder
import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.theme.PetalMaterialShapes

/**
 * Petal feature-screen shell shared by every Ever Haptics page: collapsing Petal header, a single
 * lazy column with one consistent gutter, section rhythm and edge-to-edge bottom inset.
 *
 * Content scrolls behind the navigation bar (padding comes from the Scaffold inset, so nothing is
 * clipped by gesture bars) but never under the header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetalFeatureScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    val direction = LocalLayoutDirection.current
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PetalCollapsingTopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onNavigateBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp + padding.calculateStartPadding(direction),
                top = 4.dp,
                end = 16.dp + padding.calculateEndPadding(direction),
                bottom = 28.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            content = content,
        )
    }
}

/** Colour readable on top of a vivid feature colour. */
private fun onFeatureColor(tint: Color): Color =
    if (tint.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White

/**
 * Hero banner at the top of every feature screen: a slowly turning Material 3 Expressive shape
 * carrying the feature icon, over a soft tint of the feature colour.
 */
@Composable
fun PetalFeatureHero(
    icon: ImageVector,
    tint: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
    shape: ExpressiveShapeHolder = featureShape(tint),
) {
    val spin = rememberInfiniteTransition(label = "heroSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(32_000, easing = LinearEasing), RepeatMode.Restart),
        label = "heroAngle",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = tint.copy(alpha = 0.14f).compositeOver(petalGroupColor()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = angle }
                        .clip(shape.toShape())
                        .background(tint),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onFeatureColor(tint),
                    modifier = Modifier.size(38.dp),
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Small tonal value chip used in slider headers. */
@Composable
fun PetalValuePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Group row with a badge header, a value pill and an expressive slider underneath. */
@Composable
fun PetalSliderRow(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    steps: Int = 0,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Sunny,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
    footer: (@Composable () -> Unit)? = null,
) {
    PetalGroupControlRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        position = position,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        badgeShape = badgeShape,
        variant = variant,
        headerTrailing = { PetalValuePill(valueLabel) },
    ) {
        PetalSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        footer?.invoke()
    }
}

/** 0..100 % intensity slider with per-tick haptic feedback, as a Petal group row. */
@Composable
fun PetalIntensityRow(
    intensity: Float,
    onIntensityCommit: (Float) -> Unit,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    title: String = "Intensity",
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    PetalSliderRow(
        icon = PetalIcons.Bolt,
        title = title,
        subtitle = subtitle,
        valueLabel = "${(draft * 100f).toInt()}%",
        value = draft,
        onValueChange = {
            draft = it
            val tick = slider01ToTickIndex(it)
            if (tick != lastTick) {
                lastTick = tick
                context.performHapticSliderTick()
            }
        },
        onValueChangeFinished = { onIntensityCommit(draft) },
        position = position,
        modifier = modifier,
        steps = SliderTickStepsDefault,
        enabled = enabled,
        badgeShape = PetalMaterialShapes.Sunny,
        variant = PetalBadgeVariant.TERTIARY,
    )
}

/** Pattern picker (cards + custom recorder) hosted inside one group row. */
@Composable
fun PetalContentRow(
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    PetalGroupItem(position = position, modifier = modifier.alpha(if (enabled) 1f else 0.5f)) {
        content()
    }
}

/** "Play the current haptic" row: expressive play badge + title + tonal pill. */
@Composable
fun PetalTestRow(
    label: String,
    onClick: () -> Unit,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PetalGroupRow(
        icon = Icons.Rounded.PlayArrow,
        title = label,
        subtitle = "Feel your current settings",
        position = position,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        badgeShape = PetalMaterialShapes.Cookie9Sided,
        variant = PetalBadgeVariant.SECONDARY,
        trailing = {
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        },
    )
}

/** App exclusions entry row, shared by tap and scroll screens. */
@Composable
fun PetalExclusionsRow(
    excludedCount: Int,
    onClick: () -> Unit,
    position: PetalGroupPosition,
    title: String,
    subtitleNone: String,
    subtitleSome: String,
    modifier: Modifier = Modifier,
) {
    PetalGroupRow(
        icon = PetalIcons.AppExclusions,
        title = title,
        subtitle = if (excludedCount == 0) subtitleNone else subtitleSome,
        position = position,
        modifier = modifier,
        onClick = onClick,
        badgeShape = PetalMaterialShapes.Pentagon,
        variant = PetalBadgeVariant.SURFACE_TONAL,
    )
}

/** Full-width reset action that closes a feature screen. */
@Composable
fun PetalResetFooter(
    onReset: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    PetalButton(
        onClick = onReset,
        style = PetalButtonStyle.TONAL,
        height = 52.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(PetalIcons.Reset, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The standard "how should it feel" rows every haptic feature shares: pattern picker, intensity
 * slider and a play row. Dimmed (not removed) while the feature is switched off so layouts never jump.
 */
fun PetalGroupScope.hapticTuningRows(
    enabled: Boolean,
    pattern: HapticPattern,
    intensity: Float,
    customSequence: CustomHapticSequence,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onOpenCustomEditor: () -> Unit,
    onClearCustomSequence: () -> Unit,
    testLabel: @Composable () -> String,
    onTest: () -> Unit,
) {
    row { position ->
        PetalContentRow(position = position, enabled = enabled) {
            PatternSelectorWithCustom(
                selectedPattern = pattern,
                intensity = intensity,
                customSequence = customSequence,
                onPatternSelected = onPatternSelected,
                onIntensityCommit = onIntensityCommit,
                onOpenCustomEditor = onOpenCustomEditor,
                onClearCustomSequence = onClearCustomSequence,
                showIntensity = false,
            )
        }
    }
    row { position ->
        PetalIntensityRow(
            intensity = intensity,
            onIntensityCommit = onIntensityCommit,
            position = position,
            enabled = enabled,
        )
    }
    row { position ->
        PetalTestRow(label = testLabel(), onClick = onTest, position = position, enabled = enabled)
    }
}
