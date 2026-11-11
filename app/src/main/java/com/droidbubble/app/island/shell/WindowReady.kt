package com.droidbubble.app.island.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * F5.2: may the island animate into a bigger shape yet? The overlay window has to grow first
 * (a window manager relayout takes 28 to 80 ms), or the first frames are clipped. So a shape
 * waits until the window really [fits] it, or [timeoutMs] at most, so a device that never
 * resizes can't block it.
 *
 * Pure, for tests: [want] the bigger shape is asked for, [fits] the window already holds it,
 * [waitedMs] since it was asked for.
 */
fun isWindowReady(want: Boolean, fits: Boolean, waitedMs: Long, timeoutMs: Long = WINDOW_READY_TIMEOUT_MS): Boolean =
    !want || fits || waitedMs >= timeoutMs

const val WINDOW_READY_TIMEOUT_MS = 100L

/** [isWindowReady] as composition state: true once the window fits, or after the timeout. */
@Composable
fun rememberWindowReady(want: Boolean, fits: Boolean, timeoutMs: Long = WINDOW_READY_TIMEOUT_MS): Boolean {
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(want, fits) {
        timedOut = false
        if (want && !fits) {
            delay(timeoutMs)
            timedOut = true
        }
    }
    return isWindowReady(want, fits, waitedMs = if (timedOut) timeoutMs else 0L, timeoutMs = timeoutMs)
}
