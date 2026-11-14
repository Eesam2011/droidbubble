package com.droidbubble.app.island.scene

import androidx.compose.runtime.Immutable
import com.droidbubble.app.model.IslandPriority

/**
 * M3.3: which activity a card (or the pill) is showing. Content transitions when the key changes
 * and updates in place when only the data under the same key changes (a timer tick, a new
 * message in the same conversation, the media position).
 *
 * [id] is `n:<notification key>`, `media:<package>:<title>`, or null for activities that only
 * ever have one instance (recording, flashlight). Phase 7's `ActivityId` replaces it later.
 */
@Immutable
data class ContentKey(val priority: IslandPriority, val id: String?)

enum class Slot { PRIMARY, SECONDARY }

fun contentKeyFor(scene: IslandScene, inputs: IslandInputs, slot: Slot): ContentKey {
    val priority = when (slot) {
        Slot.PRIMARY -> scene.primary
        Slot.SECONDARY -> scene.secondary ?: IslandPriority.IDLE
    }
    val id = when (priority) {
        IslandPriority.CALL, IslandPriority.POP_NOTIFICATION -> inputs.notification?.key?.let { "n:$it" }
        IslandPriority.PROGRESS -> inputs.progress?.key?.let { "n:$it" }
        IslandPriority.MEDIA -> inputs.media?.let { "media:${it.packageName}:${it.title}" }
        IslandPriority.SCREEN_RECORDING, IslandPriority.FLASHLIGHT, IslandPriority.IDLE -> null
    }
    return ContentKey(priority, id)
}
