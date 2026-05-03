package com.coolappstore.everhaptics.by.svhp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coolappstore.everhaptics.by.svhp.ui.haptics.hapticClickable

enum class BottomTab { HOME, SETTINGS }

private val bottomNavSlideSpec = tween<IntOffset>(durationMillis = 320)
private val bottomNavFadeSpec = tween<Float>(durationMillis = 280)

/** Slides between main tabs (Home → Settings moves content left; reverse moves right). */
@Composable
fun SlidingBottomTabHost(
    selectedTab: BottomTab,
    modifier: Modifier = Modifier,
    content: @Composable (BottomTab) -> Unit,
) {
    val tabBg = MaterialTheme.colorScheme.background
    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier.background(tabBg),
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(bottomNavSlideSpec) { fullWidth -> fullWidth } + fadeIn(bottomNavFadeSpec)) togetherWith
                    (slideOutHorizontally(bottomNavSlideSpec) { fullWidth -> -fullWidth } + fadeOut(bottomNavFadeSpec))
            } else {
                (slideInHorizontally(bottomNavSlideSpec) { fullWidth -> -fullWidth } + fadeIn(bottomNavFadeSpec)) togetherWith
                    (slideOutHorizontally(bottomNavSlideSpec) { fullWidth -> fullWidth } + fadeOut(bottomNavFadeSpec))
            }
        },
        label = "slidingBottomTabHost",
    ) { tab ->
        Box(
            Modifier
                .fillMaxSize()
                .background(tabBg),
        ) {
            content(tab)
        }
    }
}

@Composable
fun FloatingBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(bottom = 25.dp)
            .height(60.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        tonalElevation = 2.dp,
        shadowElevation = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTabItem(
                selected = selectedTab == BottomTab.HOME,
                icon = Icons.Rounded.Home,
                label = "Home",
                onClick = { onTabSelected(BottomTab.HOME) },
            )
            BottomTabItem(
                selected = selectedTab == BottomTab.SETTINGS,
                icon = Icons.Rounded.Settings,
                label = "Settings",
                onClick = { onTabSelected(BottomTab.SETTINGS) },
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    val tabWidth = 100.dp

    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "bottomBarContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "bottomBarContentColor",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 26.dp else 22.dp,
        label = "bottomBarHorizontalPadding",
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(tabWidth)
            .clip(shape)
            .background(containerColor, shape)
            .hapticClickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}
