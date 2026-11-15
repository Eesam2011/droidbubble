package com.droidbubble.app.ui.main

import com.droidbubble.app.island.layout.CameraGeometry
import com.droidbubble.app.island.layout.CameraSide
import com.droidbubble.app.island.layout.CutoutEdge
import com.droidbubble.app.island.layout.CutoutKind
import com.droidbubble.app.island.layout.effective
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.ui.main.DimensionBindings.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V1.4: the Dimensions sliders write deltas with "Fit to camera" on, absolute values off. */
class DimensionBindingsTest {

    private val cam = CameraGeometry(CutoutKind.PUNCH_HOLE, CameraSide.CENTER, 181.3f, 23.6f, 25.3f, 0, CutoutEdge.TOP, 360f)
    private val calibrated = IslandSettings(calibrationMigrated = true, topOffsetDeltaDp = 2f)
    private val manual = IslandSettings(autoCalibrate = false, calibrationMigrated = true)

    @Test
    fun calibrating_writesTheDeltaFields() {
        val s = DimensionBindings.apply(Field.TOP, calibrated, cam, 3.4f)
        assertEquals(3f, s.topOffsetDeltaDp)
        assertEquals(calibrated.topOffsetDp, s.topOffsetDp)
        assertEquals(-5f, DimensionBindings.apply(Field.HORIZONTAL, calibrated, cam, -5f).horizontalOffsetDeltaDp)
        assertEquals(4f, DimensionBindings.apply(Field.HEIGHT, calibrated, cam, 4f).heightDeltaDp)
    }

    @Test
    fun manual_writesTheAbsoluteFields() {
        val s = DimensionBindings.apply(Field.TOP, manual, cam, 20f)
        assertEquals(20, s.topOffsetDp)
        assertEquals(manual.topOffsetDeltaDp, s.topOffsetDeltaDp)
        assertEquals(-30, DimensionBindings.apply(Field.HORIZONTAL, manual, cam, -30f).horizontalOffsetDp)
    }

    @Test
    fun noCamera_alwaysAbsolute() {
        val s = DimensionBindings.apply(Field.HEIGHT, calibrated, CameraGeometry.None, 40f)
        assertEquals(40, s.heightDp)
        assertEquals("Height", DimensionBindings.slider(Field.HEIGHT, calibrated, CameraGeometry.None).label)
    }

    @Test
    fun labelsAndRangesMatchTheMode() {
        val top = DimensionBindings.slider(Field.TOP, calibrated, cam)
        assertEquals("Vertical fine-tune", top.label); assertEquals(-12f..12f, top.range); assertTrue(top.signed)
        assertEquals(-24f..24f, DimensionBindings.slider(Field.HORIZONTAL, calibrated, cam).range)
        assertEquals(-8f..8f, DimensionBindings.slider(Field.HEIGHT, calibrated, cam).range)
        val abs = DimensionBindings.slider(Field.TOP, manual, cam)
        assertEquals("Top Offset (Notch Line-up)", abs.label); assertEquals(0f..60f, abs.range); assertFalse(abs.signed)
    }

    @Test
    fun togglingNeverMovesThePill() {
        for (start in listOf(manual, manual.copy(topOffsetDp = 30, heightDp = 50, horizontalOffsetDp = -40), calibrated, calibrated.copy(heightDeltaDp = -3f))) {
            val before = start.effective(cam)
            val flipped = DimensionBindings.setFitToCamera(start, !start.autoCalibrate, cam)
            val after = flipped.effective(cam)
            // turning on is exact; turning off rounds to whole dp
            assertEquals(before.topDp, after.topDp, 0.5f)
            assertEquals(before.horizontalDp, after.horizontalDp, 0.5f)
            assertEquals(before.heightDp, after.heightDp, 0.5f)
            assertEquals(!start.autoCalibrate, flipped.autoCalibrate)
        }
    }

    @Test
    fun valueTexts() {
        assertEquals("+2 dp", DimensionBindings.valueText(2f, true))
        assertEquals("−3 dp", DimensionBindings.valueText(-3f, true))
        assertEquals("0 dp", DimensionBindings.valueText(0.2f, true))
        assertEquals("36 dp", DimensionBindings.valueText(36f, false))
        assertEquals("plus 2 dp", DimensionBindings.spokenValue(2f, true))
        assertEquals("minus 3 dp", DimensionBindings.spokenValue(-3f, true))
    }

    @Test
    fun detectedText() {
        assertEquals("Punch-hole camera, 25 dp, centre", DimensionBindings.detectedText(cam))
        assertEquals("No camera cutout found, so manual position is used", DimensionBindings.detectedText(CameraGeometry.None))
    }
}
