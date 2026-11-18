package com.droidbubble.app.island.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A2.1: the tier table, case by case. Pure JUnit. */
class ArbiterTest {

    private val defaultOrder = bucketOrderOf(listOf("CALL", "MEDIA", "PROGRESS", "RECORDING", "NOTIFICATION", "FLASHLIGHT"))

    private fun spec(
        id: String,
        kind: ActivityKind,
        tier: Tier,
        bucket: OrderBucket = OrderBucket.NOTIFICATION,
        pkg: String? = "pkg.$id",
        last: Long = 0L,
        started: Long = 0L,
        expiresAt: Long? = null,
        staleAt: Long? = null,
        alerting: Boolean = tier == Tier.T0_INTERRUPT,
        visibility: Int = PrivacySpec.VISIBILITY_PUBLIC,
    ) = LiveActivitySpec(
        id = ActivityId(id), kind = kind, source = ActivitySource.TEST, packageName = pkg, tier = tier, bucket = bucket,
        startedAt = started, lastSignificantAt = last, staleAt = staleAt, expiresAt = expiresAt, alerting = alerting,
        accent = AccentRole.NEUTRAL, compactLeading = Slot.Empty, compactTrailing = Slot.Empty, minimal = Slot.Empty,
        expanded = ExpandedSpec(ExpandedPresentation.NONE), open = OpenTarget.None,
        privacy = PrivacySpec(visibility), stateSignature = 0, payload = ActivityPayload.System(GlanceInfo(id)),
    )

    private val media = spec("media", ActivityKind.MEDIA, Tier.T2_ONGOING, OrderBucket.MEDIA, pkg = "com.spotify.music")
    private val ongoingCall = spec("call", ActivityKind.CALL_ONGOING, Tier.T1_LIVE, OrderBucket.CALL, pkg = "com.whatsapp")
    private val ringingCall = spec("ring", ActivityKind.CALL_RINGING, Tier.T0_INTERRUPT, OrderBucket.CALL, pkg = "com.google.android.dialer")
    private val alarm = spec("alarm", ActivityKind.ALERT, Tier.T0_INTERRUPT, pkg = "com.google.android.deskclock")
    private fun timer(id: String, last: Long) = spec(id, ActivityKind.TIMER, Tier.T1_LIVE, pkg = "com.google.android.deskclock", last = last)
    private fun message(id: String = "msg", pkg: String = "org.telegram.messenger", last: Long = 50L) =
        spec(id, ActivityKind.MESSAGE, Tier.T3_BANNER, pkg = pkg, last = last)
    private fun download(id: String, last: Long) = spec(id, ActivityKind.PROGRESS, Tier.T2_ONGOING, OrderBucket.PROGRESS, pkg = "com.android.vending", last = last)
    private fun glance(id: String, last: Long, expires: Long = 10_000L) =
        spec(id, ActivityKind.GLANCE, Tier.T4_GLANCE, OrderBucket.SYSTEM, pkg = null, last = last, expiresAt = expires)

    private fun run(vararg specs: LiveActivitySpec, now: Long = 1_000L, order: List<OrderBucket> = defaultOrder,
                    priority: Set<String> = emptySet(), foreground: String? = null, peek: Peek? = null,
                    fullscreen: FullscreenGate = FullscreenGate.NotFullscreen, lock: LockContext = LockContext.Unlocked,
                    overrides: Map<String, Tier> = emptyMap()) =
        arbitrate(ArbiterInput(specs.toList(), now, order, priority, overrides, foreground, fullscreen, lock, peek))

    @Test
    fun empty() {
        val a = run()
        assertNull(a.primary); assertNull(a.secondary); assertTrue(a.overflow.isEmpty()); assertNull(a.glance)
    }

    @Test
    fun callPlusMedia() {
        val a = run(media, ongoingCall)
        assertEquals(ongoingCall, a.primary)
        assertEquals(media, a.secondary)
    }

    @Test
    fun timerPlusMessage_messageNeverPrimary() {
        val a = run(message(last = 999L), timer("t", last = 1L))
        assertEquals("t", a.primary?.id?.value)
        assertEquals("msg", a.secondary?.id?.value)
    }

    @Test
    fun messageAlone_isPrimary() {
        assertEquals("msg", run(message()).primary?.id?.value)
    }

    @Test
    fun twoTimers_newestFirst() {
        val a = run(timer("old", last = 10L), timer("new", last = 20L))
        assertEquals("new", a.primary?.id?.value)
        assertEquals("old", a.secondary?.id?.value)
    }

    @Test
    fun twoDownloads_newestFirst() {
        val a = run(download("d1", last = 10L), download("d2", last = 30L))
        assertEquals("d2", a.primary?.id?.value)
        assertEquals("d1", a.secondary?.id?.value)
    }

    @Test
    fun ringingAlarmPlusMedia_alarmPrimaryAndAutoExpands() {
        val a = run(media, alarm)
        assertEquals(alarm, a.primary)
        assertEquals(alarm.id, a.autoExpand)
        assertEquals(media, a.secondary)
    }

    @Test
    fun ringingCallBeatsLive() {
        val a = run(timer("t", 99L), ringingCall, ongoingCall)
        assertEquals(ringingCall, a.primary)
        assertEquals(ringingCall.id, a.autoExpand)
        assertNull("a live primary doesn't auto-expand", run(timer("t", 99L)).autoExpand)
    }

    @Test
    fun glance_overLive_butNotOverT0() {
        val g = glance("sys:charging", last = 5L)
        assertEquals(g, run(timer("t", 1L), g).glance)
        assertNull(run(ringingCall, g).glance)
        assertTrue(run(ringingCall, g).reasons[g.id]!!.rule.startsWith("hidden"))
        // The newest glance wins; an expired one is gone
        val newer = glance("sys:bt", last = 9L)
        assertEquals(newer, run(g, newer).glance)
        assertNull(run(glance("old", last = 1L, expires = 500L), now = 1_000L).glance)
        // Glances never take a rank
        assertNull(run(g).primary)
    }

    @Test
    fun foregroundHides_exceptT0() {
        val a = run(media, timer("t", 1L), foreground = "com.google.android.deskclock")
        assertEquals(media, a.primary)
        assertEquals("hidden · app on screen", a.reasons[ActivityId("t")]?.rule)
        // A ringing alarm shows over the clock app
        assertEquals(alarm, run(alarm, foreground = "com.google.android.deskclock").primary)
    }

    @Test
    fun priorityApp_headOfT2_neverAboveT1() {
        val vip = message(pkg = "com.whatsapp")
        val a = run(media, download("d", 10L), vip, timer("t", 1L), priority = setOf("com.whatsapp"))
        assertEquals("t", a.primary?.id?.value)
        assertEquals("the priority app's message heads T2", vip, a.secondary)
        assertEquals(listOf(media.id, ActivityId("d")), a.overflow.map { it.id })
        // A priority app's media heads T2 as well, ahead of the drag order
        val b = run(download("d", 10L), media, priority = setOf("com.spotify.music"),
            order = bucketOrderOf(listOf("PROGRESS", "MEDIA")))
        assertEquals(media, b.primary)
    }

    @Test
    fun dragOrder_insideT2() {
        val torch = spec("torch", ActivityKind.FLASHLIGHT, Tier.T2_ONGOING, OrderBucket.FLASHLIGHT, pkg = null, last = 99L)
        assertEquals(media, run(torch, media).primary)
        assertEquals(torch, run(torch, media, order = bucketOrderOf(listOf("FLASHLIGHT", "MEDIA"))).primary)
    }

    @Test
    fun fiveActivities_primarySecondaryOverflow3() {
        val a = run(media, timer("t", 1L), download("d", 5L), message(), ongoingCall)
        assertEquals(ongoingCall, a.primary)        // T1, CALL bucket before NOTIFICATION
        assertEquals("t", a.secondary?.id?.value)   // T1
        assertEquals(listOf("media", "d", "msg"), a.overflow.map { it.id.value })
        assertEquals(3, a.overflow.size)
    }

    @Test
    fun peek_takesSecondaryWhileItLasts() {
        val msg = message()
        val specs = arrayOf(ongoingCall, media, download("d", 1L), msg)
        val peeking = run(*specs, peek = Peek(msg.id, untilMs = 2_000L))
        assertEquals(ongoingCall, peeking.primary)
        assertEquals(msg, peeking.secondary)
        assertEquals("T3 · peeking · secondary", peeking.reasons[msg.id]?.rule)
        // After the peek it recedes to its rank (overflow here)
        val after = run(*specs, now = 3_000L, peek = Peek(msg.id, untilMs = 2_000L))
        assertEquals(media, after.secondary)
        assertEquals(msg, after.overflow.last())
    }

    @Test
    fun ambient_onlyWhenNothingElse() {
        val ambient = spec("amb", ActivityKind.WIDGET, Tier.T5_AMBIENT, pkg = null)
        assertEquals(ambient, run(ambient).primary)
        assertEquals(media, run(ambient, media).primary)
        assertNull(run(ambient, media).secondary)
    }

    @Test
    fun staleAndExpired_areHidden() {
        val stale = media.copy(staleAt = 500L)
        assertNull(run(stale, now = 1_000L).primary)
        assertEquals("hidden · stale", run(stale, now = 1_000L).reasons[stale.id]?.rule)
    }

    @Test
    fun fullscreenGate() {
        val all = arrayOf(media, ongoingCall, ringingCall, timer("t", 1L))
        assertEquals(listOf(ringingCall, ongoingCall), run(*all, fullscreen = FullscreenGate.CallsAndAlarmsOnly).ranked)
        assertNull(run(*all, fullscreen = FullscreenGate.Hide).primary)
        assertEquals(4, run(*all).ranked.size)
    }

    @Test
    fun secretHiddenWhileLocked() {
        val secret = message().copy(privacy = PrivacySpec(PrivacySpec.VISIBILITY_SECRET))
        assertNull(run(secret, lock = LockContext.Locked).primary)
        assertEquals(secret, run(secret).primary)
    }

    @Test
    fun tierOverrides() {
        // An app set to Live outranks media; an override never lifts into T0 nor demotes a T0
        val podcast = message(pkg = "com.podcast")
        assertEquals(podcast, run(media, podcast, overrides = mapOf("com.podcast" to Tier.T1_LIVE)).primary)
        assertEquals(Tier.T3_BANNER, run(podcast, overrides = mapOf("com.podcast" to Tier.T0_INTERRUPT)).reasons[podcast.id]?.tier)
        assertEquals(alarm, run(media, alarm, overrides = mapOf("com.google.android.deskclock" to Tier.T3_BANNER)).primary)
    }

    @Test
    fun deterministic_andTiesBrokenById() {
        val a = timer("a", 5L)
        val b = timer("b", 5L)
        assertEquals("a", run(a, b).primary?.id?.value)
        assertEquals("a", run(b, a).primary?.id?.value)
        assertEquals(run(a, b, media), run(a, b, media))
    }

    @Test
    fun startedAt_oldestFirst_whenEquallyRecent() {
        val older = timer("z", 5L).copy(startedAt = 1L)
        val newer = timer("a", 5L).copy(startedAt = 2L)
        assertEquals(older, run(newer, older).primary)
    }
}
