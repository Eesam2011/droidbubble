package com.droidbubble.app.island.activity

import com.droidbubble.app.island.activity.controls.ControlRole
import com.droidbubble.app.island.activity.controls.ControlRole.*
import com.droidbubble.app.island.activity.controls.LocaleTables
import com.droidbubble.app.island.activity.controls.LocaleTables.StateWord
import com.droidbubble.app.island.activity.text.Digits
import com.droidbubble.app.model.parseTimerTextMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A4.2: every fixture button title resolves its role; digits and Arabic forms normalise. Pure JVM. */
class LocaleTablesTest {

    /** Fixture → (button title → role). Every title of every non-English fixture with buttons. */
    internal val fixtureRoles: Map<String, Map<String, ControlRole?>> = mapOf(
        "call/samsung_incoming_tr" to mapOf("Reddet" to DECLINE, "Yanıtla" to ANSWER),
        "call/whatsapp_incoming_ar" to mapOf("رفض" to DECLINE, "رد" to ANSWER),
        "call/whatsapp_video_es" to mapOf("Rechazar" to DECLINE, "Responder" to ANSWER),
        "clock/gclock_alarm_ringing_es" to mapOf("Posponer" to SNOOZE, "Descartar" to DISMISS),
        "clock/gclock_timer_paused_de" to mapOf("Fortsetzen" to RESUME),
        "clock/gclock_timesup_fr" to mapOf("+1:00" to ADD_TIME, "Arrêter" to STOP),
        "clock/samsung_timer_ar" to mapOf("إيقاف مؤقت" to PAUSE, "إلغاء" to CANCEL),
        "message/whatsapp_ar" to mapOf("رد" to REPLY, "تمييز كمقروء" to MARK_READ),
        "progress/chrome_download_hi" to mapOf("रोकें" to PAUSE, "रद्द करें" to CANCEL),
        "recorder/miui_recorder_zh" to mapOf("暂停" to PAUSE, "停止" to STOP),
        "recorder/samsung_voicenote_ru" to mapOf("Пауза" to PAUSE, "Стоп" to STOP),
        "screenrec/samsung_es" to mapOf("Pausar" to PAUSE, "Detener" to STOP),
        "screenrec/xiaomi_id" to mapOf("Berhenti" to STOP),
        // Not a control the island uses; must not be mistaken for one
        "nav/gmaps_turn_ar" to mapOf("الخروج من التنقل" to null),
    )

    /** The fixtures whose buttons belong to a call (category "call"). */
    internal val callFixtures = setOf("call/samsung_incoming_tr", "call/whatsapp_incoming_ar", "call/whatsapp_video_es")

    /** Same results on the plain JVM here and under Robolectric (LocaleTablesFixtureTest). */
    @Test
    fun everyFixtureButton_resolves() {
        for ((name, roles) in fixtureRoles) {
            for ((title, role) in roles) assertEquals("$name: $title", role, LocaleTables.match(title, name in callFixtures))
        }
    }

    @Test
    fun answerOrReply_dependsOnTheCall() {
        assertEquals(ANSWER, LocaleTables.match("رد", isCall = true))
        assertEquals(REPLY, LocaleTables.match("رد", isCall = false))
        assertEquals(ANSWER, LocaleTables.match("Responder", isCall = true))
        assertEquals(REPLY, LocaleTables.match("Responder", isCall = false))
    }

    @Test
    fun negatives() {
        listOf("Send", "Weekend", "Ende", "Display", "Recommend", "Settings", "Record", "Lap", "Archive", "Call back")
            .forEach { assertNull(it, LocaleTables.match(it)) }
        // A word inside another word never counts
        assertNull(LocaleTables.match("Stopwatch"))
        assertNull(LocaleTables.match("Pausenzeit"))
    }

    @Test
    fun englishTitles_unchanged() {
        mapOf(
            "Answer" to ANSWER, "Decline" to DECLINE, "Hang up" to HANG_UP, "Mute" to MUTE, "Speaker" to SPEAKER,
            "Pause" to PAUSE, "Resume" to RESUME, "Stop" to STOP, "Cancel" to CANCEL, "Reset" to CANCEL,
            "Snooze" to SNOOZE, "+1:00" to ADD_TIME, "Dismiss" to DISMISS, "Reply" to REPLY, "Mark as read" to MARK_READ,
        ).forEach { (t, r) -> assertEquals(t, r, LocaleTables.match(t)) }
    }

    @Test
    fun arabicForms_unify() {
        // Without the hamza, or with tashkeel, it is still the same word
        assertEquals(CANCEL, LocaleTables.match("الغاء"))
        assertEquals(PAUSE, LocaleTables.match("ايقاف مؤقت"))
        assertEquals(DECLINE, LocaleTables.match("رَفْض"))
    }

    @Test
    fun stateWords() {
        assertTrue(LocaleTables.has(StateWord.PAUSED, "Pausiert"))
        assertTrue(LocaleTables.has(StateWord.TIMES_UP, "Le temps est écoulé"))
        assertTrue(LocaleTables.has(StateWord.SCREEN_RECORDING, "Grabando pantalla"))
        assertTrue(LocaleTables.has(StateWord.SCREEN_RECORDING, "Merekam layar"))
        assertTrue(LocaleTables.has(StateWord.RECORDING, "正在录音"))
        assertTrue(LocaleTables.has(StateWord.RECORDING, "Запись"))
        assertTrue(LocaleTables.has(StateWord.TIMER, "المؤقت"))
        assertTrue(LocaleTables.has(StateWord.ALARM, "Alarma"))
        assertFalse(LocaleTables.has(StateWord.RECORDING, "Screen Recorder"))
        assertFalse(LocaleTables.has(StateWord.PAUSED, null))
    }

    // ---- Digits ----

    @Test
    fun digits() {
        assertEquals("00:42", Digits.normalizeDigits("٠٠:٤٢"))
        assertEquals("15", Digits.normalizeDigits("۱۵"))
        assertEquals(42_000L, parseTimerTextMillis(Digits.normalizeDigits("٠٠:٤٢")))
        assertEquals(200, Digits.parseMeters("٢٠٠ م"))
        assertEquals(200, Digits.parseMeters("200 m"))
        assertEquals(350, Digits.parseMeters("In 350 m turn left"))
        assertEquals(1500, Digits.parseMeters("1,5 km"))
        assertNull(Digits.parseMeters("12 MB of 40 MB"))
        assertEquals("unchanged", Digits.normalize("unchanged"))
    }
}
