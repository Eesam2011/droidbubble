package com.droidbubble.app.island.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V1.1: cutout rects (px, density 3) to camera geometry (dp). Portrait is 360 × 800 dp. */
class CutoutCalibrationTest {

    private val d = 3f
    private fun px(l: Float, t: Float, r: Float, b: Float) = CutoutRect(l * d, t * d, r * d, b * d)
    private fun portrait(vararg rects: CutoutRect, path: CutoutRect? = null) =
        CutoutCalibration.fromRects(rects.toList(), path, 1080, 2400, d, 0)
    private fun landscape(rotation: Int, vararg rects: CutoutRect) =
        CutoutCalibration.fromRects(rects.toList(), null, 2400, 1080, d, rotation)

    private fun assertCamera(g: CameraGeometry, kind: CutoutKind, side: CameraSide, cx: Float, cy: Float, dia: Float) {
        assertEquals("kind", kind, g.kind)
        assertEquals("side", side, g.side)
        assertEquals("centre x", cx, g.centerXDp, 0.01f)
        assertEquals("centre y", cy, g.centerYDp, 0.01f)
        assertEquals("diameter", dia, g.diameterDp, 0.01f)
    }

    @Test fun centreHole_rectStretchedToTheTop() =
        // 24 dp lens whose rect an OEM extended up to y = 0: trust the width and the bottom edge
        assertCamera(portrait(px(168f, 0f, 192f, 36f)), CutoutKind.PUNCH_HOLE, CameraSide.CENTER, 180f, 24f, 24f)

    @Test fun centreHole_exactRect() =
        assertCamera(portrait(px(168f, 12f, 192f, 36f)), CutoutKind.PUNCH_HOLE, CameraSide.CENTER, 180f, 24f, 24f)

    @Test fun topLeftHole() =
        assertCamera(portrait(px(30f, 10f, 54f, 34f)), CutoutKind.PUNCH_HOLE, CameraSide.START, 42f, 22f, 24f)

    @Test fun topRightHole() =
        assertCamera(portrait(px(306f, 10f, 330f, 34f)), CutoutKind.PUNCH_HOLE, CameraSide.END, 318f, 22f, 24f)

    @Test fun waterdrop() =
        assertCamera(portrait(px(160f, 0f, 200f, 24f)), CutoutKind.WATERDROP, CameraSide.CENTER, 180f, 4f, 40f)

    @Test fun wideNotch() {
        val g = portrait(px(80f, 0f, 280f, 30f))
        assertEquals(CutoutKind.WIDE_NOTCH, g.kind)
        assertFalse(g.isUsable)
    }

    @Test fun noCutout() = assertEquals(CameraGeometry.None, portrait())

    @Test fun twoCutouts_theOneNearestTopCentreWins() =
        assertCamera(portrait(px(320f, 760f, 344f, 784f), px(168f, 12f, 192f, 36f)), CutoutKind.PUNCH_HOLE, CameraSide.CENTER, 180f, 24f, 24f)

    @Test fun pathBoundsBeatAStretchedRect() =
        assertCamera(portrait(px(150f, 0f, 210f, 40f), path = px(168f, 12f, 192f, 36f)), CutoutKind.PUNCH_HOLE, CameraSide.CENTER, 180f, 24f, 24f)

    @Test fun rotation90_holeOnTheLeftEdge() {
        // 800 × 360 dp; the rect is stretched to x = 0, so trust its height and right edge
        val g = landscape(1, px(0f, 168f, 36f, 192f))
        assertCamera(g, CutoutKind.PUNCH_HOLE, CameraSide.START, 24f, 180f, 24f)
        assertEquals(CutoutEdge.LEFT, g.edge)
        assertEquals(1, g.rotation)
    }

    @Test fun rotation270_holeOnTheRightEdge() {
        val g = landscape(3, px(764f, 168f, 800f, 192f))
        assertCamera(g, CutoutKind.PUNCH_HOLE, CameraSide.END, 776f, 180f, 24f)
        assertEquals(CutoutEdge.RIGHT, g.edge)
    }

    @Test fun rotation90_exactRectOnTheLeftEdge() {
        val g = landscape(1, px(12f, 168f, 36f, 192f))
        assertCamera(g, CutoutKind.PUNCH_HOLE, CameraSide.START, 24f, 180f, 24f)
        assertTrue(g.isUsable)
    }
}
