package com.droidbubble.app.island.activity

/**
 * A6: the fullscreen state combined with the user's "In fullscreen apps" policy. Until A6 lands
 * the scene passes [NotFullscreen].
 */
enum class FullscreenGate {
    /** Not fullscreen, or the policy is "Always": nothing is filtered. */
    NotFullscreen,
    /** Fullscreen with "Calls & alarms only": T0 plus ongoing calls. */
    CallsAndAlarmsOnly,
    /** Fullscreen with "Hide": nothing shows. */
    Hide,
}

/** A5: whether the keyguard is up. A5.1 adds the lock-screen notification settings. */
enum class LockContext { Unlocked, Locked }

/** The current T3 pop-in: [id] peeks until [untilMs] (IslandTimings.peek). */
data class Peek(val id: ActivityId, val untilMs: Long)

data class ArbiterInput(
    val activities: Collection<LiveActivitySpec>,
    val now: Long,
    /** From settings.featurePriorities; buckets missing from the list rank after the listed ones. */
    val bucketOrder: List<OrderBucket>,
    /** Promoted to the head of T2 (a message from one leaves T3), never above T1. */
    val priorityApps: Set<String> = emptySet(),
    /** A2.5: per-app tier. */
    val tierOverrides: Map<String, Tier> = emptyMap(),
    val foregroundPackage: String? = null,
    val fullscreen: FullscreenGate = FullscreenGate.NotFullscreen,
    val lock: LockContext = LockContext.Unlocked,
    val peek: Peek? = null,
)

/** Why an activity sits where it does: "T1 · live", "hidden · app on screen" (A2.5 shows these). */
data class Reason(val tier: Tier, val rule: String)

data class Arbitration(
    val primary: LiveActivitySpec?,
    val secondary: LiveActivitySpec?,
    /** "+N" = overflow.size (A2.4). */
    val overflow: List<LiveActivitySpec>,
    /** The T4 glance overlaying the pill, if any. */
    val glance: LiveActivitySpec?,
    /** The primary, when it may open the card by itself (T0, or an alerting update). */
    val autoExpand: ActivityId?,
    val reasons: Map<ActivityId, Reason>,
) {
    /** Everything ranked, primary first (glances not included). */
    val ranked: List<LiveActivitySpec> get() = listOfNotNull(primary, secondary) + overflow

    companion object {
        val Empty = Arbitration(null, null, emptyList(), null, null, emptyMap())
    }
}

/**
 * A2.1: who is primary, secondary, in overflow and glancing, from every activity at once. Pure and
 * deterministic: equal inputs give equal outputs.
 *
 * Tiers (TierTable):
 * - T0 interrupt: always primary; shows over its own app (and in fullscreen); no glance over it.
 * - T1 live: never below a message; primary when there is no T0.
 * - T2 ongoing: in the user's drag order; priority apps first.
 * - T3 banner: after everything ongoing; a peeking banner takes the secondary place while it peeks.
 * - T4 glance: not ranked; the newest overlays the pill unless a T0 is showing.
 * - T5 ambient: only when nothing T0–T3 is visible.
 *
 * Inside a tier: priority app (T2 only), the user's bucket order, the last change of state (newest
 * first), the start (oldest first), then the id.
 *
 * Where it differs from the view's order before A2.2 (all intended, 04 §6):
 * 1. A ringing alarm or Time's up is primary over media (it followed the drag order).
 * 2. Live activities (ongoing call, timer, stopwatch, recorder, screen recording, a ride with an
 *    ETA) rank above media, downloads and the torch, whatever the drag order.
 * 3. A message ranks after media, downloads and the torch (T3); it peeks into the secondary place.
 * 4. A priority app's message heads T2 instead of beating everything.
 * 5. A ringing alarm or Time's up shows even while the clock app is on screen (before, only a
 *    ringing call did).
 * 6. Several notifications rank together (before, only one notification slot existed).
 */
fun arbitrate(input: ArbiterInput): Arbitration {
    val reasons = LinkedHashMap<ActivityId, Reason>()
    val visible = ArrayList<Pair<LiveActivitySpec, Tier>>()
    val glances = ArrayList<LiveActivitySpec>()

    for (spec in input.activities) {
        val tier = effectiveTier(spec, input)
        val hidden = hiddenBecause(spec, tier, input)
        if (hidden != null) {
            reasons[spec.id] = Reason(tier, "hidden · $hidden")
            continue
        }
        if (tier == Tier.T4_GLANCE) glances += spec else visible += spec to tier
    }

    val hasT0 = visible.any { it.second == Tier.T0_INTERRUPT }
    val hasForeground = visible.any { it.second <= Tier.T3_BANNER }
    val ranked = visible
        .filter { (spec, tier) ->
            (tier != Tier.T5_AMBIENT || !hasForeground).also { shown ->
                if (!shown) reasons[spec.id] = Reason(tier, "hidden · ambient under an activity")
            }
        }
        .sortedWith(rankComparator(input))
        .map { it.first }
        .toMutableList()

    // A peeking banner takes the secondary place (never primary over something ongoing)
    val peek = input.peek?.takeIf { it.untilMs > input.now }
    if (peek != null) {
        val i = ranked.indexOfFirst { it.id == peek.id }
        if (i > 1) ranked.add(1, ranked.removeAt(i))
    }

    val glance = if (hasT0) null else glances.maxWithOrNull(compareBy<LiveActivitySpec> { it.lastSignificantAt }.thenBy { it.id.value })
    glances.forEach { g ->
        reasons[g.id] = Reason(Tier.T4_GLANCE, if (g === glance) "T4 · glance over the pill" else "hidden · glance under a call or alarm")
    }

    ranked.forEachIndexed { index, spec ->
        val tier = visible.first { it.first === spec }.second
        val place = when (index) { 0 -> "primary"; 1 -> "secondary"; else -> "overflow" }
        val why = when {
            index == 1 && peek?.id == spec.id -> "peeking"
            tier == Tier.T2_ONGOING && spec.packageName != null && spec.packageName in input.priorityApps -> "priority app"
            input.tierOverrides.containsKey(spec.packageName) -> "app set to ${tier.label}"
            else -> tier.label
        }
        reasons[spec.id] = Reason(tier, "${tier.short} · $why · $place")
    }

    val primary = ranked.getOrNull(0)
    return Arbitration(
        primary = primary,
        secondary = ranked.getOrNull(1),
        overflow = if (ranked.size > 2) ranked.subList(2, ranked.size).toList() else emptyList(),
        glance = glance,
        autoExpand = primary?.takeIf { it.alerting }?.id,
        reasons = reasons,
    )
}

/** The tier after A2.5's per-app override and the priority-app promotion of messages. */
private fun effectiveTier(spec: LiveActivitySpec, input: ArbiterInput): Tier {
    val pkg = spec.packageName
    val override = pkg?.let { input.tierOverrides[it] }
    // An override never lifts anything into T0 (only ringing calls and alarms interrupt) and never
    // demotes a T0
    if (override != null && spec.tier != Tier.T0_INTERRUPT && override != Tier.T0_INTERRUPT) return override
    if (spec.tier == Tier.T3_BANNER && pkg != null && pkg in input.priorityApps) return Tier.T2_ONGOING
    return spec.tier
}

private fun hiddenBecause(spec: LiveActivitySpec, tier: Tier, input: ArbiterInput): String? {
    spec.expiresAt?.let { if (it <= input.now) return "expired" }
    spec.staleAt?.let { if (it <= input.now) return "stale" }
    if (tier != Tier.T0_INTERRUPT && spec.packageName != null && spec.packageName == input.foregroundPackage) {
        return "app on screen"
    }
    when (input.fullscreen) {
        FullscreenGate.NotFullscreen -> Unit
        FullscreenGate.Hide -> return "fullscreen"
        FullscreenGate.CallsAndAlarmsOnly ->
            if (tier != Tier.T0_INTERRUPT && spec.kind != ActivityKind.CALL_ONGOING) return "fullscreen, calls and alarms only"
    }
    if (input.lock == LockContext.Locked && spec.privacy.visibility == PrivacySpec.VISIBILITY_SECRET) return "secret on the lock screen"
    return null
}

private fun rankComparator(input: ArbiterInput): Comparator<Pair<LiveActivitySpec, Tier>> {
    val bucketIndex = { b: OrderBucket -> input.bucketOrder.indexOf(b).let { if (it < 0) input.bucketOrder.size + b.ordinal else it } }
    return compareBy<Pair<LiveActivitySpec, Tier>> { it.second }
        .thenBy { (spec, tier) -> if (tier == Tier.T2_ONGOING && spec.packageName != null && spec.packageName in input.priorityApps) 0 else 1 }
        .thenBy { (spec, _) -> bucketIndex(spec.bucket) }
        .thenByDescending { (spec, _) -> spec.lastSignificantAt }
        .thenBy { (spec, _) -> spec.startedAt }
        .thenBy { (spec, _) -> spec.id.value }
}
