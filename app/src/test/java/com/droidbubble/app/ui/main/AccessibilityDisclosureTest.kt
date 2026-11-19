package com.droidbubble.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.R
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** C1.3: the status bar layer's disclosure comes before Accessibility settings. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h1600dp-xxhdpi")
class AccessibilityDisclosureTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val launched = mutableListOf<String>()

    private fun show() {
        val items = PermissionHelper.allPermissions(35).filter { it.id != PermissionHelper.BLUETOOTH_CONNECT_ID }
        rule.setContent {
            DroidbubbleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val allow = rememberDisclosureGate { launched += it.id }
                    PermissionSection(items, emptySet(), false, onAllow = allow, onSkip = {}, onReviewSkipped = {})
                }
            }
        }
    }

    private fun inDialog(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    @Test
    fun allowOnTheStatusBarLayer_showsTheDisclosureFirst_notNowLaunchesNothing() {
        show()
        rule.onNodeWithContentDescription("Allow Status bar layer").performClick()
        rule.onNodeWithText(DISCLOSURE_TITLE).assertExists()
        assertTrue(launched.isEmpty())
        inDialog("Not now").performClick()
        rule.onAllNodesWithText(DISCLOSURE_TITLE).assertCountEquals(0)
        assertTrue(launched.isEmpty())
    }

    @Test
    fun continueOpensTheSettings() {
        show()
        rule.onNodeWithContentDescription("Allow Status bar layer").performClick()
        inDialog("Continue").performClick()
        assertEquals(listOf(PermissionHelper.ACCESSIBILITY_ID), launched)
    }

    @Test
    fun otherItemsSkipTheDialog() {
        show()
        rule.onNodeWithContentDescription("Allow Display over other apps").performClick()
        rule.onAllNodesWithText(DISCLOSURE_TITLE).assertCountEquals(0)
        assertEquals(listOf(PermissionHelper.OVERLAY_ID), launched)
    }

    @Test
    fun systemSettingsCopyMatchesTheDialog() {
        val res = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        assertEquals("Droidbubble status bar layer", res.getString(R.string.accessibility_service_title))
        val d = res.getString(R.string.accessibility_service_description)
        assertTrue(d.contains("above the status bar") && d.contains("its name only") && d.contains("can't read screen content"))
        assertFalse(d.contains("Dynamic Island"))
    }
}
