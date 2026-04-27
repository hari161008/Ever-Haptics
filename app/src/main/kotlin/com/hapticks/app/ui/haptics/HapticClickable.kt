package com.hapticks.app.ui.haptics

import android.content.Context
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticEngine
import com.hapticks.app.haptics.HapticPattern
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

const val SliderTickStepsDefault = 19

fun slider01ToTickIndex(
    value: Float,
    steps: Int = SliderTickStepsDefault
): Int =
    (value.coerceIn(0f, 1f) * (steps + 1))
        .roundToInt()
        .coerceIn(0, steps + 1)

private fun Context.hapticEngine(): HapticEngine? =
    (applicationContext as? HapticksApp)?.hapticEngine

fun Context.performHapticClick() {
    val app = applicationContext as? HapticksApp ?: return
    val snapshot = try {
        runBlocking { app.preferences.settings.first() }
    } catch (_: Throwable) {
        HapticsSettings.Default
    }
    app.hapticEngine.play(snapshot.edgePattern, snapshot.edgeIntensity)
}

fun Context.performHapticSliderTick() {
    hapticEngine()?.play(
        pattern = HapticPattern.TICK,
        intensity = 0.42f,
    )
}

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val context = LocalContext.current
    val app = context.applicationContext as? HapticksApp
    val engine = remember(app) { app?.hapticEngine }
    val settingsFlow = remember(app) {
        app?.preferences?.settings ?: flowOf(HapticsSettings.Default)
    }
    val settings by settingsFlow.collectAsStateWithLifecycle(HapticsSettings.Default)
    val interactionSource = remember { MutableInteractionSource() }

    clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {
            engine?.play(settings.edgePattern, settings.edgeIntensity)
            onClick()
        },
    )
}
