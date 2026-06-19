package com.hapticks.app.ui.screens

import android.content.Intent
import com.hapticks.app.util.AppVersion
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.data.ThemeMode
import com.hapticks.app.ui.components.FeatureIcon
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.haptics.hapticClickable
import com.hapticks.app.ui.theme.SeedBlue
import com.hapticks.app.ui.theme.SeedGreen
import com.hapticks.app.ui.theme.SeedRed
import com.hapticks.app.ui.theme.SeedYellow
import com.hapticks.app.ui.theme.SeedPurple
import com.hapticks.app.util.UpdateChecker
import kotlinx.coroutines.launch

private val PresetColors = listOf(
    Color(0xFF4F5A28), SeedPurple, SeedBlue, SeedGreen, SeedRed, SeedYellow,
    Color(0xFFD6E09A), Color(0xFF006A60), Color(0xFF984816), Color(0xFF8B008B),
    Color(0xFF00008B), Color(0xFF8B4513),
)

@Composable
fun SettingsScreen(
    settings: HapticsSettings,
    onUseDynamicColorsChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onSeedColorChange: (Int) -> Unit = {},
    onBatterySaverDetectionChange: (Boolean) -> Unit = {},
    onAutoCheckUpdatesChange: (Boolean) -> Unit = {},
    onOpenReviews: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersion = remember { AppVersion.get(context) }
    val appInDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 90.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") { SettingsHeader() }

            // ── Updates section FIRST ──
            item(key = "updates") {
                UpdateCard(
                    appVersion = appVersion,
                    updateCheckState = updateCheckState,
                    updateInfo = updateInfo,
                    autoCheckEnabled = settings.autoCheckUpdatesEnabled,
                    onAutoCheckChange = onAutoCheckUpdatesChange,
                    onCheckClick = {
                        if (updateCheckState is UpdateCheckState.Idle ||
                            updateCheckState is UpdateCheckState.UpToDate ||
                            updateCheckState is UpdateCheckState.Error) {
                            updateCheckState = UpdateCheckState.Checking
                            scope.launch {
                                UpdateChecker.checkForUpdate(appVersion).fold(
                                    onSuccess = { info ->
                                        updateInfo = info
                                        updateCheckState = if (info.isUpdateAvailable)
                                            UpdateCheckState.UpdateAvailable(info.latestVersion)
                                        else UpdateCheckState.UpToDate
                                    },
                                    onFailure = { e -> updateCheckState = UpdateCheckState.Error(e.message ?: "Unknown error") },
                                )
                            }
                        }
                    },
                    onDownloadClick = {
                        val info = updateInfo ?: return@UpdateCard
                        if (info.downloadUrl.isBlank()) return@UpdateCard
                        updateCheckState = UpdateCheckState.Downloading(0)
                        scope.launch {
                            UpdateChecker.downloadWithProgress(context, info.downloadUrl, info.latestVersion) { state ->
                                when (state) {
                                    is UpdateChecker.DownloadState.Progress -> updateCheckState = UpdateCheckState.Downloading(state.percent)
                                    is UpdateChecker.DownloadState.Done -> updateCheckState = UpdateCheckState.Downloaded
                                    is UpdateChecker.DownloadState.Error -> updateCheckState = UpdateCheckState.Error(state.message)
                                }
                            }
                        }
                    },
                )
            }

            item(key = "rate_review") {
                RateAndReviewSection(context = context, onOpenReviews = onOpenReviews)
            }

                        // ── Appearance section ──
            item(key = "appearance") {
                SettingsSection(title = stringResource(R.string.settings_section_appearance), icon = Icons.Rounded.Palette, iconTint = FeatureColors.Keyboard) {
                    SettingsRow(title = stringResource(R.string.settings_dynamic_color_title), subtitle = null, position = RowPosition.Top, trailing = { Switch(checked = settings.useDynamicColors, onCheckedChange = onUseDynamicColorsChange) })
                    if (!settings.useDynamicColors) {
                        RowDivider()
                        ColorPaletteRow(selectedColor = Color(settings.seedColor), onColorSelected = { onSeedColorChange(it.toArgb()) })
                    }
                    RowDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_amoled_title),
                        subtitle = if (appInDarkTheme) stringResource(R.string.settings_amoled_subtitle) else stringResource(R.string.settings_amoled_subtitle_light),
                        position = RowPosition.Bottom,
                        trailing = { Switch(checked = settings.amoledBlack, onCheckedChange = onAmoledBlackChange) },
                    )
                    RowDivider()
                    ThemeModeRow(selected = settings.themeMode, onThemeModeChange = onThemeModeChange)
                }
            }

            // ── Battery Saver Detection ──
            item(key = "battery_saver") {
                SettingsSection(
                    title = stringResource(R.string.settings_battery_saver_section),
                    icon = Icons.Rounded.BatterySaver,
                    iconTint = MaterialTheme.colorScheme.error,
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_battery_saver_title),
                        subtitle = stringResource(R.string.settings_battery_saver_subtitle),
                        position = RowPosition.Single,
                        trailing = {
                            Switch(
                                checked = settings.batterySaverDetectionEnabled,
                                onCheckedChange = onBatterySaverDetectionChange,
                            )
                        },
                    )
                }
            }

            // ── About ──
            item(key = "about") {
                AboutSection(context = context, appVersion = appVersion)
            }
            item(key = "bottom_inset") { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

private sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class UpdateAvailable(val version: String) : UpdateCheckState
    data class Downloading(val progress: Int) : UpdateCheckState
    data object Downloaded : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

@Composable
private fun RateAndReviewSection(context: android.content.Context, onOpenReviews: () -> Unit) {
    val rateColor = FeatureColors.MusicHaptics
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureIcon(icon = Icons.Rounded.Star, tint = rateColor, size = 32.dp, iconSize = 17.dp, cornerRadius = 10.dp, backgroundAlpha = 0.15f)
            Text("Rate & Review", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // Rate & Review card → opens Google Form in browser
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().hapticClickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                    "https://docs.google.com/forms/d/e/1FAIpQLSci14N7YdirM32f2I7bT6GBv_sQ3KJ4hjM6qDKTVZfsCuIPtw/viewform?usp=header".toUri())
                context.startActivity(intent)
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(rateColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.RateReview, null, tint = rateColor, modifier = Modifier.size(26.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Rate & Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Share your experience with Ever Haptics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        // Check Ratings & Reviews card → opens WebView in-app
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().hapticClickable { onOpenReviews() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(rateColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Reviews, null, tint = rateColor, modifier = Modifier.size(26.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Check Ratings & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("See what others are saying about the app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UpdateCard(
    appVersion: String,
    updateCheckState: UpdateCheckState,
    updateInfo: UpdateChecker.UpdateInfo?,
    autoCheckEnabled: Boolean,
    onAutoCheckChange: (Boolean) -> Unit,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    val isUpdateAvailable = updateCheckState is UpdateCheckState.UpdateAvailable
    val isDownloading = updateCheckState is UpdateCheckState.Downloading
    val isDownloaded = updateCheckState is UpdateCheckState.Downloaded
    val isChecking = updateCheckState is UpdateCheckState.Checking

    val accentColor = when {
        isUpdateAvailable || isDownloading || isDownloaded -> MaterialTheme.colorScheme.primary
        updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.secondary
        updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor by animateColorAsState(
        targetValue = when {
            isUpdateAvailable || isDownloading || isDownloaded -> MaterialTheme.colorScheme.primaryContainer
            updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.secondaryContainer
            updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(400), label = "uc_bg",
    )
    val onContainerColor by animateColorAsState(
        targetValue = when {
            isUpdateAvailable || isDownloading || isDownloaded -> MaterialTheme.colorScheme.onPrimaryContainer
            updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.onSecondaryContainer
            updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(400), label = "uc_fg",
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureIcon(icon = Icons.Rounded.SystemUpdate, tint = FeatureColors.Updates, size = 32.dp, iconSize = 17.dp, cornerRadius = 10.dp, backgroundAlpha = 0.15f)
            Text("Updates", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        // Main update card
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top row: icon + text + badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Crossfade(targetState = updateCheckState, label = "uc_icon") { state ->
                            when (state) {
                                is UpdateCheckState.Checking -> CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = accentColor)
                                is UpdateCheckState.UpdateAvailable -> Icon(Icons.Rounded.NewReleases, null, tint = accentColor, modifier = Modifier.size(28.dp))
                                is UpdateCheckState.UpToDate -> Icon(Icons.Rounded.CheckCircle, null, tint = accentColor, modifier = Modifier.size(28.dp))
                                is UpdateCheckState.Downloading -> CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = accentColor)
                                is UpdateCheckState.Downloaded -> Icon(Icons.Rounded.CheckCircle, null, tint = accentColor, modifier = Modifier.size(28.dp))
                                is UpdateCheckState.Error -> Icon(Icons.Rounded.ErrorOutline, null, tint = accentColor, modifier = Modifier.size(28.dp))
                                else -> Icon(Icons.Rounded.SystemUpdate, null, tint = accentColor, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = when {
                                isUpdateAvailable -> "Update Available"
                                isDownloading -> "Downloading…"
                                isDownloaded -> "Ready to Install"
                                updateCheckState is UpdateCheckState.UpToDate -> "Up to Date"
                                updateCheckState is UpdateCheckState.Error -> "Check Failed"
                                else -> "Ever Haptics"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = onContainerColor,
                        )
                        Crossfade(targetState = updateCheckState, label = "uc_sub") { state ->
                            Text(
                                text = when (state) {
                                    is UpdateCheckState.Idle -> "v$appVersion  ·  Tap to check for updates"
                                    is UpdateCheckState.Checking -> "Fetching latest release info…"
                                    is UpdateCheckState.UpToDate -> "v$appVersion is the latest release"
                                    is UpdateCheckState.UpdateAvailable -> "v${state.version} is ready  ·  current: v$appVersion"
                                    is UpdateCheckState.Downloading -> "${state.progress}%  ·  Saving to Downloads folder"
                                    is UpdateCheckState.Downloaded -> "APK saved to Downloads — tap Install"
                                    is UpdateCheckState.Error -> state.message
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = onContainerColor.copy(alpha = 0.72f),
                            )
                        }
                    }
                    // Version chip
                    Surface(color = accentColor.copy(alpha = 0.18f), shape = RoundedCornerShape(20.dp)) {
                        Text("v$appVersion", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }

                // Download progress bar
                AnimatedVisibility(visible = isDownloading) {
                    val progress = (updateCheckState as? UpdateCheckState.Downloading)?.progress ?: 0
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.2f),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Downloading APK", style = MaterialTheme.typography.labelSmall, color = onContainerColor.copy(alpha = 0.6f))
                            Text("$progress%", style = MaterialTheme.typography.labelSmall, color = onContainerColor.copy(alpha = 0.6f))
                        }
                    }
                }

                // Release notes
                if (isUpdateAvailable && !updateInfo?.releaseNotes.isNullOrBlank()) {
                    HorizontalDivider(color = onContainerColor.copy(alpha = 0.12f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What's new", style = MaterialTheme.typography.labelLarge, color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        val notes = updateInfo!!.releaseNotes
                        Text(notes.take(300).let { if (notes.length > 300) "$it…" else it }, style = MaterialTheme.typography.bodySmall, color = onContainerColor.copy(alpha = 0.75f))
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    when {
                        isUpdateAvailable -> {
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download & Install")
                            }
                        }
                        isDownloaded -> {
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            ) {
                                Icon(Icons.Rounded.InstallMobile, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Install Now")
                            }
                        }
                        !isChecking && !isDownloading -> {
                            OutlinedButton(
                                onClick = onCheckClick,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp), tint = accentColor)
                                Spacer(Modifier.width(6.dp))
                                Text(if (updateCheckState is UpdateCheckState.UpToDate || updateCheckState is UpdateCheckState.Error) "Check Again" else "Check for Updates", color = accentColor)
                            }
                        }
                    }
                }
            }
        }

        // Auto-check toggle
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Auto-check on launch", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Get notified when a new version is available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = autoCheckEnabled, onCheckedChange = onAutoCheckChange)
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.settings_color_palette_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PresetColors.chunked(6).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowColors.forEach { color ->
                    val isSelected = remember(selectedColor, color) { selectedColor.toArgb() == color.toArgb() }
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color)
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape))
                        .clickable { onColorSelected(color) }) {
                        if (isSelected) Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)).align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    val junicode = remember { FontFamily(Font(R.font.junicode_italic)) }
    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.settings_header_caption), style = MaterialTheme.typography.labelLarge.copy(fontFamily = junicode, fontSize = 15.sp), color = MaterialTheme.colorScheme.primary)
        Text(text = stringResource(R.string.settings_header_title), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, iconTint: androidx.compose.ui.graphics.Color = FeatureColors.Settings, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureIcon(icon = icon, tint = iconTint, size = 32.dp, iconSize = 17.dp, cornerRadius = 10.dp, backgroundAlpha = 0.15f)
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) { Column { content() } }
        }
    }
}

private enum class RowPosition { Top, Middle, Bottom, Single }

@Composable
private fun SettingsRow(title: String, subtitle: String? = null, position: RowPosition = RowPosition.Middle, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val vPad = when (position) {
        RowPosition.Top -> PaddingValues(top = 14.dp, bottom = 10.dp)
        RowPosition.Middle -> PaddingValues(vertical = 10.dp)
        RowPosition.Bottom -> PaddingValues(top = 10.dp, bottom = 14.dp)
        RowPosition.Single -> PaddingValues(vertical = 14.dp)
    }
    Row(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.hapticClickable(onClick = onClick) else Modifier).defaultMinSize(minHeight = 52.dp).padding(horizontal = 14.dp).padding(vPad), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing?.invoke()
    }
}

@Composable
private fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(selected: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.settings_theme_mode_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val modes = listOf(
            ThemeModeOption(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_mode_system), Icons.Rounded.Brightness6),
            ThemeModeOption(ThemeMode.LIGHT, stringResource(R.string.settings_theme_mode_light), Icons.Rounded.LightMode),
            ThemeModeOption(ThemeMode.DARK, stringResource(R.string.settings_theme_mode_dark), Icons.Rounded.DarkMode),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, option ->
                SegmentedButton(selected = selected == option.mode, onClick = { onThemeModeChange(option.mode) }, shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size), icon = { Icon(option.icon, null, modifier = Modifier.size(18.dp)) }) { Text(option.label) }
            }
        }
    }
}

private data class ThemeModeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)

@Composable
private fun AboutSection(context: android.content.Context, appVersion: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureIcon(
                icon = Icons.Rounded.Info,
                tint = FeatureColors.Settings,
                size = 32.dp,
                iconSize = 17.dp,
                cornerRadius = 10.dp,
                backgroundAlpha = 0.15f,
            )
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // App Version banner card
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Ever Haptics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Version $appVersion  |  by SVHP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        "v$appVersion",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        // Link cards
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                AboutLinkRow(
                    icon = Icons.Rounded.Code,
                    title = "GitHub",
                    subtitle = "Source code | Releases | Issue tracker",
                    isFirst = true,
                    isLast = false,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/hari161008/Ever-Haptics".toUri())
                        )
                    },
                )
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                AboutLinkRow(
                    icon = Icons.Rounded.Forum,
                    title = "Telegram App Support Group",
                    subtitle = "Announcements | Updates | Bug Fixes | Support",
                    isFirst = false,
                    isLast = false,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://t.me/EverlastingAndroidTweak".toUri())
                        )
                    },
                )
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
                AboutLinkRow(
                    icon = Icons.Rounded.Campaign,
                    title = "App Recommending Channel",
                    subtitle = "Discover | Explore | Cool Apps",
                    isFirst = false,
                    isLast = true,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://t.me/CoolAppStore".toUri())
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val topRadius    = if (isFirst) 24.dp else 4.dp
    val bottomRadius = if (isLast) 24.dp else 4.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius))
            .hapticClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
