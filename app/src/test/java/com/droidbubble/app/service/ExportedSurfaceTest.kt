package com.droidbubble.app.service

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Pins the app's exported components (B1.2), so a new exported one can't slip in unnoticed.
 * Each entry is "name|permission". To add one, check who can reach it, then update the list.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ExportedSurfaceTest {

    private val allowlist = setOf(
        "com.droidbubble.app.MainActivity|-",
        "com.droidbubble.app.service.NotificationReceiverService|android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "com.droidbubble.app.service.IslandAccessibilityService|android.permission.BIND_ACCESSIBILITY_SERVICE",
        // BOOT_COMPLETED is a protected broadcast and onReceive ignores every other action
        "com.droidbubble.app.service.BootReceiver|-",
        // profileinstaller: only the shell (DUMP) can reach it
        "androidx.profileinstaller.ProfileInstallReceiver|android.permission.DUMP",
    )

    /** From debugImplementation libraries (ui-tooling, ui-test-manifest): not in release builds. */
    private val debugOnly = setOf(
        "androidx.compose.ui.tooling.PreviewActivity|-",
        "androidx.activity.ComponentActivity|-",
    )

    @Test
    fun exportedComponents_matchAllowlist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.MATCH_DISABLED_COMPONENTS
        val info = context.packageManager.getPackageInfo(context.packageName, flags)

        val exported = buildSet {
            info.activities.orEmpty().forEach { add(entry(it, it.permission)) }
            info.services.orEmpty().forEach { add(entry(it, it.permission)) }
            info.receivers.orEmpty().forEach { add(entry(it, it.permission)) }
            info.providers.orEmpty().forEach { add(entry(it, it.readPermission ?: it.writePermission)) }
        }.filterNotNull().toSortedSet()

        val expected = (if (BuildConfig.DEBUG) allowlist + debugOnly else allowlist).toSortedSet()
        assertEquals(expected, exported)
    }

    private fun entry(c: ComponentInfo, permission: String?): String? =
        if (c.exported) "${c.name}|${permission ?: "-"}" else null
}
