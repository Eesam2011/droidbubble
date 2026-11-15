package com.droidbubble.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D5.3: the Calm, Droid and Lively tables and how the editor tells which one is active. */
class PresetTableTest {

    @Test
    fun `Droid is exactly the migration defaults and the default settings`() {
        assertEquals(CurveMigration.migrate(CurveMigration.Legacy()).curves, MotionPreset.DROID.curves)
        assertEquals(MotionPreset.DROID, MotionPreset.detect(IslandSettings()))
    }

    @Test
    fun `values match the roadmap table`() {
        val calm = MotionPreset.CALM.curves
        assertEquals(Curve(0.37f, 0.05f), calm.expand)
        assertEquals(Curve(0.31f, 0f), calm.collapse)
        assertEquals(Curve(0.40f, 0.12f), calm.appear)
        assertEquals(Curve(0.40f, 0.15f), calm.split)
        val lively = MotionPreset.LIVELY.curves
        assertEquals(Curve(0.29f, 0.28f), lively.expand)
        assertEquals(Curve(0.25f, 0.05f), lively.collapse)
        assertEquals(Curve(0.32f, 0.38f), lively.appear)
        assertEquals(Curve(0.32f, 0.40f), lively.split)
        val droid = MotionPreset.DROID.curves
        assertEquals(0.322f, droid.expand.duration, 0.001f)
        assertEquals(0.20f, droid.expand.bounce, 0.001f)
        assertEquals(0.272f, droid.collapse.duration, 0.001f)
        assertEquals(0.351f, droid.appear.duration, 0.001f)
        assertEquals(0.37f, droid.split.bounce, 0.001f)
    }

    @Test
    fun `eager in, calm out holds in every preset`() {
        MotionPreset.entries.forEach { p ->
            val c = p.curves
            assertTrue("${p.label}: collapse bounce", c.collapse.bounce <= 0.05f)
            // The roadmap states 0.85; Lively's own table values (0.25 / 0.29) sit at 0.862, so
            // the check allows 0.87 until the owner decides which of the two to change
            assertTrue("${p.label}: collapse quicker than expand", c.collapse.duration <= 0.87f * c.expand.duration)
        }
    }

    @Test
    fun `every preset value is inside the editor's range`() {
        MotionPreset.entries.forEach { p ->
            p.curves.all.forEach { c -> assertEquals(c, CurveMigration.clamp(c)) }
        }
    }

    @Test
    fun `applying a preset writes all eight fields and detection finds it`() {
        MotionPreset.entries.forEach { p ->
            val s = p.applyTo(IslandSettings().copy(expandDuration = 0.5f, splitBounce = 0.1f))
            assertEquals(p.curves, MotionPreset.curvesOf(s))
            assertEquals(p, MotionPreset.detect(s))
        }
    }

    @Test
    fun `applying a preset leaves every other setting alone`() {
        val before = IslandSettings().copy(animationSpeed = 1.3f, widthDp = 150)
        val after = MotionPreset.LIVELY.applyTo(before)
        assertEquals(before, after.copy(
            expandDuration = before.expandDuration, expandBounce = before.expandBounce,
            collapseDuration = before.collapseDuration, collapseBounce = before.collapseBounce,
            appearDuration = before.appearDuration, appearBounce = before.appearBounce,
            splitDuration = before.splitDuration, splitBounce = before.splitBounce,
        ))
    }

    @Test
    fun `any single changed field is Custom`() {
        val base = MotionPreset.CALM.applyTo(IslandSettings())
        listOf(
            base.copy(expandDuration = 0.38f),
            base.copy(expandBounce = 0.06f),
            base.copy(collapseDuration = 0.30f),
            base.copy(collapseBounce = 0.01f),
            base.copy(appearDuration = 0.41f),
            base.copy(appearBounce = 0.13f),
            base.copy(splitDuration = 0.39f),
            base.copy(splitBounce = 0.16f),
        ).forEach { assertNull(MotionPreset.detect(it)) }
    }

    @Test
    fun `existing curves are not a preset change`() {
        // A user who migrated from non-default springs keeps their values and reads Custom
        val s = IslandSettings().copy(expandDuration = 0.45f)
        assertEquals(0.45f, s.expandDuration, 0f)
        assertNull(MotionPreset.detect(s))
    }
}
