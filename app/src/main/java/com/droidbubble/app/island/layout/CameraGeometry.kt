package com.droidbubble.app.island.layout

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.hypot

/** What kind of camera cutout the screen has (V1.1). */
enum class CutoutKind { NONE, PUNCH_HOLE, WATERDROP, WIDE_NOTCH, UNKNOWN }

/** Where the camera sits along its edge, in screen terms (the consumer mirrors START/END for RTL). */
enum class CameraSide { CENTER, START, END }

/** The screen edge a cutout belongs to, in the current rotation. */
enum class CutoutEdge { TOP, LEFT, RIGHT }

/**
 * The hardware camera, measured from the display cutout (V1.1), in dp and in the current
 * rotation. [CameraGeometry.None] means no usable cutout: the island keeps its manual placement.
 */
@Immutable
data class CameraGeometry(
    val kind: CutoutKind,
    val side: CameraSide,
    /** Centre of the lens from the screen's left edge. */
    val centerXDp: Float,
    /** Centre of the lens from the screen's top edge. */
    val centerYDp: Float,
    /** Lens diameter (the cutout's extent along its edge); 0 when [CutoutKind.NONE]. */
    val diameterDp: Float,
    /** `Surface.ROTATION_*` the numbers were measured in. */
    val rotation: Int,
    val edge: CutoutEdge = CutoutEdge.TOP,
) {
    val isUsable: Boolean get() = kind == CutoutKind.PUNCH_HOLE || kind == CutoutKind.WATERDROP

    companion object {
        val None = CameraGeometry(CutoutKind.NONE, CameraSide.CENTER, 0f, 0f, 0f, 0)
    }
}

/** A cutout rectangle in px (current rotation). Plain floats, so the maths runs on the JVM. */
data class CutoutRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Turns cutout rectangles into a [CameraGeometry] (V1.1). Pure: the Android side
 * (`service/CutoutReader`) only reads the rects.
 *
 * Many phones report a punch hole's rect stretched to the screen edge, so the size is taken
 * *along* the edge (width for a top cutout, height for a side one) and the centre is placed one
 * radius in from the far side, not at the rect's middle.
 */
object CutoutCalibration {
    /** Wider than this along its edge, a cutout is a notch, not a camera hole. */
    const val MAX_HOLE_DP = 48f
    /** A rect within this of an edge touches it (the stretched-to-edge case). */
    private const val EDGE_TOUCH_DP = 1f
    /** A camera this close to the middle of its edge counts as centred (fraction of the edge). */
    private const val CENTER_BAND = 0.12f

    /**
     * @param rects the bounding rects (one per cutout), px
     * @param pathBounds the bounds of the cutout path (API 31+), px; preferred when present,
     *   because it hugs the hole instead of an OEM's stretched rect. Only used with one cutout.
     */
    fun fromRects(
        rects: List<CutoutRect>,
        pathBounds: CutoutRect?,
        screenWpx: Int,
        screenHpx: Int,
        density: Float,
        rotation: Int,
    ): CameraGeometry {
        if (rects.isEmpty() && pathBounds == null) return CameraGeometry.None
        val w = screenWpx / density
        val dpRects = rects.map { it.toDp(density) }
        // Several cutouts: the one nearest the top centre is the camera the island hugs
        val chosen = dpRects.minByOrNull { hypot(it.centerX - w / 2f, it.centerY) }
        val source = if (pathBounds != null && dpRects.size <= 1) pathBounds.toDp(density) else chosen!!

        val edge = when {
            source.top <= EDGE_TOUCH_DP || source.top <= minOf(source.left, w - source.right) -> CutoutEdge.TOP
            source.left <= w - source.right -> CutoutEdge.LEFT
            else -> CutoutEdge.RIGHT
        }
        // Along the edge, the extent is the lens; across it, only the far side is trustworthy
        val along = if (edge == CutoutEdge.TOP) source.width else source.height
        val across = if (edge == CutoutEdge.TOP) source.height else source.width
        val touches = when (edge) {
            CutoutEdge.TOP -> source.top <= EDGE_TOUCH_DP
            CutoutEdge.LEFT -> source.left <= EDGE_TOUCH_DP
            CutoutEdge.RIGHT -> source.right >= w - EDGE_TOUCH_DP
        }
        val diameter = along
        val (cx, cy) = when (edge) {
            CutoutEdge.TOP -> source.centerX to (source.bottom - diameter / 2f)
            CutoutEdge.LEFT -> (source.right - diameter / 2f) to source.centerY
            CutoutEdge.RIGHT -> (source.left + diameter / 2f) to source.centerY
        }
        val aspect = if (across > 0f) along / across else Float.POSITIVE_INFINITY
        val kind = when {
            along > MAX_HOLE_DP -> CutoutKind.WIDE_NOTCH
            aspect in 0.75f..1.35f -> CutoutKind.PUNCH_HOLE
            // Stretched to the edge: deeper than wide, but the lens is the square at the far side
            touches && aspect < 0.75f -> CutoutKind.PUNCH_HOLE
            touches && aspect > 1.35f && aspect <= 2.2f -> CutoutKind.WATERDROP
            else -> CutoutKind.UNKNOWN
        }
        val side = when (edge) {
            CutoutEdge.TOP -> when {
                abs(cx - w / 2f) <= CENTER_BAND * w -> CameraSide.CENTER
                cx < w / 2f -> CameraSide.START
                else -> CameraSide.END
            }
            CutoutEdge.LEFT -> CameraSide.START
            CutoutEdge.RIGHT -> CameraSide.END
        }
        return CameraGeometry(kind, side, cx, cy, diameter, rotation, edge)
    }

    private fun CutoutRect.toDp(density: Float) = CutoutRect(left / density, top / density, right / density, bottom / density)
}
