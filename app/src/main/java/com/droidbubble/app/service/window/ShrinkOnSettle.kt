package com.droidbubble.app.service.window

import android.view.WindowManager
import com.droidbubble.app.island.SettledShape
import com.droidbubble.app.island.scene.IslandMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * F5.3: the window grows at once but shrinks only once the island has settled, so a shape that is
 * still animating is never clipped (collapse, hide, minimise, a glance ending, becoming idle).
 */
object ShrinkOnSettle {
    /** The window never stays large longer than this after a shrink was asked for. */
    const val TIMEOUT_MS = 1_500L

    /** True when [to] fits inside [from] and differs: applying it now would clip a shape still drawn in the old space. */
    fun isShrink(from: WindowState, to: WindowState): Boolean {
        if (from.width == to.width && from.height == to.height) return false
        return extent(to.width) <= extent(from.width) && extent(to.height) <= extent(from.height)
    }

    /**
     * Waits until the island reports it has settled in [expect], or [timeoutMs]. Returns false on
     * timeout (the caller shrinks anyway: a stuck animation must not keep a large window).
     */
    suspend fun awaitSettled(settled: StateFlow<SettledShape?>, expect: IslandMode, timeoutMs: Long = TIMEOUT_MS): Boolean =
        withTimeoutOrNull(timeoutMs) { settled.first { it != null && it.mode == expect } } != null

    private fun extent(size: Int) = if (size == WindowManager.LayoutParams.MATCH_PARENT) Int.MAX_VALUE else size
}
