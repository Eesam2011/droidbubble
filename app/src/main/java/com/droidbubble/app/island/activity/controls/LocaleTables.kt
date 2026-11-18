package com.droidbubble.app.island.activity.controls

import com.droidbubble.app.island.activity.text.Digits
import java.util.Locale

/**
 * A4.2: button titles and state words in the languages the S5 fixtures cover. Step (d) of
 * ControlResolver, after the structured, semantic and icon signals.
 *
 * Every non-English token here is backed by a captured fixture (the file in the comment); none is
 * a guessed translation. Add a word only together with a fixture that shows it.
 *
 * Matching: the text is normalised first (Digits.normalize: ASCII digits, Arabic tashkeel
 * stripped, one alef), lower-cased with Locale.ROOT. A token matches as a whole word: no letter or
 * digit on either side, written out (`(?<![\p{L}\p{N}])…(?![\p{L}\p{N}])`) so the JVM and
 * Android's ICU regex agree. Chinese tokens match as plain substrings (no spaces between words).
 */
object LocaleTables {

    /** Words that mean "answer" on a call and "reply" on a message. */
    private val ANSWER_OR_REPLY = listOf(
        "رد",        // call/whatsapp_incoming_ar (answer), message/whatsapp_ar (reply)
        "responder", // call/whatsapp_video_es (answer)
    )

    private val ROLES: List<Pair<ControlRole, List<String>>> = listOf(
        ControlRole.HANG_UP to listOf("hang up", "end call"),
        ControlRole.ANSWER to listOf(
            "answer", "accept",
            "yanıtla", // call/samsung_incoming_tr
        ),
        ControlRole.DECLINE to listOf(
            "decline", "reject",
            "rechazar", // call/whatsapp_video_es
            "reddet",   // call/samsung_incoming_tr
            "رفض",      // call/whatsapp_incoming_ar
        ),
        ControlRole.UNMUTE to listOf("unmute"),
        ControlRole.MUTE to listOf("mute"),
        ControlRole.SPEAKER to listOf("speaker"),
        ControlRole.PAUSE to listOf(
            "pause",
            "pausar",       // screenrec/samsung_es
            "пауза",        // recorder/samsung_voicenote_ru
            "ايقاف مؤقت",   // clock/samsung_timer_ar ("إيقاف مؤقت")
            "रोकें",          // progress/chrome_download_hi
            "暂停",          // recorder/miui_recorder_zh
        ),
        ControlRole.RESUME to listOf(
            "resume", "continue", "play", "start",
            "fortsetzen",   // clock/gclock_timer_paused_de
        ),
        ControlRole.SNOOZE to listOf(
            "snooze",
            "posponer",     // clock/gclock_alarm_ringing_es
        ),
        ControlRole.REPLY to listOf("reply"),
        ControlRole.MARK_READ to listOf(
            "mark as read",
            "تمييز كمقروء", // message/whatsapp_ar
        ),
        ControlRole.STOP to listOf(
            "stop", "end", "finish", "save", "done",
            "arrêter",      // clock/gclock_timesup_fr
            "detener",      // screenrec/samsung_es
            "стоп",         // recorder/samsung_voicenote_ru
            "berhenti",     // screenrec/xiaomi_id
            "停止",          // recorder/miui_recorder_zh
        ),
        ControlRole.CANCEL to listOf(
            "cancel", "delete", "discard", "reset",
            "الغاء",        // clock/samsung_timer_ar ("إلغاء")
            "रद्द करें",       // progress/chrome_download_hi
        ),
        ControlRole.DISMISS to listOf(
            "dismiss", "turn off",
            "descartar",    // clock/gclock_alarm_ringing_es
        ),
        ControlRole.OPEN to listOf("open"),
    )

    // "+1:00" (en, fr fixtures), "Add 1 min"
    private val ADD_TIME = Regex("""\+\s?[0-9]|(?<![\p{L}\p{N}])(?:add|minutes?|mins?)(?![\p{L}\p{N}])""")

    private fun isCjk(c: Char) = c in '一'..'鿿' || c in '぀'..'ヿ'

    private fun matcher(token: String): (String) -> Boolean {
        val t = Digits.normalize(token).lowercase(Locale.ROOT)
        if (t.any(::isCjk)) return { text -> t in text }
        val re = Regex("""(?<![\p{L}\p{N}])${Regex.escape(t)}(?![\p{L}\p{N}])""")
        return { text -> re.containsMatchIn(text) }
    }

    private val answerOrReply = ANSWER_OR_REPLY.map(::matcher)

    /** In order: the first role that matches wins. ADD_TIME sits after SNOOZE, as in English. */
    private val roleMatchers: List<Pair<ControlRole, (String) -> Boolean>> = buildList {
        for ((role, tokens) in ROLES) {
            val ms = tokens.map(::matcher)
            add(role to { t: String -> ms.any { it(t) } })
            if (role == ControlRole.SNOOZE) add(ControlRole.ADD_TIME to { t: String -> ADD_TIME.containsMatchIn(t) })
        }
    }

    fun normalized(text: String): String = Digits.normalize(text).lowercase(Locale.ROOT)

    /** The role a button title names, in any covered language, or null. [isCall] settles "رد" and "Responder". */
    fun match(title: String, isCall: Boolean = false): ControlRole? {
        val t = normalized(title)
        val ambiguousRole = if (answerOrReply.any { it(t) }) (if (isCall) ControlRole.ANSWER else ControlRole.REPLY) else null
        for ((role, matches) in roleMatchers) {
            if (role == ambiguousRole || matches(t)) return role
        }
        return null
    }

    /** State words the kind and state rules look for (A4.3 / A4.4 wire them). */
    enum class StateWord(val tokens: List<String>) {
        PAUSED(listOf(
            "paused",
            "pausiert",          // clock/gclock_timer_paused_de
        )),
        TIMES_UP(listOf(
            "time's up", "time’s up", "time is up",
            "le temps est écoulé", // clock/gclock_timesup_fr
        )),
        SCREEN_RECORDING(listOf(
            "screen recording", "recording screen",
            "grabando pantalla", // screenrec/samsung_es
            "merekam layar",     // screenrec/xiaomi_id
        )),
        RECORDING(listOf(
            "recording",
            "запись",            // recorder/samsung_voicenote_ru
            "正在录音",            // recorder/miui_recorder_zh
        )),
        TIMER(listOf(
            "timer",
            "المؤقت",            // clock/samsung_timer_ar
        )),
        ALARM(listOf(
            "alarm",
            "alarma",            // clock/gclock_alarm_ringing_es
        )),
        STOPWATCH(listOf("stopwatch")),
        SNOOZE(listOf("snooze", "snoozing", "snoozed")),
    }

    private val stateMatchers: Map<StateWord, List<(String) -> Boolean>> =
        StateWord.entries.associateWith { w -> w.tokens.map(::matcher) }

    fun has(word: StateWord, text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val t = normalized(text)
        return stateMatchers.getValue(word).any { it(t) }
    }
}
