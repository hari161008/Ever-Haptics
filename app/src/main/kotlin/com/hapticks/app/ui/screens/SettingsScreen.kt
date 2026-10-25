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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.theme.PetalMaterialShapes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.luminance
import com.hapticks.app.ui.haptics.performHapticClick
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
    onPredictiveBackChange: (Boolean) -> Unit = {},
    onDepthBlurChange: (Boolean) -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenExploreApps: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersion = remember { AppVersion.get(context) }
    val appInDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    val scrollState = rememberScrollState()

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PetalCollapsingTopAppBar(
                title = stringResource(R.string.settings_header_title),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // ── Updates ──
            PetalSection(title = "Updates", modifier = Modifier.entrance(0)) {
                row { position ->
                    UpdateCard(
                        position = position,
                        appVersion = appVersion,
                        updateCheckState = updateCheckState,
                        updateInfo = updateInfo,
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
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Update,
                        title = "Auto-check on launch",
                        subtitle = "Get notified when a new version is available",
                        checked = settings.autoCheckUpdatesEnabled,
                        onCheckedChange = onAutoCheckUpdatesChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover8Leaf,
                        variant = PetalBadgeVariant.SECONDARY,
                    )
                }
            }

            // ── Community ──
            PetalSection(title = "Community & More", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.RateReview,
                        title = "Rate & Review",
                        subtitle = "Share your experience with Ever Haptics",
                        position = position,
                        badgeShape = PetalMaterialShapes.Flower,
                        onClick = { openUrl("https://docs.google.com/forms/d/e/1FAIpQLSci14N7YdirM32f2I7bT6GBv_sQ3KJ4hjM6qDKTVZfsCuIPtw/viewform?usp=header") },
                        trailing = { Icon(Icons.Rounded.OpenInBrowser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Reviews,
                        title = "Ratings & Reviews",
                        subtitle = "See what others are saying about the app",
                        position = position,
                        badgeShape = PetalMaterialShapes.SoftBurst,
                        variant = PetalBadgeVariant.SECONDARY,
                        onClick = onOpenReviews,
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Apps,
                        title = "Explore More Apps",
                        subtitle = "Discover other apps by the developer",
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        variant = PetalBadgeVariant.TERTIARY,
                        onClick = onOpenExploreApps,
                    )
                }
            }

            // ── Appearance ──
            PetalSection(title = stringResource(R.string.settings_section_appearance), modifier = Modifier.entrance(2)) {
                row { position ->
                    PetalGroupControlRow(
                        icon = Icons.Rounded.Brightness6,
                        title = stringResource(R.string.settings_theme_mode_title),
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                    ) {
                        PetalChoiceButtonGroup(
                            options = listOf(
                                PetalButtonGroupItem(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_mode_system), Icons.Rounded.Brightness6),
                                PetalButtonGroupItem(ThemeMode.LIGHT, stringResource(R.string.settings_theme_mode_light), Icons.Rounded.LightMode),
                                PetalButtonGroupItem(ThemeMode.DARK, stringResource(R.string.settings_theme_mode_dark), Icons.Rounded.DarkMode),
                            ),
                            selected = settings.themeMode,
                            onSelect = onThemeModeChange,
                        )
                    }
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.ColorLens,
                        title = stringResource(R.string.settings_dynamic_color_title),
                        checked = settings.useDynamicColors,
                        onCheckedChange = onUseDynamicColorsChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Flower,
                        variant = PetalBadgeVariant.TERTIARY,
                    )
                }
                if (!settings.useDynamicColors) {
                    row { position ->
                        PetalGroupControlRow(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.settings_color_palette_title),
                            position = position,
                            badgeShape = PetalMaterialShapes.Clover4Leaf,
                            variant = PetalBadgeVariant.SECONDARY,
                        ) {
                            ColorSwatches(selectedColor = Color(settings.seedColor), onColorSelected = { onSeedColorChange(it.toArgb()) })
                        }
                    }
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Contrast,
                        title = stringResource(R.string.settings_amoled_title),
                        subtitle = if (appInDarkTheme) stringResource(R.string.settings_amoled_subtitle) else stringResource(R.string.settings_amoled_subtitle_light),
                        checked = settings.amoledBlack,
                        onCheckedChange = onAmoledBlackChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie4Sided,
                        variant = PetalBadgeVariant.SECONDARY,
                    )
                }
            }

            // ── Motion ──
            PetalSection(
                title = "Motion & back gestures",
                description = "Live preview of the previous screen while you swipe back",
                modifier = Modifier.entrance(3),
            ) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Swipe,
                        title = "Predictive back animation",
                        subtitle = "Shrinks and rounds the screen as you swipe back",
                        checked = settings.predictiveBackEnabled,
                        onCheckedChange = onPredictiveBackChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Arrow,
                    )
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.BlurOn,
                        title = "Depth blur",
                        subtitle = "Blur and dim the screen behind during back gestures",
                        checked = settings.depthBlurEnabled,
                        onCheckedChange = onDepthBlurChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                        variant = PetalBadgeVariant.SECONDARY,
                    )
                }
            }

            // ── Battery saver ──
            PetalSection(title = stringResource(R.string.settings_battery_saver_section), modifier = Modifier.entrance(4)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.BatterySaver,
                        title = stringResource(R.string.settings_battery_saver_title),
                        subtitle = stringResource(R.string.settings_battery_saver_subtitle),
                        checked = settings.batterySaverDetectionEnabled,
                        onCheckedChange = onBatterySaverDetectionChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                        variant = PetalBadgeVariant.ERROR,
                    )
                }
            }

            // ── About ──
            PetalSection(title = "About", modifier = Modifier.entrance(5)) {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Ever Haptics",
                        subtitle = "Version $appVersion  |  by SVHP",
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie12Sided,
                        trailing = {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                                Text(
                                    "v$appVersion",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        },
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Code,
                        title = "GitHub",
                        subtitle = "Source code | Releases | Issue tracker",
                        position = position,
                        badgeShape = PetalMaterialShapes.Burst,
                        variant = PetalBadgeVariant.SECONDARY,
                        onClick = { openUrl("https://github.com/hari161008/Ever-Haptics") },
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Forum,
                        title = "Telegram App Support Group",
                        subtitle = "Announcements | Updates | Bug Fixes | Support",
                        position = position,
                        badgeShape = PetalMaterialShapes.Flower,
                        variant = PetalBadgeVariant.TERTIARY,
                        onClick = { openUrl("https://t.me/EverlastingAndroidTweak") },
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Campaign,
                        title = "App Recommending Channel",
                        subtitle = "Discover | Explore | Cool Apps",
                        position = position,
                        badgeShape = PetalMaterialShapes.Pentagon,
                        onClick = { openUrl("https://t.me/CoolAppStore") },
                    )
                }
            }
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
private fun UpdateCard(
    position: PetalGroupPosition,
    appVersion: String,
    updateCheckState: UpdateCheckState,
    updateInfo: UpdateChecker.UpdateInfo?,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    val isUpdateAvailable = updateCheckState is UpdateCheckState.UpdateAvailable
    val isDownloading = updateCheckState is UpdateCheckState.Downloading
    val isDownloaded = updateCheckState is UpdateCheckState.Downloaded
    val isChecking = updateCheckState is UpdateCheckState.Checking
    val isPositive = isUpdateAvailable || isDownloading || isDownloaded

    val containerColor by animateColorAsState(
        targetValue = when {
            isPositive -> MaterialTheme.colorScheme.primaryContainer
            updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.secondaryContainer
            updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> petalGroupColor()
        },
        animationSpec = tween(400), label = "uc_bg",
    )
    val onContainer by animateColorAsState(
        targetValue = when {
            isPositive -> MaterialTheme.colorScheme.onPrimaryContainer
            updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.onSecondaryContainer
            updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(400), label = "uc_fg",
    )
    val accent = when {
        updateCheckState is UpdateCheckState.Error -> MaterialTheme.colorScheme.error
        updateCheckState is UpdateCheckState.UpToDate -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    PetalGroupItem(position = position, color = containerColor) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // The single loading visual: indeterminate while checking, determinate while downloading.
                Crossfade(targetState = updateCheckState::class, label = "uc_icon") { kind ->
                    when (kind) {
                        UpdateCheckState.Checking::class ->
                            PetalContainedLoadingIndicator(size = 56.dp)
                        UpdateCheckState.Downloading::class -> {
                            val pct = (updateCheckState as? UpdateCheckState.Downloading)?.progress ?: 0
                            PetalContainedLoadingIndicator(size = 56.dp, progress = { pct / 100f })
                        }
                        else -> {
                            val icon = when (kind) {
                                UpdateCheckState.UpdateAvailable::class -> Icons.Rounded.NewReleases
                                UpdateCheckState.UpToDate::class, UpdateCheckState.Downloaded::class -> Icons.Rounded.CheckCircle
                                UpdateCheckState.Error::class -> Icons.Rounded.ErrorOutline
                                else -> Icons.Rounded.SystemUpdate
                            }
                            FeatureIcon(
                                icon = icon,
                                tint = accent,
                                size = 56.dp,
                                iconSize = 28.dp,
                                backgroundAlpha = 0.2f,
                                shape = PetalMaterialShapes.Cookie9Sided,
                            )
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
                            isChecking -> "Checking…"
                            else -> "Ever Haptics"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onContainer,
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
                            color = onContainer.copy(alpha = 0.72f),
                        )
                    }
                }
            }

            if (isUpdateAvailable && !updateInfo?.releaseNotes.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("What's new", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold)
                    val notes = updateInfo!!.releaseNotes
                    Text(
                        notes.take(300).let { if (notes.length > 300) "$it…" else it },
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.75f),
                    )
                }
            }

            when {
                isUpdateAvailable -> PetalButton(onClick = onDownloadClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download & Install")
                }
                isDownloaded -> PetalButton(onClick = onDownloadClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.InstallMobile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Install Now")
                }
                !isChecking && !isDownloading -> PetalButton(
                    onClick = onCheckClick,
                    style = PetalButtonStyle.TONAL,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (updateCheckState is UpdateCheckState.UpToDate || updateCheckState is UpdateCheckState.Error) "Check Again" else "Check for Updates")
                }
            }
        }
    }
}

/** Seed colour picker: swatches morph from a circle to a cookie when selected. */
@Composable
private fun ColorSwatches(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PresetColors.chunked(6).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                rowColors.forEach { color ->
                    val isSelected = selectedColor.toArgb() == color.toArgb()
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(if (isSelected) PetalMaterialShapes.Cookie9Sided.toShape() else CircleShape)
                            .background(color)
                            .clickable {
                                context.performHapticClick()
                                onColorSelected(color)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
