package com.droidbubble.app.ui.main

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** C1.5: Bluetooth permission on demand. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BluetoothAlertRowTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var allowed = 0

    private fun show(checked: Boolean, needsPermission: Boolean) {
        rule.setContent {
            DroidbubbleTheme {
                BluetoothAlertRow(checked = checked, needsPermission = needsPermission, onCheckedChange = {}, onAllow = { allowed++ })
            }
        }
    }

    @Test
    fun onWithoutPermission_saysSoAndOffersAllow() {
        show(checked = true, needsPermission = true)
        rule.onNodeWithText("Needs Nearby devices permission").assertExists()
        rule.onNodeWithContentDescription("Allow Nearby devices").performClick()
        assertEquals(1, allowed)
    }

    @Test
    fun offOrGranted_saysNothing() {
        show(checked = false, needsPermission = true)
        rule.onAllNodesWithText("Needs Nearby devices permission").assertCountEquals(0)
    }

    @Test
    fun nearbyDevicesIsPendingOnlyWhileTheAlertIsOn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Robolectric grants nothing at runtime: BLUETOOTH_CONNECT is missing on API 35
        assertTrue(PermissionHelper.pending(context, includeBluetooth = true).any { it.id == PermissionHelper.BLUETOOTH_CONNECT_ID })
        assertFalse(PermissionHelper.pending(context, includeBluetooth = false).any { it.id == PermissionHelper.BLUETOOTH_CONNECT_ID })
    }
}
