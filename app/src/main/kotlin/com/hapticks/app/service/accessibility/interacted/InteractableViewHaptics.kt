package com.hapticks.app.service.accessibility.interacted

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticEngine

object InteractableViewHaptics {

    private const val TOGGLE_COALESCE_THROTTLE_MS = 120L

    private val toggleContentChangeMask: Int
        get() = AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_CHECKED

    fun eventTypeMask(settings: HapticsSettings): Int {
        if (!settings.tapEnabled) return 0
        return AccessibilityEvent.TYPE_VIEW_CLICKED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    }

    fun hasToggleLikeContentChange(event: AccessibilityEvent): Boolean {
        val types = event.contentChangeTypes
        if (types == 0) return false
        return (types and toggleContentChangeMask) != 0
    }

    fun handle(engine: HapticEngine, settings: HapticsSettings, event: AccessibilityEvent): Boolean {
        if (!settings.tapEnabled) return true

        return when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                engine.play(settings.pattern, settings.intensity)
                true
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val changeTypes = event.contentChangeTypes
                if (changeTypes == 0) return true
                if (changeTypes and toggleContentChangeMask == 0) return true
                if (!isSwitchLikeToggleForWindowEvent(event, changeTypes)) return true
                engine.play(
                    settings.pattern,
                    settings.intensity,
                    throttleMs = TOGGLE_COALESCE_THROTTLE_MS,
                )
                true
            }
            else -> false
        }
    }

    private fun isSwitchLikeToggleForWindowEvent(event: AccessibilityEvent, changeTypes: Int): Boolean {
        if (isObviousSwitchClassName(event.className)) return true

        val hasChecked = (changeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_CHECKED) != 0
        val hasStateDescription = (changeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION) != 0
        if (!hasChecked && !hasStateDescription) return false

        val node = event.getSource() ?: return false
        try {
            if (isExcludedCheckable(node)) return false
            if (isObviousSwitchClassName(node.className)) return true
            if (!node.isCheckable) return false
            if (!isAmbiguousCheckableAsSwitch(node)) return false
            return hasChecked || hasStateDescription
        } finally {
            try {
                node.recycle()
            } catch (_: Exception) { }
        }
    }

    private fun isObviousSwitchClassName(className: CharSequence?): Boolean {
        val n = className?.toString() ?: return false
        if (n.contains("CheckBox", ignoreCase = true) || n.contains("Radio", ignoreCase = true)) return false
        if (n.contains("Switch", ignoreCase = true)) return true
        if (n.endsWith("ToggleButton", ignoreCase = true)) return true
        return false
    }

    private fun isAmbiguousCheckableAsSwitch(node: AccessibilityNodeInfo): Boolean {
        if (!node.isCheckable) return false
        val n = node.className?.toString() ?: return false
        if (n == "android.view.View") return true
        if (n.contains("compose", ignoreCase = true) && n.contains("ui", ignoreCase = true) && n.contains("View", ignoreCase = true)) return true
        if (n.contains("CompoundButton", ignoreCase = true) && !n.contains("Check", ignoreCase = true)) return true
        return false
    }

    private fun isExcludedCheckable(node: AccessibilityNodeInfo): Boolean {
        val n = node.className?.toString() ?: return false
        if (n.contains("CheckBox", ignoreCase = true) || n.contains("Radio", ignoreCase = true)) return true
        if (n.contains("Chip", ignoreCase = true)) return true
        if (n.contains("Rating", ignoreCase = true)) return true
        return false
    }
}
