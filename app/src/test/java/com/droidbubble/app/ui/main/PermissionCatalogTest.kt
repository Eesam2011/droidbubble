package com.droidbubble.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C1.1: the setup section's permission catalogue (order, copy, SDK gates). Pure. */
class PermissionCatalogTest {

    private val sdks = listOf(29, 30, 31, 32, 33, 34, 35, 36)

    @Test
    fun requiredThenRecommendedThenOptional() {
        for (sdk in sdks) {
            val levels = PermissionHelper.allPermissions(sdk).map { it.level }
            assertEquals("sdk $sdk", levels.sorted(), levels)
            assertEquals(PermissionLevel.REQUIRED, levels.first())
        }
        assertEquals(
            listOf("overlay", "notification_listener", "accessibility", "battery_optimization",
                "post_notifications", "record_audio", "bluetooth_connect"),
            PermissionHelper.allPermissions(35).map { it.id }
        )
    }

    @Test
    fun sdkGates() {
        for (sdk in sdks) {
            val ids = PermissionHelper.allPermissions(sdk).map { it.id }
            assertEquals("post_notifications on $sdk", sdk >= 33, "post_notifications" in ids)
            assertEquals("bluetooth_connect on $sdk", sdk >= 31, "bluetooth_connect" in ids)
        }
    }

    @Test
    fun everyItemExplainsItself() {
        for (item in PermissionHelper.allPermissions(36)) {
            assertTrue(item.id, item.title.isNotBlank())
            assertTrue(item.id, item.whatItDoes.isNotBlank())
            assertTrue(item.id, item.whyNeeded.isNotBlank())
            val copy = item.title + item.whatItDoes + item.whyNeeded
            assertFalse(item.id, copy.contains("Dynamic Island", ignoreCase = true))
            if (item.grantKind == GrantKind.RUNTIME) assertTrue(item.id, item.runtimePermission != null)
        }
    }

    @Test
    fun onlyOverlayAndListenerAreRequired() {
        val required = PermissionHelper.allPermissions(36).filter { it.level == PermissionLevel.REQUIRED }.map { it.id }
        assertEquals(listOf("overlay", "notification_listener"), required)
    }

    @Test
    fun micCopyMatchesTheRecorderWaveform() {
        // island/AudioLevels.kt opens AudioRecord(MIC) for the recorder waveform (until A9)
        val mic = PermissionHelper.allPermissions(36).single { it.id == PermissionHelper.RECORD_AUDIO_ID }
        assertTrue(mic.whyNeeded.contains("microphone"))
        assertTrue(mic.whyNeeded.contains("nothing is recorded"))
    }

    @Test
    fun sideloadRestrictedItems() {
        val restricted = PermissionHelper.allPermissions(36).filter { it.restrictedOnSideload }.map { it.id }
        assertEquals(listOf("notification_listener", "accessibility"), restricted)
    }
}
