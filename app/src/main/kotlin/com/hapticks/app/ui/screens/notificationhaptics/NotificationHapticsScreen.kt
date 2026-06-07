package com.hapticks.app.ui.screens.notificationhaptics

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hapticks.app.R
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.service.notification.NotificationHapticService
import com.hapticks.app.ui.components.FeatureColors
import com.hapticks.app.ui.components.FeatureIcon
import com.hapticks.app.ui.components.HapticTestButton
import com.hapticks.app.ui.components.ScreenIconHeader
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
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
    onOpenCustomEditor: (label: String, sequence: CustomHapticSequence, onSave: (CustomHapticSequence) -> Unit) -> Unit,
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("icon_header") {
                ScreenIconHeader(icon = Icons.Rounded.NotificationsActive, featureColor = FeatureColors.Notifications, subtitle = "Custom haptic patterns for incoming calls, notifications, and alarms.")
            }
            item("notif_access") { NotificationListenerCard() }
            item("phone_perm") { PhonePermissionCard() }
            item("notice") { SystemHapticsNoticeCard() }

            item("reset") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetToDefaults) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp))
                        Text("Reset to defaults", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item("call_header") { SectionHeader(icon = Icons.Rounded.Call, title = "Incoming Call") }
            item("call_toggle") {
                SectionCard { HapticToggleRow(title = "Call Haptic", subtitle = "Play a repeating haptic pattern when a call arrives", checked = callHapticEnabled, onCheckedChange = onCallEnabledChange) }
            }
            item("call_content") {
                AnimatedVisibility(visible = callHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithCustom(
                                pattern = callPattern, intensity = callIntensity, customSequence = callCustomSequence,
                                onPatternSelected = onCallPatternSelected, onIntensityCommit = onCallIntensityCommit,
                                onOpenCustomEditor = { onOpenCustomEditor("call", callCustomSequence, onCallCustomSequenceSave) }, label = "call",
                            )
                        }
                        HapticTestButton("Test Call Haptic", onTestCallHaptic, enabled = callHapticEnabled)
                    }
                }
            }

            item("notif_header") { SectionHeader(icon = Icons.Rounded.Notifications, title = "Notifications") }
            item("notif_toggle") {
                SectionCard { HapticToggleRow(title = "Notification Haptic", subtitle = "Play a haptic pattern when a notification arrives", checked = notifHapticEnabled, onCheckedChange = onNotifEnabledChange) }
            }
            item("notif_content") {
                AnimatedVisibility(visible = notifHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithCustom(
                                pattern = notifPattern, intensity = notifIntensity, customSequence = notifCustomSequence,
                                onPatternSelected = onNotifPatternSelected, onIntensityCommit = onNotifIntensityCommit,
                                onOpenCustomEditor = { onOpenCustomEditor("notification", notifCustomSequence, onNotifCustomSequenceSave) }, label = "notification",
                            )
                        }
                        HapticTestButton("Test Notification Haptic", onTestNotifHaptic, enabled = notifHapticEnabled)
                    }
                }
            }

            item("alarm_header") { SectionHeader(icon = Icons.Rounded.Alarm, title = "Alarm") }
            item("alarm_toggle") {
                SectionCard { HapticToggleRow(title = "Alarm Haptic", subtitle = "Play a haptic pattern when an alarm fires", checked = alarmHapticEnabled, onCheckedChange = onAlarmEnabledChange) }
            }
            item("alarm_content") {
                AnimatedVisibility(visible = alarmHapticEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard {
                            PatternSelectorWithCustom(
                                pattern = alarmPattern, intensity = alarmIntensity, customSequence = alarmCustomSequence,
                                onPatternSelected = onAlarmPatternSelected, onIntensityCommit = onAlarmIntensityCommit,
                                onOpenCustomEditor = { onOpenCustomEditor("alarm", alarmCustomSequence, onAlarmCustomSequenceSave) }, label = "alarm",
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
private fun NotificationListenerCard() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun isListenerEnabled(): Boolean {
        val cn = ComponentName(ctx, NotificationHapticService::class.java)
        val flat = try {
            AndroidSettings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: ""
        } catch (_: Exception) { "" }
        if (flat.isBlank()) return false
        return flat.split(":").any { entry ->
            val parsed = try { ComponentName.unflattenFromString(entry.trim()) } catch (_: Exception) { null }
            parsed == cn
        }
    }

    var isGranted by remember { mutableStateOf(isListenerEnabled()) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) isGranted = isListenerEnabled() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (isGranted) return

    PermissionCard(
        icon = Icons.Rounded.NotificationsOff,
        title = "Notification access required",
        body = "Grant notification listener access so Ever Haptics can detect calls, notifications, and alarms.",
        buttonLabel = "Grant",
        isError = true,
        onClick = {
            ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        },
    )
}

@Composable
private fun PhonePermissionCard() {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun isPhonePermGranted(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    var isGranted by remember { mutableStateOf(isPhonePermGranted()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) isGranted = isPhonePermGranted() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (isGranted) return

    PermissionCard(
        icon = Icons.Rounded.PhoneDisabled,
        title = "Phone permission required",
        body = "Grant phone state access so Ever Haptics can detect incoming calls accurately.",
        buttonLabel = "Grant",
        isError = false,
        onClick = { launcher.launch(Manifest.permission.READ_PHONE_STATE) },
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val btnBg = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val btnFg = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary

    Surface(color = bg, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = fg, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = fg)
            }
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = btnBg), shape = RoundedCornerShape(10.dp)) {
                Text(buttonLabel, color = btnFg)
            }
        }
    }
}

@Composable
private fun SystemHapticsNoticeCard() {
    val ctx = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Turn off system haptics for calls & notifications", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                    Text("For best results, disable your system's default vibration for calls, notifications, and alarms. Otherwise system and Ever Haptics patterns may overlap.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Button(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent("android.settings.VIBRATION_SETTINGS")
                    } else {
                        Intent(AndroidSettings.ACTION_SOUND_SETTINGS)
                    }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    try {
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            ctx.startActivity(Intent(AndroidSettings.ACTION_SOUND_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        } catch (_: Exception) {
                            ctx.startActivity(Intent(AndroidSettings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer, contentColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Vibration, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text("Open Vibration Settings", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PatternSelectorWithCustom(
    pattern: HapticPattern, intensity: Float, customSequence: CustomHapticSequence,
    onPatternSelected: (HapticPattern) -> Unit, onIntensityCommit: (Float) -> Unit,
    onOpenCustomEditor: () -> Unit, label: String,
) {
    val isCustomActive = !customSequence.isEmpty
    val patterns = remember { HapticPattern.entries }
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            patterns.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { p -> PatternCard(pattern = p, isSelected = !isCustomActive && p == pattern, onClick = { onPatternSelected(p) }, modifier = Modifier.weight(1f).fillMaxHeight()) }
                    if (rowItems.size == 1) Box(Modifier.weight(1f).fillMaxHeight())
                }
            }
            CustomPatternCard(isSelected = isCustomActive, beatCount = customSequence.beats.size, durationMs = customSequence.durationMs, onClick = onOpenCustomEditor, modifier = Modifier.fillMaxWidth())
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        IntensitySlider(intensity = intensity, onIntensityCommit = onIntensityCommit)
    }
}

@Composable
private fun PatternCard(pattern: HapticPattern, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, spring(stiffness = 300f), label = "pc")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "pb")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "pbw")
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val descColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val stateDesc = stringResource(if (isSelected) R.string.pattern_selected else R.string.pattern_not_selected)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.heightIn(min = 132.dp).selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)).semantics { this.stateDescription = stateDesc },
        color = containerColor, shape = RoundedCornerShape(24.dp), border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(40.dp).background(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(pattern.icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
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
private fun CustomPatternCard(isSelected: Boolean, beatCount: Int, durationMs: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, spring(stiffness = 300f), label = "cc")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "cb")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "cbw")
    val interactionSource = remember { MutableInteractionSource() }
    Surface(modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)), color = containerColor, shape = RoundedCornerShape(24.dp), border = BorderStroke(borderWidth, borderColor)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Draw, null, tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Custom", style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                Text(if (isSelected && beatCount > 0) "$beatCount beats · ${durationMs}ms" else "Record your own haptic rhythm", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0f, spring(stiffness = 600f), label = "sd")
    Box(modifier = Modifier.size(22.dp).scale(scale).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
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
        Slider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onIntensityCommit(draft) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}
