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
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
import com.hapticks.app.ui.components.PetalGroupItem
import com.hapticks.app.ui.components.PetalGroupPosition
import com.hapticks.app.ui.components.PetalShapedIcon
import com.hapticks.app.ui.components.entrance
import com.hapticks.app.ui.components.petalGroupPositionFor
import com.hapticks.app.ui.components.rememberPetalHeaderScrollBehavior
import com.hapticks.app.ui.theme.PetalMaterialShapes
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val scrollBehavior = rememberPetalHeaderScrollBehavior()
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

    val paused = batterySaverDetectionEnabled && isBatterySaverActive

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PetalCollapsingTopAppBar(
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
                actions = { HomeStatusChip(globalEnabled = globalEnabled, isServiceEnabled = isServiceEnabled, paused = paused) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp).entrance(0),
            )

            GlobalToggleCard(globalEnabled, onGlobalEnabledChange, Modifier.entrance(1))

            AnimatedVisibility(visible = !isServiceEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
            }

            AnimatedVisibility(visible = paused, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                BatterySaverWarningCard()
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                features.forEachIndexed { index, feature ->
                    FeatureRow(
                        title = stringResource(feature.titleRes),
                        subtitle = stringResource(feature.subtitleRes),
                        icon = feature.icon,
                        featureColor = feature.color,
                        position = petalGroupPositionFor(index, features.size),
                        onClick = feature.onClick,
                        enabled = globalEnabled && !paused,
                        modifier = Modifier.entrance(index + 2),
                    )
                }
            }
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Spacer(Modifier.height(90.dp + navBarBottom))
        }
    }
}

@Composable
private fun HomeStatusChip(globalEnabled: Boolean, isServiceEnabled: Boolean, paused: Boolean) {
    val (label, icon, container, content) = when {
        paused -> StatusStyle("Paused", Icons.Rounded.BatterySaver, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        !isServiceEnabled -> StatusStyle("Setup", Icons.Rounded.AccessibilityNew, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        globalEnabled -> StatusStyle("Active", Icons.Rounded.Vibration, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        else -> StatusStyle("Off", Icons.Rounded.PowerSettingsNew, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(color = container, shape = CircleShape, modifier = Modifier.padding(end = 12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = content)
        }
    }
}

private data class StatusStyle(val label: String, val icon: ImageVector, val container: Color, val content: Color)

@Composable
private fun BatterySaverWarningCard() {
    PetalGroupItem(position = PetalGroupPosition.SINGLE, color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PetalShapedIcon(
                icon = Icons.Rounded.BatterySaver,
                shape = PetalMaterialShapes.Sunny,
                variant = PetalBadgeVariant.ERROR,
                size = 52.dp,
                iconSize = 26.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Functionality Paused", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
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
private fun GlobalToggleCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = Spring.StiffnessLow), label = "global_toggle_bg",
    )
    val onContainer = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    PetalGroupItem(
        position = PetalGroupPosition.SINGLE,
        modifier = modifier,
        color = containerColor,
        onClick = { onEnabledChange(!enabled) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                FeatureIcon(
                    icon = Icons.Rounded.Vibration,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 56.dp,
                    iconSize = 28.dp,
                    backgroundAlpha = if (enabled) 0.22f else 0.15f,
                    shape = if (enabled) PetalMaterialShapes.Cookie12Sided else PetalMaterialShapes.Cookie6Sided,
                )
                Column {
                    Text(stringResource(R.string.home_global_toggle_title), style = MaterialTheme.typography.titleLarge, color = onContainer)
                    Text(
                        if (enabled) stringResource(R.string.home_global_toggle_on) else stringResource(R.string.home_global_toggle_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = null,
                thumbContent = if (enabled) {
                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun FeatureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    featureColor: Color,
    position: PetalGroupPosition,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = if (enabled) 1f else 0.4f
    PetalGroupItem(
        position = position,
        modifier = modifier,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FeatureIcon(
                icon = icon,
                tint = featureColor.copy(alpha = dim),
                size = 58.dp,
                iconSize = 28.dp,
                backgroundAlpha = if (enabled) 0.16f else 0.07f,
                shape = com.hapticks.app.ui.components.featureShape(featureColor),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim * 0.85f))
            }
            if (enabled) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
