package com.droidbubble.app.ui.main

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.droidbubble.app.service.NotificationReceiverService

/** C1.1: how much the island needs a permission. The setup section lists them in this order. */
enum class PermissionLevel { REQUIRED, RECOMMENDED, OPTIONAL }

/** C1.1: how a permission is granted. */
enum class GrantKind {
    /** A system settings screen ([PermissionItem.createIntent]). */
    SETTINGS,
    /** The one-tap runtime dialog for [PermissionItem.runtimePermission]; [PermissionItem.createIntent] is the fallback. */
    RUNTIME,
    /** Our own disclosure dialog first (C1.3), then the settings screen. */
    DISCLOSURE_THEN_SETTINGS,
}

/**
 * One permission in the setup section (C1.1). [whatItDoes] and [whyNeeded] are shown under the
 * title. [restrictedOnSideload]: Android 13+ blocks this setting for apps installed from a file
 * until "Allow restricted settings" is on (C1.4).
 */
data class PermissionItem(
    val id: String,
    val title: String,
    val whatItDoes: String,
    val whyNeeded: String,
    val level: PermissionLevel,
    val grantKind: GrantKind,
    val runtimePermission: String? = null,
    val restrictedOnSideload: Boolean = false,
    val isGranted: Boolean = false,
    val createIntent: (Context) -> Intent
)

object PermissionHelper {

    const val RECORD_AUDIO_ID = "record_audio"

    fun isRecordAudioGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationListenerGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun isNotificationPostingGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun isAccessibilityGranted(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        val myService = "${context.packageName}/${com.droidbubble.app.service.IslandAccessibilityService::class.java.name}"
        val myShortService = "${context.packageName}/.service.IslandAccessibilityService"
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(myService, ignoreCase = true) || componentName.equals(myShortService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    const val OVERLAY_ID = "overlay"
    const val NOTIFICATION_LISTENER_ID = "notification_listener"
    const val ACCESSIBILITY_ID = "accessibility"
    const val BATTERY_ID = "battery_optimization"
    const val POST_NOTIFICATIONS_ID = "post_notifications"
    const val BLUETOOTH_CONNECT_ID = "bluetooth_connect"

    private fun appDetails(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /**
     * Every permission the island can ask for on [sdkInt], with its copy, in the order the setup
     * section shows them: Required, then Recommended, then Optional. Pure (no grant checks), so the
     * order and copy are unit-tested (PermissionCatalogTest). The copy must stay true to the code:
     * the microphone text matches `island/AudioLevels.kt` (the recorder waveform opens the mic).
     */
    fun allPermissions(sdkInt: Int): List<PermissionItem> = buildList {
        add(PermissionItem(
            id = OVERLAY_ID,
            title = "Display over other apps",
            whatItDoes = "Draws the island on top of other apps.",
            whyNeeded = "Without it nothing can appear around the camera.",
            level = PermissionLevel.REQUIRED,
            grantKind = GrantKind.SETTINGS,
            createIntent = { ctx ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            }
        ))
        add(PermissionItem(
            id = NOTIFICATION_LISTENER_ID,
            title = "Notification access",
            whatItDoes = "Reads notifications and media playback on this phone.",
            whyNeeded = "This is how the island knows about music, calls, timers, rides and messages. Nothing leaves your phone.",
            level = PermissionLevel.REQUIRED,
            grantKind = GrantKind.SETTINGS,
            restrictedOnSideload = true,
            createIntent = { ctx ->
                if (sdkInt >= Build.VERSION_CODES.R) {
                    // Straight to our own toggle instead of the list of every listener
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(ctx, NotificationReceiverService::class.java).flattenToString()
                    )
                } else {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
            }
        ))
        add(PermissionItem(
            id = ACCESSIBILITY_ID,
            title = "Status bar layer",
            whatItDoes = "Lets the island sit above the status bar and take touches there, and tells Droidbubble which app is open.",
            whyNeeded = "Without it the island draws under the status-bar icons, and an app's own activity (for example the music pill inside your music app) isn't hidden.",
            level = PermissionLevel.RECOMMENDED,
            grantKind = GrantKind.DISCLOSURE_THEN_SETTINGS,
            restrictedOnSideload = true,
            // Not ACTION_ACCESSIBILITY_DETAILS_SETTINGS: that needs a privileged permission
            createIntent = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        ))
        add(PermissionItem(
            id = BATTERY_ID,
            title = "Unrestricted battery",
            whatItDoes = "Lets Droidbubble keep running in the background.",
            whyNeeded = "Some phones stop background apps to save power; the island then disappears until you open the app again.",
            level = PermissionLevel.RECOMMENDED,
            grantKind = GrantKind.SETTINGS,
            createIntent = { ctx ->
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
            }
        ))
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionItem(
                id = POST_NOTIFICATIONS_ID,
                title = "Droidbubble notifications",
                whatItDoes = "Shows Droidbubble's own notification.",
                // C4.2 adds: "and the "Pause for 1 hour" button"
                whyNeeded = "Needed for update alerts. The island itself works without it.",
                level = PermissionLevel.OPTIONAL,
                grantKind = GrantKind.RUNTIME,
                runtimePermission = Manifest.permission.POST_NOTIFICATIONS,
                // Fallback once the runtime dialog was denied for good
                createIntent = { ctx ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                }
            ))
        }
        add(PermissionItem(
            id = RECORD_AUDIO_ID,
            title = "Microphone (live visualizer)",
            whatItDoes = "Makes the music equalizer and the recorder waveform follow real sound.",
            // Once A9 removes the recorder's mic path, drop the second sentence
            whyNeeded = "The music equalizer reads the phone's audio output. While a voice-recorder activity is on screen, the waveform also listens to the microphone to draw levels; nothing is recorded or saved.",
            level = PermissionLevel.OPTIONAL,
            grantKind = GrantKind.RUNTIME,
            runtimePermission = Manifest.permission.RECORD_AUDIO,
            createIntent = ::appDetails
        ))
        if (sdkInt >= Build.VERSION_CODES.S) {
            add(PermissionItem(
                id = BLUETOOTH_CONNECT_ID,
                title = "Nearby devices",
                whatItDoes = "Lets the island show when headphones or a car connect.",
                whyNeeded = "Android only tells apps about Bluetooth connections with this permission.",
                level = PermissionLevel.OPTIONAL,
                grantKind = GrantKind.RUNTIME,
                runtimePermission = Manifest.permission.BLUETOOTH_CONNECT,
                createIntent = ::appDetails
            ))
        }
    }.sortedBy { it.level }

    /**
     * C1.4: whether Android 13+ may block restricted settings (notification access,
     * accessibility) because the app wasn't installed from a store. A heuristic: the hidden
     * app-op isn't probed. [packageSource] is `InstallSourceInfo.packageSource`, or null if unknown.
     */
    fun isSideloadRestricted(sdkInt: Int, packageSource: Int?): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU && packageSource != PackageInstaller.PACKAGE_SOURCE_STORE

    fun isSideloadRestricted(context: Context): Boolean {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { context.packageManager.getInstallSourceInfo(context.packageName).packageSource }.getOrNull()
        } else null
        return isSideloadRestricted(Build.VERSION.SDK_INT, source)
    }

    /** "Open App info": where Android's "Allow restricted settings" lives (in the ⋮ menu). */
    fun appInfoIntent(context: Context): Intent = appDetails(context)

    fun isBluetoothConnectGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun isGranted(context: Context, id: String): Boolean = when (id) {
        OVERLAY_ID -> isOverlayGranted(context)
        NOTIFICATION_LISTENER_ID -> isNotificationListenerGranted(context)
        ACCESSIBILITY_ID -> isAccessibilityGranted(context)
        BATTERY_ID -> isBatteryOptimizationIgnored(context)
        POST_NOTIFICATIONS_ID -> isNotificationPostingGranted(context)
        RECORD_AUDIO_ID -> isRecordAudioGranted(context)
        BLUETOOTH_CONNECT_ID -> isBluetoothConnectGranted(context)
        else -> true
    }

    /**
     * The permissions still missing, in catalogue order. [includeBluetooth]: "Nearby devices" is
     * listed only while the Bluetooth alert wants it (C1.5).
     */
    fun pending(context: Context, includeBluetooth: Boolean = false): List<PermissionItem> =
        allPermissions(Build.VERSION.SDK_INT)
            .filter { includeBluetooth || it.id != BLUETOOTH_CONNECT_ID }
            .map { it.copy(isGranted = isGranted(context, it.id)) }
            .filterNot { it.isGranted }
}
