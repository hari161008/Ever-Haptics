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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hapticks.app.ui.components.featureShape
import com.hapticks.app.ui.components.petalGroupColor
import com.hapticks.app.ui.components.PetalSectionLabel
import com.hapticks.app.ui.theme.PetalIcons
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

private data class FeatureGroup(val title: String, val description: String, val items: List<FeatureEntry>)

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
    val groups = listOf(
        FeatureGroup(
            title = "Touch & typing",
            description = "How taps, scrolls and keystrokes feel",
            items = listOf(
                FeatureEntry(R.string.home_feel_every_tap_title, R.string.home_feel_every_tap_subtitle, Icons.Rounded.TouchApp, FeatureColors.FeelEveryTap, onOpenFeelEveryTap),
                FeatureEntry(R.string.home_tactile_scrolling_title, R.string.home_tactile_scrolling_subtitle, Icons.Rounded.SwipeUp, FeatureColors.TactileScrolling, onOpenTactileScrolling),
                FeatureEntry(R.string.home_keyboard_haptics_title, R.string.home_keyboard_haptics_subtitle, Icons.Rounded.Keyboard, FeatureColors.Keyboard, onOpenKeyboardHaptics),
            ),
        ),
        FeatureGroup(
            title = "System & hardware",
            description = "Buttons, navigation, lock screen and power",
            items = listOf(
                FeatureEntry(R.string.home_button_haptics_title, R.string.home_button_haptics_subtitle, Icons.Rounded.RadioButtonChecked, FeatureColors.ButtonHaptics, onOpenButtonHaptics),
                FeatureEntry(R.string.home_navbar_haptics_title, R.string.home_navbar_haptics_subtitle, PetalIcons.NavBar, FeatureColors.NavBar, onOpenNavBarHaptics),
                FeatureEntry(R.string.home_status_bar_haptics_title, R.string.home_status_bar_haptics_subtitle, PetalIcons.StatusBar, FeatureColors.StatusBar, onOpenStatusBarHaptics),
                FeatureEntry(R.string.home_unlock_haptics_title, R.string.home_unlock_haptics_subtitle, Icons.Rounded.LockOpen, FeatureColors.Unlock, onOpenUnlockHaptics),
                FeatureEntry(R.string.home_charging_haptics_title, R.string.home_charging_haptics_subtitle, PetalIcons.Charging, FeatureColors.Charging, onOpenChargingHaptics),
            ),
        ),
        FeatureGroup(
            title = "Alerts & media",
            description = "Calls, notifications, alarms and music",
            items = listOf(
                FeatureEntry(R.string.home_notification_haptics_title, R.string.home_notification_haptics_subtitle, Icons.Rounded.NotificationsActive, FeatureColors.Notifications, onOpenNotificationHaptics),
                FeatureEntry(R.string.home_music_haptics_title, R.string.home_music_haptics_subtitle, Icons.Rounded.MusicNote, FeatureColors.MusicHaptics, onOpenMusicHaptics),
            ),
        ),
    )

    val paused = batterySaverDetectionEnabled && isBatterySaverActive
    val featuresEnabled = globalEnabled && !paused

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
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(22.dp),
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

            var entranceIndex = 2
            groups.forEach { group ->
                val groupIndex = entranceIndex++
                Column(modifier = Modifier.fillMaxWidth().entrance(groupIndex)) {
                    PetalSectionLabel(text = group.title, description = group.description)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        group.items.forEachIndexed { index, feature ->
                            FeatureRow(
                                title = stringResource(feature.titleRes),
                                subtitle = stringResource(feature.subtitleRes),
                                icon = feature.icon,
                                featureColor = feature.color,
                                position = petalGroupPositionFor(index, group.items.size),
                                onClick = feature.onClick,
                                enabled = featuresEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatusChip(globalEnabled: Boolean, isServiceEnabled: Boolean, paused: Boolean) {
    val (label, icon, container, content) = when {
        paused -> StatusStyle("Paused", PetalIcons.BatterySaver, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        !isServiceEnabled -> StatusStyle("Setup", Icons.Rounded.AccessibilityNew, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        globalEnabled -> StatusStyle("Active", PetalIcons.Vibration, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
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
                icon = PetalIcons.BatterySaver,
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
        if (enabled) MaterialTheme.colorScheme.primaryContainer else petalGroupColor(),
        spring(stiffness = Spring.StiffnessLow), label = "global_toggle_bg",
    )
    val onContainer = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val badgeColor by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        spring(stiffness = Spring.StiffnessLow), label = "global_toggle_badge",
    )
    val badgeContent = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val spin = rememberInfiniteTransition(label = "global_spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "global_angle",
    )
    PetalGroupItem(
        position = PetalGroupPosition.SINGLE,
        modifier = modifier,
        color = containerColor,
        onClick = { onEnabledChange(!enabled) },
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = if (enabled) angle else 0f }
                        .clip((if (enabled) PetalMaterialShapes.Cookie12Sided else PetalMaterialShapes.Cookie6Sided).toShape())
                        .background(badgeColor),
                )
                Icon(PetalIcons.Vibration, null, tint = badgeContent, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_global_toggle_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (enabled) stringResource(R.string.home_global_toggle_on) else stringResource(R.string.home_global_toggle_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.75f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = null,
                thumbContent = if (enabled) {
                    { Icon(PetalIcons.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
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
    val dim = if (enabled) 1f else 0.45f
    PetalGroupItem(
        position = position,
        modifier = modifier,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureIcon(
                icon = icon,
                tint = featureColor.copy(alpha = dim),
                size = 54.dp,
                iconSize = 26.dp,
                backgroundAlpha = if (enabled) 0.18f else 0.08f,
                shape = featureShape(featureColor),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (enabled) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
