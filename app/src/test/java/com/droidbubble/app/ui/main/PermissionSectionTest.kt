package com.droidbubble.app.ui.main

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** C1.2: the setup section (rows, levels, "Not now", the folded skipped row). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h1600dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PermissionSectionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val all = PermissionHelper.allPermissions(35).filter { it.id != PermissionHelper.BLUETOOTH_CONNECT_ID }

    private val skippedIds = mutableListOf<String>()
    private val allowed = mutableListOf<String>()
    private var reviewed = 0

    private fun show(items: List<PermissionItem>, skipped: Set<String> = emptySet(), showSkipped: Boolean = false) {
        rule.setContent {
            DroidbubbleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        PermissionSection(
                            items = items,
                            skipped = skipped,
                            showSkipped = showSkipped,
                            onAllow = { allowed += it.id },
                            onSkip = { skippedIds += it },
                            onReviewSkipped = { reviewed++ }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun requiredRowHasNoNotNow_andAllowIsLabelled() {
        show(all.filter { it.level == PermissionLevel.REQUIRED })
        rule.onNodeWithText("Required setup").assertExists()
        rule.onNodeWithText("2 required").assertExists()
        rule.onAllNodesWithText("Not now").assertCountEquals(0)
        rule.onNodeWithContentDescription("Allow Display over other apps").performClick()
        assertEquals(listOf("overlay"), allowed)
    }

    @Test
    fun everyRowShowsItsExplanation_andOptionalRowsCanBeSkipped() {
        show(all)
        for (item in all) {
            rule.onNode(hasText(item.whyNeeded, substring = true)).assertExists()
            rule.onNode(hasContentDescription("Allow ${item.title}")).assertExists()
        }
        // 4 non-required rows: accessibility, battery, notifications, microphone
        rule.onAllNodesWithText("Not now").assertCountEquals(4)
        rule.onAllNodesWithText("Not now")[0].performClick()
        assertEquals(listOf(PermissionHelper.ACCESSIBILITY_ID), skippedIds)
    }

    @Test
    fun onlyOptionalPending_isFinishSetup() {
        show(all.filter { it.level != PermissionLevel.REQUIRED })
        rule.onNodeWithText("Finish setup").assertExists()
        rule.onNodeWithText("4 optional").assertExists()
    }

    @Test
    fun onlySkippedLeft_foldsToOneLine() {
        val optional = all.filter { it.level == PermissionLevel.OPTIONAL }
        show(optional, skipped = optional.map { it.id }.toSet())
        rule.onNodeWithText("2 optional items skipped").assertExists()
        rule.onAllNodesWithText("Finish setup").assertCountEquals(0)
        rule.onNodeWithText("Review").performClick()
        assertEquals(1, reviewed)
    }

    @Test
    fun model_requiredCanNeverBeSkipped() {
        val m = SetupSectionModel.of(all, skipped = setOf("overlay", "record_audio"), showSkipped = false)
        assertTrue(m.visible.any { it.id == "overlay" })
        assertEquals(listOf("record_audio"), m.folded.map { it.id })
        val reviewed = SetupSectionModel.of(all, skipped = setOf("record_audio"), showSkipped = true)
        assertEquals(all, reviewed.visible)
        assertTrue(SetupSectionModel.of(emptyList(), setOf("record_audio"), false).isEmpty)
    }

    /** Not an assertion: writes the section to a PNG for review (build/…/c1.2-setup.png). */
    @Test
    fun renderForReview() {
        show(all, skipped = setOf(PermissionHelper.BATTERY_ID))
        rule.waitForIdle()
        val dir = File(System.getProperty("user.home"), ".droidbubble-build/app/outputs/review").apply { mkdirs() }
        // Draw the window ourselves: captureToImage waits for a frame Robolectric never sends
        val bitmap = rule.runOnIdle {
            val view = rule.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(android.graphics.Canvas(it)) }
        }
        File(dir, "c1.2-setup.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
