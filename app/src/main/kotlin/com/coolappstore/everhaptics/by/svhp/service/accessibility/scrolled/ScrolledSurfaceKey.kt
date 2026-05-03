package com.coolappstore.everhaptics.by.svhp.service.accessibility.scrolled

import android.view.accessibility.AccessibilityEvent

internal fun scrolledSurfaceKey(event: AccessibilityEvent): String {
    val source = try { event.source } catch (_: Exception) { null }
    val viewId = try { source?.viewIdResourceName } catch (_: Exception) { null }
    val className = event.className?.toString()
    return when {
        viewId != null -> "w${event.windowId}\u001f${className}\u001f$viewId"
        className != null -> "w${event.windowId}\u001f$className"
        else -> "w${event.windowId}"
    }
}
