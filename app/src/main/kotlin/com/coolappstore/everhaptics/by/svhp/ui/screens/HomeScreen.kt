package com.coolappstore.everhaptics.by.svhp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.R
import com.coolappstore.everhaptics.by.svhp.ui.components.EnableServiceCard
import com.coolappstore.everhaptics.by.svhp.ui.haptics.hapticClickable

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
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
        ) {
            HomeHeader()
            Spacer(modifier = Modifier.height(16.dp))

            // Global Master Toggle
            GlobalToggleCard(enabled = globalEnabled, onEnabledChange = onGlobalEnabledChange)
            Spacer(modifier = Modifier.height(16.dp))

            // Permission Card
            if (!isServiceEnabled) {
                EnableServiceCard(onOpenSettings = onOpenAccessibilitySettings)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FeatureCard(
                    title = stringResource(id = R.string.home_feel_every_tap_title),
                    subtitle = stringResource(id = R.string.home_feel_every_tap_subtitle),
                    icon = Icons.Rounded.TouchApp,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenFeelEveryTap,
                    enabled = globalEnabled,
                )
                FeatureCard(
                    title = stringResource(id = R.string.home_tactile_scrolling_title),
                    subtitle = stringResource(id = R.string.home_tactile_scrolling_subtitle),
                    icon = Icons.Rounded.SwipeUp,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenTactileScrolling,
                    enabled = globalEnabled,
                )
                FeatureCard(
                    title = stringResource(id = R.string.home_charging_haptics_title),
                    subtitle = stringResource(id = R.string.home_charging_haptics_subtitle),
                    icon = Icons.Rounded.BatteryChargingFull,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenChargingHaptics,
                    enabled = globalEnabled,
                )
                FeatureCard(
                    title = stringResource(id = R.string.home_button_haptics_title),
                    subtitle = stringResource(id = R.string.home_button_haptics_subtitle),
                    icon = Icons.Rounded.RadioButtonChecked,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenButtonHaptics,
                    enabled = globalEnabled,
                )
                FeatureCard(
                    title = stringResource(id = R.string.home_navbar_haptics_title),
                    subtitle = stringResource(id = R.string.home_navbar_haptics_subtitle),
                    icon = Icons.Rounded.Home,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenNavBarHaptics,
                    enabled = globalEnabled,
                )
                FeatureCard(
                    title = stringResource(id = R.string.home_unlock_haptics_title),
                    subtitle = stringResource(id = R.string.home_unlock_haptics_subtitle),
                    icon = Icons.Rounded.LockOpen,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconBg = MaterialTheme.colorScheme.secondary,
                    iconTint = MaterialTheme.colorScheme.onSecondary,
                    onClick = onOpenUnlockHaptics,
                    enabled = globalEnabled,
                )
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun GlobalToggleCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.home_global_toggle_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (enabled) stringResource(id = R.string.home_global_toggle_on) else stringResource(id = R.string.home_global_toggle_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun HomeHeader() {
    val junicodeFontFamily = remember { FontFamily(Font(R.font.junicode_italic)) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(id = R.string.home_greeting),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = junicodeFontFamily,
                fontSize = 15.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(id = R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "v3.0.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onAccent: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.5f
    Surface(
        color = accent.copy(alpha = if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.hapticClickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = iconBg.copy(alpha = alpha), shape = RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = alpha),
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = onAccent.copy(alpha = alpha),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onAccent.copy(alpha = alpha * 0.78f),
                )
            }
            if (enabled) {
                ChevronPill()
            }
        }
    }
}

@Composable
private fun ChevronPill() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
