package com.hapticks.app.ui.screens.notificationhaptics

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hapticks.app.R
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticBeat
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.service.notification.NotificationHapticService
import com.hapticks.app.ui.components.HapticTestButton
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHapticsScreen(
    callHapticEnabled: Boolean,
    callPattern: HapticPattern,
    callIntensity: Float,
    callCustomSequence: CustomHapticSequence,
    onCallEnabledChange: (Boolean) -> Unit,
    onCallPatternSelected: (HapticPattern) -> Unit,
    onCallIntensityCommit: (Float) -> Unit,
    onCallCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestCallHaptic: () -> Unit,
    notifHapticEnabled: Boolean,
    notifPattern: HapticPattern,
    notifIntensity: Float,
    notifCustomSequence: CustomHapticSequence,
    onNotifEnabledChange: (Boolean) -> Unit,
    onNotifPatternSelected: (HapticPattern) -> Unit,
    onNotifIntensityCommit: (Float) -> Unit,
    onNotifCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestNotifHaptic: () -> Unit,
    alarmHapticEnabled: Boolean,
    alarmPattern: HapticPattern,
    alarmIntensity: Float,
    alarmCustomSequence: CustomHapticSequence,
    onAlarmEnabledChange: (Boolean) -> Unit,
    onAlarmPatternSelected: (HapticPattern) -> Unit,
    onAlarmIntensityCommit: (Float) -> Unit,
    onAlarmCustomSequenceSave: (CustomHapticSequence) -> Unit,
    onTestAlarmHaptic: () -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("Calls, Alerts & Alarms", style = MaterialTheme.typography.displaySmall) },
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
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("notif_access") { NotificationAccessCard() }
            item("notice") { SystemHapticsNoticeCard() }

            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Reset to defaults", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item("call_header") { SectionHeader(icon = Icons.Rounded.Call, title = "Incoming Call") }
            item("call_toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = "Call Haptic",
                        subtitle = "Play a repeating haptic pattern when a call arrives",
                        checked = callHapticEnabled,
                        onCheckedChange = onCallEnabledChange,
                        leadingIcon = Icons.Rounded.Call,
                    )
                }
            }
            item("call_content") {
                AnimatedVisibility(visible = callHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithRecorder(
                                pattern = callPattern,
                                intensity = callIntensity,
                                customSequence = callCustomSequence,
                                onPatternSelected = onCallPatternSelected,
                                onIntensityCommit = onCallIntensityCommit,
                                onCustomSequenceSave = onCallCustomSequenceSave,
                                label = "call",
                            )
                        }
                        HapticTestButton("Test Call Haptic", onTestCallHaptic, enabled = callHapticEnabled)
                    }
                }
            }

            item("notif_header") { SectionHeader(icon = Icons.Rounded.Notifications, title = "Notifications") }
            item("notif_toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = "Notification Haptic",
                        subtitle = "Play a haptic pattern when a notification arrives",
                        checked = notifHapticEnabled,
                        onCheckedChange = onNotifEnabledChange,
                        leadingIcon = Icons.Rounded.Notifications,
                    )
                }
            }
            item("notif_content") {
                AnimatedVisibility(visible = notifHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithRecorder(
                                pattern = notifPattern,
                                intensity = notifIntensity,
                                customSequence = notifCustomSequence,
                                onPatternSelected = onNotifPatternSelected,
                                onIntensityCommit = onNotifIntensityCommit,
                                onCustomSequenceSave = onNotifCustomSequenceSave,
                                label = "notification",
                            )
                        }
                        HapticTestButton("Test Notification Haptic", onTestNotifHaptic, enabled = notifHapticEnabled)
                    }
                }
            }

            item("alarm_header") { SectionHeader(icon = Icons.Rounded.Alarm, title = "Alarm") }
            item("alarm_toggle") {
                SectionCard {
                    HapticToggleRow(
                        title = "Alarm Haptic",
                        subtitle = "Play a haptic pattern when an alarm fires",
                        checked = alarmHapticEnabled,
                        onCheckedChange = onAlarmEnabledChange,
                        leadingIcon = Icons.Rounded.Alarm,
                    )
                }
            }
            item("alarm_content") {
                AnimatedVisibility(visible = alarmHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithRecorder(
                                pattern = alarmPattern,
                                intensity = alarmIntensity,
                                customSequence = alarmCustomSequence,
                                onPatternSelected = onAlarmPatternSelected,
                                onIntensityCommit = onAlarmIntensityCommit,
                                onCustomSequenceSave = onAlarmCustomSequenceSave,
                                label = "alarm",
                            )
                        }
                        HapticTestButton("Test Alarm Haptic", onTestAlarmHaptic, enabled = alarmHapticEnabled)
                    }
                }
            }

            item("bottom") { Spacer(Modifier.size(4.dp)) }
        }
    }
}

@Composable
private fun NotificationAccessCard() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun isListenerEnabled(): Boolean {
        val flat = try {
            AndroidSettings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: ""
        } catch (e: Exception) { "" }
        val pkgName = ctx.packageName
        val serviceName = NotificationHapticService::class.java.name
        return flat.split(":").any { entry ->
            val trimmed = entry.trim()
            trimmed.startsWith(pkgName) && (trimmed.endsWith(serviceName) || trimmed.contains(serviceName.substringAfterLast(".")))
        }
    }

    var isGranted by remember { mutableStateOf(isListenerEnabled()) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = isListenerEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (isGranted) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.NotificationsOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Notification access required", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                Text("Grant notification listener access so Ever Haptics can fire haptics for calls, notifications, and alarms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Button(
                onClick = {
                    ctx.startActivity(
                        android.content.Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Grant", color = MaterialTheme.colorScheme.onError) }
        }
    }
}

@Composable
private fun SystemHapticsNoticeCard() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Turn off system haptics for calls & notifications", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                Text(
                    "For best results, disable your system's default vibration for calls, notifications, and alarms in:\nSettings → Sound & vibration → Vibration & haptics\n\nOtherwise system and Ever Haptics patterns may overlap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PatternSelectorWithRecorder(
    pattern: HapticPattern,
    intensity: Float,
    customSequence: CustomHapticSequence,
    onPatternSelected: (HapticPattern) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onCustomSequenceSave: (CustomHapticSequence) -> Unit,
    label: String,
) {
    var showCustomRecorder by remember(customSequence) { mutableStateOf(!customSequence.isEmpty) }

    Column(Modifier.fillMaxWidth()) {
        PatternSelectorWithCustom(
            selected = pattern,
            isCustomSelected = showCustomRecorder,
            onPatternSelected = { p ->
                showCustomRecorder = false
                onPatternSelected(p)
            },
            onCustomSelected = { showCustomRecorder = true },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

        AnimatedVisibility(visible = showCustomRecorder) {
            Column {
                HapticRecorderPanel(existingSequence = customSequence, onSave = onCustomSequenceSave, label = label)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }

        IntensitySlider(intensity = intensity, onIntensityCommit = onIntensityCommit)
    }
}

@Composable
private fun PatternSelectorWithCustom(
    selected: HapticPattern,
    isCustomSelected: Boolean,
    onPatternSelected: (HapticPattern) -> Unit,
    onCustomSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val patterns = remember { HapticPattern.entries }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        patterns.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { p ->
                    PatternCard(
                        pattern = p,
                        isSelected = !isCustomSelected && p == selected,
                        onClick = { onPatternSelected(p) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) Box(modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        CustomPatternCard(isSelected = isCustomSelected, onClick = onCustomSelected, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PatternCard(pattern: HapticPattern, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = 300f), label = "pc",
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        spring(stiffness = 300f), label = "pb",
    )
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "pbw")
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val descColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val stateDesc = stringResource(if (isSelected) R.string.pattern_selected else R.string.pattern_not_selected)
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.heightIn(min = 132.dp)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true))
            .semantics { this.stateDescription = stateDesc },
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier.size(40.dp).background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(14.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(pattern.icon, null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                SelectionDot(isSelected)
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(pattern.labelRes), style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(stringResource(pattern.descriptionRes), style = MaterialTheme.typography.bodySmall, color = descColor)
        }
    }
}

@Composable
private fun CustomPatternCard(isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        spring(stiffness = 300f), label = "cc",
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        spring(stiffness = 300f), label = "cb",
    )
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "cbw")
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)),
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(14.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Draw, null, tint = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Custom", style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                Text("Record your own haptic rhythm", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SelectionDot(isSelected)
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0f, spring(stiffness = 600f), label = "sd")
    Box(
        modifier = Modifier.size(22.dp).scale(scale).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Icon(Icons.Rounded.Check, stringResource(R.string.pattern_selected), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun IntensitySlider(intensity: Float, onIntensityCommit: (Float) -> Unit) {
    val ctx = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTick by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Intensity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text("${(draft * 100f).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = draft,
            onValueChange = {
                draft = it
                val t = slider01ToTickIndex(it)
                if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
private fun HapticRecorderPanel(existingSequence: CustomHapticSequence, onSave: (CustomHapticSequence) -> Unit, label: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var strength by remember { mutableFloatStateOf(0.8f) }
    var lastStrengthTick by remember { mutableIntStateOf(slider01ToTickIndex(0.8f)) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartMs by remember { mutableLongStateOf(0L) }
    var beats by remember(existingSequence) { mutableStateOf(existingSequence.beats) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    val vibrator = remember(ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    }

    var notifPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifPermissionGranted = it }

    LaunchedEffect(isRecording) {
        if (isRecording) while (isRecording) { delay(33); elapsedMs = System.currentTimeMillis() - recordingStartMs }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.FiberManualRecord, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Text("Custom Haptic Recorder", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("Tap the beat button in rhythm, then save to use it for ${label}s.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.NotificationsOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Text("POST_NOTIFICATIONS permission needed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Grant", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        // Strength slider
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Vibration, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text("Strength", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(64.dp))
            Slider(
                value = strength,
                onValueChange = {
                    strength = it
                    val t = slider01ToTickIndex(it)
                    if (t != lastStrengthTick) { lastStrengthTick = t; ctx.performHapticSliderTick() }
                },
                valueRange = 0.1f..1f,
                steps = 17,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                Text("${(strength * 100f).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }

        BeatVisualizer(beats = beats, elapsedMs = elapsedMs, isRecording = isRecording)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (isRecording) {
                        val offsetMs = System.currentTimeMillis() - recordingStartMs
                        val amp = (strength * 255f).roundToInt().coerceIn(1, 255)
                        beats = beats + HapticBeat(offsetMs, amp)
                        vibrator.vibrate(VibrationEffect.createOneShot(60, amp))
                    }
                },
                enabled = isRecording,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.Rounded.TouchApp, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (isRecording) "Tap! ${"%.1f".format(elapsedMs / 1000f)}s" else "Tap to record beat", style = MaterialTheme.typography.labelLarge)
            }

            IconButton(
                onClick = {
                    if (!isRecording) {
                        beats = emptyList(); elapsedMs = 0L
                        recordingStartMs = System.currentTimeMillis(); isRecording = true
                    } else {
                        isRecording = false
                    }
                },
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    if (isRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                    if (isRecording) "Stop" else "Record",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (beats.isNotEmpty()) {
            Text("${beats.size} beat${if (beats.size == 1) "" else "s"} · ${CustomHapticSequence(beats).durationMs}ms total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (!isPlaying) { isPlaying = true; scope.launch { playCustomSequence(vibrator, CustomHapticSequence(beats)); isPlaying = false } } },
                    enabled = !isPlaying && !isRecording,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(if (isPlaying) Icons.Rounded.Vibration else Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(if (isPlaying) "Playing…" else "Preview")
                }
                OutlinedButton(
                    onClick = { beats = emptyList(); onSave(CustomHapticSequence()) },
                    enabled = !isRecording,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Clear")
                }
                Button(
                    onClick = { onSave(CustomHapticSequence(beats)) },
                    enabled = !isRecording && beats.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun BeatVisualizer(beats: List<HapticBeat>, elapsedMs: Long, isRecording: Boolean) {
    val totalMs = maxOf(elapsedMs, beats.maxOfOrNull { it.offsetMs }?.plus(500L) ?: 500L, 3000L)
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(10.dp)),
    ) {
        if (beats.isEmpty() && !isRecording) {
            Text("Record beats to see them here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            beats.forEach { beat ->
                val xFraction = (beat.offsetMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 0.98f)
                val heightFraction = (beat.amplitude / 255f).coerceIn(0.25f, 1f)
                Box(
                    modifier = Modifier
                        .offset(x = (xFraction * maxWidth.value).dp)
                        .width(4.dp)
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .align(Alignment.CenterStart),
                )
            }
        }
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
