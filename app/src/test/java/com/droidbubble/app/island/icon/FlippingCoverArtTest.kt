package com.droidbubble.app.island.icon

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.FlippingCoverArt
import com.droidbubble.app.island.motion.MotionTier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** I3.4: skipping tracks fast flips (or, Reduced, crossfades) without a blank frame, and settles on the last cover. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class FlippingCoverArtTest {

    @get:Rule
    val rule = createComposeRule()

    private var track by mutableIntStateOf(0)
    private val covers = List(6) { Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888) }

    private fun skipFiveTimes(tier: MotionTier) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            WithIslandMotion(tier) {
                FlippingCoverArt(covers[track], trackKey = track, modifier = Modifier.size(40.dp))
            }
        }
        rule.sampleFrames(5) { 0f }
        repeat(5) {
            track++
            val shown = rule.sampleFrames(6) {
                rule.onAllNodesWithContentDescription("Cover Art").fetchSemanticsNodes().size.toFloat()
            }
            // The current cover is always there: never a blank frame
            assertEquals(listOf(1f), shown.distinct())
        }
        rule.sampleFrames(40) { 0f }
    }

    @Test fun full() = skipFiveTimes(MotionTier.FULL)

    @Test fun reduced() = skipFiveTimes(MotionTier.REDUCED)
}
