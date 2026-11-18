package com.droidbubble.app.island.activity.controls

import android.app.PendingIntent

/**
 * A4.1: what the resolver knows beyond the action itself.
 *
 * [answer], [decline] and [hangUp] are the CallStyle extras (`android.answerIntent` …). The
 * system builds those buttons itself, so an action whose intent is one of them is that role.
 * [titleTable] is step (d): A4.2 swaps in the per-locale tables; until then it is English only.
 */
data class ResolveContext(
    val isCall: Boolean = false,
    val answer: PendingIntent? = null,
    val decline: PendingIntent? = null,
    val hangUp: PendingIntent? = null,
    val titleTable: (String) -> ControlRole? = EnglishTitleTable::match,
)

/**
 * A4.1: a button's role, from the most to the least reliable signal:
 * (a) structured extras (CallStyle intents, a RemoteInput),
 * (b) `Action.getSemanticAction()`,
 * (c) the icon's resource entry name in the posting package,
 * (d) the title, through the locale tables.
 * Pure: no Android calls, so the JVM tests and the device agree.
 */
object ControlResolver {

    // Notification.Action.SEMANTIC_ACTION_* (API 28). Copied as constants so this stays pure.
    const val SEMANTIC_REPLY = 1
    const val SEMANTIC_MARK_AS_READ = 2
    const val SEMANTIC_DELETE = 4
    const val SEMANTIC_MUTE = 6
    const val SEMANTIC_UNMUTE = 7
    const val SEMANTIC_CALL = 10

    fun resolve(a: ActionSnapshot, ctx: ResolveContext = ResolveContext()): ControlRole =
        structured(a, ctx)
            ?: semantic(a.semanticAction, ctx)
            ?: a.iconResName?.let(::fromIconName)
            ?: a.title.takeIf { it.isNotBlank() }?.let(ctx.titleTable)
            ?: ControlRole.UNKNOWN

    private fun structured(a: ActionSnapshot, ctx: ResolveContext): ControlRole? {
        val intent = a.intent
        if (intent != null) {
            if (intent == ctx.answer) return ControlRole.ANSWER
            if (intent == ctx.hangUp) return ControlRole.HANG_UP
            if (intent == ctx.decline) return ControlRole.DECLINE
        }
        return if (a.hasRemoteInput) ControlRole.REPLY else null
    }

    internal fun semantic(semanticAction: Int, ctx: ResolveContext): ControlRole? = when (semanticAction) {
        SEMANTIC_REPLY -> ControlRole.REPLY
        SEMANTIC_MARK_AS_READ -> ControlRole.MARK_READ
        SEMANTIC_DELETE -> ControlRole.DISMISS
        SEMANTIC_MUTE -> ControlRole.MUTE
        SEMANTIC_UNMUTE -> ControlRole.UNMUTE
        // "Call" places a call: on a call notification that's picking it up; elsewhere ("Call
        // back" on a missed call) it isn't a call control, so the later steps decide
        SEMANTIC_CALL -> if (ctx.isCall) ControlRole.ANSWER else null
        else -> null
    }

    /**
     * Icon entry names look like "ic_pause_white_24dp" or "quantum_ic_call_end_vd_theme_24".
     * A token must stand on its own (no letter on either side), so "display" isn't "play" and
     * "stopwatch" isn't "stop". Order matters: "unmute" before "mute", "call_end" before "call".
     */
    private val ICON_TOKENS: List<Pair<Regex, ControlRole>> = listOf(
        "call_end|hangup|hang_up|end_call" to ControlRole.HANG_UP,
        "call_decline|decline|reject" to ControlRole.DECLINE,
        "call_answer|answer|accept" to ControlRole.ANSWER,
        "unmute|mic_on" to ControlRole.UNMUTE,
        "mic_off|mute|mic_mute" to ControlRole.MUTE,
        "volume|speaker|speakerphone" to ControlRole.SPEAKER,
        "pause" to ControlRole.PAUSE,
        "play|resume|play_arrow" to ControlRole.RESUME,
        "stop" to ControlRole.STOP,
        "snooze|alarm_off" to ControlRole.SNOOZE,
        "add|plus" to ControlRole.ADD_TIME,
        "close|cancel|clear" to ControlRole.CANCEL,
        "reply" to ControlRole.REPLY,
    ).map { (alts, role) -> Regex("""(?<![a-z])(?:$alts)(?![a-z])""") to role }

    fun fromIconName(name: String): ControlRole? {
        val n = name.lowercase()
        return ICON_TOKENS.firstOrNull { (re, _) -> re.containsMatchIn(n) }?.second
    }
}

/**
 * A4.1 step (d) until A4.2: the English words today's regexes already use
 * (`LiveActivities.kt` PAUSE_RESUME / STOP / ALERT_*; NRS answer and decline words). Boundaries
 * are explicit so the JVM and Android's ICU regex agree.
 */
object EnglishTitleTable {
    private fun words(vararg w: String) =
        Regex("""(?<![\p{L}\p{N}])(?:${w.joinToString("|")})(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

    private val TABLE: List<Pair<Regex, ControlRole>> = listOf(
        words("hang up", "end call") to ControlRole.HANG_UP,
        words("answer", "accept") to ControlRole.ANSWER,
        words("decline", "reject") to ControlRole.DECLINE,
        words("unmute") to ControlRole.UNMUTE,
        words("mute") to ControlRole.MUTE,
        words("speaker") to ControlRole.SPEAKER,
        words("pause") to ControlRole.PAUSE,
        words("resume", "continue", "play", "start") to ControlRole.RESUME,
        words("snooze") to ControlRole.SNOOZE,
        Regex("""\+\s?\d|(?<![\p{L}\p{N}])(?:add|minutes?|mins?)(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE) to ControlRole.ADD_TIME,
        words("reply") to ControlRole.REPLY,
        words("mark as read") to ControlRole.MARK_READ,
        words("stop", "end", "finish", "save", "done") to ControlRole.STOP,
        words("cancel", "delete", "discard", "reset") to ControlRole.CANCEL,
        words("dismiss", "turn off") to ControlRole.DISMISS,
        words("open") to ControlRole.OPEN,
    )

    fun match(title: String): ControlRole? = TABLE.firstOrNull { (re, _) -> re.containsMatchIn(title) }?.second
}
