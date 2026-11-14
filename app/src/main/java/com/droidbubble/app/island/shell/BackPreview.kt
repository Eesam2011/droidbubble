package com.droidbubble.app.island.shell

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.motion.LocalIslandMotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * M4.5: Back collapses the expanded island (05 R7, §5; iOS has no back gesture, this is
 * Droidbubble's own). The overlay window (service/window/OverlayBack.kt) reports the system's Back
 * here; on API 34+ the predictive-back progress previews a small shrink toward the pill first.
 */

/** Where a system Back gesture is. */
sealed interface BackPhase {
    data object Idle : BackPhase
    /** A predictive back swipe in flight, [progress] 0 to 1. */
    data class Previewing(val progress: Float) : BackPhase
    /** The swipe was abandoned: spring back. */
    data object Cancelled : BackPhase
}

/** The bridge from the overlay window's Back callbacks to the shell. */
object IslandBackGesture {
    private val _phase = MutableStateFlow<BackPhase>(BackPhase.Idle)
    val phase: StateFlow<BackPhase> = _phase.asStateFlow()

    /** Set by the shell while it is composed: hands the gesture's speed to the collapse. */
    internal var beforeCollapse: (() -> Unit)? = null

    fun onStarted() {
        _phase.value = BackPhase.Previewing(0f)
    }

    fun onProgressed(progress: Float) {
        _phase.value = BackPhase.Previewing(progress.coerceIn(0f, 1f))
    }

    fun onCancelled() {
        _phase.value = BackPhase.Cancelled
    }

    /** Back committed (the predictive gesture, the platform callback or the key): collapse. */
    fun onInvoked() {
        if (IslandStateManager.isExpanded.value) {
            beforeCollapse?.invoke()
            IslandStateManager.collapse()
        }
        _phase.value = BackPhase.Idle
    }

    @VisibleForTesting
    fun resetForTest() {
        _phase.value = BackPhase.Idle
        beforeCollapse = null
    }
}

/** At full predictive-back progress the body has moved this fraction of the way to the pill. */
const val BACK_PREVIEW_SHRINK = 0.10f

/** And its corner has grown by this much, so the card reads as about to fold away. */
val BackPreviewCornerGrowth = 8.dp

/** Pure: the body at predictive-back [progress], from [open] toward [closed]. */
fun backPreviewGeometry(progress: Float, closed: IslandGeometry, open: IslandGeometry): IslandGeometry {
    val p = progress.coerceIn(0f, 1f)
    val f = BACK_PREVIEW_SHRINK * p
    return IslandGeometry(
        width = open.width + (closed.width - open.width) * f,
        height = open.height + (closed.height - open.height) * f,
        corner = open.corner + BackPreviewCornerGrowth * p,
        top = open.top + (closed.top - open.top) * f,
    )
}

/** Pure: the body's velocity for a gesture moving at [progressPerSec], along the preview path. */
fun backPreviewVelocity(progressPerSec: Float, closed: IslandGeometry, open: IslandGeometry): IslandGeometry {
    val f = BACK_PREVIEW_SHRINK * progressPerSec.coerceAtLeast(0f)
    return IslandGeometry(
        width = (closed.width - open.width) * f,
        height = (closed.height - open.height) * f,
        corner = BackPreviewCornerGrowth * progressPerSec.coerceAtLeast(0f),
        top = (closed.top - open.top) * f,
    )
}

/** How fast the predictive-back progress is moving, from its last two samples. */
internal class BackSpeed {
    private var lastProgress = 0f
    private var lastNanos = 0L
    var progressPerSec = 0f
        private set

    fun add(progress: Float, nanos: Long) {
        if (lastNanos != 0L && nanos > lastNanos) {
            progressPerSec = ((progress - lastProgress) / ((nanos - lastNanos) / 1e9f)).coerceAtLeast(0f)
        }
        lastProgress = progress
        lastNanos = nanos
    }

    fun reset() {
        lastProgress = 0f
        lastNanos = 0L
        progressPerSec = 0f
    }
}

/**
 * The shell's side of Back (M4.5): previews the shrink while a predictive back swipe is in flight,
 * springs back when it is cancelled, and on commit hands the swipe's speed to the collapse.
 *
 * @param open the body's current target (the open card)
 */
@Composable
internal fun BackPreview(shell: IslandShellState, isExpanded: Boolean, closed: IslandGeometry, open: IslandGeometry) {
    val motion = LocalIslandMotion.current
    val latestExpanded by rememberUpdatedState(isExpanded)
    val latestClosed by rememberUpdatedState(closed)
    val latestOpen by rememberUpdatedState(open)
    val latestSpring by rememberUpdatedState(motion.spring(motion.interactive, IslandGeometry.VisibilityThreshold))
    val speed = remember { BackSpeed() }

    LaunchedEffect(shell) {
        IslandBackGesture.phase.collect { phase ->
            when (phase) {
                is BackPhase.Previewing -> if (latestExpanded) {
                    speed.add(phase.progress, System.nanoTime())
                    shell.previewBack(phase.progress, latestClosed, latestOpen)
                }
                BackPhase.Cancelled -> {
                    speed.reset()
                    if (latestExpanded) {
                        launch { shell.release(latestOpen, latestSpring, IslandGeometry(0.dp, 0.dp, 0.dp, 0.dp)) }
                    }
                }
                BackPhase.Idle -> speed.reset()
            }
        }
    }
    DisposableEffect(shell) {
        IslandBackGesture.beforeCollapse = {
            val v = speed.progressPerSec
            if (v > 0f) shell.handOff(backPreviewVelocity(v, latestClosed, latestOpen))
        }
        onDispose { IslandBackGesture.beforeCollapse = null }
    }
}
