package com.droidbubble.app.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * B8.6: every [IslandSettings] field survives a write and a read through the single mapper. The
 * fields are found by reflection, so a new setting whose key is forgotten fails here.
 */
class SettingsRepositoryRoundTripTest {

    private val fields = IslandSettings::class.java.declaredFields
        .filter { !Modifier.isStatic(it.modifiers) }
        .onEach { it.isAccessible = true }

    /** A value of the field's type that differs from the default and is still valid. */
    private fun changed(name: String, default: Any?): Any? = when (default) {
        is Boolean -> !default
        is Int -> default + 1
        is Long -> default + 1
        is Float -> default + 0.25f
        is String -> if (name == "islandStyle") {
            if (default == IslandSettings.STYLE_NOTCH) IslandSettings.STYLE_PILL else IslandSettings.STYLE_NOTCH
        } else "$default-changed"
        is Set<*> -> default + "com.example.changed"
        is List<*> -> default.reversed() // featurePriorities: a reorder of the valid keys
        else -> error("Add a changed value for $name (${default?.javaClass})")
    }

    private fun nonDefaultSettings(): IslandSettings {
        val defaults = IslandSettings()
        val ctor = IslandSettings::class.java.declaredConstructors
            .single { it.parameterCount == fields.size && !it.isSynthetic }
        return ctor.newInstance(*fields.map { changed(it.name, it.get(defaults)) }.toTypedArray()) as IslandSettings
    }

    @Test
    fun everyField_survivesWriteAndRead() {
        val defaults = IslandSettings()
        val original = nonDefaultSettings()
        val prefs = mutablePreferencesOf()
        prefs.write(original)
        val back = prefs.toIslandSettings()
        for (field in fields) {
            assertNotEquals("${field.name}: the test value must differ from the default", field.get(defaults), field.get(original))
            assertEquals("${field.name} was lost on the way through the preferences", field.get(original), field.get(back))
        }
        assertEquals(original, back)
    }

    @Test
    fun emptyPreferences_readAsTheDefaults() {
        assertEquals(IslandSettings(), mutablePreferencesOf().toIslandSettings())
    }
}
