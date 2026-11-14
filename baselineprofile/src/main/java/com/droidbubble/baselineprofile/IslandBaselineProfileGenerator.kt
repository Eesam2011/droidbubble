package com.droidbubble.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.journeys.IslandJourneys.PACKAGE
import com.droidbubble.journeys.IslandJourneys.collapse
import com.droidbubble.journeys.IslandJourneys.expand
import com.droidbubble.journeys.IslandJourneys.showDemo
import com.droidbubble.journeys.IslandJourneys.showDownloadDemo
import com.droidbubble.journeys.IslandJourneys.startApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's baseline profile (F6.3) from the S4 journeys, so the first expands after
 * install run precompiled instead of stuttering while Compose JIT-warms.
 *
 * Journeys: service start (the app's process, which also hosts the island's accessibility
 * service), compact appear (Download demo), expand and collapse; then the same for a few other
 * demos when the panel has them. Media card and dual split: there is no media demo, so start
 * playing something in any media app before the run; the Download demo then joins it as a dual
 * island, and the expands cover the media card.
 *
 * Needs the island turned on in the app and the accessibility service enabled. The command is in
 * baselineprofile/README.md.
 */
@RunWith(AndroidJUnit4::class)
class IslandBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        startApp()
        showDownloadDemo(device)
        expand(device)
        collapse(device)
        // Other card layouts; best effort, as a demo that isn't in the panel is skipped
        for (label in listOf("Timer", "Ride", "Live score")) {
            if (showDemo(device, label)) {
                expand(device)
                collapse(device)
            }
        }
    }
}
