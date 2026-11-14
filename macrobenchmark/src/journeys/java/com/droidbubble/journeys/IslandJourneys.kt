package com.droidbubble.journeys

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * The island journeys (S4.2), shared by the macrobenchmarks (`:macrobenchmark`) and the baseline
 * profile generator (`:baselineprofile`, F6.3). Both modules compile this source folder.
 *
 * They drive the app the way a person would, without the debug receiver (B1 removes it from
 * release-like builds): grant the overlay permission by shell, open the settings app in a fresh
 * task (so it starts on the main screen), swipe down to the Test Live Activities panel and tap a
 * demo. The accessibility service must already be on, and the island turned on in the app.
 *
 * The pill is found from the island's own overlay window, so the user's size and offset settings
 * don't matter.
 */
object IslandJourneys {

    const val PACKAGE = "com.droidbubble.app"

    /** Service start: grants the overlay permission and opens the settings app in a fresh task. */
    fun MacrobenchmarkScope.startApp() {
        device.executeShellCommand("appops set $PACKAGE SYSTEM_ALERT_WINDOW allow")
        startActivityAndWait { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
    }

    /** Compact appear: taps the "Download" demo, the one that shows on a fresh install. */
    fun showDownloadDemo(device: UiDevice) {
        if (!showDemo(device, "Download")) error("Download demo button not found in the Test Live Activities panel")
    }

    /**
     * Taps the demo button labelled [label] and waits for the island's window. Returns false when
     * the button isn't found; fails when it was tapped but the island didn't show.
     *
     * Swipes rather than UiObject2.scroll: the settings list recomposes and goes stale while
     * scrolling. Slow swipes plus a pause, because a tap on a list that is still flinging only stops
     * the fling. The tap is retried until the island's window shows.
     */
    fun showDemo(device: UiDevice, label: String): Boolean {
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5_000)
        val x = device.displayWidth / 2
        repeat(15) {
            Thread.sleep(700)
            // The island itself may show the same text from the previous iteration: skip the top band
            val button = device.findObjects(By.text(label))
                .firstOrNull { it.visibleBounds.top > device.displayHeight / 8 }
            if (button != null) {
                repeat(3) {
                    button.click()
                    if (islandWindowBounds(device) != null) { Thread.sleep(1_000); return true }
                }
                error("tapped the $label demo but the island did not show")
            }
            device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 4, 60)
            device.waitForIdle()
        }
        return false
    }

    /** Expand: taps the pill. */
    fun expand(device: UiDevice) {
        val pill = islandWindowBounds(device) ?: error("island overlay window not found")
        device.click(pill.centerX(), pill.centerY())
        device.waitForIdle()
        Thread.sleep(800)
    }

    /** Collapse: taps below the card. */
    fun collapse(device: UiDevice) {
        device.click(device.displayWidth / 2, device.displayHeight * 3 / 4)
        device.waitForIdle()
        Thread.sleep(800)
    }

    /**
     * Bounds of the island's compact overlay window; its centre lies inside the pill. Polls for up
     * to 3 s: right after a collapse the window can still be full screen or briefly unreadable.
     */
    fun islandWindowBounds(device: UiDevice): Rect? {
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
}
