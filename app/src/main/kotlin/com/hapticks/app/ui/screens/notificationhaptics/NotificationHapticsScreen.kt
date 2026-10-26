package com.hapticks.app.ui.screens.notificationhaptics

import com.hapticks.app.ui.theme.PetalIcons
import com.hapticks.app.ui.theme.PetalMaterialShapes
import com.hapticks.app.ui.components.PetalFeatureScreen
import com.hapticks.app.ui.components.PetalFeatureHero
import com.hapticks.app.ui.components.PetalSection
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalContentRow
import com.hapticks.app.ui.components.PetalTestRow
import com.hapticks.app.ui.components.PetalResetFooter
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.entrance
import com.hapticks.app.ui.theme.ExpressiveShapeHolder
import androidx.compose.ui.graphics.vector.ImageVector
import android.Manifest
import com.hapticks.app.ui.components.PetalButtonStyle
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
import com.hapticks.app.ui.components.PetalSlider
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    callHapticAuto: Boolean,
    onOpenCallAutoSettings: () -> Unit,
    onCallAutoChange: (Boolean) -> Unit,
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
    alarmHapticAuto: Boolean,
    onOpenAlarmAutoSettings: () -> Unit,
    onAlarmAutoChange: (Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onOpenCustomEditor: (label: String, sequence: CustomHapticSequence, onSave: (CustomHapticSequence) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalFeatureScreen(title = "Calls, Alerts & Alarms", onBack = onBack, modifier = modifier) {
        item("hero") {
            PetalFeatureHero(
                icon = Icons.Rounded.NotificationsActive,
                tint = FeatureColors.Notifications,
                subtitle = "Custom haptic patterns for incoming calls, notifications, and alarms.",
                modifier = Modifier.entrance(0),
            )
        }
        item("notif_access") { NotificationListenerCard() }
        item("phone_perm") { PhonePermissionCard() }
        item("notice") { SystemHapticsNoticeCard() }

        item("call") {
            AlertSection(
                title = "Incoming Call",
                icon = Icons.Rounded.Call,
                toggleTitle = "Call Haptic",
                toggleSubtitle = "Play a repeating haptic pattern when a call arrives",
                enabled = callHapticEnabled,
                onEnabledChange = onCallEnabledChange,
                testLabel = "Test Call Haptic",
                onTest = onTestCallHaptic,
                badgeShape = PetalMaterialShapes.Cookie9Sided,
                variant = PetalBadgeVariant.PRIMARY,
                modifier = Modifier.entrance(1),
            ) {
                PatternSelectorWithCustom(
                    pattern = callPattern, intensity = callIntensity, customSequence = callCustomSequence,
                    isAutoActive = callHapticAuto,
                    onPatternSelected = onCallPatternSelected, onIntensityCommit = onCallIntensityCommit,
                    onOpenCustomEditor = { onOpenCustomEditor("call", callCustomSequence) { seq -> onCallCustomSequenceSave(seq); if (!seq.isEmpty) onCallAutoChange(false) } },
                    onOpenAutoSettings = onOpenCallAutoSettings,
                    onClearCustom = { onCallCustomSequenceSave(CustomHapticSequence()) },
                    onDisableAuto = { onCallAutoChange(false) },
                    label = "call",
                )
            }
        }
        item("notif") {
            AlertSection(
                title = "Notifications",
                icon = Icons.Rounded.Notifications,
                toggleTitle = "Notification Haptic",
                toggleSubtitle = "Play a haptic pattern when a notification arrives",
                enabled = notifHapticEnabled,
                onEnabledChange = onNotifEnabledChange,
                testLabel = "Test Notification Haptic",
                onTest = onTestNotifHaptic,
                badgeShape = PetalMaterialShapes.Clover4Leaf,
                variant = PetalBadgeVariant.SECONDARY,
                modifier = Modifier.entrance(2),
            ) {
                PatternSelectorWithCustom(
                    pattern = notifPattern, intensity = notifIntensity, customSequence = notifCustomSequence,
                    onPatternSelected = onNotifPatternSelected, onIntensityCommit = onNotifIntensityCommit,
                    onOpenCustomEditor = { onOpenCustomEditor("notification", notifCustomSequence, onNotifCustomSequenceSave) }, label = "notification",
                )
            }
        }
        item("alarm") {
            AlertSection(
                title = "Alarm",
                icon = Icons.Rounded.Alarm,
                toggleTitle = "Alarm Haptic",
                toggleSubtitle = "Play a haptic pattern when an alarm fires",
                enabled = alarmHapticEnabled,
                onEnabledChange = onAlarmEnabledChange,
                testLabel = "Test Alarm Haptic",
                onTest = onTestAlarmHaptic,
                badgeShape = PetalMaterialShapes.Sunny,
                variant = PetalBadgeVariant.TERTIARY,
                modifier = Modifier.entrance(3),
            ) {
                PatternSelectorWithCustom(
                    pattern = alarmPattern, intensity = alarmIntensity, customSequence = alarmCustomSequence,
                    isAutoActive = alarmHapticAuto,
                    onPatternSelected = onAlarmPatternSelected, onIntensityCommit = onAlarmIntensityCommit,
                    onOpenCustomEditor = { onOpenCustomEditor("alarm", alarmCustomSequence) { seq -> onAlarmCustomSequenceSave(seq); if (!seq.isEmpty) onAlarmAutoChange(false) } },
                    onOpenAutoSettings = onOpenAlarmAutoSettings,
                    onClearCustom = { onAlarmCustomSequenceSave(CustomHapticSequence()) },
                    onDisableAuto = { onAlarmAutoChange(false) },
                    label = "alarm",
                )
            }
        }
        item("reset") {
            PetalResetFooter(onReset = onResetToDefaults, label = "Reset to defaults", modifier = Modifier.entrance(4))
        }
    }
}

/** One alert family (call / notification / alarm): toggle row, pattern card row and a play row. */
@Composable
private fun AlertSection(
    title: String,
    icon: ImageVector,
    toggleTitle: String,
    toggleSubtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    testLabel: String,
    onTest: () -> Unit,
    badgeShape: ExpressiveShapeHolder,
    variant: PetalBadgeVariant,
    modifier: Modifier = Modifier,
    patternContent: @Composable () -> Unit,
) {
    PetalSection(title = title, modifier = modifier) {
        row { position ->
            PetalGroupToggleRow(
                icon = icon,
                title = toggleTitle,
                subtitle = toggleSubtitle,
                checked = enabled,
                onCheckedChange = onEnabledChange,
                position = position,
                badgeShape = badgeShape,
                variant = variant,
            )
        }
        row { position ->
            PetalContentRow(position = position, enabled = enabled) { patternContent() }
        }
        row { position ->
            PetalTestRow(label = testLabel, onClick = onTest, position = position, enabled = enabled)
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

    Surface(color = bg, shape = com.hapticks.app.ui.components.PetalContainmentDefaults.shape, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = fg, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = fg)
            }
            PetalButton(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = btnBg)) {
                Text(buttonLabel, color = btnFg)
            }
        }
    }
}

@Composable
private fun SystemHapticsNoticeCard() {
    val ctx = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = com.hapticks.app.ui.components.PetalContainmentDefaults.shape, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(PetalIcons.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Turn off system haptics for calls & notifications", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                    Text("For best results, disable your system's default vibration for calls, notifications, and alarms. Otherwise system and Ever Haptics patterns may overlap.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            PetalButton(
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
                modifier = Modifier.fillMaxWidth()) {
                Icon(PetalIcons.Vibration, null, modifier = Modifier.size(16.dp))
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
    isAutoActive: Boolean = false,
    onPatternSelected: (HapticPattern) -> Unit, onIntensityCommit: (Float) -> Unit,
    onOpenCustomEditor: () -> Unit,
    onOpenAutoSettings: () -> Unit = {},
    onClearCustom: () -> Unit = {},
    onDisableAuto: () -> Unit = {},
    label: String,
) {
    val isCustomActive = !customSequence.isEmpty
    val patterns = remember { HapticPattern.entries }
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            patterns.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { p ->
                        PatternCard(
                            pattern = p,
                            isSelected = !isCustomActive && !isAutoActive && p == pattern,
                            onClick = { onPatternSelected(p); onClearCustom(); onDisableAuto() },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (rowItems.size == 1) Box(Modifier.weight(1f).fillMaxHeight())
                }
            }
            AutoPatternCard(
                isSelected = isAutoActive,
                onClick = { onOpenAutoSettings(); onClearCustom() },
                modifier = Modifier.fillMaxWidth(),
            )
            CustomPatternCard(
                isSelected = isCustomActive && !isAutoActive,
                beatCount = customSequence.beats.size,
                durationMs = customSequence.durationMs,
                onClick = { onOpenCustomEditor(); onDisableAuto() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        IntensitySlider(intensity = intensity, onIntensityCommit = onIntensityCommit)
    }
}

@Composable
private fun PatternCard(pattern: HapticPattern, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest, spring(stiffness = 300f), label = "pc")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "pb")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "pbw")
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val descColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val stateDesc = stringResource(if (isSelected) R.string.pattern_selected else R.string.pattern_not_selected)
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")
    Surface(
        modifier = modifier.heightIn(min = 132.dp).selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)).semantics { this.stateDescription = stateDesc },
        color = containerColor, shape = RoundedCornerShape(corner),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(40.dp).background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = com.hapticks.app.ui.theme.PetalMaterialShapes.Cookie6Sided.toShape()), contentAlignment = Alignment.Center) {
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
private fun AutoPatternCard(isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest, spring(stiffness = 300f), label = "ac")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "ab")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "abw")
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")
    Surface(modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)), color = containerColor, shape = RoundedCornerShape(corner)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = com.hapticks.app.ui.theme.PetalMaterialShapes.Cookie6Sided.toShape()), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GraphicEq, null, tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Auto", style = MaterialTheme.typography.titleMedium, color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("Vibrate in sync with audio beats in real time", style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CustomPatternCard(isSelected: Boolean, beatCount: Int, durationMs: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest, spring(stiffness = 300f), label = "cc")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, spring(stiffness = 300f), label = "cb")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 1.dp, label = "cbw")
    val interactionSource = remember { MutableInteractionSource() }
    val corner by animateDpAsState(if (isSelected) 32.dp else 20.dp, spring(dampingRatio = 0.6f, stiffness = 380f), label = "pattern_corner")
    Surface(modifier = modifier.selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton, interactionSource = interactionSource, indication = ripple(bounded = true)), color = containerColor, shape = RoundedCornerShape(corner)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = com.hapticks.app.ui.theme.PetalMaterialShapes.Cookie6Sided.toShape()), contentAlignment = Alignment.Center) {
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
        if (isSelected) Icon(PetalIcons.Check, stringResource(R.string.pattern_selected), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
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
        PetalSlider(value = draft, onValueChange = { draft = it; val t = slider01ToTickIndex(it); if (t != lastTick) { lastTick = t; ctx.performHapticSliderTick() } }, onValueChangeFinished = { onIntensityCommit(draft) }, valueRange = 0f..1f, steps = SliderTickStepsDefault, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}
