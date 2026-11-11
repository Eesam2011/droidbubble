package com.droidbubble.app.service

import android.view.WindowManager
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandMode
import com.droidbubble.app.island.scene.IslandScene
import com.droidbubble.app.island.scene.IslandTargets
import com.droidbubble.app.island.scene.MeasuredHeights
import com.droidbubble.app.island.scene.bubbleRestCenterX
import com.droidbubble.app.island.scene.deriveIslandTargets
import com.droidbubble.app.model.IslandSettings
import kotlin.math.abs

/** What the overlay window should be. The service turns it into pixels and window flags. */
sealed interface WindowSpec {
    /** 1×1 and not touchable. */
    data object Gone : WindowSpec

    /** MATCH_PARENT × MATCH_PARENT and touchable, so a tap outside the card collapses it. */
    data object FullScreen : WindowSpec

    /** A strip at the top of the screen. [widthDp] null means MATCH_PARENT (left cutout). */
    data class Compact(val widthDp: Float?, val heightDp: Float, val touchable: Boolean) : WindowSpec
}

/** A [WindowSpec] in window pixels. Width and height may be `MATCH_PARENT`. */
data class WindowPx(val width: Int, val height: Int, val touchable: Boolean)

fun WindowSpec.toWindowPx(density: Float): WindowPx = when (this) {
    WindowSpec.Gone -> WindowPx(1, 1, touchable = false)
    WindowSpec.FullScreen -> WindowPx(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = true)
    is WindowSpec.Compact -> WindowPx(
        width = widthDp?.let { (it * density).toInt() } ?: WindowManager.LayoutParams.MATCH_PARENT,
        height = (heightDp * density).toInt(),
        touchable = touchable,
    )
}

/**
 * The island's targets as the window needs them. The compact strip depends on no measured text,
 * so the card heights are left at 0; the bubble counts as emerged, so the strip has room for it
 * from the first frame.
 */
fun windowTargets(scene: IslandScene, settings: IslandSettings, env: IslandEnv): IslandTargets =
    deriveIslandTargets(scene, settings, env, MeasuredHeights(0.dp), card2Expanded = false, bubbleEmerged = true)

/**
 * The window for a scene (F1.4). The scene is the one the view draws, so the window is touchable
 * exactly when something touchable is drawn: an app's own card hidden while the app is in front,
 * a notification its toggle hides, or a call with no card all leave the window out of the way.
 *
 * Only the "what size" decision lives here: when to apply it (the collapse latch, the delayed
 * hide) stays in the service.
 *
 * [settled] (B2.2): the island has finished animating. Settled, the window hugs what is drawn
 * ([SETTLED_MARGIN_DP] per side); while it morphs it keeps the wide envelope, so a spring
 * overshoot or a shrinking pill is never clipped.
 */
fun compactWindowSpec(scene: IslandScene, targets: IslandTargets, settings: IslandSettings, settled: Boolean): WindowSpec {
    if (scene.mode == IslandMode.OFF) return WindowSpec.Gone
    if (scene.isExpanded) return WindowSpec.FullScreen

    val shouldShowCompact = (scene.isOccupied || settings.alwaysShowPill) && !scene.userHidden
    if (!shouldShowCompact) return WindowSpec.Gone

    // B2.2: room beside the drawn pill. Settled: the camera brackets (about 7.6 dp out) and the
    // tap bounce (up to 1.04x). Morphing: the old flat margin, for spring overshoot.
    val side = if (settled) SETTLED_MARGIN_DP else MORPH_MARGIN_DP
    val pill = targets.compactWidth.value
    val height = settings.heightDp.toFloat()

    val totalWidthDp = when {
        scene.hasAlert -> pill + 2f * side
        scene.userMinimized -> pill + 2f * SETTLED_MARGIN_DP
        scene.isDual -> if (settled) {
            (bubbleRestCenterX(settings).value + height / 2f + SETTLED_MARGIN_DP) * 2f
        } else {
            val rightEdge = settings.widthDp / 2f + 8f + height + MORPH_MARGIN_DP
            (rightEdge * 2f).coerceAtLeast(settings.widthDp + 2f * MORPH_MARGIN_DP)
        }
        else -> pill + 2f * side
    }

    return WindowSpec.Compact(
        widthDp = if (settings.isLeftCutout) null else totalWidthDp + 2f * abs(settings.horizontalOffsetDp),
        // Settled: the tap bounce moves the pill down about 4 dp
        heightDp = (settings.topOffsetDp + settings.heightDp + if (settled) SETTLED_BOTTOM_DP else 12).toFloat(),
        touchable = scene.wantsTouch,
    )
}

/** B2.2: the settled window's room beside and below the drawn pill. */
const val SETTLED_MARGIN_DP = 12f
const val SETTLED_BOTTOM_DP = 6
private const val MORPH_MARGIN_DP = 40f

/**
 * B2.2: how long a shape change keeps the wide envelope: long enough for the split spring at the
 * lowest damping the settings allow, scaled by the island's animation speed.
 */
fun morphHoldMs(animationSpeed: Float): Long = kotlin.math.round(1200f / animationSpeed.coerceIn(0.3f, 3f)).toLong()
