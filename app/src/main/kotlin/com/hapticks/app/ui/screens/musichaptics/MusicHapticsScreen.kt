package com.hapticks.app.ui.screens.musichaptics

import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.theme.PetalMaterialShapes
import com.hapticks.app.ui.components.PetalFeatureScreen
import android.Manifest
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
import com.hapticks.app.ui.components.PetalSlider
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hapticks.app.R
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalButtonGroupItem
import com.hapticks.app.ui.components.PetalChoiceButtonGroup
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.data.MusicHapticsSource
import com.hapticks.app.service.MusicHapticsService
import com.hapticks.app.service.MusicHapticsTileService
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalGroup
import com.hapticks.app.ui.components.PetalGroupRow
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalSliderRow
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.components.ScreenIconHeader
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicHapticsScreen(
    settings: HapticsSettings,
    onMusicHapticsEnabledChange: (Boolean) -> Unit,
    onMusicHapticsSourceChange: (MusicHapticsSource) -> Unit,
    onSensitivityCommit: (Float) -> Unit,
    onStrengthCommit: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // RECORD_AUDIO is needed for every source: the microphone foreground service type and the
    // device-audio Visualizer both require it.
    val hasMicPermission = remember {
        mutableStateOf(MusicHapticsService.hasRecordAudioPermission(ctx))
    }
    val latestSettings by rememberUpdatedState(settings)
    var pendingEnable by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasMicPermission.value = MusicHapticsService.hasRecordAudioPermission(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.RECORD_AUDIO] == true ||
            MusicHapticsService.hasRecordAudioPermission(ctx)
        hasMicPermission.value = granted
        if (granted && pendingEnable) {
            onMusicHapticsEnabledChange(true)
            MusicHapticsService.start(ctx, latestSettings.musicHapticsSource)
        }
        pendingEnable = false
    }

    fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    // Keep saved state and reality in sync: no permission means the feature cannot be on.
    LaunchedEffect(settings.musicHapticsEnabled, hasMicPermission.value) {
        if (settings.musicHapticsEnabled) {
            if (hasMicPermission.value) MusicHapticsService.start(ctx, settings.musicHapticsSource)
            else onMusicHapticsEnabledChange(false)
        } else {
            MusicHapticsService.stop(ctx)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beat_pulse",
    )

    var sensitivityDraft by remember(settings.musicHapticsSensitivity) { mutableFloatStateOf(settings.musicHapticsSensitivity) }
    var sensitivityTick by remember { mutableIntStateOf(slider01ToTickIndex(settings.musicHapticsSensitivity)) }
    var strengthDraft by remember(settings.musicHapticsStrength) { mutableFloatStateOf(settings.musicHapticsStrength) }
    var strengthTick by remember { mutableIntStateOf(slider01ToTickIndex(settings.musicHapticsStrength)) }

    PetalFeatureScreen(title = stringResource(R.string.music_haptics_screen_title), onBack = onBack, modifier = modifier) {
            item("icon_header") {
                ScreenIconHeader(
                    icon = Icons.Rounded.MusicNote,
                    featureColor = FeatureColors.MusicHaptics,
                    subtitle = "Vibrate in sync with music beats and sounds. Choose where Ever Haptics listens.",
                )
            }

            // Active indicator
            if (settings.musicHapticsEnabled) {
                item("active_badge") {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beatPulse * 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = beatPulse), modifier = Modifier.size(24.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    when (settings.musicHapticsSource) {
                                        MusicHapticsSource.DEVICE -> "Listening to in-device audio…"
                                        MusicHapticsSource.SURROUNDINGS -> "Listening via microphone…"
                                        MusicHapticsSource.BOTH -> "Listening to device audio & surroundings…"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    when (settings.musicHapticsSource) {
                                        MusicHapticsSource.DEVICE -> "Capturing audio playing on your device"
                                        MusicHapticsSource.SURROUNDINGS -> "Capturing sounds from your environment"
                                        MusicHapticsSource.BOTH -> "Capturing both device audio and surrounding sounds"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Mic permission card — only when source needs mic
            if (!hasMicPermission.value) {
                item("mic_permission") {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                Text("Microphone Permission Required", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Music & Sound Haptics needs microphone access to analyse audio. Nothing is recorded or stored.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            PetalButton(onClick = { requestPermissions() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.MicNone, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Grant Microphone Access")
                            }
                        }
                    }
                }
            }

            // Master toggle
            item("toggle") {
                PetalGroup {
                    row { position ->
                        PetalGroupToggleRow(
                            icon = Icons.Rounded.MusicNote,
                            title = stringResource(R.string.music_haptics_toggle_title),
                            subtitle = stringResource(R.string.music_haptics_toggle_subtitle),
                            checked = settings.musicHapticsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (hasMicPermission.value) {
                                        onMusicHapticsEnabledChange(true)
                                        MusicHapticsService.start(ctx, settings.musicHapticsSource)
                                    } else {
                                        pendingEnable = true
                                        requestPermissions()
                                    }
                                } else {
                                    onMusicHapticsEnabledChange(false)
                                    MusicHapticsService.stop(ctx)
                                }
                            },
                            position = position,
                            badgeShape = PetalMaterialShapes.Cookie12Sided,
                        )
                    }
                }
            }

            // Audio source selector
            item("source_selector") {
                SectionCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Audio Source", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("Choose where Ever Haptics listens for beats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PetalChoiceButtonGroup(
                            options = listOf(
                                PetalButtonGroupItem(MusicHapticsSource.DEVICE, "Device", Icons.Rounded.PhoneAndroid),
                                PetalButtonGroupItem(MusicHapticsSource.SURROUNDINGS, "Mic", Icons.Rounded.Mic),
                                PetalButtonGroupItem(MusicHapticsSource.BOTH, "Both", Icons.Rounded.GraphicEq),
                            ),
                            selected = settings.musicHapticsSource,
                            onSelect = { source ->
                                onMusicHapticsSourceChange(source)
                                if (settings.musicHapticsEnabled && hasMicPermission.value) {
                                    MusicHapticsService.start(ctx, source)
                                }
                            },
                        )
                        Text(
                            text = when (settings.musicHapticsSource) {
                                MusicHapticsSource.DEVICE -> "Detect beats from audio playing on this device: music, videos, games."
                                MusicHapticsSource.SURROUNDINGS -> "Detect beats from sounds around you using the microphone."
                                MusicHapticsSource.BOTH -> "Combine in-device audio and microphone for the most responsive experience."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Sliders
            item("sliders") {
                PetalGroup {
                    row { position ->
                        PetalSliderRow(
                            icon = PetalIcons.Speed,
                            title = stringResource(R.string.music_haptics_sensitivity_title),
                            subtitle = stringResource(R.string.music_haptics_sensitivity_subtitle),
                            valueLabel = "${(sensitivityDraft * 100f).roundToInt()}%",
                            value = sensitivityDraft,
                            onValueChange = { v ->
                                sensitivityDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != sensitivityTick) { sensitivityTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onSensitivityCommit(sensitivityDraft) },
                            position = position,
                            steps = SliderTickStepsDefault,
                            enabled = settings.musicHapticsEnabled,
                            badgeShape = PetalMaterialShapes.Sunny,
                            footer = { SliderEndLabels("Less sensitive", "More sensitive") },
                        )
                    }
                    row { position ->
                        PetalSliderRow(
                            icon = PetalIcons.Bolt,
                            title = stringResource(R.string.music_haptics_strength_title),
                            subtitle = stringResource(R.string.music_haptics_strength_subtitle),
                            valueLabel = "${(strengthDraft * 100f).roundToInt()}%",
                            value = strengthDraft,
                            onValueChange = { v ->
                                strengthDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != strengthTick) { strengthTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onStrengthCommit(strengthDraft) },
                            position = position,
                            steps = SliderTickStepsDefault,
                            enabled = settings.musicHapticsEnabled,
                            badgeShape = PetalMaterialShapes.Cookie9Sided,
                            variant = PetalBadgeVariant.TERTIARY,
                            footer = { SliderEndLabels("Gentle", "Strong") },
                        )
                    }
                }
            }

            item("quick_settings_tile") {
                QuickSettingsTileCard()
            }

            item("info") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = com.hapticks.app.ui.components.PetalContainmentDefaults.shape, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(PetalIcons.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How it works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "In-Device mode uses Android's audio Visualizer API to read the device's audio output directly — no microphone or screen sharing required. Surroundings mode uses the microphone to capture sounds around you. Both combines the two for maximum responsiveness.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun QuickSettingsTileCard() {
    val ctx = LocalContext.current
    val canAddTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    PetalGroup {
        row { position ->
            PetalGroupRow(
                icon = Icons.Rounded.GridView,
                title = "Quick Settings Shortcut",
                subtitle = if (canAddTile)
                    "Add a tile to your notification panel to toggle Music Haptics quickly."
                else
                    "Open Quick Settings, tap Edit, then drag the Music Haptics tile into place.",
                position = position,
                badgeShape = PetalMaterialShapes.Clover4Leaf,
                variant = PetalBadgeVariant.SECONDARY,
            )
        }
        if (canAddTile) {
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Add,
                    title = "Add tile",
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie6Sided,
                    onClick = {
                        val statusBarManager = ctx.getSystemService(android.app.StatusBarManager::class.java)
                        statusBarManager?.requestAddTileService(
                            ComponentName(ctx, MusicHapticsTileService::class.java),
                            "Music Haptics",
                            android.graphics.drawable.Icon.createWithResource(ctx, R.drawable.ic_launcher_foreground),
                            {},
                            {},
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SliderEndLabels(startLabel: String, endLabel: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
