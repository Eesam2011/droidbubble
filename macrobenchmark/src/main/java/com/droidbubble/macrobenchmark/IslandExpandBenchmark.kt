package com.droidbubble.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.journeys.IslandJourneys.PACKAGE
import com.droidbubble.journeys.IslandJourneys.collapse
import com.droidbubble.journeys.IslandJourneys.expand
import com.droidbubble.journeys.IslandJourneys.showDownloadDemo
import com.droidbubble.journeys.IslandJourneys.startApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing of the island's expand and collapse (S4.2), on a real device.
 *
 * Set-up opens the app and shows the "Download" demo (see [com.droidbubble.journeys.IslandJourneys],
 * shared with the baseline profile generator). Each iteration then taps the pill to expand and
 * taps below the card to collapse. Run it with `adb install -r` plus `am instrument` (see
 * BASELINE.txt) to keep the app's data: the Gradle connected task uninstalls the app afterwards.
 */
@RunWith(AndroidJUnit4::class)
class IslandExpandBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun expandAndCollapse() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = {
            startApp()
            showDownloadDemo(device)
        },
    ) {
        expand(device)
        collapse(device)
    }
}
