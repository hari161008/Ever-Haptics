package com.hapticks.app.service.accessibility.scrolled

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.hapticks.app.data.HapticsSettings
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

internal object ScrollContentVibration {

    private const val REFERENCE_PX = 100f
    private const val MAX_TRACKED_SURFACES = 128

    /**
     * Noise floor in virtual pixels. Index-based steps are scaled up (~56 vp each),
     * so this only filters sub-pixel jitter on true pixel-scroll surfaces.
     */
    private const val NOISE_FLOOR_VP = 2f

    /** Minimum gap between emitted haptic ticks (ms). */
    private const val MIN_EMIT_INTERVAL_MS = 38L

    /** Above this speed (vp/s), credit gain tapers so fast flings don't over-tick. */
    private const val FLING_BLEND_START_VPS = 900f
    private const val FLING_BLEND_END_VPS = 5200f
    private const val FLING_CREDIT_GAIN_MIN = 0.62f

    /** Below this speed, ticks stay soft (light finger drag). */
    private const val SLOW_DRAG_BLEND_VPS = 180f
    private const val SLOW_INTENSITY_MIN_SCALE = 0.35f

    private const val VELOCITY_DT_CAP_MS = 200L
    private const val VELOCITY_SMOOTHING = 0.55f
    private const val TAIL_DECAY_FRACTION = 0.50f

    /**
     * Virtual-pixel scale applied when the view reports item indices instead of
     * pixel offsets (RecyclerView, ListView, GridView, etc.).
     * Approximates a typical list-item height so the credit system stays consistent.
     */
    private const val INDEX_VIRTUAL_PX_PER_ITEM = 56f

    private val perSurface = ConcurrentHashMap<String, ContentState>(128)

    // ──────────────────────────────────────────────────────────────
    // Public entry point
    // ──────────────────────────────────────────────────────────────

    fun onViewScrolled(event: AccessibilityEvent, settings: HapticsSettings): Decision {
        val key = scrolledSurfaceKey(event) ?: return Decision.None
        val nowUptime = SystemClock.uptimeMillis()

        // Resolve position in "virtual pixels" so the credit system is uniform
        // whether the view reports pixel offsets or item indices.
        val resolved = resolvePosition(event) ?: return Decision.None
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
            )
            evictIfNeeded()
            return Decision.None
        }

        val signedStep = pos - prev.lastPos
        if (signedStep == 0) return Decision.None

        val effectiveStepVp = abs(signedStep).toFloat() * vpScale

        // Filter micro-jitter
        if (effectiveStepVp < NOISE_FLOOR_VP) {
            perSurface[key] = prev.copy(lastPos = pos, lastEventTime = event.eventTime)
            return Decision.None
        }

        // Velocity in virtual px/s
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

        // Track velocity peak for tail-cutoff
        val newPeakV: Float
        val newPeakUptimeMs: Long
        if (smoothedV > prev.peakSmoothedVelocity) {
            newPeakV = smoothedV
            newPeakUptimeMs = nowUptime
        } else {
            newPeakV = prev.peakSmoothedVelocity
            newPeakUptimeMs = prev.velocityPeakUptimeMs
        }

        // Credit accumulation in virtual pixels since last emit anchor
        val currentVp = pos.toFloat() * vpScale
        val rate = settings.scrollHapticEventsPerHundredPx.coerceIn(
            HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX,
            HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX,
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

        perSurface[key] = ContentState(
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

        // Tail cutoff: suppress haptics deep into a decelerating fling
        val tailCutoffMs = settings.scrollTailCutoffMs.toLong()
        if (tailCutoffMs > 0L &&
            newPeakV > SLOW_DRAG_BLEND_VPS &&
            smoothedV < newPeakV * TAIL_DECAY_FRACTION &&
            (nowUptime - newPeakUptimeMs) > tailCutoffMs
        ) {
            return Decision.None
        }

        // Compute intensity
        val baseIntensity = settings.scrollIntensity.coerceIn(0f, 1f)
        val intensityScale = slowDragIntensityScale(smoothedV)
        val pulseIntensity = (baseIntensity * intensityScale).coerceIn(0.05f, 1f)

        // Compute count: fixed base + speed bonus
        val baseCount = settings.scrollVibrationsPerEvent.coerceIn(
            HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
            HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
        ).roundToInt()

        val speedExtra = if (settings.scrollSpeedVibrationScale > 0f) {
            val fraction = (smoothedV / FLING_BLEND_END_VPS).coerceIn(0f, 1f)
            (fraction * settings.scrollSpeedVibrationScale * settings.scrollVibrationsPerEvent).roundToInt()
        } else 0

        val totalCount = (baseCount + speedExtra).coerceIn(1, 8)

        return Decision.Play(intensity = pulseIntensity, count = totalCount)
    }

    // ──────────────────────────────────────────────────────────────
    // Position resolution
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns (position, virtualPixelScale) or null if no usable scroll position found.
     *
     * Priority (first non-null wins):
     *  1. Vertical pixel scroll with valid maxScrollY  → scale 1.0
     *  2. Horizontal pixel scroll with valid maxScrollX → scale 1.0
     *  3. Raw scrollY > 0 (max unreported)            → scale 1.0
     *  4. Raw scrollX > 0 (max unreported)            → scale 1.0
     *  5. fromIndex (RecyclerView, ListView, etc.)    → scale INDEX_VIRTUAL_PX_PER_ITEM
     */
    private fun resolvePosition(event: AccessibilityEvent): Pair<Int, Float>? {
        val scrollY = event.scrollY
        val scrollX = event.scrollX
        val maxScrollY = event.maxScrollY
        val maxScrollX = event.maxScrollX
        val fromIndex = event.fromIndex

        if (maxScrollY > 0 && scrollY >= 0)
            return Pair(scrollY.coerceIn(0, maxScrollY), 1f)

        if (maxScrollX > 0 && scrollX >= 0)
            return Pair(scrollX.coerceIn(0, maxScrollX), 1f)

        if (scrollY > 0)
            return Pair(scrollY, 1f)

        if (scrollX > 0)
            return Pair(scrollX, 1f)

        if (fromIndex >= 0)
            return Pair(fromIndex, INDEX_VIRTUAL_PX_PER_ITEM)

        return null
    }

    // ──────────────────────────────────────────────────────────────
    // Math helpers
    // ──────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────
    // State & result types
    // ──────────────────────────────────────────────────────────────

    private data class ContentState(
        val lastPos: Int,
        val lastEventTime: Long,
        val smoothedVelocityVps: Float,
        val lastHapticEmitUptimeMs: Long,
        val emitAnchorVp: Float,
        val peakSmoothedVelocity: Float,
        val velocityPeakUptimeMs: Long,
        val vpScale: Float,
    )

    sealed class Decision {
        data object None : Decision()
        data class Play(val intensity: Float, val count: Int = 1) : Decision()
    }
}
