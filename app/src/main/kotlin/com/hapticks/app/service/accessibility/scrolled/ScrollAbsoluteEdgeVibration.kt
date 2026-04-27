package com.hapticks.app.service.accessibility.scrolled

import android.view.accessibility.AccessibilityEvent
import java.util.LinkedHashMap

internal object ScrollAbsoluteEdgeVibration {
    private const val MAX_TRACKED_SURFACES = 128
    private val perSurface = object : LinkedHashMap<String, AbsoluteEdgeState>(
        16, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AbsoluteEdgeState>) =
            size > MAX_TRACKED_SURFACES
    }

    fun onViewScrolled(event: AccessibilityEvent): Result {
        val my = event.maxScrollY
        val key = scrolledSurfaceKey(event) ?: return Result.NoHaptic

        if (my <= 0) {
            perSurface.remove(key)
            return Result.NoHaptic
        }

        val y = event.scrollY.coerceIn(0, my)
        val snap = AbsoluteEdgeSnapshot(scrollY = y, maxScrollY = my)
        val old = perSurface[key] ?: AbsoluteEdgeState()
        val (newState, action) = advanceAbsoluteEdge(old, snap)
        perSurface[key] = newState

        return when (action) {
            AbsoluteEdgeAction.REACHED_TOP,
            AbsoluteEdgeAction.REACHED_BOTTOM -> Result.PlayEdgeHaptic
            null -> Result.NoHaptic
        }
    }

    enum class Result { PlayEdgeHaptic, NoHaptic }
}

private enum class AbsoluteEdgeAction { REACHED_TOP, REACHED_BOTTOM }
private data class AbsoluteEdgeState(val lastScrollY: Int? = null)
private data class AbsoluteEdgeSnapshot(val scrollY: Int, val maxScrollY: Int)
private fun advanceAbsoluteEdge(
    state: AbsoluteEdgeState,
    snap: AbsoluteEdgeSnapshot,
): Pair<AbsoluteEdgeState, AbsoluteEdgeAction?> {
    val my = snap.maxScrollY
    if (my <= 0) return AbsoluteEdgeState(lastScrollY = snap.scrollY.coerceAtLeast(0)) to null

    val y = snap.scrollY.coerceIn(0, my)
    val last = state.lastScrollY

    val action = if (last != null) {
        when {
            y == 0 && last > 0 -> AbsoluteEdgeAction.REACHED_TOP
            y == my && last < my -> AbsoluteEdgeAction.REACHED_BOTTOM
            else -> null
        }
    } else {
        null
    }

    return AbsoluteEdgeState(lastScrollY = y) to action
}
