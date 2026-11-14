package com.droidbubble.app.island.scene

import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandPriority.CALL
import com.droidbubble.app.model.IslandPriority.FLASHLIGHT
import com.droidbubble.app.model.IslandPriority.IDLE
import com.droidbubble.app.model.IslandPriority.MEDIA
import com.droidbubble.app.model.IslandPriority.POP_NOTIFICATION
import com.droidbubble.app.model.IslandPriority.PROGRESS
import com.droidbubble.app.model.IslandPriority.SCREEN_RECORDING
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** M3.3: which activity each slot shows. Pure JUnit. */
class ContentKeyTest {

    private fun notif(key: String, pkg: String = "com.chat", text: String = "hi") =
        NotificationInfo(key = key, packageName = pkg, appName = "Chat", title = "Sam", text = text)

    private val song = MediaInfo(packageName = "com.music", title = "Song A", artist = "Band")
    private val inputs = IslandInputs(media = song, notification = notif("k1"), progress = notif("p1", "com.dl"))

    private fun scene(primary: IslandPriority, secondary: IslandPriority? = null) =
        IslandScene.Off.copy(primary = primary, secondary = secondary)

    @Test
    fun table() {
        val rows = listOf(
            Triple(scene(CALL), Slot.PRIMARY, ContentKey(CALL, "n:k1")),
            Triple(scene(POP_NOTIFICATION), Slot.PRIMARY, ContentKey(POP_NOTIFICATION, "n:k1")),
            Triple(scene(PROGRESS), Slot.PRIMARY, ContentKey(PROGRESS, "n:p1")),
            Triple(scene(MEDIA), Slot.PRIMARY, ContentKey(MEDIA, "media:com.music:Song A")),
            Triple(scene(SCREEN_RECORDING), Slot.PRIMARY, ContentKey(SCREEN_RECORDING, null)),
            Triple(scene(FLASHLIGHT), Slot.PRIMARY, ContentKey(FLASHLIGHT, null)),
            Triple(scene(IDLE), Slot.PRIMARY, ContentKey(IDLE, null)),
            Triple(scene(POP_NOTIFICATION, MEDIA), Slot.SECONDARY, ContentKey(MEDIA, "media:com.music:Song A")),
            Triple(scene(MEDIA), Slot.SECONDARY, ContentKey(IDLE, null)),
        )
        rows.forEach { (s, slot, want) -> assertEquals("${s.primary}/${s.secondary} $slot", want, contentKeyFor(s, inputs, slot)) }
    }

    @Test
    fun missingData_givesANullId() {
        assertEquals(ContentKey(MEDIA, null), contentKeyFor(scene(MEDIA), IslandInputs(), Slot.PRIMARY))
        assertEquals(ContentKey(POP_NOTIFICATION, null), contentKeyFor(scene(POP_NOTIFICATION), IslandInputs(), Slot.PRIMARY))
    }

    @Test
    fun sameKey_updatesInPlace() {
        // A new message in the same conversation, or the media position moving, keeps the key
        val before = contentKeyFor(scene(POP_NOTIFICATION), inputs, Slot.PRIMARY)
        val after = contentKeyFor(scene(POP_NOTIFICATION), inputs.copy(notification = notif("k1", text = "second message")), Slot.PRIMARY)
        assertEquals(before, after)
        val moved = inputs.copy(media = song.copy(positionMs = 42_000, isPlaying = true))
        assertEquals(contentKeyFor(scene(MEDIA), inputs, Slot.PRIMARY), contentKeyFor(scene(MEDIA), moved, Slot.PRIMARY))
    }

    @Test
    fun newKey_transitions() {
        val a = contentKeyFor(scene(POP_NOTIFICATION), inputs, Slot.PRIMARY)
        // Same priority, another app's notification
        val b = contentKeyFor(scene(POP_NOTIFICATION), inputs.copy(notification = notif("k2", "com.other")), Slot.PRIMARY)
        assertNotEquals(a, b)
        val nextTrack = inputs.copy(media = song.copy(title = "Song B"))
        assertNotEquals(contentKeyFor(scene(MEDIA), inputs, Slot.PRIMARY), contentKeyFor(scene(MEDIA), nextTrack, Slot.PRIMARY))
        assertNotEquals(contentKeyFor(scene(MEDIA), inputs, Slot.PRIMARY), a)
    }
}
