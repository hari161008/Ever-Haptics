package com.hapticks.app.ui.screens.notificationhaptics

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticBeat
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHapticEditorScreen(
    label: String,
    initialSequence: CustomHapticSequence,
    onSave: (CustomHapticSequence) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val vibrator = remember(ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    }

    var beats by remember(initialSequence) {
        mutableStateOf(initialSequence.beats.sortedBy { it.offsetMs }.toMutableList() as List<HapticBeat>)
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var recordStrength by remember { mutableFloatStateOf(0.8f) }
    var lastStrengthTick by remember { mutableIntStateOf(slider01ToTickIndex(0.8f)) }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }

    var isPlaying by remember { mutableStateOf(false) }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    val hasChanges = beats != initialSequence.beats.sortedBy { it.offsetMs }

    LaunchedEffect(isRecording) {
        if (isRecording) while (isRecording) { delay(33); elapsedMs = System.currentTimeMillis() - recordingStartMs }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Edit ${label.replaceFirstChar { it.uppercaseChar() }} Haptic", style = MaterialTheme.typography.displaySmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (hasChanges) {
                        TextButton(onClick = { onSave(CustomHapticSequence(beats)); onBack() }) {
                            Text("Save", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isPlaying && beats.isNotEmpty()) {
                                isPlaying = true
                                scope.launch {
                                    playCustomSequence(vibrator, CustomHapticSequence(beats))
                                    isPlaying = false
                                }
                            }
                        },
                        enabled = !isPlaying && !isRecording && beats.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(if (isPlaying) Icons.Rounded.Vibration else Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(if (isPlaying) "Playing…" else "Preview")
                    }
                    Button(
                        onClick = { onSave(CustomHapticSequence(beats)); onBack() },
                        enabled = beats.isNotEmpty() && !isRecording,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Save")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("timeline") {
                TimelineVisualizer(
                    beats = beats,
                    elapsedMs = elapsedMs,
                    isRecording = isRecording,
                    onTapToAdd = { fractionX ->
                        val totalMs = beats.maxOfOrNull { it.offsetMs }?.plus(500L) ?: 3000L
                        val offsetMs = (fractionX * totalMs).toLong().coerceAtLeast(0L)
                        val amp = (recordStrength * 255f).roundToInt().coerceIn(1, 255)
                        beats = (beats + HapticBeat(offsetMs, amp)).sortedBy { it.offsetMs }
                        vibrator.vibrate(VibrationEffect.createOneShot(40, amp))
                    },
                )
            }

            item("rec_controls") {
                RecordingControls(
                    isRecording = isRecording,
                    isCountingDown = isCountingDown,
                    countdownValue = countdownValue,
                    elapsedMs = elapsedMs,
                    strength = recordStrength,
                    onStrengthChange = {
                        recordStrength = it
                        val t = slider01ToTickIndex(it)
                        if (t != lastStrengthTick) { lastStrengthTick = t; ctx.performHapticSliderTick() }
                    },
                    onTapBeat = {
                        val offsetMs = System.currentTimeMillis() - recordingStartMs
                        val amp = (recordStrength * 255f).roundToInt().coerceIn(1, 255)
                        beats = (beats + HapticBeat(offsetMs, amp)).sortedBy { it.offsetMs }
                        vibrator.vibrate(VibrationEffect.createOneShot(60, amp))
                    },
                    onStartStop = {
                        if (!isRecording && !isCountingDown) {
                            isCountingDown = true
                            countdownValue = 3
                            scope.launch {
                                for (i in 3 downTo 1) {
                                    countdownValue = i
                                    vibrator.vibrate(VibrationEffect.createOneShot(80, if (i == 1) 200 else 120))
                                    delay(1000L)
                                    if (!isCountingDown) return@launch
                                }
                                isCountingDown = false
                                recordingStartMs = System.currentTimeMillis()
                                elapsedMs = 0L
                                isRecording = true
                                vibrator.vibrate(VibrationEffect.createOneShot(120, 255))
                            }
                        } else if (isCountingDown) {
                            isCountingDown = false
                            countdownValue = 3
                        } else {
                            isRecording = false
                        }
                    },
                    onClear = {
                        beats = emptyList()
                        expandedIndex = -1
                    },
                    vibrator = vibrator,
                    recordingStartMs = recordingStartMs,
                )
            }

            if (beats.isNotEmpty()) {
                item("list_header") {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Beats (${beats.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Tap to edit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                itemsIndexed(beats, key = { idx, _ -> idx }) { index, beat ->
                    BeatEditorRow(
                        index = index,
                        beat = beat,
                        totalBeats = beats.size,
                        isExpanded = expandedIndex == index,
                        prevBeatOffsetMs = beats.getOrNull(index - 1)?.offsetMs,
                        nextBeatOffsetMs = beats.getOrNull(index + 1)?.offsetMs,
                        onToggleExpand = { expandedIndex = if (expandedIndex == index) -1 else index },
                        onOffsetChange = { newOffset ->
                            beats = beats.toMutableList().also { it[index] = beat.copy(offsetMs = newOffset) }.sortedBy { it.offsetMs }
                            expandedIndex = -1
                        },
                        onAmplitudeChange = { newAmp ->
                            beats = beats.toMutableList().also { it[index] = beat.copy(amplitude = newAmp) }
                        },
                        onDelete = {
                            beats = beats.toMutableList().also { it.removeAt(index) }
                            if (expandedIndex >= beats.size) expandedIndex = -1
                        },
                        onPreview = {
                            vibrator.vibrate(VibrationEffect.createOneShot(80, beat.amplitude.coerceIn(1, 255)))
                        },
                    )
                }

                item("add_beat") {
                    OutlinedButton(
                        onClick = {
                            val lastOffset = beats.lastOrNull()?.offsetMs ?: 0L
                            val amp = (recordStrength * 255f).roundToInt().coerceIn(1, 255)
                            beats = beats + HapticBeat(lastOffset + 300L, amp)
                            vibrator.vibrate(VibrationEffect.createOneShot(60, amp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Add beat at end")
                    }
                }
            }

            item("bottom_space") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TimelineVisualizer(
    beats: List<HapticBeat>,
    elapsedMs: Long,
    isRecording: Boolean,
    onTapToAdd: (fractionX: Float) -> Unit,
) {
    val totalMs = maxOf(elapsedMs, beats.maxOfOrNull { it.offsetMs }?.plus(500L) ?: 3000L)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Timeline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Timeline", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text("${(totalMs / 1000f).let { "%.1f".format(it) }}s · tap to add", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .pointerInput(totalMs) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onTapToAdd(fraction)
                        }
                    },
            ) {
                if (beats.isEmpty()) {
                    Text(
                        "Tap here to add beats, or use the recorder below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
                    )
                }
                for (i in 1..9) {
                    Box(Modifier.fillMaxHeight().width(1.dp).offset(x = (i * 10).dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)).align(Alignment.CenterStart))
                }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val totalWidthDp = maxWidth
                    beats.forEachIndexed { idx, beat ->
                        val xFraction = (beat.offsetMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 0.97f)
                        val heightFraction = (beat.amplitude / 255f).coerceIn(0.15f, 1f)
                        val barColor = amplitudeToColor(beat.amplitude, MaterialTheme.colorScheme)
                        Box(modifier = Modifier.offset(x = totalWidthDp * xFraction).width(6.dp).fillMaxHeight(heightFraction).clip(RoundedCornerShape(3.dp)).background(barColor).align(Alignment.CenterStart))
                        Text("${idx + 1}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp), color = barColor, modifier = Modifier.offset(x = totalWidthDp * xFraction - 1.dp, y = (-32).dp).align(Alignment.BottomStart))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    isCountingDown: Boolean,
    countdownValue: Int,
    elapsedMs: Long,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    onTapBeat: () -> Unit,
    onStartStop: () -> Unit,
    onClear: () -> Unit,
    vibrator: Vibrator,
    recordingStartMs: Long,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val borderAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "border_alpha")
    val recDotAlpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "rec_dot")
    val countdownPulse by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cd_pulse")

    val borderColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.error.copy(alpha = borderAlpha)
            isCountingDown -> MaterialTheme.colorScheme.tertiary.copy(alpha = borderAlpha)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(200), label = "rec_border",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isRecording || isCountingDown) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Rounded.FiberManualRecord, null,
                        tint = when {
                            isRecording -> MaterialTheme.colorScheme.error.copy(alpha = recDotAlpha)
                            isCountingDown -> MaterialTheme.colorScheme.tertiary.copy(alpha = recDotAlpha)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Recorder", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                AnimatedContent(
                    targetState = when {
                        isCountingDown -> "countdown"
                        isRecording -> "recording"
                        else -> "idle"
                    },
                    transitionSpec = { fadeIn(tween(200)) + scaleIn(tween(200)) togetherWith fadeOut(tween(160)) + scaleOut(tween(160)) },
                    label = "rec_badge",
                ) { state ->
                    when (state) {
                        "recording" -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) {
                            Text("REC  ${"%.1f".format(elapsedMs / 1000f)}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                        }
                        "countdown" -> Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                            Text("Starting in  $countdownValue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                        }
                        else -> Spacer(Modifier.size(0.dp))
                    }
                }
            }

            // Countdown overlay
            AnimatedVisibility(
                visible = isCountingDown,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(200)) + shrinkVertically(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = countdownValue.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = (72 * countdownPulse).sp,
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Get ready to tap beats…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            // Strength slider
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Vibration, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("Strength", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
                Slider(value = strength, onValueChange = onStrengthChange, valueRange = 0.1f..1f, steps = 17, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                    Text("${(strength * 100f).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            // Controls row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // TAP BEAT button with continuous vibration on long press
                val tapBeatInteraction = remember { MutableInteractionSource() }
                val isPressed by tapBeatInteraction.collectIsPressedAsState()

                // Continuous vibration while button is held
                LaunchedEffect(isPressed, isRecording) {
                    if (isPressed && isRecording) {
                        while (isActive) {
                            val offsetMs = System.currentTimeMillis() - recordingStartMs
                            val amp = (strength * 255f).roundToInt().coerceIn(1, 255)
                            onTapBeat()
                            vibrator.vibrate(VibrationEffect.createOneShot(80, amp))
                            delay(80L)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!isPressed) onTapBeat()
                    },
                    enabled = isRecording,
                    interactionSource = tapBeatInteraction,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isRecording) "TAP BEAT" else "Tap beat", style = MaterialTheme.typography.labelLarge)
                }

                IconButton(
                    onClick = onStartStop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(
                        when {
                            isRecording -> MaterialTheme.colorScheme.errorContainer
                            isCountingDown -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                ) {
                    Icon(
                        imageVector = when {
                            isRecording -> Icons.Rounded.Stop
                            isCountingDown -> Icons.Rounded.Close
                            else -> Icons.Rounded.FiberManualRecord
                        },
                        contentDescription = when {
                            isRecording -> "Stop"
                            isCountingDown -> "Cancel"
                            else -> "Record"
                        },
                        tint = when {
                            isRecording -> MaterialTheme.colorScheme.error
                            isCountingDown -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, "Clear all", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun BeatEditorRow(
    index: Int,
    beat: HapticBeat,
    totalBeats: Int,
    isExpanded: Boolean,
    prevBeatOffsetMs: Long?,
    nextBeatOffsetMs: Long?,
    onToggleExpand: () -> Unit,
    onOffsetChange: (Long) -> Unit,
    onAmplitudeChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
) {
    val ctx = LocalContext.current
    val containerColor by animateColorAsState(if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHigh, spring(stiffness = 300f), label = "bc")
    val borderColor by animateColorAsState(if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "bb")
    val barColor = amplitudeToColor(beat.amplitude, MaterialTheme.colorScheme)

    Surface(color = containerColor, shape = RoundedCornerShape(16.dp), border = BorderStroke(if (isExpanded) 1.5.dp else 1.dp, borderColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(36.dp).background(barColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).border(1.dp, barColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = barColor, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("${beat.offsetMs}ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    Text("Strength ${(beat.amplitude / 255f * 100f).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.BottomCenter) {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(beat.amplitude / 255f).clip(RoundedCornerShape(2.dp)).background(barColor))
                }
                IconButton(onClick = onPreview, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Vibration, "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(34.dp)) { Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Close, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    var offsetDraft by remember(beat.offsetMs) { mutableLongStateOf(beat.offsetMs) }
                    var offsetTick by remember { mutableIntStateOf(0) }
                    val minOffset = (prevBeatOffsetMs?.plus(10L)) ?: 0L
                    val maxOffset = (nextBeatOffsetMs?.minus(10L)) ?: (beat.offsetMs + 5000L)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Time offset", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                                Text("${offsetDraft}ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                        }
                        Slider(
                            value = offsetDraft.toFloat(),
                            onValueChange = { offsetDraft = it.toLong(); val t = (it / 100).toInt(); if (t != offsetTick) { offsetTick = t; ctx.performHapticSliderTick() } },
                            onValueChangeFinished = { onOffsetChange(offsetDraft) },
                            valueRange = minOffset.toFloat()..maxOffset.toFloat().coerceAtLeast((minOffset + 10).toFloat()),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${minOffset}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${maxOffset}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    var ampDraft by remember(beat.amplitude) { mutableIntStateOf(beat.amplitude) }
                    var ampTick by remember { mutableIntStateOf(0) }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Vibration, null, tint = amplitudeToColor(ampDraft, MaterialTheme.colorScheme), modifier = Modifier.size(16.dp))
                                Text("Strength", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Surface(color = amplitudeToColor(ampDraft, MaterialTheme.colorScheme).copy(alpha = 0.15f), shape = CircleShape) {
                                Text("${(ampDraft / 255f * 100f).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = amplitudeToColor(ampDraft, MaterialTheme.colorScheme), modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Slider(
                            value = ampDraft.toFloat(),
                            onValueChange = { ampDraft = it.roundToInt().coerceIn(1, 255); val t = it.toInt() / 10; if (t != ampTick) { ampTick = t; ctx.performHapticSliderTick() } },
                            onValueChangeFinished = { onAmplitudeChange(ampDraft) },
                            valueRange = 1f..255f, steps = 25,
                            colors = SliderDefaults.colors(thumbColor = amplitudeToColor(ampDraft, MaterialTheme.colorScheme), activeTrackColor = amplitudeToColor(ampDraft, MaterialTheme.colorScheme), inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Soft", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Strong", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun amplitudeToColor(amplitude: Int, colorScheme: ColorScheme = MaterialTheme.colorScheme): Color {
    val fraction = amplitude / 255f
    return when {
        fraction < 0.35f -> colorScheme.tertiary
        fraction < 0.65f -> colorScheme.secondary
        else -> colorScheme.primary
    }
}

suspend fun playCustomSequence(vibrator: Vibrator, seq: CustomHapticSequence) {
    if (seq.isEmpty) return
    val sorted = seq.beats.sortedBy { it.offsetMs }
    val startMs = System.currentTimeMillis()
    for (beat in sorted) {
        val elapsed = System.currentTimeMillis() - startMs
        val wait = beat.offsetMs - elapsed
        if (wait > 0) delay(wait)
        vibrator.vibrate(VibrationEffect.createOneShot(60L, beat.amplitude.coerceIn(1, 255)))
    }
}
