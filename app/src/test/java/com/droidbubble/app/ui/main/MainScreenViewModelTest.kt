package com.droidbubble.app.ui.main

import com.droidbubble.app.model.Curve
import com.droidbubble.app.model.CurveMigration
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun islandSettings_bubbleEffectEnabledByDefault() {
        val settings = IslandSettings()
        assertTrue(settings.bubbleEffectEnabled)
    }

    @Test
    fun islandSettings_newTogglesDisabledByDefault() {
        val settings = IslandSettings()
        org.junit.Assert.assertFalse(settings.dismissMediaOnPause)
        org.junit.Assert.assertFalse(settings.alwaysShowPill)
    }

    @Test
    fun mediaInfo_computeCurrentPosition_pausedReturnsBasePosition() {
        val media = MediaInfo(
            packageName = "com.google.android.youtube",
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = false,
            durationMs = 180000L,
            positionMs = 45000L,
            lastPositionUpdateTimeMs = 100000L
        )
        assertEquals(45000L, media.computeCurrentPosition(nowMs = 120000L))
    }

    @Test
    fun mediaInfo_computeCurrentPosition_clampsToDuration() {
        val media = MediaInfo(
            packageName = "com.google.android.youtube",
            title = "Test Song",
            artist = "Test Artist",
            isPlaying = true,
            durationMs = 60000L,
            positionMs = 50000L,
            playbackSpeed = 1f,
            lastPositionUpdateTimeMs = 100000L
        )
        // Elapsed is 20s, 50s + 20s = 70s, but clamped to duration 60s
        assertEquals(60000L, media.computeCurrentPosition(nowMs = 120000L))
    }

    @Test
    fun curveEditor_writesTheDurationAndBounceFields() {
        // D5.2: each segment writes its own pair (D5.1's fields), clamped, and leaves the rest
        val base = IslandSettings()
        val c = Curve(0.45f, 0.12f)
        val expand = CurveTarget.EXPAND.withCurve(base, c)
        assertEquals(base.copy(expandDuration = 0.45f, expandBounce = 0.12f), expand)
        assertEquals(base.copy(collapseDuration = 0.45f, collapseBounce = 0.12f), CurveTarget.COLLAPSE.withCurve(base, c))
        assertEquals(base.copy(appearDuration = 0.45f, appearBounce = 0.12f), CurveTarget.APPEAR.withCurve(base, c))
        assertEquals(base.copy(splitDuration = 0.45f, splitBounce = 0.12f), CurveTarget.SPLIT.withCurve(base, c))
        val wild = CurveTarget.EXPAND.withCurve(base, Curve(2f, 0.9f))
        assertEquals(CurveMigration.MAX_DURATION, wild.expandDuration, 0f)
        assertEquals(CurveMigration.MAX_BOUNCE, wild.expandBounce, 0f)
        // Reset goes back to the selected curve's Droid preset
        CurveTarget.entries.forEach { t ->
            assertEquals(t.droid, t.curveOf(t.withCurve(expand, t.droid)))
            assertEquals(t.droid, t.curveOf(base))
        }
    }
}
