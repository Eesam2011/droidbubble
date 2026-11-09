package com.droidbubble.macrobenchmark

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing of the island's expand and collapse (S4.2), on a real device.
 *
 * Set-up drives the app the way a person would, without the debug receiver (B1 removes it from
 * release-like builds): grant the overlay permission by shell, open the settings app in a fresh
 * task (so it starts on the main screen), swipe down to the Test Live Activities panel and tap its
 * "Download" demo. Each iteration then taps the pill to expand and taps below the card to collapse.
 *
 * The pill is found from the island's own overlay window, so the user's size and offset settings
 * don't matter. Run it with `adb install -r` plus `am instrument` (see BASELINE.txt) to keep the
 * app's data: the Gradle connected task uninstalls the app afterwards.
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
            startActivityAndWait { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            showDownloadDemo(device)
        },
    ) {
        val pill = islandWindowBounds(device) ?: error("island overlay window not found")
        device.click(pill.centerX(), pill.centerY())   // expand
        device.waitForIdle()
        Thread.sleep(800)
        device.click(device.displayWidth / 2, device.displayHeight * 3 / 4)   // tap outside: collapse
        device.waitForIdle()
        Thread.sleep(800)
    }

    /**
     * Swipes rather than UiObject2.scroll: the settings list recomposes and goes stale while
     * scrolling. Slow swipes plus a pause, because a tap on a list that is still flinging only stops
     * the fling. The tap is retried until the island's window shows.
     */
    private fun showDownloadDemo(device: UiDevice) {
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5_000)
        val x = device.displayWidth / 2
        repeat(15) {
            Thread.sleep(700)
            // The island itself may say "Download" from the previous iteration: skip the top band
            val button = device.findObjects(By.text("Download"))
                .firstOrNull { it.visibleBounds.top > device.displayHeight / 8 }
            if (button != null) {
                repeat(3) {
                    button.click()
                    if (islandWindowBounds(device) != null) { Thread.sleep(1_000); return }
                }
                error("tapped the Download demo but the island did not show")
            }
            device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 4, 60)
            device.waitForIdle()
        }
        error("Download demo button not found in the Test Live Activities panel")
    }

    /**
     * Bounds of the island's compact overlay window; its centre lies inside the pill. Polls for up
     * to 3 s: right after a collapse the window can still be full screen or briefly unreadable.
     */
    private fun islandWindowBounds(device: UiDevice): Rect? {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        repeat(30) {
            automation.windows.firstNotNullOfOrNull { window ->
                val root = window.root ?: return@firstNotNullOfOrNull null
                if (root.packageName != PACKAGE || window.type == AccessibilityWindowInfo.TYPE_APPLICATION) return@firstNotNullOfOrNull null
                Rect().also(window::getBoundsInScreen).takeIf { it.height() < device.displayHeight / 4 }
            }?.let { return it }
            Thread.sleep(100)
        }
        return null
    }

    private companion object {
        const val PACKAGE = "com.droidbubble.app"
    }
}
