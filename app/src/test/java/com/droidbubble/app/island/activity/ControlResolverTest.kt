package com.droidbubble.app.island.activity

import com.droidbubble.app.island.activity.controls.ActionSnapshot
import com.droidbubble.app.island.activity.controls.ControlResolver
import com.droidbubble.app.island.activity.controls.ControlRole
import com.droidbubble.app.island.activity.controls.ControlRole.*
import com.droidbubble.app.island.activity.controls.ResolveContext
import org.junit.Assert.assertEquals
import org.junit.Test

/** A4.1: structured → semantic → icon name → title, and the title is never needed when the rest is there. */
class ControlResolverTest {

    private fun role(title: String, semantic: Int = 0, icon: String? = null, reply: Boolean = false, call: Boolean = false) =
        ControlResolver.resolve(ActionSnapshot(title, semantic, icon, reply), ResolveContext(isCall = call))

    @Test
    fun semanticActionWinsOverAnyTitle() {
        // Titles in other languages, or nonsense: the semantic action alone decides
        assertEquals(MUTE, role("Silenciar", semantic = ControlResolver.SEMANTIC_MUTE))
        assertEquals(UNMUTE, role("كتم", semantic = ControlResolver.SEMANTIC_UNMUTE))
        assertEquals(REPLY, role("Antworten", semantic = ControlResolver.SEMANTIC_REPLY))
        assertEquals(MARK_READ, role("既読", semantic = ControlResolver.SEMANTIC_MARK_AS_READ))
        assertEquals(DISMISS, role("Sil", semantic = ControlResolver.SEMANTIC_DELETE))
    }

    @Test
    fun callSemanticIsAnswerOnlyOnACall() {
        assertEquals(ANSWER, role("Contestar", semantic = ControlResolver.SEMANTIC_CALL, call = true))
        // "Call back" on a missed call is not an answer button; with no other signal it's unknown
        assertEquals(UNKNOWN, role("Devolver llamada", semantic = ControlResolver.SEMANTIC_CALL))
    }

    @Test
    fun remoteInputIsReply() {
        assertEquals(REPLY, role("Responder", reply = true))
    }

    @Test
    fun iconNamesWithoutTitles() {
        val cases = mapOf(
            "ic_pause_white_24dp" to PAUSE,
            "quantum_ic_pause_vd_theme_24" to PAUSE,
            "ic_play_arrow_24" to RESUME,
            "ic_resume" to RESUME,
            "ic_stop_24" to STOP,
            "ic_close" to CANCEL,
            "ic_cancel_24dp" to CANCEL,
            "ic_clear_all" to CANCEL,
            "ic_snooze" to SNOOZE,
            "ic_alarm_off_24" to SNOOZE,
            "ic_add_24" to ADD_TIME,
            "ic_plus_one" to ADD_TIME,
            "ic_mic_off_24" to MUTE,
            "ic_mute" to MUTE,
            "ic_unmute" to UNMUTE,
            "ic_volume_up" to SPEAKER,
            "ic_speaker_phone" to SPEAKER,
            "quantum_ic_call_end_vd_theme_24" to HANG_UP,
            "ic_hangup" to HANG_UP,
            "ic_call_answer" to ANSWER,
            "ic_call_decline" to DECLINE,
            "ic_reply" to REPLY,
        )
        cases.forEach { (icon, expected) -> assertEquals(icon, expected, role("…", icon = icon)) }
    }

    @Test
    fun iconTokensNeedBoundaries() {
        // "display" isn't play, "stopwatch" isn't stop, "address" isn't add: fall through to the title
        assertEquals(UNKNOWN, role("…", icon = "ic_display_settings"))
        assertEquals(UNKNOWN, role("…", icon = "ic_stopwatch"))
        assertEquals(UNKNOWN, role("…", icon = "ic_address_book"))
        assertEquals(PAUSE, role("Pause", icon = "ic_stopwatch"))
    }

    @Test
    fun iconBeatsTitle() {
        // A recorder whose button says "Save" but shows a stop square is a stop
        assertEquals(STOP, role("Save", icon = "ic_stop"))
        assertEquals(PAUSE, role("Resume", icon = "ic_pause"))
    }

    @Test
    fun englishTitlesAsLastResort() {
        val cases = mapOf(
            "Answer" to ANSWER, "Accept" to ANSWER, "Decline" to DECLINE, "Reject" to DECLINE,
            "Hang up" to HANG_UP, "End call" to HANG_UP, "Mute" to MUTE, "Unmute" to UNMUTE,
            "Speaker" to SPEAKER, "Pause" to PAUSE, "Resume" to RESUME, "Continue" to RESUME,
            "Stop" to STOP, "Finish" to STOP, "Cancel" to CANCEL, "Delete" to CANCEL,
            "Snooze" to SNOOZE, "+1:00" to ADD_TIME, "Add 1 min" to ADD_TIME, "Dismiss" to DISMISS,
            "Turn off" to DISMISS, "Reply" to REPLY, "Mark as read" to MARK_READ, "Open" to OPEN,
        )
        cases.forEach { (title, expected) -> assertEquals(title, expected, role(title)) }
    }

    @Test
    fun englishTitleBoundaries() {
        // B4.3 style: whole words only
        assertEquals(UNKNOWN, role("Send"))
        assertEquals(UNKNOWN, role("Weekend"))
        assertEquals(UNKNOWN, role("Display"))
        assertEquals(UNKNOWN, role("Ende")) // German, waits for A4.2
    }

    @Test
    fun structuredCallIntentsAreChecked() {
        // Without real PendingIntents (pure JVM) the structured step is skipped, never guessed
        val ctx = ResolveContext(isCall = true)
        assertEquals(ControlRole.UNKNOWN, ControlResolver.resolve(ActionSnapshot("رد"), ctx))
    }
}
