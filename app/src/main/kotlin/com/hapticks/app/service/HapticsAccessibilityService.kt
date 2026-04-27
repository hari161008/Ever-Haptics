package com.hapticks.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticEngine
import com.hapticks.app.service.accessibility.isAccessibilityEventFromOwnApplication
import com.hapticks.app.service.accessibility.interacted.InteractableViewHaptics
import com.hapticks.app.service.accessibility.scrolled.ScrollAbsoluteEdgeVibration
import com.hapticks.app.service.accessibility.scrolled.ScrollContentVibration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HapticsAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settingsJob: Job? = null

    @Volatile
    private var current: HapticsSettings = HapticsSettings.Default

    private lateinit var engine: HapticEngine

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as HapticksApp
        engine = app.hapticEngine

        applyEventMask(HapticsSettings.Default)

        settingsJob = app.preferences.settings
            .distinctUntilChanged()
            .onEach { snapshot ->
                current = snapshot
                applyEventMask(snapshot)
            }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val fromOwnApp = isAccessibilityEventFromOwnApplication(ev)
        if (fromOwnApp && ev.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return
        }

        val type = ev.eventType

        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (current.tapEnabled && InteractableViewHaptics.hasToggleLikeContentChange(ev)) {
                InteractableViewHaptics.handle(engine, current, ev)
            }
            return
        }

        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            InteractableViewHaptics.handle(engine, current, ev)
            return
        }

        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            var consumedByEdge = false
            if (current.a11yScrollBoundEdge) {
                if (ScrollAbsoluteEdgeVibration.onViewScrolled(ev) ==
                    ScrollAbsoluteEdgeVibration.Result.PlayEdgeHaptic
                ) {
                    engine.play(current.edgePattern, current.edgeIntensity, throttleMs = EDGE_THROTTLE_MS)
                    consumedByEdge = true
                }
            }
            if (current.scrollEnabled && !consumedByEdge) {
                when (val scroll = ScrollContentVibration.onViewScrolled(ev, current)) {
                    is ScrollContentVibration.Decision.Play -> {
                        val count = scroll.count
                        if (count <= 1) {
                            engine.play(
                                current.scrollPattern,
                                scroll.intensity,
                                throttleMs = 0L,
                            )
                        } else {
                            scope.launch {
                                repeat(count) { i ->
                                    if (i > 0) delay(45L)
                                    engine.play(
                                        current.scrollPattern,
                                        scroll.intensity,
                                        throttleMs = 0L,
                                    )
                                }
                            }
                        }
                    }
                    ScrollContentVibration.Decision.None -> Unit
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        settingsJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun applyEventMask(settings: HapticsSettings) {
        val info = serviceInfo ?: return
        var mask = InteractableViewHaptics.eventTypeMask(settings)
        if (settings.scrollEnabled || settings.a11yScrollBoundEdge) {
            mask = mask or AccessibilityEvent.TYPE_VIEW_SCROLLED
        }
        if (mask == 0) mask = AccessibilityEvent.TYPE_VIEW_CLICKED
        if (info.eventTypes == mask) return
        info.eventTypes = mask
        serviceInfo = info
    }

    private companion object {
        const val EDGE_THROTTLE_MS = 200L
    }
}
