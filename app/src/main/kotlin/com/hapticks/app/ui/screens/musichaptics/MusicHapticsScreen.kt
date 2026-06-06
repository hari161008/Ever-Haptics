package com.hapticks.app.ui.screens.musichaptics

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.service.MusicHapticsService
import com.hapticks.app.ui.components.FeatureColors
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
    onSensitivityCommit: (Float) -> Unit,
    onStrengthCommit: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
        if (granted && settings.musicHapticsEnabled) {
            MusicHapticsService.start(ctx)
        }
    }

    // Start/stop service based on enabled state and permission
    LaunchedEffect(settings.musicHapticsEnabled, hasPermission.value) {
        if (settings.musicHapticsEnabled && hasPermission.value) {
            MusicHapticsService.start(ctx)
        } else {
            MusicHapticsService.stop(ctx)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
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
                    featureColor = FeatureColors.MusicHaptics,
                    subtitle = "Vibrate in sync with music beats and sounds picked up by the microphone.",
                )
            }

            // Live beat visualiser when active
            if (settings.musicHapticsEnabled && hasPermission.value) {
                item("beat_visualiser") {
                    Surface(
                        color = FeatureColors.MusicHaptics.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(FeatureColors.MusicHaptics.copy(alpha = beatPulse * 0.3f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    null,
                                    tint = FeatureColors.MusicHaptics.copy(alpha = beatPulse),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Listening for beats…",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FeatureColors.MusicHaptics,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Hold your device near the speaker for best results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Permission card
            if (!hasPermission.value) {
                item("permission") {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                Text(
                                    stringResource(R.string.music_haptics_permission_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                stringResource(R.string.music_haptics_permission_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.MicNone, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.music_haptics_permission_button))
                            }
                        }
                    }
                }
            }

            item("toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = stringResource(R.string.music_haptics_toggle_title),
                        subtitle = stringResource(R.string.music_haptics_toggle_subtitle),
                        checked = settings.musicHapticsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasPermission.value) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                onMusicHapticsEnabledChange(enabled)
                            }
                        },
                        leadingIcon = Icons.Rounded.MusicNote,
                    )
                }
            }

            item("sliders") {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Sensitivity slider
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.music_haptics_sensitivity_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        stringResource(R.string.music_haptics_sensitivity_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(color = FeatureColors.MusicHaptics.copy(alpha = 0.15f), shape = CircleShape) {
                                    Text(
                                        "${(sensitivityDraft * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FeatureColors.MusicHaptics,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Slider(
                                value = sensitivityDraft,
                                onValueChange = { v ->
                                    sensitivityDraft = v
                                    val t = slider01ToTickIndex(v)
                                    if (t != sensitivityTick) { sensitivityTick = t; ctx.performHapticSliderTick() }
                                },
                                onValueChangeFinished = { onSensitivityCommit(sensitivityDraft) },
                                valueRange = 0f..1f,
                                steps = SliderTickStepsDefault,
                                enabled = settings.musicHapticsEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = FeatureColors.MusicHaptics,
                                    activeTrackColor = FeatureColors.MusicHaptics,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Less sensitive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("More sensitive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Strength slider
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.music_haptics_strength_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        stringResource(R.string.music_haptics_strength_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(color = FeatureColors.MusicHaptics.copy(alpha = 0.15f), shape = CircleShape) {
                                    Text(
                                        "${(strengthDraft * 100f).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FeatureColors.MusicHaptics,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Slider(
                                value = strengthDraft,
                                onValueChange = { v ->
                                    strengthDraft = v
                                    val t = slider01ToTickIndex(v)
                                    if (t != strengthTick) { strengthTick = t; ctx.performHapticSliderTick() }
                                },
                                onValueChangeFinished = { onStrengthCommit(strengthDraft) },
                                valueRange = 0f..1f,
                                steps = SliderTickStepsDefault,
                                enabled = settings.musicHapticsEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = FeatureColors.MusicHaptics,
                                    activeTrackColor = FeatureColors.MusicHaptics,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gentle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Strong", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item("how_it_works") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("How it works", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Ever Haptics uses the microphone to continuously analyse audio. When a beat or sudden sound is detected, the device vibrates. Sensitivity controls the detection threshold — higher sensitivity triggers on quieter beats. Strength controls how hard the device vibrates.",
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
