package com.hapticks.app.ui.screens

import com.hapticks.app.ui.theme.PetalIcons
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.selection.SelectionContainer
import com.hapticks.app.HapticksApp
import com.hapticks.app.haptics.HapticPattern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.hapticks.app.ui.components.*
import com.hapticks.app.ui.theme.PetalMaterialShapes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.luminance
import com.hapticks.app.ui.haptics.performHapticClick
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.hapticks.app.util.BackupInfo
import com.hapticks.app.util.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RATE_REVIEW_URL =
    "https://docs.google.com/forms/d/e/1FAIpQLSci14N7YdirM32f2I7bT6GBv_sQ3KJ4hjM6qDKTVZfsCuIPtw/viewform?usp=header"

private val PresetColors = listOf(
    Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
    Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031),
    Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B),
    Color(0xFF4F5A28), SeedPurple, SeedBlue, SeedGreen, SeedRed, SeedYellow,
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
    onOpenRateReview: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenExploreApps: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onRestoreAllSettings: (HapticsSettings) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersion = remember { AppVersion.get(context) }
    val appInDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var showFloatingColorPicker by remember { mutableStateOf(false) }
    var hexInput by remember(settings.seedColor) {
        mutableStateOf(String.format("%06X", 0xFFFFFF and settings.seedColor))
    }
    var hexError by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showBackupDialog by remember { mutableStateOf(false) }
    var pendingRestoreInfo by remember { mutableStateOf<BackupInfo?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = BackupManager.writeBackupToUri(context, uri, settings)
                android.widget.Toast.makeText(
                    context,
                    if (ok) "Backup saved successfully" else "Failed to save backup",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val info = BackupManager.readBackupFromUri(context, uri)
                if (info != null) {
                    pendingRestoreInfo = info
                    showRestoreDialog = true
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Invalid backup file format",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun applyHexColor(hex: String) {
        val cleaned = hex.trimStart('#').uppercase()
        if (cleaned.length == 6) {
            try {
                val colorInt = android.graphics.Color.parseColor("#$cleaned")
                onSeedColorChange(colorInt)
                hexError = false
            } catch (_: Exception) {
                hexError = true
            }
        } else {
            hexError = true
        }
    }

    var showHapticCheck by remember { mutableStateOf(false) }
    var hapticCheckTick by remember { mutableIntStateOf(0) }
    val hapticEngine = remember { (context.applicationContext as? HapticksApp)?.hapticEngine }
    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    val scrollState = rememberScrollState()

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            runCatching {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    "Open with",
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }.onFailure {
                android.widget.Toast.makeText(context, "Could not open link: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // ── Updates ──
            PetalSection(title = "Updates", modifier = Modifier.entrance(0)) {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Check For Updates",
                        subtitle = "Current version: v$appVersion",
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                        onClick = onOpenUpdates,
                    )
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Autorenew,
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

            // ── Collab ──
            CollabCard(
                onClick = { openUrl("https://my-petal-browser.vercel.app/") },
                modifier = Modifier.entrance(1),
            )

            // ── Community ──
            PetalSection(title = "Community & More", modifier = Modifier.entrance(1)) {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.RateReview,
                        title = "Rate & Review",
                        subtitle = "Share your experience with Ever Haptics",
                        position = position,
                        badgeShape = PetalMaterialShapes.Flower,
                        onClick = { openUrl(RATE_REVIEW_URL) },
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
                        icon = PetalIcons.Brightness,
                        title = stringResource(R.string.settings_theme_mode_title),
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                    ) {
                        PetalChoiceButtonGroup(
                            options = listOf(
                                PetalButtonGroupItem(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_mode_system), PetalIcons.Brightness),
                                PetalButtonGroupItem(ThemeMode.LIGHT, stringResource(R.string.settings_theme_mode_light), PetalIcons.LightMode),
                                PetalButtonGroupItem(ThemeMode.DARK, stringResource(R.string.settings_theme_mode_dark), PetalIcons.DarkMode),
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
                            CustomColorContainer(
                                currentColor = Color(settings.seedColor),
                                onColorSelected = { onSeedColorChange(it.toArgb()) },
                                onPickInteractive = { showFloatingColorPicker = true },
                                hexInput = hexInput,
                                onHexInputChange = {
                                    hexInput = it.trimStart('#').uppercase().take(6)
                                    hexError = false
                                },
                                hexError = hexError,
                                onApplyHex = {
                                    applyHexColor(hexInput)
                                    keyboardController?.hide()
                                },
                            )
                        }
                    }
                }
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.Brightness,
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

            // ── Backup & restore ──
            PetalSection(
                title = "Backup & restore",
                description = "Backup or restore all app configurations and custom haptic sequences",
                modifier = Modifier.entrance(4),
            ) {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Backup,
                        title = "Create backup",
                        subtitle = "Save configurations, custom sequences & preferences",
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie9Sided,
                        variant = PetalBadgeVariant.PRIMARY,
                        onClick = { showBackupDialog = true },
                    )
                }
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Rounded.Restore,
                        title = "Restore backup",
                        subtitle = "Restore all configurations from a backup file",
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        variant = PetalBadgeVariant.TERTIARY,
                        onClick = {
                            try {
                                restoreLauncher.launch("application/json")
                            } catch (_: Exception) {
                                restoreLauncher.launch("*/*")
                            }
                        },
                    )
                }
            }

            // ── Battery saver ──
            PetalSection(title = stringResource(R.string.settings_battery_saver_section), modifier = Modifier.entrance(5)) {
                row { position ->
                    PetalGroupToggleRow(
                        icon = PetalIcons.BatterySaver,
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
                        icon = PetalIcons.Vibration,
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
                        icon = PetalIcons.Vibration,
                        title = "Haptics check",
                        subtitle = "Test vibration on this phone and see what the engine is using",
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                        variant = PetalBadgeVariant.SECONDARY,
                        enabled = hapticEngine != null,
                        onClick = { showHapticCheck = true },
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

            if (showHapticCheck && hapticEngine != null) {
                val report = remember(hapticCheckTick) { hapticEngine.diagnostics() }
                PetalAlertDialog(
                    onDismissRequest = { showHapticCheck = false },
                    title = "Haptics check",
                    icon = PetalIcons.Vibration,
                    message = "Tap Run test and feel for a vibration. If nothing happens, run it a few times so the engine can try other delivery methods, then share this report.",
                    confirmText = "Run test",
                    onConfirm = {
                        hapticEngine.playStrong(HapticPattern.CLICK)
                        scope.launch {
                            delay(900L)
                            hapticCheckTick++
                        }
                    },
                    dismissText = "Close",
                    content = {
                        SelectionContainer {
                            Text(
                                text = report,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            }

            if (showFloatingColorPicker) {
                FloatingColorPickerDialog(
                    initialColor = Color(settings.seedColor),
                    onDismiss = { showFloatingColorPicker = false },
                    onColorSelected = { color ->
                        onSeedColorChange(color.toArgb())
                        hexInput = String.format("%06X", 0xFFFFFF and color.toArgb())
                        hexError = false
                        showFloatingColorPicker = false
                    }
                )
            }

            if (showBackupDialog) {
                CreateBackupDialog(
                    onDismiss = { showBackupDialog = false },
                    onShare = {
                        showBackupDialog = false
                        scope.launch {
                            val file = BackupManager.createBackupFile(context, settings)
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                            } else {
                                android.widget.Toast.makeText(context, "Failed to create backup", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSave = {
                        showBackupDialog = false
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        saveBackupLauncher.launch("EverHaptics_backup_$timestamp.json")
                    }
                )
            }

            if (showRestoreDialog && pendingRestoreInfo != null) {
                val info = pendingRestoreInfo!!
                RestoreBackupDialog(
                    info = info,
                    onDismiss = {
                        showRestoreDialog = false
                        pendingRestoreInfo = null
                    },
                    onRestore = {
                        showRestoreDialog = false
                        onRestoreAllSettings(info.settings)
                        pendingRestoreInfo = null
                        android.widget.Toast.makeText(
                            context,
                            "Configuration restored successfully!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomColorContainer(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onPickInteractive: () -> Unit,
    hexInput: String,
    onHexInputChange: (String) -> Unit,
    hexError: Boolean,
    onApplyHex: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Preset Colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(PresetColors) { color ->
                val isSelected = currentColor.toArgb() == color.toArgb()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable {
                            context.performHapticClick()
                            onColorSelected(color)
                        },
                )
            }
        }

        OutlinedButton(
            onClick = onPickInteractive,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50.dp),
        ) {
            Icon(
                Icons.Outlined.Colorize,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Pick Custom Color (Interactive)")
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor("#${hexInput.trimStart('#')}"))
                        } catch (_: Exception) {
                            Color.Gray
                        }
                    ),
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = onHexInputChange,
                label = { Text("Hex Color") },
                prefix = { Text("#") },
                isError = hexError,
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onApplyHex()
                    keyboardController?.hide()
                }),
                shape = RoundedCornerShape(12.dp),
                supportingText = if (hexError) {
                    { Text("Enter a valid 6-digit hex code") }
                } else null,
            )
            Button(
                onClick = {
                    onApplyHex()
                    keyboardController?.hide()
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun FloatingColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val hsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(initialColor) { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember(initialColor) { mutableFloatStateOf(hsv[2].coerceIn(0f, 1f)) }

    val currentColor = remember(hue, sat, value) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        Color(rgb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Color Picker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(currentColor)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Shade & Brightness",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                sat = (down.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - down.position.y / size.height).coerceIn(0f, 1f)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, pureHueColor),
                            ),
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                            ),
                        )

                        val pointerX = (sat * size.width).coerceIn(0f, size.width)
                        val pointerY = ((1f - value) * size.height).coerceIn(0f, size.height)
                        val pointerCenter = Offset(pointerX, pointerY)

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.6f),
                            radius = 13.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                        drawCircle(
                            color = currentColor,
                            radius = 6.dp.toPx(),
                            center = pointerCenter,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Hue Spectrum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                hue = (down.position.x / size.width * 360f).coerceIn(0f, 360f)
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                    change.consume()
                                }
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rainbow = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red,
                        )
                        drawRect(brush = Brush.horizontalGradient(rainbow))

                        val thumbX = ((hue / 360f) * size.width).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
                        val thumbCenter = Offset(thumbX, size.height / 2)

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = 14.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        val currentHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawCircle(
                            color = currentHueColor,
                            radius = 8.dp.toPx(),
                            center = thumbCenter,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val hexStr = remember(currentColor) {
                    String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Hex Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = hexStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Select Color")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FeatureIcon(
                    icon = Icons.Rounded.Backup,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 56.dp,
                    iconSize = 28.dp,
                    shape = PetalMaterialShapes.Cookie9Sided,
                )
                Text(
                    text = "Create Backup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Backup all Ever Haptics configurations, custom haptic sequences, sensitivities, and excluded apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PetalButton(
                        onClick = onShare,
                        style = PetalButtonStyle.TONAL,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                    PetalButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupDialog(
    info: BackupInfo,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FeatureIcon(
                    icon = Icons.Rounded.Restore,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 56.dp,
                    iconSize = 28.dp,
                    shape = PetalMaterialShapes.Clover4Leaf,
                )
                Text(
                    text = "Restore Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Backup Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(info.formattedDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("App Version:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("v${info.appVersion}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active Features:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${info.activeFeaturesCount} configured", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    text = "This will replace your current settings and restore all custom patterns and configurations from the backup file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PetalButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Restore")
                    }
                }
            }
        }
    }
}

@Composable
private fun CollabCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PetalContainment(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(PetalMaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFlorist, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(26.dp))
            }
            Icon(PetalIcons.Close, null, modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PetalIcons.Vibration, null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(26.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Petal \u00D7 Ever Haptics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "A collab between Petal Browser and Ever Haptics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
        PetalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Rounded.Handshake, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Collab", style = MaterialTheme.typography.labelLarge)
        }
    }
}
