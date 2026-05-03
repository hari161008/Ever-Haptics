package com.coolappstore.everhaptics.by.svhp.service.accessibility.scrolled

import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

internal object ScrollContentVibration {

    private const val REFERENCE_PX = 100f
    private const val MAX_TRACKED_SURFACES = 128
    private const val NOISE_FLOOR_VP = 2f
    private const val MIN_EMIT_INTERVAL_MS = 38L
    private const val FLING_BLEND_START_VPS = 900f
    private const val FLING_BLEND_END_VPS = 5200f
    private const val FLING_CREDIT_GAIN_MIN = 0.62f
    private const val SLOW_DRAG_BLEND_VPS = 180f
    private const val SLOW_INTENSITY_MIN_SCALE = 0.35f
    private const val VELOCITY_DT_CAP_MS = 200L
    private const val VELOCITY_SMOOTHING = 0.55f
    private const val TAIL_DECAY_FRACTION = 0.50f
    private const val INDEX_VIRTUAL_PX_PER_ITEM = 56f

    private val perSurface = ConcurrentHashMap<String, ContentState>(128)

    fun onViewScrolled(event: AccessibilityEvent, settings: HapticsSettings): Decision {
        val key = scrolledSurfaceKey(event)
        val nowUptime = SystemClock.uptimeMillis()

        val resolved = resolvePosition(event, settings.scrollHorizontalEnabled)

        // API 30+: delta-based fallback for apps that don't report absolute scroll position
        if (resolved == null) {
            return if (Build.VERSION.SDK_INT >= 30) {
                handleDeltaScroll(key, event, settings, nowUptime)
            } else {
                Decision.None
            }
        }

        val pos = resolved.first
        val vpScale = resolved.second

        val prev = perSurface[key]
        if (prev == null) {
            perSurface[key] = ContentState(
                lastPos = pos,
                lastEventTime = event.eventTime,
                smoothedVelocityVps = -1f,
                lastHapticEmitUptimeMs = 0L,
                emitAnchorVp = pos.toFloat() * vpScale,
                peakSmoothedVelocity = 0f,
                velocityPeakUptimeMs = 0L,
                vpScale = vpScale,
                syntheticVp = 0f,
            )
            evictIfNeeded()
            return Decision.None
        }

        val signedStep = pos - prev.lastPos
        if (signedStep == 0) return Decision.None

        val effectiveStepVp = abs(signedStep).toFloat() * vpScale
        if (effectiveStepVp < NOISE_FLOOR_VP) {
            perSurface[key] = prev.copy(lastPos = pos, lastEventTime = event.eventTime)
            return Decision.None
        }

        return computeAndEmit(key, prev, pos, vpScale, effectiveStepVp, event, settings, nowUptime)
    }

    private fun handleDeltaScroll(
        key: String,
        event: AccessibilityEvent,
        settings: HapticsSettings,
        nowUptime: Long,
    ): Decision {
        if (Build.VERSION.SDK_INT < 30) return Decision.None
        val deltaY = event.scrollDeltaY
        val deltaX = if (settings.scrollHorizontalEnabled) event.scrollDeltaX else 0
        val effectiveStepVp = (abs(deltaY) + abs(deltaX)).toFloat()
        if (effectiveStepVp < NOISE_FLOOR_VP) return Decision.None

        val prev = perSurface[key]
        val syntheticPos = ((prev?.syntheticVp ?: 0f) + effectiveStepVp)
        if (prev == null) {
            perSurface[key] = ContentState(
                lastPos = 0,
                lastEventTime = event.eventTime,
                smoothedVelocityVps = -1f,
                lastHapticEmitUptimeMs = 0L,
                emitAnchorVp = 0f,
                peakSmoothedVelocity = 0f,
                velocityPeakUptimeMs = 0L,
                vpScale = 1f,
                syntheticVp = syntheticPos,
            )
            evictIfNeeded()
            return Decision.None
        }
        // Use syntheticVp as the running position for delta-based surfaces
        val syntheticPrev = prev.copy(lastPos = prev.syntheticVp.toInt())
        val newState = prev.copy(syntheticVp = syntheticPos)
        perSurface[key] = newState
        return computeAndEmit(key, syntheticPrev, syntheticPos.toInt(), 1f, effectiveStepVp, event, settings, nowUptime)
    }

    private fun computeAndEmit(
        key: String,
        prev: ContentState,
        pos: Int,
        vpScale: Float,
        effectiveStepVp: Float,
        event: AccessibilityEvent,
        settings: HapticsSettings,
        nowUptime: Long,
    ): Decision {
        val dtRaw = run {
            val d = event.eventTime - prev.lastEventTime
            if (d > 0L) d else 1L
        }
        val dtCapped = dtRaw.coerceIn(1L, VELOCITY_DT_CAP_MS)
        val instantVps = effectiveStepVp * 1000f / dtCapped.toFloat()
        val smoothedV = if (prev.smoothedVelocityVps < 0f) {
            instantVps
        } else {
            prev.smoothedVelocityVps * (1f - VELOCITY_SMOOTHING) + instantVps * VELOCITY_SMOOTHING
        }

        val newPeakV: Float
        val newPeakUptimeMs: Long
        if (smoothedV > prev.peakSmoothedVelocity) {
            newPeakV = smoothedV
            newPeakUptimeMs = nowUptime
        } else {
            newPeakV = prev.peakSmoothedVelocity
            newPeakUptimeMs = prev.velocityPeakUptimeMs
        }

        val currentVp = pos.toFloat() * vpScale
        val cmToPx = 157f // ~1cm at 400dpi
        val eventsPerHundredPx = (settings.scrollHapticEventsPerCm * 100f / cmToPx).coerceIn(0.1f, 20f)
        val rate = eventsPerHundredPx.coerceIn(
            0.1f,
            20f,
        )
        val flingScale = flingCreditGainScale(smoothedV)
        val k = (rate / REFERENCE_PX) * flingScale
        val signedFromAnchor = currentVp - prev.emitAnchorVp
        val distFromAnchor = abs(signedFromAnchor)
        val credits = distFromAnchor * k

        val emitElapsed = if (prev.lastHapticEmitUptimeMs == 0L) Long.MAX_VALUE
        else nowUptime - prev.lastHapticEmitUptimeMs
        val canEmit = emitElapsed >= MIN_EMIT_INTERVAL_MS

        var newAnchorVp = prev.emitAnchorVp
        var newLastEmit = prev.lastHapticEmitUptimeMs
        var pulses = 0

        if (credits >= 1f && canEmit) {
            val denom = (rate * flingScale).coerceAtLeast(1e-5f)
            val vpPerCredit = REFERENCE_PX / denom
            val dir = if (signedFromAnchor >= 0f) 1f else -1f
            newAnchorVp += dir * vpPerCredit
            pulses = 1
            newLastEmit = nowUptime
        }

        perSurface[key] = prev.copy(
            lastPos = pos,
            lastEventTime = event.eventTime,
            smoothedVelocityVps = smoothedV,
            lastHapticEmitUptimeMs = newLastEmit,
            emitAnchorVp = newAnchorVp,
            peakSmoothedVelocity = newPeakV,
            velocityPeakUptimeMs = newPeakUptimeMs,
            vpScale = vpScale,
        )

        if (pulses <= 0) return Decision.None

        // Tail cutoff (only if enabled)
        if (settings.scrollTailCutoffEnabled) {
            val tailCutoffMs = settings.scrollTailCutoffMs.toLong()
            if (tailCutoffMs > 0L &&
                newPeakV > SLOW_DRAG_BLEND_VPS &&
                smoothedV < newPeakV * TAIL_DECAY_FRACTION &&
                (nowUptime - newPeakUptimeMs) > tailCutoffMs
            ) {
                return Decision.None
            }
        }

        val baseIntensity = if (settings.scrollIntensityEnabled) {
            settings.scrollIntensity.coerceIn(0f, 1f)
        } else { 0.45f }
        val intensityScale = slowDragIntensityScale(smoothedV)
        val pulseIntensity = (baseIntensity * intensityScale).coerceIn(0.05f, 1f)

        val baseCount = if (settings.scrollVibrationsPerEventEnabled) {
            settings.scrollVibrationsPerEvent.coerceIn(1f, 3f).roundToInt()
        } else { 1 }

        val speedExtra = if (settings.scrollSpeedVibrationEnabled && settings.scrollSpeedVibrationScale > 0f) {
            val fraction = (smoothedV / FLING_BLEND_END_VPS).coerceIn(0f, 1f)
            (fraction * settings.scrollSpeedVibrationScale * settings.scrollVibrationsPerEvent).roundToInt()
        } else 0

        val totalCount = (baseCount + speedExtra).coerceIn(1, 8)
        return Decision.Play(intensity = pulseIntensity, count = totalCount)
    }

    /**
     * Resolve scroll position from the event.
     * Priority: bounded vertical → bounded horizontal → raw vertical → index-based → raw horizontal
     * If scrollHorizontalEnabled is false, horizontal scroll is deprioritized.
     */
    private fun resolvePosition(event: AccessibilityEvent, horizontalEnabled: Boolean): Pair<Int, Float>? {
        val scrollY = event.scrollY
        val scrollX = event.scrollX
        val maxScrollY = event.maxScrollY
        val maxScrollX = event.maxScrollX
        val fromIndex = event.fromIndex

        // Vertical bounded scroll
        if (maxScrollY > 0 && scrollY >= 0)
            return Pair(scrollY.coerceIn(0, maxScrollY), 1f)

        // Horizontal bounded scroll (only if enabled, or no vertical fallback)
        if (maxScrollX > 0 && scrollX >= 0 && horizontalEnabled)
            return Pair(scrollX.coerceIn(0, maxScrollX), 1f)

        // Raw vertical (max unreported)
        if (scrollY > 0)
            return Pair(scrollY, 1f)

        // Index-based (RecyclerView, ListView, etc.)
        if (fromIndex >= 0)
            return Pair(fromIndex, INDEX_VIRTUAL_PX_PER_ITEM)

        // Horizontal raw (max unreported) - only if enabled
        if (scrollX > 0 && horizontalEnabled)
            return Pair(scrollX, 1f)

        // Horizontal bounded as last resort even if not explicitly enabled
        if (maxScrollX > 0 && scrollX >= 0)
            return Pair(scrollX.coerceIn(0, maxScrollX), 1f)

        return null
    }

    private fun flingCreditGainScale(vps: Float): Float {
        if (vps <= FLING_BLEND_START_VPS) return 1f
        val span = FLING_BLEND_END_VPS - FLING_BLEND_START_VPS
        val t = ((vps - FLING_BLEND_START_VPS) / span).coerceIn(0f, 1f)
        return 1f - (1f - FLING_CREDIT_GAIN_MIN) * t
    }

    private fun slowDragIntensityScale(vps: Float): Float {
        val t = (vps / SLOW_DRAG_BLEND_VPS).coerceIn(0f, 1f)
        return SLOW_INTENSITY_MIN_SCALE + (1f - SLOW_INTENSITY_MIN_SCALE) * t
    }

    private fun evictIfNeeded() {
        while (perSurface.size > MAX_TRACKED_SURFACES) {
            perSurface.keys.firstOrNull()?.let { perSurface.remove(it) } ?: break
        }
    }

    private data class ContentState(
        val lastPos: Int,
        val lastEventTime: Long,
        val smoothedVelocityVps: Float,
        val lastHapticEmitUptimeMs: Long,
        val emitAnchorVp: Float,
        val peakSmoothedVelocity: Float,
        val velocityPeakUptimeMs: Long,
        val vpScale: Float,
        val syntheticVp: Float = 0f,
    )

    sealed class Decision {
        data object None : Decision()
        data class Play(val intensity: Float, val count: Int = 1) : Decision()
    }
}
