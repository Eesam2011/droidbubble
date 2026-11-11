package com.droidbubble.app.island.scene

import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** F1.5: one scene flow that emits only when what the island shows changes. */
@OptIn(ExperimentalCoroutinesApi::class)
class IslandSceneHolderTest {

    private val song = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "Artist", isPlaying = true, positionMs = 1_000)
    private val message = NotificationInfo(key = "msg", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Hello")

    private fun TestScope.holder(inputs: MutableStateFlow<IslandInputs>, settings: MutableStateFlow<IslandSettings>) =
        IslandSceneHolder(backgroundScope, inputs, settings, MutableStateFlow(IslandEnv()), inputs.value)

    @Test
    fun relevantChanges_emitOnce_irrelevantOnesDont() = runTest(UnconfinedTestDispatcher()) {
        val inputs = MutableStateFlow(IslandInputs(media = song))
        val settings = MutableStateFlow(IslandSettings(showNotifications = true))
        val h = holder(inputs, settings)
        val seen = ArrayList<IslandScene>()
        backgroundScope.launch { h.scene.collect { seen += it } }
        runCurrent()
        assertEquals(1, seen.size)
        assertEquals(IslandPriority.MEDIA, seen.last().primary)

        // A position tick and a new art bitmap: same scene, nothing emitted
        inputs.value = inputs.value.copy(media = song.copy(positionMs = 2_000))
        inputs.value = inputs.value.copy(media = song.copy(positionMs = 3_000, artist = "Other"))
        runCurrent()
        assertEquals(1, seen.size)

        // A message arrives: dual, emitted once
        inputs.value = inputs.value.copy(notification = message)
        runCurrent()
        assertEquals(2, seen.size)
        assertEquals(IslandMode.DUAL, seen.last().mode)

        // A settings change that changes nothing shown: no emission
        settings.value = settings.value.copy(spokenAlerts = true)
        runCurrent()
        assertEquals(2, seen.size)

        // One that does: messages off, back to compact
        settings.value = settings.value.copy(showNotifications = false)
        runCurrent()
        assertEquals(3, seen.size)
        assertEquals(IslandMode.COMPACT, seen.last().mode)
    }

    @Test
    fun startsWithTheCurrentScene() = runTest(UnconfinedTestDispatcher()) {
        val h = holder(MutableStateFlow(IslandInputs(media = song, isExpanded = true)), MutableStateFlow(IslandSettings()))
        assertEquals(IslandMode.EXPANDED, h.scene.value.mode)
    }
}
