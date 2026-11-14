package com.droidbubble.app.island.icon

import com.droidbubble.app.island.torchLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/** I3.3: a torch without brightness steps is always fully lit; with steps it lights to its level. */
class TorchLevelTest {
    @Test
    fun levels() {
        assertEquals(1f, torchLevel(1, 1), 0f)
        assertEquals(1f, torchLevel(1, 0), 0f)
        assertEquals(0.5f, torchLevel(2, 4), 0f)
        assertEquals(1f, torchLevel(4, 4), 0f)
        assertEquals(3, litLayers(torchLevel(2, 4), 6))
    }
}
