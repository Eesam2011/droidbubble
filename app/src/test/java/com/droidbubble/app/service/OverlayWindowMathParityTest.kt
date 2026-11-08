package com.droidbubble.app.service

import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hand-computed rows that pin the window math moved out of IslandOverlayService (S3.1).
 * Defaults: width 180, height 36, top offset 12, no horizontal offset.
 */
class OverlayWindowMathParityTest {

    private val defaults = IslandSettings()

    @Test
    fun idle_isGone() {
        assertEquals(WindowSpec.Gone, compactWindowSpec(WindowInputs(defaults)))
        assertEquals(WindowSpec.Gone, initialWindowSpec(WindowInputs(defaults)))
    }

    @Test
    fun media_isPillPlus80() {
        // 180 + 80 wide; 12 + 36 + 12 tall
        assertEquals(
            WindowSpec.Compact(widthDp = 260f, heightDp = 60f, touchable = true),
            compactWindowSpec(WindowInputs(defaults, media = true))
        )
    }

    @Test
    fun dual_mediaAndProgress() {
        // rightEdge = 90 + 8 + 36 + 40 = 174 -> 348 (more than 180 + 80)
        assertEquals(
            WindowSpec.Compact(widthDp = 348f, heightDp = 60f, touchable = true),
            compactWindowSpec(WindowInputs(defaults, media = true, progress = true))
        )
    }

    @Test
    fun charging_isChargingPillPlus80() {
        // max(180 + 20, 140) + 80
        assertEquals(
            WindowSpec.Compact(widthDp = 280f, heightDp = 60f, touchable = true),
            compactWindowSpec(WindowInputs(defaults, charging = true))
        )
    }

    @Test
    fun expanded_isFullScreen() {
        assertEquals(WindowSpec.FullScreen, compactWindowSpec(WindowInputs(defaults, expanded = true, media = true)))
    }

    @Test
    fun initial_media_usesItsOwnWidthFormula() {
        // mainHalf = max(162, 124) / 2 = 81; dualRightEdge = 81 + 8 + 36 + 44 = 169
        // chargingEdge = (200 + 80) / 2 = 140; max(169, 140) * 2 = 338 (more than 180 + 100)
        assertEquals(
            WindowSpec.Compact(widthDp = 338f, heightDp = 60f, touchable = true),
            initialWindowSpec(WindowInputs(defaults, media = true))
        )
    }
}
