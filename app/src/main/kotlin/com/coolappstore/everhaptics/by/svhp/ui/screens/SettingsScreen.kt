package com.coolappstore.everhaptics.by.svhp.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import com.coolappstore.everhaptics.by.svhp.data.ThemeMode
import com.coolappstore.everhaptics.by.svhp.ui.haptics.hapticClickable
import com.coolappstore.everhaptics.by.svhp.ui.theme.SeedBlue
import com.coolappstore.everhaptics.by.svhp.ui.theme.SeedGreen
import com.coolappstore.everhaptics.by.svhp.ui.theme.SeedPurple
import com.coolappstore.everhaptics.by.svhp.ui.theme.SeedRed
import com.coolappstore.everhaptics.by.svhp.ui.theme.SeedYellow
import com.coolappstore.everhaptics.by.svhp.ui.theme.HapticksSage

private val PresetColors = listOf(
    Color(0xFF4F5A28), // Olive Green (default)
    SeedPurple,
    SeedBlue,
    SeedGreen,
    SeedRed,
    SeedYellow,
    HapticksSage,
    Color(0xFF006A60), // Teal
    Color(0xFF984816), // Deep Orange
    Color(0xFF8B008B), // Dark Magenta
    Color(0xFF00008B), // Dark Blue
    Color(0xFF8B4513), // Saddle Brown
)

@Composable
fun SettingsScreen(
    settings: HapticsSettings,
    onUseDynamicColorsChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onSeedColorChange: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val appInDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") { SettingsHeader() }

            item(key = "appearance") {
                SettingsSection(
                    title = stringResource(R.string.settings_section_appearance),
                    icon = Icons.Rounded.Palette,
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_dynamic_color_title),
                        subtitle = null,
                        position = RowPosition.Top,
                        trailing = {
                            Switch(checked = settings.useDynamicColors, onCheckedChange = onUseDynamicColorsChange)
                        },
                    )

                    if (!settings.useDynamicColors) {
                        RowDivider()
                        ColorPaletteRow(
                            selectedColor = Color(settings.seedColor),
                            onColorSelected = { onSeedColorChange(it.toArgb()) },
                        )
                    }

                    RowDivider()

                    SettingsRow(
                        title = stringResource(R.string.settings_amoled_title),
                        subtitle = if (appInDarkTheme) {
                            stringResource(R.string.settings_amoled_subtitle)
                        } else {
                            stringResource(R.string.settings_amoled_subtitle_light)
                        },
                        position = RowPosition.Bottom,
                        trailing = {
                            Switch(checked = settings.amoledBlack, onCheckedChange = onAmoledBlackChange)
                        },
                    )

                    RowDivider()

                    ThemeModeRow(selected = settings.themeMode, onThemeModeChange = onThemeModeChange)
                }
            }

            item(key = "about") {
                SettingsSection(
                    title = stringResource(R.string.settings_section_about),
                    icon = Icons.Rounded.Settings,
                ) {
                    SettingsRow(
                        title = stringResource(R.string.settings_github_title),
                        subtitle = stringResource(R.string.settings_github_subtitle),
                        position = RowPosition.Single,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/hari161008/Hapticks-Fork".toUri(),
                            )
                            context.startActivity(intent)
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }
            }
            item(key = "bottom_inset") { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_color_palette_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Wrap colors in two rows
        val rows = PresetColors.chunked(6)
        rows.forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowColors.forEach { color ->
                    val isSelected = remember(selectedColor, color) {
                        selectedColor.toArgb() == color.toArgb()
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            )
                            .clickable { onColorSelected(color) },
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    val junicodeFontFamily = remember { FontFamily(Font(R.font.junicode_italic)) }
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_header_caption),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = junicodeFontFamily, fontSize = 15.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.settings_header_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column { content() }
            }
        }
    }
}

private enum class RowPosition { Top, Middle, Bottom, Single }

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    position: RowPosition = RowPosition.Middle,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val verticalPadding = when (position) {
        RowPosition.Top -> PaddingValues(top = 14.dp, bottom = 10.dp)
        RowPosition.Middle -> PaddingValues(vertical = 10.dp)
        RowPosition.Bottom -> PaddingValues(top = 10.dp, bottom = 14.dp)
        RowPosition.Single -> PaddingValues(vertical = 14.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.hapticClickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 14.dp)
            .padding(verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeRow(selected: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = stringResource(R.string.settings_theme_mode_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val modes = listOf(
            ThemeModeOption(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_mode_system), Icons.Rounded.Brightness6),
            ThemeModeOption(ThemeMode.LIGHT, stringResource(R.string.settings_theme_mode_light), Icons.Rounded.LightMode),
            ThemeModeOption(ThemeMode.DARK, stringResource(R.string.settings_theme_mode_dark), Icons.Rounded.DarkMode),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selected == option.mode,
                    onClick = { onThemeModeChange(option.mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    icon = { Icon(imageVector = option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                ) { Text(option.label) }
            }
        }
    }
}

private data class ThemeModeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)
