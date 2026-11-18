package com.droidbubble.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.LiveActivityTester
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Owner review only: renders the Test lab panel, in light and dark, with three demos stacked.
 * Runs only with `-Pcheckpoint=TESTLAB`; writes to the checkpoint output folder.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h1600dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TestLabPanelRender(private val dark: Boolean) {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun render() {
        assumeTrue(System.getProperty("island.checkpoint") == "TESTLAB")
        val out = System.getProperty("island.checkpoint.out") + "/TESTLAB"
        IslandStateManager.resetForTest()
        rule.runOnIdle {
            LiveActivityTester.stack = true
            LiveActivityTester.show(LiveActivityTester.Demo.TIMER)
            LiveActivityTester.show(LiveActivityTester.Demo.RECORDER)
            LiveActivityTester.show(LiveActivityTester.Demo.CALL)
        }
        rule.setContent {
            DroidbubbleTheme(darkTheme = dark, dynamicColor = false) {
                Box(Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(16.dp)) {
                    LiveActivityTestPanel(islandRunning = true)
                }
            }
        }
        rule.waitForIdle()
        val image = rule.runOnIdle {
            val view = rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                .also { view.draw(android.graphics.Canvas(it)) }
        }
        val file = java.io.File(out, "testlab_${if (dark) "dark" else "light"}.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        LiveActivityTester.stack = false
        LiveActivityTester.clear()
        IslandStateManager.resetForTest()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }
}
