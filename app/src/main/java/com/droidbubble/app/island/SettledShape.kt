package com.droidbubble.app.island

import androidx.compose.runtime.Immutable
import com.droidbubble.app.island.scene.IslandMode

/**
 * The shape the island has come to rest in (F5.3). The view publishes it when every shape
 * animation has stopped, and clears it (null) as soon as one starts. The window waits for it
 * before shrinking, so a shape still being drawn is never clipped.
 */
@Immutable
data class SettledShape(val mode: IslandMode)
