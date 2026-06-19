package com.hapticks.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
    isBatterySaverActive: Boolean,
    batterySaverDetectionEnabled: Boolean,
    scrollState: ScrollState,
    onGlobalEnabledChange: (Boolean) -> Unit,
    onOpenFeelEveryTap: () -> Unit,
    onOpenTactileScrolling: () -> Unit,
    onOpenChargingHaptics: () -> Unit,
    onOpenButtonHaptics: () -> Unit,
    onOpenNavBarHaptics: () -> Unit,
    onOpenStatusBarHaptics: () -> Unit,
    onOpenUnlockHaptics: () -> Unit,
    onOpenNotificationHaptics: () -> Unit,
    onOpenKeyboardHaptics: () -> Unit,
    onOpenMusicHaptics: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); entered = true }

    val features = listOf(
        FeatureEntry(R.string.home_feel_every_tap_title, R.string.home_feel_every_tap_subtitle, Icons.Rounded.TouchApp, FeatureColors.FeelEveryTap, onOpenFeelEveryTap),
        FeatureEntry(R.string.home_tactile_scrolling_title, R.string.home_tactile_scrolling_subtitle, Icons.Rounded.SwipeUp, FeatureColors.TactileScrolling, onOpenTactileScrolling),
        FeatureEntry(R.string.home_charging_haptics_title, R.string.home_charging_haptics_subtitle, Icons.Rounded.BatteryChargingFull, FeatureColors.Charging, onOpenChargingHaptics),
        FeatureEntry(R.string.home_button_haptics_title, R.string.home_button_haptics_subtitle, Icons.Rounded.RadioButtonChecked, FeatureColors.ButtonHaptics, onOpenButtonHaptics),
        FeatureEntry(R.string.home_navbar_haptics_title, R.string.home_navbar_haptics_subtitle, Icons.Rounded.Home, FeatureColors.NavBar, onOpenNavBarHaptics),
        FeatureEntry(R.string.home_status_bar_haptics_title, R.string.home_status_bar_haptics_subtitle, Icons.Rounded.Notifications, FeatureColors.StatusBar, onOpenStatusBarHaptics),
        FeatureEntry(R.string.home_unlock_haptics_title, R.string.home_unlock_haptics_subtitle, Icons.Rounded.LockOpen, FeatureColors.Unlock, onOpenUnlockHaptics),
        FeatureEntry(R.string.home_keyboard_haptics_title, R.string.home_keyboard_haptics_subtitle, Icons.Rounded.Keyboard, FeatureColors.Keyboard, onOpenKeyboardHaptics),
        FeatureEntry(R.string.home_notification_haptics_title, R.string.home_notification_haptics_subtitle, Icons.Rounded.NotificationsActive, FeatureColors.Notifications, onOpenNotificationHaptics),
        FeatureEntry(R.string.home_music_haptics_title, R.string.home_music_haptics_subtitle, Icons.Rounded.MusicNote, FeatureColors.MusicHaptics, onOpenMusicHaptics),
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

            AnimatedVisibility(
                visible = batterySaverDetectionEnabled && isBatterySaverActive,
                enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(250)) + shrinkVertically(tween(250)),
            ) {
                Column {
                    BatterySaverWarningCard()
                    Spacer(Modifier.height(16.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEachIndexed { index, feature ->
                    FeatureCard(
                        title = stringResource(feature.titleRes),
                        subtitle = stringResource(feature.subtitleRes),
                        icon = feature.icon,
                        featureColor = feature.color,
                        onClick = feature.onClick,
                        enabled = globalEnabled && !(batterySaverDetectionEnabled && isBatterySaverActive),
                        entered = entered,
                        staggerIndex = index,
                    )
                }
            }
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Spacer(Modifier.height(90.dp + navBarBottom))
        }
    }
}

@Composable
private fun BatterySaverWarningCard() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.BatterySaver, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Functionality Paused",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    "Paused the functionality due to Battery Saver being turned on",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AnimatedHomeHeader(entered: Boolean) {
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(500, easing = FastOutSlowInEasing), label = "h_a")
    val offsetY by animateDpAsState(if (entered) 0.dp else 24.dp, tween(500, easing = FastOutSlowInEasing), label = "h_y")
    Column(modifier = Modifier.offset(y = offsetY).alpha(alpha), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                FeatureIcon(icon = Icons.Rounded.Vibration, tint = MaterialTheme.colorScheme.primary, size = 44.dp, iconSize = 22.dp, cornerRadius = 14.dp, backgroundAlpha = 0.15f)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .alpha(alpha)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (enabled) Modifier.hapticClickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(featureColor.copy(alpha = if (enabled) 0.15f else 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = featureColor.copy(alpha = dimAlpha), modifier = Modifier.size(30.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimAlpha))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimAlpha * 0.85f))
            }
            if (enabled) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
