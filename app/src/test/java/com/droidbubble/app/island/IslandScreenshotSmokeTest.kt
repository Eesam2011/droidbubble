package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi smoke test (S2.1): one media-pill screenshot with a frozen clock.
 * `./gradlew :app:recordRoborazziDebug` records, `./gradlew :app:verifyRoborazziDebug` compares.
 * Record and verify on the same machine and JDK: font rendering differs between machines.
 *
 * Gradle's build cache can replay an earlier test result and skip the comparison. To really
 * verify: `./gradlew :app:cleanTestDebugUnitTest :app:verifyRoborazziDebug --no-build-cache`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandScreenshotSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    @Test
    fun mediaPill() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.advanceFrozen(1500)
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].captureRoboImage("src/test/screenshots/island/smoke_media_pill.png")
    }

    @Test
    fun timerPill() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.timer) }
        h.advanceFrozen(1500)
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].captureRoboImage("src/test/screenshots/island/smoke_timer_pill.png")
    }
}
