package com.coolappstore.everhaptics.by.svhp.xposed

import android.app.Application
import android.content.Context
import android.widget.EdgeEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.coolappstore.everhaptics.by.svhp.edge.EdgeHapticsBridge
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import java.util.WeakHashMap

class EdgeEffectHapticsModule : XposedModule() {

    private enum class Phase { IDLE, STRETCHED, RELEASED }

    private class Session(
        @Volatile var phase: Phase = Phase.IDLE,
        @Volatile var lastDistance: Float = 0f,
    )

    private val sessions = WeakHashMap<Any, Session>()

    @Volatile private var vibrator: Vibrator? = null
    @Volatile private var enabled = false
    @Volatile private var cachedPattern: HapticPattern? = null
    @Volatile private var cachedIntensity = 1f

    private val touchAttrs: VibrationAttributes by lazy {
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        log(Log.INFO, TAG, "Loaded — process=${param.processName}")

        loadRemotePrefs()
        hookApplicationForVibrator()
        installEdgeEffectHooks()
    }

    private fun loadRemotePrefs() {
        if ((getFrameworkProperties() and PROP_CAP_REMOTE) == 0L) {
            log(Log.WARN, TAG, "Remote prefs unsupported in this process")
            return
        }
        val prefs = runCatching {
            getRemotePreferences(XposedEdgeRemotePrefs.GROUP)
        }.onFailure {
            log(Log.WARN, TAG, "Remote prefs error: ${it.message}")
        }.getOrNull() ?: return

        applyPrefs(prefs)
        prefs.registerOnSharedPreferenceChangeListener { p, _ -> applyPrefs(p) }
    }

    private fun applyPrefs(prefs: android.content.SharedPreferences) {
        enabled         = prefs.getBoolean(XposedEdgeRemotePrefs.KEY_ENABLED, false)
        cachedPattern   = HapticPattern.fromStorageKey(
            prefs.getString(XposedEdgeRemotePrefs.KEY_PATTERN, null)
        )
        cachedIntensity = prefs.getFloat(XposedEdgeRemotePrefs.KEY_INTENSITY, 1f).coerceIn(0f, 1f)
    }

    private fun hookApplicationForVibrator() {
        runCatching {
            hookAfter(Application::class.java, "attach", Context::class.java) { _, args ->
                if (vibrator != null) return@hookAfter
                val ctx = args[0] as Context
                vibrator = ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            }
        }

        runCatching {
            hookAfter(Application::class.java, "onCreate") { app, _ ->
                if (vibrator != null) return@hookAfter
                vibrator = (app as Application)
                    .getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
            }
        }
    }

    private fun installEdgeEffectHooks() {
        val cls = EdgeEffect::class.java

        runCatching {
            hookAfter(cls, "onPull",
                Float::class.javaPrimitiveType
            ) { effect, _ -> processDistanceChange(effect) }
        }.onFailure { log(Log.ERROR, TAG, "onPull(float) hook failed", it) }

        runCatching {
            hookAfter(cls, "onPull",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            ) { effect, _ -> processDistanceChange(effect) }
        }.onFailure { log(Log.ERROR, TAG, "onPull(float, float) hook failed", it) }

        runCatching {
            hookAfter(cls, "onPullDistance",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType
            ) { effect, _ -> processDistanceChange(effect) }
        }.onFailure { log(Log.DEBUG, TAG, "onPullDistance unavailable (< API 31)") }

        runCatching {
            hookAfter(cls, "onRelease") { effect, _ -> processRelease(effect) }
        }.onFailure { log(Log.ERROR, TAG, "onRelease hook failed", it) }

        runCatching {
            hookAfter(cls, "onAbsorb", Int::class.javaPrimitiveType) { effect, _ ->
                processAbsorb(effect)
            }
        }.onFailure { log(Log.ERROR, TAG, "onAbsorb hook failed", it) }

        runCatching {
            hookAfter(cls, "finish") { effect, _ -> removeState(effect) }
        }.onFailure { log(Log.DEBUG, TAG, "finish() unavailable") }
    }

    private fun processDistanceChange(effect: Any) {
        if (!enabled) return

        val distance = readDistance(effect)
        val state = getState(effect)

        if (distance == state.lastDistance) return
        state.lastDistance = distance

        val isStretched = distance > EPSILON

        when {
            isStretched && state.phase != Phase.STRETCHED -> {
                triggerHaptic(HapticEventType.PULL)
                state.phase = Phase.STRETCHED
            }
            !isStretched && state.phase == Phase.STRETCHED -> {
                state.phase = Phase.IDLE
            }
            !isStretched && state.phase == Phase.RELEASED -> {
                state.phase = Phase.IDLE
            }
        }
    }

    private fun processRelease(effect: Any) {
        if (!enabled) return
        val state = getState(effect)

        if (state.phase == Phase.STRETCHED) {
            triggerHaptic(HapticEventType.RELEASE)
            state.phase = Phase.RELEASED
        }
    }

    private fun processAbsorb(effect: Any) {
        if (!enabled) return
        triggerHaptic(HapticEventType.ABSORB)
        getState(effect).phase = Phase.RELEASED
    }

    private fun getState(effect: Any): Session = synchronized(sessions) {
        sessions.getOrPut(effect) { Session() }
    }

    private fun removeState(effect: Any) {
        synchronized(sessions) { sessions.remove(effect) }
    }

    private fun readDistance(effect: Any): Float {
        return (effect as? EdgeEffect)?.distance ?: 0f
    }

    private enum class HapticEventType { PULL, RELEASE, ABSORB }

    private fun triggerHaptic(type: HapticEventType) {
        val pattern   = cachedPattern   ?: return
        val intensity = cachedIntensity
        val vib       = vibrator        ?: return
        if (!vib.hasVibrator()) return

        val effect = runCatching {
            EdgeHapticsBridge.edgeVibrationEffect(
                pattern = pattern,
                intensity = intensity
            )
        }.onFailure {
            log(Log.DEBUG, TAG, "VibrationEffect build failed [$type]: ${it.message}")
        }.getOrNull() ?: return

        runCatching {
            vib.vibrate(effect, touchAttrs)
        }.onFailure {
            log(Log.DEBUG, TAG, "vibrate() failed [$type]: ${it.message}")
        }
    }

    private inline fun hookAfter(
        cls: Class<*>,
        name: String,
        vararg paramTypes: Class<*>?,
        crossinline block: (thisObj: Any, args: List<Any?>) -> Unit,
    ) {
        val method = cls.getDeclaredMethod(name, *paramTypes)
        hook(method)
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val result = chain.proceed()
                runCatching { block(chain.thisObject, chain.args) }
                    .onFailure { log(Log.DEBUG, TAG, "Hook [$name] error: ${it.message}") }
                result
            }
    }

    companion object {
        private const val TAG = "HapticksEdge"
        private const val EPSILON = 0.001f
    }
}
