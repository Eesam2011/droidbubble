package com.droidbubble.app.island

import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.parseTimerTextMillis
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Which native live-activity design a notification gets (or NONE for the regular card). */
enum class LiveActivityKind { CALL, ALERT, RECORDING, TIMER, STOPWATCH, ALARM, TRACKING, SPORTS, NONE }

// Substrings of package names. Generic words catch most apps; the explicit OEM names below are
// the ones whose package doesn't contain an obvious word (vivo "bbkclock", ColorOS "alarmclock"...)
private val RECORDER_PACKAGE_HINTS = listOf(
    "recorder", "soundrecord", "voicerecord", "audiorecord", "voicenote", "voicememo", "dictaphone", "voice_record",
    "recordings", "smartrecord"
)
private val KNOWN_RECORDER_PACKAGES = setOf(
    "com.android.soundrecorder",        // Xiaomi / Redmi / POCO (MIUI, HyperOS), AOSP
    "com.miui.soundrecorder",
    "com.oneplus.soundrecorder",        // OnePlus (OxygenOS)
    "com.coloros.soundrecorder",        // Oppo / Realme (ColorOS)
    "com.oplus.soundrecorder",
    "com.realme.soundrecorder",
    "com.android.bbksoundrecorder",     // vivo / iQOO
    "com.vivo.soundrecorder",
    "com.huawei.soundrecorder",
    "com.hihonor.soundrecorder",
    "com.motorola.audiorecorder",
    "com.asus.soundrecorder",
    "com.sec.android.app.voicenote",    // Samsung
    "com.google.android.apps.recorder"  // Pixel
)
private val CLOCK_PACKAGE_HINTS = listOf("deskclock", "clockpackage", "alarmclock", ".clock", "timer", "bbkclock", "worldclock")
private val KNOWN_CLOCK_PACKAGES = setOf(
    "com.android.deskclock",            // Xiaomi / AOSP
    "com.google.android.deskclock",     // Pixel / Google Clock
    "com.oneplus.deskclock",            // OnePlus
    "com.coloros.alarmclock",           // Oppo / Realme
    "com.oplus.alarmclock",
    "com.android.bbkclock",             // vivo
    "com.huawei.deskclock",
    "com.hihonor.deskclock",
    "com.sec.android.app.clockpackage", // Samsung
    "com.asus.deskclock"
)
private val TRACKING_PACKAGE_HINTS = listOf(
    // rides
    "uber", "lyft", "careem", "bolt", "didi", "olacabs", "grab", "rapido", "indriver", "cabify", "freenow",
    "yandex.taxi", "gojek", "blablacar",
    // food / grocery delivery
    "doordash", "deliveroo", "talabat", "swiggy", "zomato", "instacart", "glovo", "wolt", "hungerstation",
    "jahez", "keeta", "postmates", "grubhub", "foodpanda", "justeat", "menulog",
    // flights
    "flightradar", "flighty", "tripit", "flightaware", "emirates", "qatarairways", "etihad", "saudia",
    "flydubai", "ryanair", "easyjet", "lufthansa", "turkishairlines", "britishairways", "delta", "united",
    "aa.android", "airfrance", "klm", "wizzair", "airasia",
    // parcels
    "parcel", "aftership", "17track"
)
private val SPORTS_PACKAGE_HINTS = listOf(
    "espn", "fotmob", "mobilefootball", "onefootball", "sofascore", "livescore", "thescore", "sportacular",
    "bbc.sport", "forzafootball", "365scores", "scores365", "flashscore", "cricbuzz", "cricinfo", "nba", "nfl",
    "mlb", "premierleague", "uefa", "fifa"
)
private val PAUSE_RESUME_REGEX = Regex("""\b(pause|resume|continue|play|start)\b""", RegexOption.IGNORE_CASE)
private val RESUME_REGEX = Regex("""\b(resume|continue|play|start)\b""", RegexOption.IGNORE_CASE)
private val STOP_REGEX = Regex("""\b(stop|end|finish|save|done|cancel|delete|dismiss|reset|discard|hang up)\b""", RegexOption.IGNORE_CASE)
private val ALERT_STOP_REGEX = Regex("""\b(stop|dismiss|turn off|cancel|done)\b""", RegexOption.IGNORE_CASE)
private val ALERT_EXTRA_REGEX = Regex("""(\+\s?\d|\badd\b|\bsnooze\b|\bminute|\bmin\b)""", RegexOption.IGNORE_CASE)
private val CLOCK_TIME_REGEX = Regex("""\b\d{1,2}:\d{2}(\s?[AaPp]\.?[Mm]\.?)?""")
private val ETA_REGEX = Regex("""\b(\d{1,3})\s?(min|mins|minutes)\b""", RegexOption.IGNORE_CASE)
// "Arsenal 2 - 1 Chelsea", "ARS 2–1 CHE", "Lakers 98 : 101 Celtics" (a colon only from a sports app)
private val SCORE_REGEX = Regex("""^\s*(.+?)\s+(\d{1,3})\s*([-–—:])\s*(\d{1,3})\s+(.+?)\s*$""")
// A clock time, not a score: "10:45 PM", "10:30 p.m.", "18:00 h"
private val TIME_SUFFIX_REGEX = Regex("""^(a\.?m\b\.?|p\.?m\b\.?|h\b)""", RegexOption.IGNORE_CASE)
// Words that sit next to a time, never a team: "Arrive at 10-11", "Boarding until 10-30 gate B"
private val NOT_A_TEAM = setOf("at", "by", "from", "until", "gate")

private fun String.hasAny(hints: List<String>) = hints.any { it in this }

fun isRecorderPackage(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    return pkg in KNOWN_RECORDER_PACKAGES || pkg.hasAny(RECORDER_PACKAGE_HINTS)
}

fun isClockPackage(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    return pkg in KNOWN_CLOCK_PACKAGES || pkg.hasAny(CLOCK_PACKAGE_HINTS)
}

/** Clock (timer / stopwatch / alarm) or voice-recorder app, judged by package name. */
fun isClockOrRecorderPackage(packageName: String): Boolean = isRecorderPackage(packageName) || isClockPackage(packageName)

/** Ride-share, delivery, flight or parcel tracking app, judged by package name. */
fun isTrackingPackage(packageName: String): Boolean = packageName.lowercase().hasAny(TRACKING_PACKAGE_HINTS)

/** Live-score app, judged by package name. (The Google app is not one: B4.1.) */
fun isSportsPackage(packageName: String): Boolean = packageName.lowercase().hasAny(SPORTS_PACKAGE_HINTS)

/** A parsed live score. */
data class LiveScore(val home: String, val homeScore: String, val awayScore: String, val away: String, val status: String?)

fun parseLiveScore(n: NotificationInfo): LiveScore? {
    val colonOk = isSportsPackage(n.packageName)
    return matchScore(n.title, colonOk, status = n.text) ?: matchScore(n.text, colonOk, status = n.title)
}

/**
 * B4.1: a score is "team N - M team". The separator is a hyphen or a dash; a colon only from a
 * sports app, and never before AM / PM / h. Both teams need a letter, and a word that sits next to
 * a time ("at", "by", "from", "until", "gate") can't border the numbers.
 */
private fun matchScore(line: String, colonOk: Boolean, status: String): LiveScore? {
    val m = SCORE_REGEX.matchEntire(line) ?: return null
    val (home, x, sep, y, away) = m.destructured
    if (sep == ":" && !colonOk) return null
    if (TIME_SUFFIX_REGEX.containsMatchIn(away.trimStart())) return null
    if (home.none(Char::isLetter) || away.none(Char::isLetter)) return null
    val homeEdge = home.trim().substringAfterLast(' ').lowercase()
    val awayEdge = away.trim().substringBefore(' ').lowercase()
    if (homeEdge in NOT_A_TEAM || awayEdge in NOT_A_TEAM) return null
    return LiveScore(home.trim(), x, y, away.trim(), status.takeIf { it.isNotBlank() })
}

/** Wall-clock time an alarm is set for, as printed ("7:00 AM"). */
fun alarmTimeText(n: NotificationInfo): String? =
    CLOCK_TIME_REGEX.find(n.text)?.value ?: CLOCK_TIME_REGEX.find(n.title)?.value

/** Short status for a ride / delivery / flight: the app's own chip, an ETA, or the progress. */
fun trackingChipText(n: NotificationInfo): String? =
    n.shortCriticalText?.takeIf { it.isNotBlank() }
        ?: ETA_REGEX.find(n.title)?.let { "${it.groupValues[1]} min" }
        ?: ETA_REGEX.find(n.text)?.let { "${it.groupValues[1]} min" }
        ?: n.progressFraction.takeIf { n.maxProgress > 0 && n.progress >= 0 }?.let { "${(it * 100).toInt()}%" }

/** Whether a ringing alert is an expired timer (vs an alarm). */
fun isTimerAlert(n: NotificationInfo): Boolean {
    val words = "${n.title} ${n.text}".lowercase()
    return "timer" in words || "time's up" in words || "time’s up" in words || "time is up" in words
}

/** Buttons of a ringing alert: Stop / Dismiss, and +1:00 / Snooze. */
data class AlertControls(val stop: NotificationAction?, val extra: NotificationAction?)

fun alertControls(n: NotificationInfo): AlertControls {
    val extra = n.actions.firstOrNull { ALERT_EXTRA_REGEX.containsMatchIn(it.title) }
    val stop = n.actions.firstOrNull { it !== extra && ALERT_STOP_REGEX.containsMatchIn(it.title) }
        ?: n.actions.firstOrNull { it !== extra }
    return AlertControls(stop, extra)
}

/**
 * Picks the native design for [n]: calls, alerts, voice recorders, timers, stopwatches, alarms,
 * rides / deliveries / flights and live scores get their own cards (like iOS live activities);
 * everything else uses the regular notification card.
 */
fun liveActivityKind(n: NotificationInfo?): LiveActivityKind {
    if (n == null) return LiveActivityKind.NONE
    if (n.isCall) return LiveActivityKind.CALL
    if (n.isAlert) return LiveActivityKind.ALERT
    val words = "${n.appName} ${n.title} ${n.text}".lowercase()
    val isRecorderApp = isRecorderPackage(n.packageName)
    val isClockApp = isClockPackage(n.packageName) || "clock" in n.appName.lowercase()
    val hasStaticTime = parseTimerTextMillis(n.title) != null || parseTimerTextMillis(n.text) != null
    // Running (live timer), or a paused recorder / timer that is still an ongoing activity
    val isLive = n.liveTimerBaseMs > 0L || (n.isOngoing && (hasStaticTime || isRecorderApp || isClockApp))
    if (isLive) {
        val timed = when {
            !n.liveTimerCountDown && (isRecorderApp || "recording" in words || "recorder" in words) -> LiveActivityKind.RECORDING
            n.liveTimerCountDown -> LiveActivityKind.TIMER
            "stopwatch" in words -> LiveActivityKind.STOPWATCH
            "timer" in words || "time's up" in words -> LiveActivityKind.TIMER
            isClockApp && ("alarm" in words || "snooz" in words) -> LiveActivityKind.ALARM
            isClockApp -> LiveActivityKind.STOPWATCH
            else -> null
        }
        if (timed != null) return timed
    }
    // Upcoming / snoozed alarm from a clock app
    if (isClockApp && ("alarm" in words || "snooz" in words) && alarmTimeText(n) != null) return LiveActivityKind.ALARM
    // B4.1: a tracking app's live update is tracking before anything parses as a score. A promoted
    // notification from any other app can still be a live score.
    val trackingApp = isTrackingPackage(n.packageName)
    if (trackingApp && (n.isOngoing || n.isPromoted)) return LiveActivityKind.TRACKING
    if ((isSportsPackage(n.packageName) || n.isOngoing || n.isPromoted) && parseLiveScore(n) != null) return LiveActivityKind.SPORTS
    if (n.isPromoted) return LiveActivityKind.TRACKING
    return LiveActivityKind.NONE
}

/**
 * B3.1: the toggle that governs a notification in the island's notification slot. Plain
 * downloads already went to the progress card, so a live activity with a bar counts as LIVE.
 */
fun notificationSlotGate(n: NotificationInfo): com.droidbubble.app.model.NotificationGateKind =
    com.droidbubble.app.model.gateKind(
        isCall = n.isCall,
        isLive = n.isLiveActivity || liveActivityKind(n) != LiveActivityKind.NONE,
        hasProgress = false,
    )

/** Remembers each countdown's full length so the pill can draw how much is left as a ring. */
object CountdownTotals {
    private val totals = HashMap<String, Long>()

    /** Total for [key]; grows when the timer gets more time (+1:00) than it had at the start. */
    @Synchronized
    fun totalFor(key: String, remainingMs: Long): Long {
        if (totals.size > 64) totals.clear()
        val prev = totals[key]
        val total = if (prev == null || remainingMs > prev + 1_500L) remainingMs else prev
        totals[key] = total
        return total.coerceAtLeast(1L)
    }

    /** Tests only: a process singleton, so one test's timer total would leak into the next. */
    @Synchronized
    @androidx.annotation.VisibleForTesting
    fun resetForTest() = totals.clear()
}

/** The notification's own buttons mapped onto the live-activity controls. */
data class LiveActivityControls(
    val pauseResume: NotificationAction?,
    val isPaused: Boolean,
    val stop: NotificationAction?
)

fun liveActivityControls(n: NotificationInfo): LiveActivityControls {
    val pauseResume = n.actions.firstOrNull { PAUSE_RESUME_REGEX.containsMatchIn(it.title) }
    val stop = n.actions.firstOrNull { it !== pauseResume && STOP_REGEX.containsMatchIn(it.title) }
    val isPaused = (pauseResume != null && RESUME_REGEX.containsMatchIn(pauseResume.title)) ||
        n.title.contains("paused", ignoreCase = true) || n.text.contains("paused", ignoreCase = true)
    return LiveActivityControls(pauseResume, isPaused, stop)
}

object LiveActivityColors {
    val RecordingRed = IslandTokens.LiveRecordingRed
    val TimerBlue = IslandTokens.LiveTimerBlue
    val StopwatchOrange = IslandTokens.AccentOrange
    val CallGreen = IslandTokens.LiveCallGreen
    val CallRed = IslandTokens.AccentRed
    val AvatarPlaceholder = IslandTokens.SurfaceDivider

    val AlarmYellow = IslandTokens.LiveAlarmYellow
    val TrackingGreen = IslandTokens.AccentGreen
    val SportsWhite = IslandTokens.LiveSportsWhite

    /** Secondary ("glass") controls: a faint white tint that sits on the black island. */
    val Glass: Color get() = Color.White.copy(alpha = if (IslandTokens.highContrast) 0.28f else 0.14f)

    fun accentFor(kind: LiveActivityKind): Color = when (kind) {
        LiveActivityKind.RECORDING -> RecordingRed
        LiveActivityKind.TIMER -> TimerBlue
        LiveActivityKind.STOPWATCH -> StopwatchOrange
        LiveActivityKind.CALL -> CallGreen
        LiveActivityKind.ALERT, LiveActivityKind.ALARM -> AlarmYellow
        LiveActivityKind.TRACKING -> TrackingGreen
        LiveActivityKind.SPORTS -> SportsWhite
        LiveActivityKind.NONE -> IslandTokens.TextBody
    }

    /** Background of a tinted control: the accent at low strength, so its icon in full accent reads clearly. */
    fun tint(accent: Color): Color = accent.copy(alpha = if (IslandTokens.highContrast) 0.34f else 0.22f)
}

/**
 * Layout rules shared by every live-activity card. The idea, borrowed from how the best island
 * designs are built rather than from any one of them:
 *
 * 1. **Concentric**: round things at the ends of a row (avatar, buttons) sit [margin] away from
 *    the edge on every side, and the card's corner radius is `control / 2 + margin`, so their
 *    curves run parallel to the card's. A one-row card is therefore a capsule.
 * 2. **One margin**: the same [margin] on all four sides; [gap] between items in a row.
 * 3. **One hero**: each card has one big value (time, ETA, score) in the activity's accent with
 *    steady-width digits; everything else is small and quiet.
 * 4. **Three button weights**: solid accent for the decisive action (stop, answer, hang up),
 *    tinted accent for the main toggle (pause / resume), glass for cancel / secondary.
 */
class LiveLayout(scale: Float, fontScale: Float = 1f) {
    /**
     * How much the phone's font size setting grows the cards. Text follows the setting fully;
     * controls and rows grow 60% as much, so big text still fits without the card ballooning.
     */
    private val textGrowth = fontScale.coerceIn(1f, 1.5f)
    private val rowGrowth = 1f + (textGrowth - 1f) * 0.6f

    val margin = (14.dp * scale).coerceIn(12.dp, 18.dp)
    val gap = (10.dp * scale).coerceIn(10.dp, 14.dp)

    /** Space between two buttons side by side: wide enough that a finger can't hit both. */
    val buttonGap = (gap * 1.2f).coerceAtLeast(12.dp)

    /** Round controls and avatars: never smaller than the 48dp minimum touch target. */
    val control = ((50.dp * scale).coerceIn(48.dp, 58.dp) * rowGrowth)
    val cornerRadius = control / 2 + margin

    // Text sizes come from IslandType (F4.1). Hero numbers grow with the row, not with the font
    // setting, so they always fit the row.
    private val type = IslandType(scale, fontScale)
    val heroSize = type.hero
    val heroSmallSize = type.heroSmall
    val labelSize = type.secondary
    val titleSize = type.title(IslandType.Card.LIVE)
    val captionSize = type.caption

    /** Height of a card with one row of content. */
    val rowCardHeight = control + margin * 2

    /** Height of the full-island alert: header row plus a row of pill buttons as tall as the controls. */
    val alertCardHeight = control * 2 + gap + margin * 2
}

/** [LiveLayout] for the current card scale and the phone's font size setting. */
@Composable
fun rememberLiveLayout(scale: Float): LiveLayout {
    val fontScale = LocalDensity.current.fontScale
    return remember(scale, fontScale) { LiveLayout(scale, fontScale) }
}

/** Uniform [LiveLayout.margin] on every side. */
private fun Modifier.liveCardPadding(layout: LiveLayout): Modifier = padding(layout.margin)

/** Digits that all take the same width, so a ticking time doesn't wobble. */
private const val TABULAR_NUMBERS = "tnum"

/** Time printed in the notification itself ("12:04"), shown while a recorder / timer is paused. */
fun staticLiveTimeText(n: NotificationInfo): String? =
    (parseTimerTextMillis(n.title) ?: parseTimerTextMillis(n.text))?.let { formatTimerText(it) }

/** Ticking time for a live activity, or the last time it showed while paused. */
@Composable
fun liveActivityTimeText(n: NotificationInfo): String =
    rememberLiveTimerText(n) ?: staticLiveTimeText(n) ?: "--:--"

/** Tighter tracking for big numbers so a time reads as one word, not spaced-out digits. */
private fun numberLetterSpacing(fontSize: TextUnit): TextUnit =
    if (fontSize.isSp && fontSize.value >= 20f) (-0.025).em else (-0.01).em

/**
 * Text for numbers that change in place (times, scores, ETAs). It's laid out once as a whole
 * string, so spacing, kerning and the baseline are exactly those of a normal Text and nothing
 * around it moves. When the value changes, only the characters that changed slide to their new
 * value (up when counting up, down when counting down) and fade, clipped to the text's box; the
 * animation runs in the draw phase, so it costs no recomposition. Long or digit-free text is
 * drawn plainly.
 */
@Composable
fun RollingNumberText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    countDown: Boolean = false
) {
    val style = LocalTextStyle.current.merge(
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFeatureSettings = TABULAR_NUMBERS,
            letterSpacing = numberLetterSpacing(fontSize)
        )
    )
    if (text.length > 12 || text.none { it.isDigit() }) {
        Text(text = text, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = modifier)
        return
    }
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val current = remember(text, style) { measurer.measure(text, style, maxLines = 1, softWrap = false) }

    // The value we're rolling away from, while a roll is running
    var shown by remember { mutableStateOf(text) }
    var previous by remember { mutableStateOf<String?>(null) }
    val motion = LocalIslandMotion.current
    val progress = remember { Animatable(1f) }
    LaunchedEffect(text) {
        if (text == shown) return@LaunchedEffect
        previous = shown
        shown = text
        progress.snapTo(0f)
        // Fixed length, always well inside one tick, so a ticking clock never queues rolls up
        progress.animateTo(1f, motion.tween(MotionTween.ROLL))
        previous = null
    }
    // Until the effect above runs, the new value must start from the old one, not snap in
    val from = if (shown != text) shown else previous
    val fromLayout = remember(from, style) { from?.let { measurer.measure(it, style, maxLines = 1, softWrap = false) } }
    val pending = shown != text

    val w = current.size.width
    val h = current.size.height
    // Old text is lined up on its right edge, where the digits that change sit ("9:59" -> "10:00")
    val fromShift = if (fromLayout != null) (w - fromLayout.size.width).toFloat() else 0f
    Spacer(
        modifier = modifier
            .semantics { contentDescription = text }
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(
                    w, h,
                    mapOf(FirstBaseline to current.firstBaseline.roundToInt(), LastBaseline to current.lastBaseline.roundToInt())
                ) { placeable.place(0, 0) }
            }
            .drawBehind {
                val p = if (pending) 0f else progress.value.coerceIn(0f, 1f)
                if (from == null || fromLayout == null || p >= 1f) {
                    drawText(current)
                    return@drawBehind
                }
                val dir = if (countDown) -1f else 1f
                val travel = size.height * 0.72f
                // Every character is drawn from the whole-string layout, cut out by its own column,
                // so unchanged characters (the ":" above all) stay on exactly the pixels they had
                // before and after the roll. Re-measuring single glyphs shifted them by the letter
                // spacing and lost kerning, so the ":" visibly jumped while digits rolled.
                fun column(layout: TextLayoutResult, index: Int, shift: Float, dy: Float, alpha: Float) {
                    if (alpha <= 0.002f) return
                    val box = layout.getBoundingBox(index)
                    clipRect(left = box.left + shift, top = 0f, right = box.right + shift, bottom = size.height) {
                        drawText(layout, topLeft = Offset(shift, dy), alpha = alpha)
                    }
                }
                // Vertical clip only: a dropped leading "1" may sit just left of the new, narrower box
                clipRect(left = -size.width, top = 0f, right = size.width * 2f, bottom = size.height) {
                    for (i in text.indices) {
                        val j = from.length - (text.length - i) // compared from the right
                        if (j >= 0 && from[j] == text[i]) {
                            column(current, i, 0f, 0f, 1f)
                        } else {
                            column(current, i, 0f, (1f - p) * travel * dir, p)
                            if (j >= 0) column(fromLayout, j, fromShift, -p * travel * dir, 1f - p)
                        }
                    }
                    // Characters that dropped off the left ("10:00" -> "9:59")
                    for (j in 0 until (from.length - text.length).coerceAtLeast(0)) {
                        column(fromLayout, j, fromShift, -p * travel * dir, 1f - p)
                    }
                }
            }
    )
}

/** How a round control is filled. See [LiveLayout] rule 4. */
enum class LiveButtonStyle { SOLID, TINTED, GLASS }

/** Round control button used by every live-activity card. */
@Composable
fun LiveRoundButton(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    size: Dp,
    style: LiveButtonStyle = LiveButtonStyle.SOLID,
    iconScale: Float = 0.46f,
    onClick: () -> Unit
) {
    val container = when (style) {
        LiveButtonStyle.SOLID -> accent
        LiveButtonStyle.TINTED -> LiveActivityColors.tint(accent)
        LiveButtonStyle.GLASS -> LiveActivityColors.Glass
    }
    val iconColor = when (style) {
        LiveButtonStyle.TINTED -> accent
        else -> Color.White
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = iconColor, modifier = Modifier.size(size * iconScale))
    }
}

/** Stop button: a rounded white square on a solid accent circle. */
@Composable
private fun StopSquareButton(size: Dp, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.30f)
                .clip(RoundedCornerShape(size * 0.07f))
                .background(Color.White)
        )
    }
}

/** Small label over a hero value ("Timer", "Recording"), in the accent at reduced strength. */
@Composable
private fun LiveEyebrow(text: String, color: Color, layout: LiveLayout) {
    Text(
        text = text,
        color = color,
        fontSize = layout.labelSize,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Recorder waveform. While recording it shows the real microphone level as a scrolling history
 * (newest on the right), like a voice-memo app; if the mic level can't be read it falls back to a
 * wobble animation. Frames are only requested while [active] (and the card is on screen), so a
 * paused recording costs nothing.
 */
@Composable
fun LiveWaveform(color: Color, active: Boolean, modifier: Modifier = Modifier, bars: Int = 27) {
    MicLevelEffect(active = active)
    val useMic = active && MicLevelMeter.hasSignal
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active, useMic) {
        if (!active || useMic) return@LaunchedEffect
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { phase = (it - start) / 1000f }
        }
    }
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val step = size.width / bars
        val stroke = (step * 0.42f).coerceAtLeast(1f)
        val midY = size.height / 2f
        if (useMic) {
            MicLevelMeter.version // redraw on every new sample
            val count = bars.coerceAtMost(MicLevelMeter.HISTORY)
            for (i in 0 until count) {
                val level = MicLevelMeter.levelAt(count - 1 - i)
                val fade = (i + 1f) / count // oldest samples on the left fade out
                val h = (size.height * (0.1f + 0.9f * level)).coerceIn(stroke, size.height)
                val x = step * (i + 0.5f)
                drawLine(
                    color = color.copy(alpha = (0.2f + 0.8f * fade).coerceIn(0f, 1f)),
                    start = Offset(x, midY - h / 2f),
                    end = Offset(x, midY + h / 2f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            return@Canvas
        }
        for (i in 0 until bars) {
            val x = step * (i + 0.5f)
            val edge = sin(PI * (i + 0.5) / bars).toFloat() // 0 at the ends, 1 in the middle
            val shape = 0.3f + 0.7f * abs(sin(i * 2.17f + 0.4f))
            val wobble = if (active) 0.55f + 0.45f * sin(phase * (2.4f + (i % 5) * 0.6f) + i * 1.3f) else 0.35f
            val h = (size.height * (0.14f + 0.86f * shape * wobble * (0.45f + 0.55f * edge))).coerceIn(stroke, size.height)
            drawLine(
                color = color.copy(alpha = (0.18f + 0.82f * edge).coerceIn(0f, 1f)),
                start = Offset(x, midY - h / 2f),
                end = Offset(x, midY + h / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Call card: caller photo, app label over the caller's name, red decline and green answer
 * buttons. Once answered, the label becomes the green call timer and a single hang-up stays.
 */
@Composable
fun LiveCallView(notification: NotificationInfo, onCollapse: () -> Unit, scale: Float) {
    val context = LocalContext.current
    val layout = rememberLiveLayout(scale)
    val liveTimer = rememberLiveTimerText(notification)
    val showTimer = !notification.isIncomingCall && liveTimer != null

    Row(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationAvatar(
            notification = notification,
            size = layout.control,
            showAppBadge = false,
            modifier = Modifier.clickable {
                sendIslandPendingIntent(context, notification.contentIntent)
                onCollapse()
            },
            fallback = {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(LiveActivityColors.AvatarPlaceholder))
            }
        )
        Spacer(modifier = Modifier.width(layout.gap))
        Column(modifier = Modifier.weight(1f)) {
            if (showTimer && liveTimer != null) {
                RollingNumberText(
                    text = liveTimer,
                    color = LiveActivityColors.CallGreen,
                    fontSize = layout.labelSize,
                    fontWeight = FontWeight.Medium
                )
            } else {
                LiveEyebrow(notification.appName.ifBlank { "Phone" }, IslandTokens.TextSecondary, layout)
            }
            Text(
                text = notification.title,
                color = IslandTokens.TextPrimary,
                fontSize = layout.titleSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
        }
        Spacer(modifier = Modifier.width(layout.gap))
        LiveRoundButton(
            icon = Icons.Rounded.CallEnd,
            contentDescription = if (notification.isIncomingCall) "Decline" else "Hang up",
            accent = LiveActivityColors.CallRed,
            size = layout.control
        ) {
            // No decline intent from the phone app: open its call screen instead of silently failing
            sendIslandPendingIntent(context, notification.declineIntent ?: notification.contentIntent)
            IslandStateManager.dismissCurrentNotification(endingRingingOrAlert = true)
        }
        if (notification.isIncomingCall) {
            Spacer(modifier = Modifier.width(layout.buttonGap))
            LiveRoundButton(
                icon = Icons.Rounded.Call,
                contentDescription = "Answer",
                accent = LiveActivityColors.CallGreen,
                size = layout.control
            ) {
                sendIslandPendingIntent(context, notification.answerIntent)
                onCollapse()
            }
        }
    }
}

/** Voice / screen recorder card: big red time, live waveform, pause / resume and stop. */
@Composable
fun LiveRecordingRow(
    timeText: String,
    isPaused: Boolean,
    scale: Float,
    onOpen: () -> Unit,
    onPauseResume: (() -> Unit)?,
    onStop: (() -> Unit)?
) {
    val layout = rememberLiveLayout(scale)
    val red = LiveActivityColors.RecordingRed
    Row(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(layout.margin * 0.5f))
        RollingNumberText(
            text = timeText,
            color = if (isPaused) red.copy(alpha = 0.6f) else red,
            fontSize = layout.heroSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onOpen)
        )
        Spacer(modifier = Modifier.width(layout.gap))
        LiveWaveform(
            color = red,
            active = !isPaused,
            modifier = Modifier
                .weight(1f)
                .height(layout.control * 0.62f)
                .clickable(onClick = onOpen)
        )
        Spacer(modifier = Modifier.width(layout.gap))
        if (onPauseResume != null) {
            LiveRoundButton(
                icon = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                accent = red,
                size = layout.control,
                style = LiveButtonStyle.TINTED,
                onClick = onPauseResume
            )
            Spacer(modifier = Modifier.width(layout.buttonGap))
        }
        if (onStop != null) {
            StopSquareButton(size = layout.control, color = red, onClick = onStop)
        }
    }
}

/** Timer / stopwatch / alarm card: big time with a small label, cancel (X) and pause / resume. */
@Composable
fun LiveTimerRow(
    timeText: String,
    label: String,
    accent: Color,
    isPaused: Boolean,
    scale: Float,
    onOpen: () -> Unit,
    onCancel: (() -> Unit)?,
    onPauseResume: (() -> Unit)?,
    countDown: Boolean = false
) {
    val layout = rememberLiveLayout(scale)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel sits at the leading end, concentric with the card's corner; the time takes the middle
        if (onCancel != null) {
            LiveRoundButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Cancel",
                accent = accent,
                size = layout.control,
                style = LiveButtonStyle.GLASS,
                iconScale = 0.42f,
                onClick = onCancel
            )
            Spacer(modifier = Modifier.width(layout.gap))
        } else {
            Spacer(modifier = Modifier.width(layout.margin * 0.5f))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = accent.copy(alpha = 0.75f),
                fontSize = layout.labelSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline()
            )
            Spacer(modifier = Modifier.width(6.dp))
            RollingNumberText(
                text = timeText,
                color = if (isPaused && onPauseResume != null) accent.copy(alpha = 0.6f) else accent,
                fontSize = layout.heroSize,
                fontWeight = FontWeight.Medium,
                countDown = countDown,
                modifier = Modifier.alignByBaseline()
            )
        }
        if (onPauseResume != null) {
            Spacer(modifier = Modifier.width(layout.gap))
            LiveRoundButton(
                icon = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                accent = accent,
                size = layout.control,
                style = LiveButtonStyle.TINTED,
                onClick = onPauseResume
            )
        } else {
            Spacer(modifier = Modifier.width(layout.margin * 0.5f))
        }
    }
}

/**
 * Expanded card for a live activity (recorder, timer, stopwatch, alarm, alert, ride / delivery /
 * flight, live score). Buttons fire the notification's own actions; the card stays until the app
 * ends the activity or the user swipes it away.
 */
@Composable
fun LiveActivityNotificationView(notification: NotificationInfo, kind: LiveActivityKind, onCollapse: () -> Unit, scale: Float) {
    val context = LocalContext.current
    val controls = remember(notification) { liveActivityControls(notification) }
    val timeText = liveActivityTimeText(notification)
    val isPaused = controls.isPaused || notification.liveTimerBaseMs <= 0L
    val open = {
        openTarget(context, notification.contentIntent, notification.packageName)
        onCollapse()
    }
    fun fire(action: NotificationAction?): (() -> Unit)? = action?.let { a ->
        {
            if (!sendIslandPendingIntent(context, a.actionIntent)) LiveActivityTester.onAction(notification, a)
            IslandStateManager.onNotificationActionPressed()
        }
    }
    when (kind) {
        LiveActivityKind.RECORDING -> LiveRecordingRow(
            timeText = timeText,
            isPaused = isPaused,
            scale = scale,
            onOpen = open,
            onPauseResume = fire(controls.pauseResume),
            onStop = fire(controls.stop)
        )
        LiveActivityKind.ALERT -> {
            val alert = remember(notification) { alertControls(notification) }
            LiveAlertView(
                notification = notification,
                scale = scale,
                onOpen = open,
                onStop = fire(alert.stop) ?: { IslandStateManager.dismissCurrentNotification(endingRingingOrAlert = true); Unit },
                onExtra = fire(alert.extra),
                extraLabel = alert.extra?.title
            )
        }
        LiveActivityKind.ALARM -> LiveTimerRow(
            timeText = if (notification.liveTimerBaseMs > 0L) timeText else alarmTimeText(notification) ?: timeText,
            label = if ("snooz" in "${notification.title} ${notification.text}".lowercase()) "Snoozed" else "Alarm",
            accent = LiveActivityColors.AlarmYellow,
            isPaused = true,
            scale = scale,
            onOpen = open,
            onCancel = fire(controls.stop),
            onPauseResume = null,
            countDown = notification.liveTimerCountDown
        )
        LiveActivityKind.TRACKING -> LiveTrackingRow(notification, scale, onOpen = open)
        LiveActivityKind.SPORTS -> {
            val score = remember(notification) { parseLiveScore(notification) }
            if (score != null) LiveScoreRow(score, scale, onOpen = open) else LiveTrackingRow(notification, scale, onOpen = open)
        }
        else -> LiveTimerRow(
            timeText = timeText,
            label = when {
                kind == LiveActivityKind.STOPWATCH -> "Stopwatch"
                "snooz" in "${notification.title} ${notification.text}".lowercase() -> "Snooze"
                else -> notification.title.takeIf { it.isNotBlank() && parseTimerTextMillis(it) == null && !it.equals("Paused", true) }
                    ?: "Timer"
            },
            accent = LiveActivityColors.accentFor(kind),
            isPaused = isPaused,
            scale = scale,
            onOpen = open,
            onCancel = fire(controls.stop),
            onPauseResume = fire(controls.pauseResume),
            countDown = notification.liveTimerCountDown
        )
    }
}

/** Height of the expanded card for each live-activity design. */
fun liveActivityCardHeight(kind: LiveActivityKind, scale: Float, fontScale: Float = 1f): Dp {
    val layout = LiveLayout(scale, fontScale)
    return if (kind == LiveActivityKind.ALERT) layout.alertCardHeight else layout.rowCardHeight
}

/** Corner radius of a live-activity card: concentric with its round end controls (see [LiveLayout]). */
fun liveActivityCornerRadius(scale: Float, fontScale: Float = 1f): Dp = LiveLayout(scale, fontScale).cornerRadius

/**
 * Ringing alarm or expired timer, shown on the full island: icon, "Time's up" / alarm name, the
 * overtime (or alarm time), then two wide pill buttons, +1:00 / Snooze and Stop.
 */
@Composable
fun LiveAlertView(
    notification: NotificationInfo,
    scale: Float,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onExtra: (() -> Unit)?,
    extraLabel: String?
) {
    val layout = rememberLiveLayout(scale)
    val isTimer = isTimerAlert(notification)
    val accent = LiveActivityColors.AlarmYellow
    val liveTime = rememberLiveTimerText(notification)
    val trailing = liveTime?.let { "-$it" } ?: alarmTimeText(notification)
    val headline = when {
        isTimer && notification.title.isNotBlank() && !notification.title.equals("Timer", ignoreCase = true) -> notification.title
        isTimer -> "Time's up"
        else -> notification.title.ifBlank { "Alarm" }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.control)
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingAlertIcon(
                icon = if (isTimer) Icons.Rounded.Timer else Icons.Rounded.Alarm,
                color = accent,
                size = layout.control
            )
            Spacer(modifier = Modifier.width(layout.gap))
            Column(modifier = Modifier.weight(1f)) {
                LiveEyebrow(if (isTimer) "Timer" else notification.appName.ifBlank { "Alarm" }, IslandTokens.TextSecondary, layout)
                Text(
                    text = headline,
                    color = IslandTokens.TextPrimary,
                    fontSize = layout.titleSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(layout.gap))
                RollingNumberText(
                    text = trailing,
                    color = accent,
                    fontSize = layout.heroSmallSize,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(layout.margin * 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(layout.gap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.buttonGap)
        ) {
            if (onExtra != null) {
                AlertPillButton(
                    text = extraLabel?.takeIf { it.isNotBlank() } ?: if (isTimer) "+1:00" else "Snooze",
                    container = LiveActivityColors.Glass,
                    content = Color.White,
                    height = layout.control,
                    fontSize = layout.titleSize,
                    modifier = Modifier.weight(1f),
                    onClick = onExtra
                )
            }
            AlertPillButton(
                text = "Stop",
                container = accent,
                content = Color.Black,
                height = layout.control,
                fontSize = layout.titleSize,
                modifier = Modifier.weight(1f),
                onClick = onStop
            )
        }
    }
}

@Composable
private fun AlertPillButton(
    text: String,
    container: Color,
    content: Color,
    height: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = content, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Alert icon in a tinted circle that gently pulses while ringing. */
@Composable
private fun PulsingAlertIcon(icon: ImageVector, color: Color, size: Dp) {
    val motion = LocalIslandMotion.current
    val pulse by rememberInfiniteTransition(label = "alert_pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(motion.tween(MotionTween.ALERT_PULSE), RepeatMode.Reverse),
        label = "alert_scale"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(LiveActivityColors.tint(color)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.5f).scale(pulse)
        )
    }
}

/**
 * Ride / delivery / flight card: app icon, status title over details with a progress bar, and the
 * ETA (or the app's own status chip) as the hero in green.
 */
@Composable
fun LiveTrackingRow(notification: NotificationInfo, scale: Float, onOpen: () -> Unit) {
    val layout = rememberLiveLayout(scale)
    val accent = LiveActivityColors.TrackingGreen
    val chip = trackingChipText(notification)
    val hasBar = notification.maxProgress > 0 && notification.progress >= 0
    val motion = LocalIslandMotion.current
    val progress by animateFloatAsState(
        targetValue = notification.progressFraction,
        animationSpec = motion.spring(motion.trackingProgress),
        label = "tracking_progress"
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationAvatar(
            notification = notification,
            size = layout.control,
            showAppBadge = false,
            fallback = {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(LiveActivityColors.tint(accent)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = accent, modifier = Modifier.size(layout.control * 0.44f))
                }
            }
        )
        Spacer(modifier = Modifier.width(layout.gap))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = notification.title,
                color = IslandTokens.TextPrimary,
                fontSize = layout.titleSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    color = IslandTokens.TextSecondary,
                    fontSize = layout.captionSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasBar) {
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(LiveActivityColors.Glass)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
        }
        if (chip != null) {
            Spacer(modifier = Modifier.width(layout.gap))
            RollingNumberText(
                text = chip,
                color = accent,
                fontSize = layout.heroSmallSize,
                fontWeight = FontWeight.SemiBold,
                countDown = true
            )
            Spacer(modifier = Modifier.width(layout.margin * 0.5f))
        }
    }
}

/** Live score: team, big score, team, with the match clock / status under the score. */
@Composable
fun LiveScoreRow(score: LiveScore, scale: Float, onOpen: () -> Unit) {
    val layout = rememberLiveLayout(scale)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .liveCardPadding(layout)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamBadge(score.home, layout)
        Spacer(modifier = Modifier.width(layout.gap))
        Text(
            text = score.home,
            color = IslandTokens.TextPrimary,
            fontSize = layout.titleSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier.padding(horizontal = layout.gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RollingNumberText(score.homeScore, LiveActivityColors.SportsWhite, layout.heroSmallSize, FontWeight.Bold)
                Text(
                    text = " – ",
                    color = IslandTokens.TextTertiary,
                    fontSize = layout.heroSmallSize,
                    fontWeight = FontWeight.Medium
                )
                RollingNumberText(score.awayScore, LiveActivityColors.SportsWhite, layout.heroSmallSize, FontWeight.Bold)
            }
            if (!score.status.isNullOrBlank()) {
                Text(
                    text = score.status,
                    color = LiveActivityColors.TrackingGreen,
                    fontSize = layout.captionSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = score.away,
            color = IslandTokens.TextPrimary,
            fontSize = layout.titleSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(layout.gap))
        TeamBadge(score.away, layout)
    }
}

/** Round badge with a team's initials, at the card's ends so it's concentric with the corners. */
@Composable
private fun TeamBadge(team: String, layout: LiveLayout) {
    val initials = remember(team) {
        team.split(' ', '-').filter { it.isNotBlank() }.let { words ->
            if (words.size >= 2) "${words[0].first()}${words[1].first()}" else team.take(3)
        }.uppercase()
    }
    Box(
        modifier = Modifier
            .size(layout.control)
            .clip(CircleShape)
            .background(LiveActivityColors.Glass),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = Color.White, fontSize = layout.captionSize, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * Countdown ring for the compact pill: a dim track with an arc for the time that's left, which
 * drains smoothly (one linear tween per second, no per-frame work in between).
 */
@Composable
fun TimerRingIcon(notification: NotificationInfo, size: Dp, color: Color) {
    val base = notification.liveTimerBaseMs
    val pausedRemaining = remember(notification) {
        parseTimerTextMillis(notification.text) ?: parseTimerTextMillis(notification.title)
    }
    fun remainingNow(): Long = if (base > 0L) (base - IslandClock.wallMs()).coerceAtLeast(0L) else pausedRemaining ?: 0L
    val fraction = remember { Animatable(1f) }
    val motion = LocalIslandMotion.current
    LaunchedEffect(notification.key, base, pausedRemaining) {
        val total = CountdownTotals.totalFor(notification.key, remainingNow())
        fraction.snapTo((remainingNow().toFloat() / total).coerceIn(0f, 1f))
        if (base <= 0L) return@LaunchedEffect
        while (fraction.value > 0f) {
            val next = ((remainingNow() - 1000L).coerceAtLeast(0L).toFloat() / total).coerceIn(0f, 1f)
            fraction.animateTo(next, motion.tween(MotionTween.COUNTDOWN_TICK))
        }
    }
    Canvas(modifier = Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.16f
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
        drawCircle(color = color.copy(alpha = 0.25f), radius = this.size.minDimension / 2f - inset, style = Stroke(width = stroke))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * fraction.value,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/** Leading icon of the compact pill for a live activity. */
@Composable
fun LiveActivityCompactIcon(kind: LiveActivityKind, notification: NotificationInfo) {
    val accent = LiveActivityColors.accentFor(kind)
    when (kind) {
        LiveActivityKind.CALL -> Icon(Icons.Rounded.Call, contentDescription = "Call", tint = accent, modifier = Modifier.size(18.dp))
        LiveActivityKind.RECORDING -> LiveWaveform(
            color = accent,
            active = notification.liveTimerBaseMs > 0L,
            bars = 7,
            modifier = Modifier.width(26.dp).height(16.dp)
        )
        LiveActivityKind.TIMER -> if (notification.liveTimerCountDown || notification.liveTimerBaseMs <= 0L) {
            TimerRingIcon(notification = notification, size = 18.dp, color = accent)
        } else {
            TimerClockIcon(baseMs = notification.liveTimerBaseMs, countDown = false, size = 18.dp, color = accent)
        }
        LiveActivityKind.STOPWATCH -> if (notification.liveTimerBaseMs > 0L) {
            TimerClockIcon(baseMs = notification.liveTimerBaseMs, countDown = notification.liveTimerCountDown, size = 18.dp, color = accent)
        } else {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        LiveActivityKind.ALERT -> Icon(
            if (isTimerAlert(notification)) Icons.Rounded.Timer else Icons.Rounded.Alarm,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        LiveActivityKind.ALARM -> Icon(Icons.Rounded.Alarm, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        LiveActivityKind.TRACKING -> NotificationAvatar(
            notification = notification,
            size = 20.dp,
            showAppBadge = false,
            fallback = { Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp)) }
        )
        LiveActivityKind.SPORTS -> Icon(Icons.Rounded.SportsSoccer, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        LiveActivityKind.NONE -> Icon(Icons.Rounded.Mic, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
    }
}

/**
 * Trailing text of the compact pill for the kinds that don't show a ticking time: ETA for rides,
 * the score, "Time's up", the alarm time. Null means "use the usual time / title".
 */
fun liveCompactText(n: NotificationInfo, kind: LiveActivityKind): String? = when (kind) {
    LiveActivityKind.TRACKING -> trackingChipText(n) ?: n.title
    LiveActivityKind.SPORTS -> parseLiveScore(n)?.let { "${it.homeScore}–${it.awayScore}" }
    LiveActivityKind.ALERT -> if (isTimerAlert(n)) "Time's up" else alarmTimeText(n) ?: "Alarm"
    LiveActivityKind.ALARM -> if (n.liveTimerBaseMs > 0L) null else alarmTimeText(n)
    else -> null
}
