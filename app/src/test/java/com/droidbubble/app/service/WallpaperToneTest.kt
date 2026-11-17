package com.droidbubble.app.service

import android.app.WallpaperColors
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V6.1: which wallpapers count as dark for the AUTO keyline. */
class WallpaperToneTest {

    private val darkText = WallpaperColors.HINT_SUPPORTS_DARK_TEXT

    @Test
    fun api31_followsTheDarkTextHint() {
        assertTrue(WallpaperTone.isDark(31, colorHints = 0, primaryLuminance = 0.9f))
        assertFalse(WallpaperTone.isDark(31, colorHints = darkText, primaryLuminance = 0.05f))
        assertFalse(WallpaperTone.isDark(34, colorHints = darkText or 4, primaryLuminance = null))
    }

    @Test
    fun api29and30_useTheMainColoursLuminance() {
        assertTrue(WallpaperTone.isDark(29, colorHints = null, primaryLuminance = 0.29f))
        assertFalse(WallpaperTone.isDark(30, colorHints = null, primaryLuminance = 0.3f))
        // Hints read on an older version are ignored
        assertFalse(WallpaperTone.isDark(30, colorHints = 0, primaryLuminance = 0.8f))
    }

    @Test
    fun noColours_isNotDark() {
        assertFalse(WallpaperTone.isDark(29, colorHints = null, primaryLuminance = null))
        assertFalse(WallpaperTone.isDark(31, colorHints = null, primaryLuminance = null))
    }
}
