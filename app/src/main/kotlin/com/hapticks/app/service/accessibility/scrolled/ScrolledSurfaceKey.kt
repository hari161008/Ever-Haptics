package com.hapticks.app.service.accessibility.scrolled

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

internal fun scrolledSurfaceKey(event: AccessibilityEvent): String? {
    val source: AccessibilityNodeInfo? = event.source
    return if (source != null) {
        try {
            val viewId = source.viewIdResourceName
            "w${event.windowId}\u001f${event.className}\u001f$viewId"
        } finally {
            source.recycle()
        }
    } else {
        event.className?.toString()?.let { cn -> "w${event.windowId}\u001f$cn" }
    }
}
