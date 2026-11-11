package com.droidbubble.app.island

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shape of the island's body: its size, corner radius and distance from the top of the
 * screen. Animated as one 4D spring, so the four values move together and keep their velocity.
 */
@Immutable
data class IslandGeometry(val width: Dp, val height: Dp, val corner: Dp, val top: Dp) {
    companion object {
        val VectorConverter = TwoWayConverter<IslandGeometry, AnimationVector4D>(
            convertToVector = { AnimationVector4D(it.width.value, it.height.value, it.corner.value, it.top.value) },
            convertFromVector = { IslandGeometry(it.v1.dp, it.v2.dp, it.v3.dp, it.v4.dp) }
        )

        // Stop the spring once it's within 0.1dp (same as Dp's default) instead of running extra frames
        val VisibilityThreshold = IslandGeometry(0.1.dp, 0.1.dp, 0.1.dp, 0.1.dp)
    }
}
