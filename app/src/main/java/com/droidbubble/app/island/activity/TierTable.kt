package com.droidbubble.app.island.activity

/**
 * A2.1: the tier table. Which kinds belong where is decided by the adapters (LegacySpecMapper
 * today); this names the tiers for the "why" text and documents the members.
 *
 * | Tier | Members |
 * |---|---|
 * | T0 interrupt | ringing call, ringing alarm, timer "Time's up" |
 * | T1 live | ongoing call, navigation, voice or screen recording, timer and stopwatch (running or paused), a Live Update with an ETA or chip |
 * | T2 ongoing | media, downloads, live score, flashlight, VPN, privacy pill, snoozed alarm, a Live Update without ETA, pinned |
 * | T3 banner | messages, missed call |
 * | T4 glance | charging, low / full battery, Bluetooth / Wi-Fi, ringer, DND, unlock, VPN on/off |
 * | T5 ambient | nothing today (weather and network speed were removed) |
 */
val Tier.short: String get() = "T$ordinal"

val Tier.label: String get() = when (this) {
    Tier.T0_INTERRUPT -> "interrupt"
    Tier.T1_LIVE -> "live"
    Tier.T2_ONGOING -> "ongoing"
    Tier.T3_BANNER -> "banner"
    Tier.T4_GLANCE -> "glance"
    Tier.T5_AMBIENT -> "ambient"
}

/** The user's drag order as buckets; unknown keys are skipped. */
fun bucketOrderOf(featurePriorities: List<String>): List<OrderBucket> =
    featurePriorities.mapNotNull(OrderBucket::fromStored)
