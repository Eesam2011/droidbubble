package com.droidbubble.app.island

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Opens what an island card points at (B6.3): the notification's own intent first (the exact
 * chat, the player screen through the media `sessionActivity`, the download's detail), then the
 * app's launcher as the fallback. Returns false if nothing could be opened.
 *
 * On Android 14+ the PendingIntent is sent with background activity start allowed, because the
 * overlay service is not a foreground activity.
 */
fun openTarget(context: Context, primary: PendingIntent?, fallbackPackage: String?): Boolean {
    if (primary != null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }.toBundle()
                primary.send(context, 0, null, null, null, null, options)
            } else {
                primary.send()
            }
            return true
        } catch (e: Throwable) {
            Log.e("OpenTarget", "Failed to send the card's PendingIntent", e)
        }
    }
    if (!fallbackPackage.isNullOrBlank()) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(fallbackPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
                return true
            }
        } catch (e: Throwable) {
            Log.e("OpenTarget", "Failed to launch $fallbackPackage", e)
        }
    }
    return false
}
