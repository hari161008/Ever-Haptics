package com.hapticks.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.ui.haptics.performHapticClick

enum class BottomTab(val label: String, val icon: ImageVector, val selectedRotation: Float) {
    HOME("Home", Icons.Rounded.Home, 0f),
    SETTINGS("Settings", Icons.Rounded.Settings, 90f),
}

/**
 * Floating bottom navigation, ported from Petal Browser's floating `PetalBottomNavBar`: a Material 3
 * Expressive [HorizontalFloatingToolbar] where the selected item grows into a `primaryContainer`
 * pill and reveals its label with a bouncy spring. Every tap plays the user's Ever Haptics tap
 * haptic. The bar handles the navigation-bar inset itself.
 */
@Composable
fun FloatingBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .wrapContentWidth()
                .height(64.dp)
                .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                .border(
                    0.75.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        ),
                    ),
                    CircleShape,
                )
                .clip(CircleShape),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            BottomTab.entries.forEachIndexed { index, tab ->
                BottomTabItem(
                    selected = selectedTab == tab,
                    tab = tab,
                    index = index,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    selected: Boolean,
    tab: BottomTab,
    index: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tab_press_$index",
    )
    val labelWidth by animateDpAsState(
        targetValue = if (selected) 76.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 360f),
        label = "tab_label_$index",
    )
    val rotation by animateFloatAsState(
        targetValue = if (selected) tab.selectedRotation else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 450f),
        label = "tab_rotation_$index",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "tab_scale_$index",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "tab_color_$index",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "tab_bg_$index",
    )

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .height(48.dp)
            .width(48.dp + labelWidth)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = ripple()) {
                context.performHapticClick()
                onClick()
            }
            .semantics { contentDescription = tab.label },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = if (selected) 8.dp else 0.dp)
                .fillMaxHeight(),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            if (labelWidth > 4.dp) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
