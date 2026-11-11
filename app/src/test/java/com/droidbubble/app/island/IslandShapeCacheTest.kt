package com.droidbubble.app.island

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/** M2.3: one shape per exact corner radius, equal to what islandShape() builds. */
class IslandShapeCacheTest {

    @Test
    fun sameCorner_sameInstance() {
        assertSame(IslandShapeCache.get(22.dp), IslandShapeCache.get(22.dp))
        assertSame(IslandShapeCache.get(37.437.dp), IslandShapeCache.get(37.437.dp))
    }

    @Test
    fun exactKeys_noRounding() {
        assertNotSame(IslandShapeCache.get(22.dp), IslandShapeCache.get(22.01.dp))
        assertEquals(islandShape(22.01.dp), IslandShapeCache.get(22.01.dp))
    }

    @Test
    fun stays_bounded() {
        repeat(500) { IslandShapeCache.get((it * 0.1f).dp) }
        assertEquals(islandShape(1.dp), IslandShapeCache.get(1.dp))
    }
}
