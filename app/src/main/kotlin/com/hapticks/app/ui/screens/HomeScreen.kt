package com.hapticks.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.R
import com.hapticks.app.ui.components.EnableServiceCard
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.FeatureIcon
import com.hapticks.app.ui.haptics.hapticClickable
import kotlinx.coroutines.delay

private data class FeatureEntry(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

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
    onOpenKeyboardHaptics: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); entered = true }

    val features = listOf(
        FeatureEntry(R.string.home_feel_every_tap_title, R.string.home_feel_every_tap_subtitle, Icons.Rounded.TouchApp, FeatureColors.FeelEveryTap, onOpenFeelEveryTap),
        FeatureEntry(R.string.home_tactile_scrolling_title, R.string.home_tactile_scrolling_subtitle, Icons.Rounded.SwipeUp, FeatureColors.TactileScrolling, onOpenTactileScrolling),
        FeatureEntry(R.string.home_charging_haptics_title, R.string.home_charging_haptics_subtitle, Icons.Rounded.BatteryChargingFull, FeatureColors.Charging, onOpenChargingHaptics),
        FeatureEntry(R.string.home_button_haptics_title, R.string.home_button_haptics_subtitle, Icons.Rounded.RadioButtonChecked, FeatureColors.ButtonHaptics, onOpenButtonHaptics),
        FeatureEntry(R.string.home_navbar_haptics_title, R.string.home_navbar_haptics_subtitle, Icons.Rounded.Home, FeatureColors.NavBar, onOpenNavBarHaptics),
        FeatureEntry(R.string.home_unlock_haptics_title, R.string.home_unlock_haptics_subtitle, Icons.Rounded.LockOpen, FeatureColors.Unlock, onOpenUnlockHaptics),
        FeatureEntry(-1, -1, Icons.Rounded.Keyboard, FeatureColors.Keyboard, onOpenKeyboardHaptics),
        FeatureEntry(-1, -1, Icons.Rounded.NotificationsActive, FeatureColors.Notifications, onOpenNotificationHaptics),
    )

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
        ) {
            AnimatedHomeHeader(entered)
            Spacer(Modifier.height(20.dp))
            AnimatedGlobalToggleCard(entered, globalEnabled, onGlobalEnabledChange)
            Spacer(Modifier.height(16.dp))

            if (!isServiceEnabled) {
                AnimatedVisibility(visible = entered, enter = fadeIn(tween(320)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 2 }) {
                    Column { EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings); Spacer(Modifier.height(16.dp)) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEachIndexed { index, feature ->
                    val title = if (feature.titleRes == -1) when (feature.icon) {
                        Icons.Rounded.Keyboard -> "Keyboard Haptics"
                        else -> "Calls, Alerts & Alarms"
                    } else stringResource(feature.titleRes)
                    val subtitle = if (feature.subtitleRes == -1) when (feature.icon) {
                        Icons.Rounded.Keyboard -> "Custom haptic feedback on every keystroke"
                        else -> "Custom haptic patterns for incoming calls, notifications, and alarms"
                    } else stringResource(feature.subtitleRes)

                    FeatureCard(
                        title = title,
                        subtitle = subtitle,
                        icon = feature.icon,
                        featureColor = feature.color,
                        onClick = feature.onClick,
                        enabled = globalEnabled,
                        entered = entered,
                        staggerIndex = index,
                    )
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AnimatedHomeHeader(entered: Boolean) {
    val junicode = remember { FontFamily(Font(R.font.junicode_italic)) }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(500, easing = FastOutSlowInEasing), label = "h_a")
    val offsetY by animateDpAsState(if (entered) 0.dp else 24.dp, tween(500, easing = FastOutSlowInEasing), label = "h_y")
    Column(modifier = Modifier.offset(y = offsetY).alpha(alpha), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.labelLarge.copy(fontFamily = junicode, fontSize = 15.sp), color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnimatedGlobalToggleCard(entered: Boolean, enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(440, delayMillis = 80, easing = FastOutSlowInEasing), label = "t_a")
    val containerColor by animateColorAsState(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, tween(300), label = "t_c")
    Surface(color = containerColor, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                FeatureIcon(icon = Icons.Rounded.Vibration, tint = MaterialTheme.colorScheme.primary, size = 44.dp, iconSize = 22.dp, cornerRadius = 14.dp, backgroundAlpha = if (enabled) 0.25f else 0.12f)
                Column {
                    Text(stringResource(R.string.home_global_toggle_title), style = MaterialTheme.typography.titleLarge, color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                    Text(if (enabled) stringResource(R.string.home_global_toggle_on) else stringResource(R.string.home_global_toggle_off), style = MaterialTheme.typography.bodySmall, color = (if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f))
                }
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    featureColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    entered: Boolean,
    staggerIndex: Int,
) {
    val delay = 140 + staggerIndex * 50
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(380, delayMillis = delay, easing = FastOutSlowInEasing), label = "c_a$staggerIndex")
    val offsetY by animateDpAsState(if (entered) 0.dp else 30.dp, tween(380, delayMillis = delay, easing = FastOutSlowInEasing), label = "c_y$staggerIndex")
    val dimAlpha = if (enabled) 1f else 0.4f
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .alpha(alpha)
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .border(1.dp, featureColor.copy(alpha = if (enabled) 0.18f else 0.06f), RoundedCornerShape(24.dp))
            .then(if (enabled) Modifier.hapticClickable(onClick = onClick) else Modifier),
    ) {
        // subtle colour wash in top-right corner
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(featureColor.copy(alpha = if (enabled) 0.10f else 0.03f), Color.Transparent),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FeatureIcon(
                icon = icon,
                tint = featureColor.copy(alpha = dimAlpha),
                size = 58.dp,
                iconSize = 30.dp,
                cornerRadius = 20.dp,
                backgroundAlpha = if (enabled) 0.16f else 0.06f,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimAlpha))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimAlpha * 0.85f))
            }
            if (enabled) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(featureColor.copy(alpha = 0.14f)).border(1.dp, featureColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = featureColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
