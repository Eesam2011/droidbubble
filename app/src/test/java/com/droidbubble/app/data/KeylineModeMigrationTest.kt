package com.droidbubble.app.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.droidbubble.app.island.layout.Keyline
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.KeylineMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V6.1: the old outline switch becomes the keyline mode without changing anyone's look. */
class KeylineModeMigrationTest {

    private val showOutline = booleanPreferencesKey("show_outline")
    private val keylineMode = stringPreferencesKey("keyline_mode")

    @Test
    fun outlineOn_becomesAlways() {
        assertEquals(KeylineMode.ALWAYS, mutablePreferencesOf(showOutline to true).toIslandSettings().keylineMode)
    }

    @Test
    fun outlineOff_becomesOff() {
        assertEquals(KeylineMode.OFF, mutablePreferencesOf(showOutline to false).toIslandSettings().keylineMode)
    }

    @Test
    fun newInstall_isOff() {
        assertEquals(KeylineMode.OFF, IslandSettings().keylineMode)
        assertEquals(KeylineMode.OFF, mutablePreferencesOf().toIslandSettings().keylineMode)
    }

    @Test
    fun storedMode_winsOverTheOldSwitch() {
        val prefs = mutablePreferencesOf(showOutline to true, keylineMode to "AUTO")
        assertEquals(KeylineMode.AUTO, prefs.toIslandSettings().keylineMode)
    }

    @Test
    fun unknownStoredName_fallsBackToTheOldSwitch() {
        assertEquals(KeylineMode.OFF, mutablePreferencesOf(keylineMode to "SPARKLY").toIslandSettings().keylineMode)
    }

    @Test
    fun writing_keepsTheOldSwitchForRollback() {
        for ((mode, legacy) in listOf(KeylineMode.OFF to false, KeylineMode.AUTO to false, KeylineMode.ALWAYS to true)) {
            val prefs = mutablePreferencesOf().apply { write(IslandSettings(keylineMode = mode)) }
            assertEquals(mode.name, prefs[keylineMode])
            assertEquals(legacy, prefs[showOutline])
        }
    }

    @Test
    fun visibility() {
        assertFalse(Keyline.visible(KeylineMode.OFF, darkBackdrop = true, highContrast = false))
        assertTrue(Keyline.visible(KeylineMode.OFF, darkBackdrop = false, highContrast = true))
        assertTrue(Keyline.visible(KeylineMode.ALWAYS, darkBackdrop = false, highContrast = false))
        assertTrue(Keyline.visible(KeylineMode.AUTO, darkBackdrop = true, highContrast = false))
        assertFalse(Keyline.visible(KeylineMode.AUTO, darkBackdrop = false, highContrast = false))
    }
}
