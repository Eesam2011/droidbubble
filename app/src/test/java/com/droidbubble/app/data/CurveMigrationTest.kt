package com.droidbubble.app.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionToken
import com.droidbubble.app.island.motion.toPhysical
import com.droidbubble.app.model.CurveMigration
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** D5.1: stored springs become duration and bounce, and every running spring stays the same. */
class CurveMigrationTest {

    private fun legacyPrefs(
        expand: Pair<Float, Float> = 0.8f to 380f,
        appear: Pair<Float, Float> = 0.6f to 280f,
        split: Pair<Float, Float> = 0.63f to 320f,
    ): MutablePreferences = mutablePreferencesOf().apply {
        this[PreferencesKeys.EXPAND_DAMPING] = expand.first
        this[PreferencesKeys.EXPAND_STIFFNESS] = expand.second
        this[PreferencesKeys.APPEAR_DAMPING] = appear.first
        this[PreferencesKeys.APPEAR_STIFFNESS] = appear.second
        this[PreferencesKeys.SPLIT_DAMPING] = split.first
        this[PreferencesKeys.SPLIT_STIFFNESS] = split.second
    }

    /** Same damping ratio, and the effective stiffness (k · speed²) within [tol]. */
    private fun assertSpring(what: String, zeta: Float, k: Float, actual: MotionToken.Physical, tol: Float = 0.01f) {
        assertEquals("$what damping", zeta, actual.dampingRatio, 1e-4f)
        assertTrue("$what stiffness ${actual.stiffness} vs $k", abs(actual.stiffness - k) / k <= tol)
    }

    @Test
    fun defaults_keepEverySpring() {
        val m = IslandMotion.from(legacyPrefs().toIslandSettings())
        assertSpring("expand", 0.8f, 380f, m.expand)
        assertSpring("collapse", 1f, 380f * 1.4f, m.collapse)
        assertSpring("split", 0.63f, 320f, m.split)
        // The glances now ride Appear, taken from the old Split: under 7% from the old Appear's stiffness
        assertSpring("appear", 0.63f, 320f, m.appear)
        assertTrue(abs(m.appear.toPerceptualDuration() - MotionToken.Physical(0.6f, 280f).toPerceptualDuration()) / 0.376f < 0.07f)
    }

    @Test
    fun freshInstall_isTheDroidPreset_andEqualsMigratedDefaults() {
        assertEquals(IslandSettings(), mutablePreferencesOf().toIslandSettings())
        val fresh = IslandSettings()
        assertEquals(CurveMigration.DROID.expand.duration, fresh.expandDuration)
        assertEquals(legacyPrefs().toIslandSettings(), fresh)
        assertEquals(0.322f, fresh.expandDuration, 0.001f); assertEquals(0.2f, fresh.expandBounce, 1e-5f)
        assertEquals(0.272f, fresh.collapseDuration, 0.001f); assertEquals(0f, fresh.collapseBounce, 1e-5f)
        assertEquals(0.351f, fresh.splitDuration, 0.001f); assertEquals(0.37f, fresh.splitBounce, 1e-5f)
        assertEquals(0.351f, fresh.appearDuration, 0.001f); assertEquals(0.37f, fresh.appearBounce, 1e-5f)
    }

    @Test
    fun edgeValues() {
        // The old sliders' extremes: k 150–700, ζ 0.3–1.0
        val soft = IslandMotion.from(legacyPrefs(expand = 1.0f to 150f, split = 0.5f to 700f).toIslandSettings())
        assertSpring("expand k 150", 1.0f, 150f, soft.expand)
        assertSpring("split k 700", 0.5f, 700f, soft.split)
        // ζ 0.3 is bounce 0.7, beyond the new range: clamped to 0.5 (ζ 0.5)
        val r = CurveMigration.migrate(CurveMigration.Legacy(expandDamping = 0.3f))
        assertTrue(r.clamped)
        assertEquals(CurveMigration.MAX_BOUNCE, r.curves.expand.bounce, 1e-6f)
        assertFalse(CurveMigration.migrate(CurveMigration.Legacy()).clamped)
    }

    @Test
    fun overdamped_inverse() {
        val c = CurveMigration.fromSpring(1.25f, 400f)
        assertEquals(-0.2f, c.bounce, 1e-5f)
        val back = MotionToken.Perceptual(c.duration, c.bounce).toPhysical()
        assertEquals(1.25f, back.dampingRatio, 1e-4f)
        assertEquals(400f, back.stiffness, 0.05f)
    }

    @Test
    fun idempotent_writeAndReadAgain() {
        val once = legacyPrefs(expand = 0.7f to 300f).toIslandSettings()
        val prefs = legacyPrefs(expand = 0.7f to 300f).apply { write(once) }
        assertEquals(CurveMigration.SCHEMA, prefs[PreferencesKeys.MOTION_SCHEMA])
        // The old keys are left alone (a rollback still finds them) and no longer read
        assertEquals(0.7f, prefs[PreferencesKeys.EXPAND_DAMPING])
        val twice = prefs.toIslandSettings()
        assertEquals(once, twice)
        prefs.write(twice)
        assertEquals(once, prefs.toIslandSettings())
    }

    @Test
    fun withTheLegacySpeedMigration_speedIsAppliedOnce() {
        val prefs = legacyPrefs().apply { this[PreferencesKeys.LEGACY_ANIMATION_SPEED] = "FAST" }
        val s = prefs.toIslandSettings()
        assertEquals(1.5f, s.animationSpeed)
        val m = IslandMotion.from(s)
        // k · speed² is the old expand spring at the old speed, not speed applied twice
        val run = m.spring<Float>(m.expand)
        assertEquals(0.8f, run.dampingRatio, 1e-4f)
        assertTrue(abs(run.stiffness - 380f * 1.5f * 1.5f) / (380f * 2.25f) <= 0.01f)
    }

    @Test
    fun inverseOfForward_isIdentity() {
        for (d in listOf(0.2f, 0.32f, 0.5f, 0.8f)) for (b in listOf(-0.2f, 0f, 0.15f, 0.37f, 0.5f)) {
            val p = MotionToken.Perceptual(d, b).toPhysical()
            val c = CurveMigration.fromSpring(p.dampingRatio, p.stiffness)
            assertEquals(d, c.duration, 1e-4f)
            assertEquals(b, c.bounce, 1e-4f)
        }
    }

    private fun MotionToken.Physical.toPerceptualDuration() = CurveMigration.fromSpring(dampingRatio, stiffness).duration
}
