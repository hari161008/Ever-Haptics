package com.hapticks.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.R
import com.hapticks.app.ui.components.EnableServiceCard
import com.hapticks.app.ui.haptics.hapticClickable
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    globalEnabled: Boolean,
    isServiceEnabled: Boolean,
    onGlobalEnabledChange: (Boolean) -> Unit,
    onOpenFeelEveryTap: () -> Unit,
    onOpenTactileScrolling: () -> Unit,
    onOpenChargingHaptics: () -> Unit,
    onOpenButtonHaptics: () -> Unit,
    onOpenNavBarHaptics: () -> Unit,
    onOpenUnlockHaptics: () -> Unit,
    onOpenNotificationHaptics: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); entered = true }

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
        ) {
            AnimatedHomeHeader(entered = entered)
            Spacer(Modifier.height(16.dp))
            AnimatedGlobalToggleCard(entered = entered, enabled = globalEnabled, onEnabledChange = onGlobalEnabledChange)
            Spacer(Modifier.height(16.dp))

            if (!isServiceEnabled) {
                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn(tween(320)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 2 },
                ) {
                    Column {
                        EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            val callbacks = listOf(
                onOpenFeelEveryTap, onOpenTactileScrolling, onOpenChargingHaptics,
                onOpenButtonHaptics, onOpenNavBarHaptics, onOpenUnlockHaptics, onOpenNotificationHaptics,
            )
            val items = listOf(
                Triple(R.string.home_feel_every_tap_title, R.string.home_feel_every_tap_subtitle, Icons.Rounded.TouchApp),
                Triple(R.string.home_tactile_scrolling_title, R.string.home_tactile_scrolling_subtitle, Icons.Rounded.SwipeUp),
                Triple(R.string.home_charging_haptics_title, R.string.home_charging_haptics_subtitle, Icons.Rounded.BatteryChargingFull),
                Triple(R.string.home_button_haptics_title, R.string.home_button_haptics_subtitle, Icons.Rounded.RadioButtonChecked),
                Triple(R.string.home_navbar_haptics_title, R.string.home_navbar_haptics_subtitle, Icons.Rounded.Home),
                Triple(R.string.home_unlock_haptics_title, R.string.home_unlock_haptics_subtitle, Icons.Rounded.LockOpen),
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEachIndexed { index, (titleRes, subtitleRes, icon) ->
                    StaggeredFeatureCard(
                        title = stringResource(titleRes),
                        subtitle = stringResource(subtitleRes),
                        icon = icon,
                        onClick = callbacks[index],
                        enabled = globalEnabled,
                        entered = entered,
                        staggerIndex = index,
                    )
                }
                StaggeredFeatureCard(
                    title = "Calls, Alerts & Alarms",
                    subtitle = "Custom haptic patterns for incoming calls, notifications, and alarms",
                    icon = Icons.Rounded.NotificationsActive,
                    onClick = onOpenNotificationHaptics,
                    enabled = globalEnabled,
                    entered = entered,
                    staggerIndex = items.size,
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AnimatedHomeHeader(entered: Boolean) {
    val junicode = remember { FontFamily(Font(R.font.junicode_italic)) }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing), label = "hdr_a",
    )
    val offsetY by animateDpAsState(
        targetValue = if (entered) 0.dp else 24.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing), label = "hdr_y",
    )
    Column(modifier = Modifier.offset(y = offsetY).alpha(alpha), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = junicode, fontSize = 15.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(text = stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnimatedGlobalToggleCard(entered: Boolean, enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(440, delayMillis = 80, easing = FastOutSlowInEasing), label = "tog_a",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(440, delayMillis = 80, easing = FastOutSlowInEasing), label = "tog_s",
    )
    val containerColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(300), label = "tog_c",
    )
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().alpha(alpha).scale(scale),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_global_toggle_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (enabled) stringResource(R.string.home_global_toggle_on) else stringResource(R.string.home_global_toggle_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun StaggeredFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    entered: Boolean,
    staggerIndex: Int,
    modifier: Modifier = Modifier,
) {
    val delay = 140 + staggerIndex * 55
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(380, delayMillis = delay, easing = FastOutSlowInEasing), label = "card_a$staggerIndex",
    )
    val offsetY by animateDpAsState(
        targetValue = if (entered) 0.dp else 28.dp,
        animationSpec = tween(380, delayMillis = delay, easing = FastOutSlowInEasing), label = "card_y$staggerIndex",
    )
    val dimAlpha = if (enabled) 1f else 0.5f

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = dimAlpha),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .alpha(alpha)
            .then(if (enabled) Modifier.hapticClickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = dimAlpha), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = dimAlpha), modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = dimAlpha))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = dimAlpha * 0.78f))
            }
            if (enabled) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
