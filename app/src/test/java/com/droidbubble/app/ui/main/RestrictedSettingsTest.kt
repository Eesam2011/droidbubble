package com.droidbubble.app.ui.main

import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C1.4: when the "Allow restricted settings" help appears. Pure. */
class RestrictedSettingsTest {

    @Test
    fun heuristic() {
        // Android 13+ and not from a store
        assertTrue(PermissionHelper.isSideloadRestricted(33, PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE))
        assertTrue(PermissionHelper.isSideloadRestricted(34, PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE))
        assertTrue(PermissionHelper.isSideloadRestricted(35, PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED))
        assertTrue("unknown source", PermissionHelper.isSideloadRestricted(36, null))
        assertFalse(PermissionHelper.isSideloadRestricted(34, PackageInstaller.PACKAGE_SOURCE_STORE))
        assertFalse("Android 12 has no restricted settings", PermissionHelper.isSideloadRestricted(32, PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE))
    }

    @Test
    fun helpOnlyAfterAFailedTry_onRestrictedItems() {
        val pending = PermissionHelper.allPermissions(34)
        assertEquals(emptySet<String>(), restrictedHelpFor(pending, emptyMap(), sideloadRestricted = true))
        assertEquals(
            setOf("notification_listener"),
            restrictedHelpFor(pending, mapOf("notification_listener" to 1, "overlay" to 2), sideloadRestricted = true)
        )
        assertEquals(emptySet<String>(), restrictedHelpFor(pending, mapOf("notification_listener" to 1), sideloadRestricted = false))
        // Granted since: no longer pending, so no help
        val granted = pending.filterNot { it.id == "notification_listener" }
        assertEquals(emptySet<String>(), restrictedHelpFor(granted, mapOf("notification_listener" to 1), sideloadRestricted = true))
    }
}
