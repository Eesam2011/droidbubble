package com.droidbubble.app.island.layout

import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V1.2: effective placement and the one-time calibration migration. Portrait is 360 dp wide. */
class EffectiveDimensionsTest {

    private fun cam(kind: CutoutKind, cx: Float, cy: Float, d: Float, edge: CutoutEdge = CutoutEdge.TOP) =
        CameraGeometry(kind, CameraSide.CENTER, cx, cy, d, 0, edge, screenWidthDp = 360f)

    private val centreHole = cam(CutoutKind.PUNCH_HOLE, 180f, 24f, 24f)
    private val sideHole = cam(CutoutKind.PUNCH_HOLE, 42f, 22f, 24f)
    private val bigHole = cam(CutoutKind.PUNCH_HOLE, 180f, 30f, 30f)
    private val waterdrop = cam(CutoutKind.WATERDROP, 180f, 4f, 40f)
    private val calibrated = IslandSettings(calibrationMigrated = true)

    private fun assertDims(e: EffectiveDimensions, top: Float, x: Float, h: Float, zone: Float) {
        assertEquals("top", top, e.topDp, 1e-4f)
        assertEquals("horizontal", x, e.horizontalDp, 1e-4f)
        assertEquals("height", h, e.heightDp, 1e-4f)
        assertEquals("camera zone", zone, e.cameraZoneDp, 1e-4f)
    }

    // ---- effective() ----

    @Test fun noCutout_isTodaysAbsolutePlacement() =
        assertDims(calibrated.effective(CameraGeometry.None), 12f, 0f, 36f, 36f)

    @Test fun centreHole_centresOnTheLens() =
        assertDims(calibrated.effective(centreHole), 6f, 0f, 36f, 36f)

    @Test fun sideHole_movesUnderTheLens() =
        assertDims(calibrated.effective(sideHole), 4f, -138f, 36f, 36f)

    @Test fun biggerLens_growsThePillAndTheGap() =
        assertDims(calibrated.effective(bigHole), 9f, 0f, 42f, 42f)

    @Test fun waterdrop_tighterGap_topClampedAtZero() =
        assertDims(calibrated.effective(waterdrop), 0f, 0f, 52f, 48f)

    @Test fun deltasAtTheirRangeLimits() {
        val s = calibrated.copy(topOffsetDeltaDp = 12f, horizontalOffsetDeltaDp = -24f, heightDeltaDp = 8f)
        // height 44: centred top 2, plus 12
        assertDims(s.effective(centreHole), 14f, -24f, 44f, 36f)
    }

    @Test fun manualMode_ignoresTheCamera() =
        assertDims(calibrated.copy(autoCalibrate = false).effective(centreHole), 12f, 0f, 36f, 36f)

    @Test fun beforeMigration_nothingMoves() =
        assertDims(IslandSettings().effective(centreHole), 12f, 0f, 36f, 36f)

    @Test fun sideEdgeCamera_waitsForLandscape() =
        assertDims(calibrated.effective(cam(CutoutKind.PUNCH_HOLE, 24f, 180f, 24f, CutoutEdge.LEFT)), 12f, 0f, 36f, 36f)

    // ---- migration ----

    @Test fun freshInstall_centresOnTheCamera() {
        val m = Calibration.migrate(IslandSettings(), centreHole, hasStoredDims = false)
        assertTrue(m.autoCalibrate); assertTrue(m.calibrationMigrated)
        assertEquals(0f, m.topOffsetDeltaDp); assertEquals(0f, m.horizontalOffsetDeltaDp); assertEquals(0f, m.heightDeltaDp)
        assertDims(m.effective(centreHole), 6f, 0f, 36f, 36f)
    }

    @Test fun upgrade_withDefaultDims_staysExactlyWhereItWas() {
        val before = IslandSettings()
        for (c in listOf(centreHole, bigHole, cam(CutoutKind.PUNCH_HOLE, 181.33f, 23.67f, 25.33f))) {
            val m = Calibration.migrate(before, c, hasStoredDims = true)
            assertTrue(m.autoCalibrate)
            val e = m.effective(c)
            assertEquals(12f, e.topDp, 1e-4f); assertEquals(0f, e.horizontalDp, 1e-4f); assertEquals(36f, e.heightDp, 1e-4f)
            // the absolute fields are kept for manual mode and rollback
            assertEquals(before.topOffsetDp, m.topOffsetDp); assertEquals(before.heightDp, m.heightDp)
        }
    }

    @Test fun upgrade_withExtremeDims_keepsManualMode() {
        val before = IslandSettings(topOffsetDp = 60, heightDp = 56, horizontalOffsetDp = -50)
        val m = Calibration.migrate(before, centreHole, hasStoredDims = true)
        assertFalse(m.autoCalibrate); assertTrue(m.calibrationMigrated)
        assertDims(m.effective(centreHole), 60f, -50f, 56f, 36f)
    }

    @Test fun migration_isIdempotent() {
        val once = Calibration.migrate(IslandSettings(topOffsetDp = 14), centreHole, hasStoredDims = true)
        assertEquals(once, Calibration.migrate(once, centreHole, hasStoredDims = true))
        assertEquals(once, Calibration.migrate(once, bigHole, hasStoredDims = false))
    }

    @Test fun noUsableCamera_doesNotMigrate() {
        val s = IslandSettings()
        assertEquals(s, Calibration.migrate(s, CameraGeometry.None, hasStoredDims = true))
        assertEquals(s, Calibration.migrate(s, cam(CutoutKind.WIDE_NOTCH, 180f, 15f, 200f), hasStoredDims = false))
    }
}
