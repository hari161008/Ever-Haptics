package com.hapticks.app.service.accessibility

import android.view.accessibility.AccessibilityEvent
private const val HAPTICKS_APPLICATION_ID = "com.hapticks.app"

/**
 * [com.hapticks.app.service.HapticsAccessibilityService] receives events from all apps, including Hapticks itself.
 * Non-scroll events from our package are ignored so accessibility-driven tap/toggle haptics do not
 * double with Compose; [android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED] is still
 * handled when scroll haptics are enabled so lists in Hapticks can use the same path as other apps.
 */
fun isAccessibilityEventFromOwnApplication(event: AccessibilityEvent): Boolean =
    event.packageName?.toString() == HAPTICKS_APPLICATION_ID
