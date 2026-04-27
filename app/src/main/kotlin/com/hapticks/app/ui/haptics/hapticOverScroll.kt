@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.hapticks.app.ui.haptics

import android.content.Context
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.rememberPlatformOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticEngine
import com.hapticks.app.haptics.HapticPattern
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

private const val EdgePullSlopPx = 0.5f
private class HapticInstrumentedOverscrollEffect(
    private val delegate: OverscrollEffect,
    private val engine: HapticEngine?,
    private val intensity: Float,
) : OverscrollEffect {

    private enum class Phase { IDLE, STRETCHED, RELEASED }
    private enum class HapticEventType { EDGE_HIT, RELEASE, ABSORB }

    @Volatile
    private var phase: Phase = Phase.IDLE

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset = delegate.applyToScroll(delta, source) { scrollDelta ->
        val consumedByChild = performScroll(scrollDelta)

        if (source == NestedScrollSource.UserInput) {
            val overscroll = scrollDelta - consumedByChild
            val isAtEdge = abs(overscroll.x) > EdgePullSlopPx ||
                           abs(overscroll.y) > EdgePullSlopPx

            when {
                isAtEdge && phase == Phase.IDLE -> {
                    triggerHaptic(HapticEventType.EDGE_HIT)
                    phase = Phase.STRETCHED
                }
                !isAtEdge && phase == Phase.STRETCHED -> {
                    phase = Phase.IDLE
                }
                isAtEdge && phase == Phase.RELEASED -> {
                    triggerHaptic(HapticEventType.EDGE_HIT)
                    phase = Phase.STRETCHED
                }
                !isAtEdge && phase == Phase.RELEASED -> {
                    phase = Phase.IDLE
                }
            }
        }

        consumedByChild
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val wasStretched = phase == Phase.STRETCHED
        val wasInProgress = delegate.isInProgress

        delegate.applyToFling(velocity, performFling)

        when {
            wasStretched -> {
                triggerHaptic(HapticEventType.RELEASE)
                phase = Phase.RELEASED
            }
            !wasInProgress && delegate.isInProgress && phase == Phase.IDLE -> {
                triggerHaptic(HapticEventType.ABSORB)
                phase = Phase.RELEASED
            }
        }

        if (!delegate.isInProgress) {
            phase = Phase.IDLE
        }
    }

    override val isInProgress: Boolean
        get() = delegate.isInProgress

    override val node: DelegatableNode
        get() = delegate.node

    private fun triggerHaptic(type: HapticEventType) {
        val pattern = HapticPattern.SOFT_BUMP
        engine?.play(
            pattern = pattern,
            intensity = intensity,
            throttleMs = 0L,
        )
    }
}

private class HapticInstrumentedOverscrollFactory(
    private val delegate: OverscrollFactory,
    private val engine: HapticEngine?,
    private val intensity: Float,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect =
        HapticInstrumentedOverscrollEffect(
            delegate.createOverscrollEffect(),
            engine,
            intensity,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticInstrumentedOverscrollFactory) return false
        return delegate == other.delegate &&
            engine === other.engine &&
            intensity == other.intensity
    }

    override fun hashCode(): Int {
        var result = delegate.hashCode()
        result = 31 * result + (engine?.hashCode() ?: 0)
        result = 31 * result + intensity.hashCode()
        return result
    }
}

@Composable
fun ProvideHapticksEdgeOverscrollHaptics(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as? HapticksApp
    val engine = remember(app) { app?.hapticEngine }
    val settingsFlow = remember(app) {
        app?.preferences?.settings ?: flowOf(HapticsSettings.Default)
    }
    val settings by settingsFlow.collectAsStateWithLifecycle(HapticsSettings.Default)
    val baseFactory = rememberPlatformOverscrollFactory()
    val factory = remember(baseFactory, engine, settings.edgeIntensity) {
        HapticInstrumentedOverscrollFactory(baseFactory, engine, settings.edgeIntensity)
    }
    CompositionLocalProvider(LocalOverscrollFactory provides factory) {
        content()
    }
}

fun Context.performAppEdgeOverscrollHaptic() {
    val app = applicationContext as? HapticksApp ?: return
    val snapshot = try {
        runBlocking { app.preferences.settings.first() }
    } catch (_: Throwable) {
        HapticsSettings.Default
    }
    app.hapticEngine.play(
        pattern = HapticPattern.SOFT_BUMP,
        intensity = snapshot.edgeIntensity,
        throttleMs = 0L,
    )
}