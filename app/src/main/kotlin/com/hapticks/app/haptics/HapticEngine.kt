package com.hapticks.app.haptics

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Single entry point for every vibration the app makes.
 *
 * Phones differ (and Android keeps changing) in which vibration usage they honour for a
 * third-party app, and some drop a vibration silently instead of throwing. The engine therefore
 * keeps an ordered list of delivery strategies (a vibration usage + a rich/simple effect) and
 * watches the vibrator's own state callback to see whether a vibration actually started. If a
 * strategy keeps getting dropped it moves to the next one, remembers the one that works, and
 * re-checks occasionally so a later OS update cannot leave the app silent.
 */
class HapticEngine(context: Context) {

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator = resolveVibrator(appContext)

    val hasVibrator: Boolean = safe(false) { vibrator.hasVibrator() }
    private val hasAmplitudeControl: Boolean = safe(false) { vibrator.hasAmplitudeControl() }
    @Volatile private var predefinedWorks: Boolean = true

    private enum class Route { ALARM, TOUCH, MEDIA, UNSPECIFIED }
    private enum class Tier { RICH, SIMPLE }
    private data class Candidate(val route: Route, val tier: Tier)

    private val candidates: List<Candidate> = buildList {
        for (tier in Tier.entries) for (route in Route.entries) add(Candidate(route, tier))
    }

    private val attributes: Map<Route, VibrationAttributes?> = mapOf(
        Route.ALARM to attributesFor(VibrationAttributes.USAGE_ALARM),
        Route.TOUCH to attributesFor(VibrationAttributes.USAGE_TOUCH),
        Route.MEDIA to attributesFor(VibrationAttributes.USAGE_MEDIA),
        Route.UNSPECIFIED to null,
    )

    private val enginePrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val savedIndex: Int = enginePrefs.getInt(savedKey(), -1)

    @Volatile private var index: Int = if (savedIndex in candidates.indices) savedIndex else 0
    @Volatile private var confirmed: Boolean = savedIndex in candidates.indices
    @Volatile private var cooldownUntil: Long = 0L
    private val failStreak = AtomicInteger(0)
    private val sampleCounter = AtomicInteger(0)
    private val startSignals = AtomicInteger(0)
    private val stateCallbackWorks: Boolean

    init {
        stateCallbackWorks = hasVibrator && runCatching {
            val listenerClass = Class.forName("android.os.Vibrator\$OnVibratorStateChangedListener")
            val method = vibrator.javaClass.getMethod(
                "addVibratorStateListener",
                java.util.concurrent.Executor::class.java,
                listenerClass
            )
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, m, args ->
                if (m.name == "onVibratorStateChanged") {
                    val vibrating = args?.getOrNull(0) as? Boolean ?: false
                    if (vibrating) startSignals.incrementAndGet()
                }
                null
            }
            method.invoke(vibrator, appContext.mainExecutor, proxy)
        }.isSuccess
    }

    private val primitiveSupport: Map<HapticPattern, Boolean> =
        HapticPattern.entries.associateWith { pattern ->
            try { vibrator.areAllPrimitivesSupported(*primitivesRequired(pattern)) }
            catch (_: Throwable) { false }
        }

    private val effectCache: ConcurrentHashMap<Int, VibrationEffect> =
        ConcurrentHashMap(HapticPattern.entries.size * INTENSITY_BUCKETS)
    private val lastPlayMs: ConcurrentHashMap<HapticPattern, AtomicLong> =
        ConcurrentHashMap(HapticPattern.entries.size)

    fun play(pattern: HapticPattern, intensity: Float, throttleMs: Long = 0L): Boolean {
        if (!hasVibrator) return false
        if (intensity <= MIN_AUDIBLE_INTENSITY) return false
        if (throttleMs > 0L) {
            val now = System.currentTimeMillis()
            val lastMs = lastPlayMs.getOrPut(pattern) { AtomicLong(0L) }
            val prev = lastMs.get()
            if (now - prev < throttleMs) return false
            lastMs.compareAndSet(prev, now)
        }
        val clamped = intensity.coerceIn(0f, 1f)
        val simple = simpleEffect(pattern, clamped)
        val rich = try { effectFor(pattern, clamped) } catch (_: Throwable) { simple }
        dispatch(rich, simple)
        return true
    }

    fun playOneShot(durationMs: Long, amplitude: Int) {
        if (!hasVibrator) return
        val effect = try {
            VibrationEffect.createOneShot(durationMs.coerceAtLeast(1L), amplitude.coerceIn(1, 255))
        } catch (_: Throwable) { return }
        dispatch(effect, effect)
    }

    fun playStrong(pattern: HapticPattern) {
        if (!hasVibrator) return
        val simple = simpleEffect(pattern, 1.0f)
        val rich = try { effectFor(pattern, 1.0f) } catch (_: Throwable) { simple }
        dispatch(rich, simple)
    }

    /** Forget the remembered strategy and search again on the next vibrations. */
    fun resetStrategy() {
        enginePrefs.edit().remove(savedKey()).apply()
        failStreak.set(0)
        cooldownUntil = 0L
        confirmed = false
        index = 0
    }

    /** Plain-text description of this device and what the engine is currently doing. */
    fun diagnostics(): String = buildString {
        val current = candidates[index.coerceIn(0, candidates.lastIndex)]
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Vibrator: ${if (hasVibrator) "present" else "missing"}, amplitude control: ${if (hasAmplitudeControl) "yes" else "no"}")
        appendLine("Vibration state callback: ${if (stateCallbackWorks) "available" else "unavailable"}")
        appendLine(
            "Primitives: " + HapticPattern.entries.joinToString { p ->
                "${p.name.lowercase()}=${if (primitiveSupport[p] == true) "yes" else "no"}"
            }
        )
        append("Delivery: ${current.route.name.lowercase()} / ${current.tier.name.lowercase()}")
        append(if (confirmed) " (confirmed)" else " (checking)")
    }

    private fun dispatch(rich: VibrationEffect, simple: VibrationEffect) {
        var attempts = 0
        while (attempts < candidates.size) {
            val candidate = candidates[index]
            val effect = if (candidate.tier == Tier.RICH) rich else simple
            val wasVibrating = safe(false) {
                val method = vibrator.javaClass.getMethod("isVibrating")
                method.invoke(vibrator) as? Boolean ?: false
            }
            val sent = try {
                submit(candidate.route, effect)
                true
            } catch (_: Throwable) {
                false
            }
            if (sent) {
                scheduleVerification(wasVibrating)
                return
            }
            advance()
            attempts++
        }
    }

    private fun submit(route: Route, effect: VibrationEffect) {
        val attrs = attributes[route]
        if (attrs != null) vibrator.vibrate(effect, attrs) else vibrator.vibrate(effect)
    }

    private fun scheduleVerification(wasVibrating: Boolean) {
        if (!stateCallbackWorks || wasVibrating) return
        if (SystemClock.elapsedRealtime() < cooldownUntil) return
        if (confirmed && sampleCounter.incrementAndGet() % SAMPLE_EVERY != 0) return
        val baseline = startSignals.get()
        val checked = index
        mainHandler.postDelayed({ judge(checked, baseline) }, VERIFY_DELAY_MS)
    }

    private fun judge(checkedIndex: Int, baseline: Int) {
        if (checkedIndex != index) return
        if (startSignals.get() > baseline) {
            failStreak.set(0)
            if (!confirmed) {
                confirmed = true
                enginePrefs.edit().putInt(savedKey(), checkedIndex).apply()
            }
            return
        }
        if (failStreak.incrementAndGet() >= FAILS_BEFORE_SWITCH) {
            failStreak.set(0)
            advance()
        }
    }

    private fun advance() {
        confirmed = false
        val next = index + 1
        if (next >= candidates.size) {
            // Nothing got through: vibration is most likely switched off in system settings right
            // now. Go back to the preferred strategy and stop probing for a while.
            index = 0
            cooldownUntil = SystemClock.elapsedRealtime() + COOLDOWN_MS
            enginePrefs.edit().remove(savedKey()).apply()
        } else {
            index = next
        }
    }

    private fun effectFor(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val bucket = ((intensity * (INTENSITY_BUCKETS - 1)) + 0.5f).toInt().coerceIn(0, INTENSITY_BUCKETS - 1)
        val key = pattern.ordinal * INTENSITY_BUCKETS + bucket
        effectCache[key]?.let { return it }
        val bucketIntensity = bucket.toFloat() / (INTENSITY_BUCKETS - 1)
        val built = buildEffect(pattern, bucketIntensity)
        return effectCache.putIfAbsent(key, built) ?: built
    }

    private fun buildEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        if (primitiveSupport[pattern] == true) {
            try {
                val composition = VibrationEffect.startComposition()
                when (pattern) {
                    HapticPattern.CLICK -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    HapticPattern.TICK -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    HapticPattern.HEAVY_CLICK -> {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (intensity * 0.6f).coerceIn(0f, 1f), 40)
                    }
                    HapticPattern.DOUBLE_CLICK -> {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80)
                    }
                    HapticPattern.SOFT_BUMP -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, intensity)
                    HapticPattern.DOUBLE_TICK -> {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60)
                    }
                }
                return composition.compose()
            } catch (_: Throwable) { /* primitives failed, fall through */ }
        }
        val effectId = when (pattern) {
            HapticPattern.CLICK -> VibrationEffect.EFFECT_CLICK
            HapticPattern.TICK -> VibrationEffect.EFFECT_TICK
            HapticPattern.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            HapticPattern.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
            HapticPattern.SOFT_BUMP -> VibrationEffect.EFFECT_TICK
            HapticPattern.DOUBLE_TICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
        }
        if (predefinedWorks && hasAmplitudeControl && intensity < 0.9f) {
            return simpleEffect(pattern, intensity)
        }
        return try {
            VibrationEffect.createPredefined(effectId)
        } catch (_: Throwable) {
            predefinedWorks = false
            simpleEffect(pattern, intensity)
        }
    }

    /** Plain one-shot pulse every vibrator can play, used as the universal fallback. */
    private fun simpleEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val durationMs = when (pattern) {
            HapticPattern.CLICK -> 40L
            HapticPattern.TICK -> 25L
            HapticPattern.HEAVY_CLICK -> 70L
            HapticPattern.DOUBLE_CLICK -> 60L
            HapticPattern.SOFT_BUMP -> 30L
            HapticPattern.DOUBLE_TICK -> 50L
        }
        val amplitude = (intensity * 200f + 30f).toInt().coerceIn(1, 255)
        return VibrationEffect.createOneShot(durationMs, amplitude)
    }

    private fun savedKey(): String = "strategy_${Build.FINGERPRINT.hashCode()}"

    private companion object {
        const val PREFS_NAME = "hapticks_engine"
        const val MIN_AUDIBLE_INTENSITY = 0.01f
        const val INTENSITY_BUCKETS = 21
        const val VERIFY_DELAY_MS = 300L
        const val FAILS_BEFORE_SWITCH = 2
        const val SAMPLE_EVERY = 32
        const val COOLDOWN_MS = 60_000L

        fun primitivesRequired(pattern: HapticPattern): IntArray = when (pattern) {
            HapticPattern.CLICK, HapticPattern.DOUBLE_CLICK, HapticPattern.HEAVY_CLICK ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_CLICK)
            HapticPattern.TICK, HapticPattern.DOUBLE_TICK ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_TICK)
            HapticPattern.SOFT_BUMP ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        }

        fun attributesFor(usage: Int): VibrationAttributes? =
            try { VibrationAttributes.createForUsage(usage) } catch (_: Throwable) { null }

        fun resolveVibrator(context: Context): Vibrator = try {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } catch (_: Throwable) {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        inline fun <T> safe(fallback: T, block: () -> T): T =
            try { block() } catch (_: Throwable) { fallback }
    }
}
