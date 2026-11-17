package com.droidbubble.app.service

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * V6.1: is the wallpaper behind the island dark? Drives the keyline's AUTO mode. Needs no
 * permission; follows wallpaper changes through [WallpaperManager.addOnColorsChangedListener].
 */
object WallpaperTone {
    /** Luminance under which a wallpaper's main colour counts as dark (API 29–30). */
    private const val DARK_LUMINANCE = 0.3f

    /**
     * API 31+: dark unless the system says dark text reads on it. API 29–30 (the hints are hidden
     * there): the main colour's luminance. No colours (some live wallpapers): not dark, so the
     * borderless look stays.
     */
    fun isDark(sdk: Int, colorHints: Int?, primaryLuminance: Float?): Boolean = when {
        sdk >= Build.VERSION_CODES.S && colorHints != null ->
            colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT == 0
        primaryLuminance != null -> primaryLuminance < DARK_LUMINANCE
        else -> false
    }

    /** The lock screen wallpaper's tone while [locked] says the device is locked, else the home screen's. */
    fun darkFlow(context: Context, locked: Flow<Boolean>): Flow<Boolean> {
        val wm = WallpaperManager.getInstance(context.applicationContext)
        val changes = callbackFlow {
            trySend(Unit)
            val listener = WallpaperManager.OnColorsChangedListener { _, _ -> trySend(Unit) }
            wm.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            awaitClose { wm.removeOnColorsChangedListener(listener) }
        }
        return combine(changes, locked) { _, isLocked -> read(wm, isLocked) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    private fun read(wm: WallpaperManager, locked: Boolean): Boolean {
        val colors = runCatching {
            // A lock screen that shows the home wallpaper has no colours of its own
            (if (locked) wm.getWallpaperColors(WallpaperManager.FLAG_LOCK) else null)
                ?: wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull() ?: return false
        val hints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colors.colorHints else null
        return isDark(Build.VERSION.SDK_INT, hints, colors.primaryColor.luminance())
    }
}
