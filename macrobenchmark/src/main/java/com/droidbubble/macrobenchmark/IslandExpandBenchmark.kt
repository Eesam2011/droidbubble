package com.droidbubble.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing of the island's expand and collapse (S4.2), on a real device.
 *
 * Set-up drives the app the way a person would, without the debug receiver (B1 removes it from
 * release-like builds): grant the overlay permission by shell, open the settings app, and tap the
 * Test Live Activities panel's "Download" demo, which shows on a fresh install. Each iteration
 * then taps the pill to expand and taps below the card to collapse.
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
            device.executeShellCommand("appops set $PACKAGE SYSTEM_ALERT_WINDOW allow")
            startActivityAndWait()
            showDownloadDemo(device)
        },
    ) {
        val pill = pillCenter(device)
        device.click(pill.first, pill.second)   // expand
        device.waitForIdle()
        Thread.sleep(800)
        device.click(device.displayWidth / 2, device.displayHeight * 3 / 4)   // tap outside: collapse
        device.waitForIdle()
        Thread.sleep(800)
    }

    private fun showDownloadDemo(device: UiDevice) {
        val list = device.wait(Until.findObject(By.scrollable(true)), 5_000) ?: error("settings list not found")
        repeat(12) {
            device.findObject(By.text("Download"))?.let { it.click(); Thread.sleep(1_500); return }
            list.scroll(Direction.DOWN, 0.8f)
        }
        error("Download demo button not found in the Test Live Activities panel")
    }

    /** The pill sits at the top centre: default top offset 12 dp, height 36 dp. */
    private fun pillCenter(device: UiDevice): Pair<Int, Int> {
        val density = device.displaySizeDp.let { device.displayWidth.toFloat() / it.x }
        return device.displayWidth / 2 to ((12 + 36 / 2) * density).toInt()
    }

    private companion object {
        const val PACKAGE = "com.droidbubble.app"
    }
}
