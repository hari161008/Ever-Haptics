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
     * Ignore tiny steps from touch noise / layout jitter. Slightly higher than 2px so
     * alternating 2px+2px micro-shifts do not stack into meaningful credit.
     */
    private const val NOISE_FLOOR_PX = 4

    /** At most one tick per accessibility event so pulses never stack in the same frame. */

    /** Spacing between emitted ticks (ms). */
    private const val MIN_EMIT_INTERVAL_MS = 55L

    /** Above this speed (px/s), credit gain tapers so flings feel less mechanical. */
    private const val FLING_BLEND_START_PPS = 900f
    private const val FLING_BLEND_END_PPS = 5200f
    /** Minimum multiplier on distance→credit at high speed (wider virtual ridges). */
    private const val FLING_CREDIT_GAIN_MIN = 0.62f

    /** Below this speed, ticks stay soft (light contact with the surface). */
    private const val SLOW_DRAG_BLEND_PPS = 220f
    private const val SLOW_INTENSITY_MIN_SCALE = 0.38f

    /** Cap dt when inferring speed so a long pause does not read as ultra-slow drag. */
    private const val VELOCITY_DT_CAP_MS = 200L

    private const val VELOCITY_SMOOTHING = 0.55f

    /** Velocity must drop below this fraction of peak to be considered a decelerating fling tail. */
    private const val TAIL_DECAY_FRACTION = 0.55f

    private val perSurface = ConcurrentHashMap<String, ContentState>()

    fun onViewScrolled(event: AccessibilityEvent, settings: HapticsSettings): Decision {
        val my = event.maxScrollY
        if (my <= 0) {
            return Decision.None
        }

        val pos = event.scrollY.coerceIn(0, my)
        val key = scrolledSurfaceKey(event) ?: return Decision.None
        val eventTime = event.eventTime

        val prev = perSurface[key]
        if (prev == null) {
            perSurface[key] = ContentState(
                lastPos = pos,
                lastEventTime = eventTime,
                smoothedVelocityPps = -1f,
                lastHapticEmitUptimeMs = 0L,
                emitAnchorPx = pos.toFloat(),
                peakSmoothedVelocity = 0f,
                velocityPeakUptimeMs = 0L,
            )
            evictIfNeeded()
            return Decision.None
        }

        val signedStep = pos - prev.lastPos
        if (signedStep == 0) {
            return Decision.None
        }

        if (abs(signedStep) < NOISE_FLOOR_PX) {
            perSurface[key] = prev.copy(
                lastPos = pos,
                lastEventTime = eventTime,
            )
            return Decision.None
        }

        val dtRaw = if (prev.lastEventTime >= 0L) {
            val d = eventTime - prev.lastEventTime
            if (d > 0L) d else 1L
        } else {
            16L
        }
        val dtForVelocity = dtRaw.coerceIn(1L, VELOCITY_DT_CAP_MS)
        val stepAbs = abs(signedStep).toFloat()
        val instantVelocityPps = stepAbs * 1000f / dtForVelocity.toFloat()
        val smoothedV = if (prev.smoothedVelocityPps < 0f) {
            instantVelocityPps
        } else {
            prev.smoothedVelocityPps * (1f - VELOCITY_SMOOTHING) +
                instantVelocityPps * VELOCITY_SMOOTHING
        }

        // Track peak velocity for tail cutoff
        val nowUptime = SystemClock.uptimeMillis()
        val newPeakSmoothedVelocity: Float
        val newVelocityPeakUptimeMs: Long
        if (smoothedV > prev.peakSmoothedVelocity) {
            newPeakSmoothedVelocity = smoothedV
            newVelocityPeakUptimeMs = nowUptime
        } else {
            newPeakSmoothedVelocity = prev.peakSmoothedVelocity
            newVelocityPeakUptimeMs = prev.velocityPeakUptimeMs
        }

        val rate = settings.scrollHapticEventsPerHundredPx.coerceIn(
            HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX,
            HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX,
        )
        val flingScale = flingCreditGainScale(smoothedV)
        val k = (rate / REFERENCE_PX) * flingScale
        val signedFromAnchor = pos.toFloat() - prev.emitAnchorPx
        val distFromAnchor = abs(signedFromAnchor)
        val creditsAvailable = distFromAnchor * k

        val baseIntensity = settings.scrollIntensity.coerceIn(0f, 1f)
        val intensityScale = slowDragIntensityScale(smoothedV)
        val pulseIntensity = (baseIntensity * intensityScale).coerceIn(0f, 1f)

        val emitElapsed = if (prev.lastHapticEmitUptimeMs == 0L) {
            Long.MAX_VALUE
        } else {
            nowUptime - prev.lastHapticEmitUptimeMs
        }
        val canEmitTick = emitElapsed >= MIN_EMIT_INTERVAL_MS

        var pulses = 0
        var lastEmit = prev.lastHapticEmitUptimeMs
        var emitAnchorPx = prev.emitAnchorPx
        if (creditsAvailable >= 1f && canEmitTick) {
            val denom = (rate * flingScale).coerceAtLeast(1e-5f)
            val pxPerCredit = REFERENCE_PX / denom
            val towardPos = if (signedFromAnchor >= 0f) 1f else -1f
            emitAnchorPx += towardPos * pxPerCredit
            pulses = 1
            lastEmit = nowUptime
        }

        perSurface[key] = ContentState(
            lastPos = pos,
            lastEventTime = eventTime,
            smoothedVelocityPps = smoothedV,
            lastHapticEmitUptimeMs = lastEmit,
            emitAnchorPx = emitAnchorPx,
            peakSmoothedVelocity = newPeakSmoothedVelocity,
            velocityPeakUptimeMs = newVelocityPeakUptimeMs,
        )

        if (pulses <= 0) return Decision.None

        // Tail cutoff: suppress haptics if we are deep into a decelerating fling tail
        val tailCutoffMs = settings.scrollTailCutoffMs.toLong()
        if (tailCutoffMs > 0L &&
            newPeakSmoothedVelocity > SLOW_DRAG_BLEND_PPS &&
            smoothedV < newPeakSmoothedVelocity * TAIL_DECAY_FRACTION &&
            (nowUptime - newVelocityPeakUptimeMs) > tailCutoffMs
        ) {
            return Decision.None
        }

        // Vibrations per event: fixed base count
        val baseCount = settings.scrollVibrationsPerEvent.coerceIn(
            HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
            HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
        ).roundToInt()

        // Speed-based extra vibrations
        val speedExtra = if (settings.scrollSpeedVibrationScale > 0f) {
            val speedFraction = (smoothedV / FLING_BLEND_END_PPS).coerceIn(0f, 1f)
            (speedFraction * settings.scrollSpeedVibrationScale * settings.scrollVibrationsPerEvent).roundToInt()
        } else {
            0
        }

        val totalCount = (baseCount + speedExtra).coerceIn(1, 8)

        return Decision.Play(intensity = pulseIntensity, count = totalCount)
    }

    private fun flingCreditGainScale(velocityPps: Float): Float {
        if (velocityPps <= FLING_BLEND_START_PPS) return 1f
        val span = FLING_BLEND_END_PPS - FLING_BLEND_START_PPS
        val t = ((velocityPps - FLING_BLEND_START_PPS) / span).coerceIn(0f, 1f)
        return 1f - (1f - FLING_CREDIT_GAIN_MIN) * t
    }

    private fun slowDragIntensityScale(velocityPps: Float): Float {
        val t = (velocityPps / SLOW_DRAG_BLEND_PPS).coerceIn(0f, 1f)
        return SLOW_INTENSITY_MIN_SCALE + (1f - SLOW_INTENSITY_MIN_SCALE) * t
    }

    private fun evictIfNeeded() {
        while (perSurface.size > MAX_TRACKED_SURFACES) {
            val drop = perSurface.keys.firstOrNull() ?: return
            perSurface.remove(drop)
        }
    }

    private data class ContentState(
        val lastPos: Int,
        val lastEventTime: Long,
        val smoothedVelocityPps: Float,
        val lastHapticEmitUptimeMs: Long,
        val emitAnchorPx: Float,
        val peakSmoothedVelocity: Float,
        val velocityPeakUptimeMs: Long,
    )

    sealed class Decision {
        data object None : Decision()
        data class Play(val intensity: Float, val count: Int = 1) : Decision()
    }
}
