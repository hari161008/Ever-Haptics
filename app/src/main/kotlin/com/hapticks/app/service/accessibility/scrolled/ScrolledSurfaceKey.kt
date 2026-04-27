package com.hapticks.app.service.accessibility.scrolled

import android.view.accessibility.AccessibilityEvent

internal fun scrolledSurfaceKey(event: AccessibilityEvent): String? {
    val source = event.source
    val viewId = source?.viewIdResourceName
    return if (viewId != null) {
        "w${event.windowId}\u001f${event.className}\u001f$viewId"
    } else {
        event.className?.toString()?.let { cn -> "w${event.windowId}\u001f$cn" }
    }
}

