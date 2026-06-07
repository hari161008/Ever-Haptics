package com.hapticks.app.ui.screens.musichaptics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
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
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.data.MusicHapticsSource
import com.hapticks.app.service.MusicHapticsService
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.components.ScreenIconHeader
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

    val hasMicPermission = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission.value = granted
    }

    // MediaProjection launcher (for device audio capture, API 29+)
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            MusicHapticsService.start(ctx, settings.musicHapticsSource, result.resultCode, result.data!!)
        }
    }

    fun needsMic(source: MusicHapticsSource) = source == MusicHapticsSource.SURROUNDINGS || source == MusicHapticsSource.BOTH
    fun needsProjection(source: MusicHapticsSource) = (source == MusicHapticsSource.DEVICE || source == MusicHapticsSource.BOTH) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun launchService(source: MusicHapticsSource) {
        if (needsProjection(source)) {
            val mgr = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mgr.createScreenCaptureIntent())
        } else {
            MusicHapticsService.start(ctx, source)
        }
    }

    // Sync service state when settings change
    LaunchedEffect(settings.musicHapticsEnabled, settings.musicHapticsSource, hasMicPermission.value) {
        if (!settings.musicHapticsEnabled) {
            MusicHapticsService.stop(ctx)
        }
        // Service is started manually via toggle or source change to handle projection flow
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

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.music_haptics_screen_title), style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("icon_header") {
                ScreenIconHeader(
                    icon = Icons.Rounded.MusicNote,
                    featureColor = MaterialTheme.colorScheme.primary,
                    subtitle = "Vibrate in sync with music beats and sounds. Choose where Ever Haptics listens.",
                )
            }

            // Active indicator
            if (settings.musicHapticsEnabled) {
                item("active_badge") {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = beatPulse * 0.3f)),
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
                                    color = MaterialTheme.colorScheme.primary,
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

            // Microphone permission card (shown when source needs mic)
            if (needsMic(settings.musicHapticsSource) && !hasMicPermission.value) {
                item("mic_permission") {
                    PermissionCard(
                        icon = Icons.Rounded.Mic,
                        title = "Microphone Permission Required",
                        body = "The selected source needs microphone access to capture sounds from your surroundings.",
                        buttonLabel = "Grant Microphone Access",
                        onGrant = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    )
                }
            }

            // Device audio notice (API < 29)
            if ((settings.musicHapticsSource == MusicHapticsSource.DEVICE || settings.musicHapticsSource == MusicHapticsSource.BOTH)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            ) {
                item("device_audio_unsupported") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Text(
                                "In-device audio capture requires Android 10 or later. The microphone will be used as a fallback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Master toggle
            item("toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(R.string.music_haptics_toggle_title),
                        subtitle = stringResource(R.string.music_haptics_toggle_subtitle),
                        checked = settings.musicHapticsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val micOk = !needsMic(settings.musicHapticsSource) || hasMicPermission.value
                                if (!micOk) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@HapticToggleRow
                                }
                                onMusicHapticsEnabledChange(true)
                                launchService(settings.musicHapticsSource)
                            } else {
                                onMusicHapticsEnabledChange(false)
                                MusicHapticsService.stop(ctx)
                            }
                        },
                        leadingIcon = Icons.Rounded.MusicNote,
                    )
                }
            }

            // Audio Source Selector
            item("source_selector") {
                SectionCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Audio Source",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Choose where Ever Haptics listens for beats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        SourceOption(
                            icon = Icons.Rounded.PhoneAndroid,
                            title = "In-Device Sounds",
                            subtitle = "Detect beats from audio playing on this device — music, videos, games.",
                            selected = settings.musicHapticsSource == MusicHapticsSource.DEVICE,
                            onClick = {
                                onMusicHapticsSourceChange(MusicHapticsSource.DEVICE)
                                if (settings.musicHapticsEnabled) {
                                    MusicHapticsService.stop(ctx)
                                    launchService(MusicHapticsSource.DEVICE)
                                }
                            },
                        )
                        SourceOption(
                            icon = Icons.Rounded.Mic,
                            title = "Surroundings",
                            subtitle = "Detect beats from sounds around you using the microphone.",
                            selected = settings.musicHapticsSource == MusicHapticsSource.SURROUNDINGS,
                            onClick = {
                                if (!hasMicPermission.value) { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                onMusicHapticsSourceChange(MusicHapticsSource.SURROUNDINGS)
                                if (settings.musicHapticsEnabled) {
                                    MusicHapticsService.stop(ctx)
                                    MusicHapticsService.start(ctx, MusicHapticsSource.SURROUNDINGS)
                                }
                            },
                        )
                        SourceOption(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Both",
                            subtitle = "Combine in-device audio and microphone for the most responsive experience.",
                            selected = settings.musicHapticsSource == MusicHapticsSource.BOTH,
                            onClick = {
                                if (!hasMicPermission.value) { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                onMusicHapticsSourceChange(MusicHapticsSource.BOTH)
                                if (settings.musicHapticsEnabled) {
                                    MusicHapticsService.stop(ctx)
                                    launchService(MusicHapticsSource.BOTH)
                                }
                            },
                        )
                    }
                }
            }

            // Sliders
            item("sliders") {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        HapticSlider(
                            title = stringResource(R.string.music_haptics_sensitivity_title),
                            subtitle = stringResource(R.string.music_haptics_sensitivity_subtitle),
                            value = sensitivityDraft,
                            enabled = settings.musicHapticsEnabled,
                            startLabel = "Less sensitive",
                            endLabel = "More sensitive",
                            onValueChange = { v ->
                                sensitivityDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != sensitivityTick) { sensitivityTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onSensitivityCommit(sensitivityDraft) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        HapticSlider(
                            title = stringResource(R.string.music_haptics_strength_title),
                            subtitle = stringResource(R.string.music_haptics_strength_subtitle),
                            value = strengthDraft,
                            enabled = settings.musicHapticsEnabled,
                            startLabel = "Gentle",
                            endLabel = "Strong",
                            onValueChange = { v ->
                                strengthDraft = v
                                val t = slider01ToTickIndex(v)
                                if (t != strengthTick) { strengthTick = t; ctx.performHapticSliderTick() }
                            },
                            onValueChangeFinished = { onStrengthCommit(strengthDraft) },
                        )
                    }
                }
            }

            item("info") {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How it works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Ever Haptics analyses audio in real-time and vibrates when a beat is detected. In-Device capture uses Android's audio playback API — your device's speaker output is not affected. Sensitivity controls the detection threshold; Strength controls how hard the device vibrates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    onGrant: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun HapticSlider(
    title: String,
    subtitle: String,
    value: Float,
    enabled: Boolean,
    startLabel: String,
    endLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape) {
                Text(
                    "${(value * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
