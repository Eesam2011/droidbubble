package com.droidbubble.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
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
 * The **first** expand after a cold start (F6.3), with and without the baseline profile: the
 * profile should lower its P90 frame duration compared with [CompilationMode.None] (no AOT code,
 * everything JIT-warms). [withBaselineProfile] fails if the app has no profile: generate it first
 * (baselineprofile/README.md).
 *
 * COLD kills the app's process before each iteration, which also stops the accessibility service
 * that hosts the island; the system restarts it, and the journey waits for the island's window.
 */
@RunWith(AndroidJUnit4::class)
class IslandFirstExpandBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun withoutCompilation() = firstExpand(CompilationMode.None())

    @Test
    fun withBaselineProfile() = firstExpand(CompilationMode.Partial(BaselineProfileMode.Require))

    @OptIn(ExperimentalMetricApi::class)
    private fun firstExpand(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
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
