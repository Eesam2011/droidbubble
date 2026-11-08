package com.droidbubble.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FeaturePrioritiesTest {

    @Test
    fun missingValue_usesDefaults() {
        assertEquals(DEFAULT_FEATURE_PRIORITIES, parseFeaturePriorities(null))
    }

    @Test
    fun removedFps_isDroppedAndNotReAdded() {
        val parsed = parseFeaturePriorities("CALL,MEDIA,FPS,WEATHER")
        assertEquals(listOf("CALL", "MEDIA", "WEATHER"), parsed.take(3))
        assertFalse("FPS" in parsed)
        assertEquals(DEFAULT_FEATURE_PRIORITIES.toSet(), parsed.toSet())
    }

    @Test
    fun removedTimer_isDropped_andOrderKept() {
        val parsed = parseFeaturePriorities(" MEDIA , TIMER,CALL,,")
        assertEquals(listOf("MEDIA", "CALL"), parsed.take(2))
        assertFalse("TIMER" in parsed)
    }

    @Test
    fun defaults_haveNoRemovedKeys() {
        assertFalse(DEFAULT_FEATURE_PRIORITIES.any { it in REMOVED_FEATURE_KEYS })
    }
}
