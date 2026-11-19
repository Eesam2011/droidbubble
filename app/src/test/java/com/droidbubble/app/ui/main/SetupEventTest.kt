package com.droidbubble.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** C1.6: the guided first run's snackbars come from the difference between two checks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SetupEventTest {

    private val all = PermissionHelper.allPermissions(35)
    private fun only(vararg ids: String) = all.filter { it.id in ids }
    private val overlay = all.single { it.id == "overlay" }
    private val listener = all.single { it.id == "notification_listener" }

    @Test
    fun firstCheck_saysNothing() {
        assertNull(setupEventFor(null, only("overlay", "notification_listener")))
        assertNull("everything already granted", setupEventFor(null, emptyList()))
    }

    @Test
    fun overlayThenListener_twoSnackbarsInOrder() {
        val start = only("overlay", "notification_listener", "record_audio")
        val afterOverlay = only("notification_listener", "record_audio")
        assertEquals(SetupEvent.Granted(overlay, listener), setupEventFor(start, afterOverlay))
        assertEquals(SetupEvent.Ready, setupEventFor(afterOverlay, only("record_audio")))
    }

    @Test
    fun optionalGrantsAndNoChange_sayNothing() {
        val start = only("notification_listener", "record_audio")
        assertNull(setupEventFor(start, only("notification_listener")))
        assertNull(setupEventFor(start, start))
    }

    @Test
    fun bothRequiredAtOnce_isReady() {
        assertEquals(SetupEvent.Ready, setupEventFor(only("overlay", "notification_listener"), emptyList()))
    }

    @Test
    fun stepLabels() {
        assertEquals("Step 1 of 2", setupStepLabel(overlay))
        assertEquals("Step 2 of 2", setupStepLabel(listener))
        assertEquals("Optional", setupStepLabel(all.single { it.id == "record_audio" }))
        assertEquals("Recommended", setupStepLabel(all.single { it.id == "accessibility" }))
    }
}
