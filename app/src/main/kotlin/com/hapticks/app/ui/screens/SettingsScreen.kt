package com.hapticks.app.ui.screens

import android.content.Intent
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appInDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var updateCheckState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    if (showUpdateDialog && updateInfo != null) {
        UpdateAvailableDialog(
            info = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onDownload = {
                showUpdateDialog = false
                val info = updateInfo!!
                if (info.downloadUrl.isNotBlank()) UpdateChecker.downloadAndInstall(context, info.downloadUrl, info.latestVersion)
            },
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") { SettingsHeader() }

            // ── Updates section FIRST ──
            item(key = "updates") {
                SettingsSection(title = "Updates", icon = Icons.Rounded.SystemUpdate, iconTint = FeatureColors.Updates) {
                    SettingsRow(
                        title = "Check for updates",
                        subtitle = when (val s = updateCheckState) {
                            is UpdateCheckState.Idle -> "Tap to check for a newer version of Ever Haptics"
                            is UpdateCheckState.Checking -> "Checking…"
                            is UpdateCheckState.UpToDate -> "You are on the latest version"
                            is UpdateCheckState.UpdateAvailable -> "Version ${s.version} is available!"
                            is UpdateCheckState.Error -> "Failed to check: ${s.message}"
                        },
                        position = RowPosition.Single,
                        onClick = {
                            if (updateCheckState !is UpdateCheckState.Checking) {
                                updateCheckState = UpdateCheckState.Checking
                                scope.launch {
                                    UpdateChecker.checkForUpdate().fold(
                                        onSuccess = { info ->
                                            if (info.isUpdateAvailable) {
                                                updateCheckState = UpdateCheckState.UpdateAvailable(info.latestVersion)
                                                updateInfo = info; showUpdateDialog = true
                                            } else { updateCheckState = UpdateCheckState.UpToDate }
                                        },
                                        onFailure = { e -> updateCheckState = UpdateCheckState.Error(e.message ?: "Unknown error") },
                                    )
                                }
                            }
                        },
                        trailing = {
                            when (updateCheckState) {
                                is UpdateCheckState.Checking -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                is UpdateCheckState.UpdateAvailable -> Icon(Icons.Rounded.NewReleases, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                is UpdateCheckState.UpToDate -> Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                is UpdateCheckState.Error -> Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                else -> Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
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
                SettingsSection(title = stringResource(R.string.settings_section_about), icon = Icons.Rounded.Settings) {
                    SettingsRow(
                        title = stringResource(R.string.settings_github_title),
                        subtitle = stringResource(R.string.settings_github_subtitle),
                        position = RowPosition.Single,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/hari161008/Ever-Haptics".toUri())) },
                        trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    )
                }
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
    data class Error(val message: String) : UpdateCheckState
}

@Composable
private fun UpdateAvailableDialog(info: UpdateChecker.UpdateInfo, onDismiss: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.NewReleases, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Update Available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${info.latestVersion} is available. Download and install it now?", style = MaterialTheme.typography.bodyMedium)
                if (info.releaseNotes.isNotBlank()) {
                    HorizontalDivider()
                    Text("What's new:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(info.releaseNotes.take(400).let { if (info.releaseNotes.length > 400) "$it…" else it }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { Button(onClick = onDownload) { Text("Download & Install") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
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
