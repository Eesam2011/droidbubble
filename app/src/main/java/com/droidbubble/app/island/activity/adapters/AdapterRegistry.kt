package com.droidbubble.app.island.activity.adapters

import com.droidbubble.app.island.activity.LiveActivitySpec
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.util.CrashLog

/**
 * A3.1: what an adapter may ask the listener for. Every member is a plain value or a callback the
 * listener answers, so adapters stay free of Android calls and testable on the JVM.
 */
class AdapterContext(
    val settings: IslandSettings,
    val foregroundPackage: String?,
    /** The custom view's Chronometer (inflates RemoteViews): ask only when ChronometerReader wants it. */
    val customViewTimer: () -> ChronometerReader.CustomViewTimer?,
    /**
     * The app label, bitmaps and (for a call) the CallStyle person: ask only for a notification you
     * keep. The argument says whether it is a call.
     */
    val details: (Boolean) -> SnapshotDetails,
    /** LiveTimerBaseStabilizer, owned by the listener: (key, raw base, base came from text) -> base. */
    val stabilizeBase: (String, Long, Boolean) -> Long,
    val previous: LiveActivitySpec? = null,
)

/** A3.1: what an adapter decided for one posted notification. */
sealed interface AdapterOutcome {
    /** Filtered out: a card showing for this key goes too. */
    data object Drop : AdapterOutcome
    /** Ignored: a card showing for this key stays (its app is on screen). */
    data object Ignore : AdapterOutcome
    /**
     * Kept. [info] feeds today's single-slot pipeline until A1.9 removes it; [spec] is the
     * activity the store holds.
     */
    data class Post(val info: NotificationInfo, val hasProgress: Boolean, val spec: LiveActivitySpec) : AdapterOutcome
}

/**
 * A3.1: one way of reading notifications. An adapter that accepts a snapshot decides for it; one
 * that doesn't leaves it to the next in the chain.
 *
 * Deviation from the sketch in the task (which returns `LiveActivitySpec?`): adapters return an
 * [AdapterOutcome], because the listener still needs Drop versus Ignore and, until A1.9, the
 * NotificationInfo of today's slots. Adapters produce data (slots), never composables.
 */
interface NotificationAdapter {
    /** For breadcrumbs (B11) and A2.5's "why". */
    val name: String
    fun accepts(n: NotificationSnapshot): Boolean
    fun adapt(n: NotificationSnapshot, ctx: AdapterContext): AdapterOutcome?
}

object AdapterRegistry {
    /**
     * Ordered: the first adapter that accepts and returns an outcome wins. A3.2–A3.6 and A8.1 add
     * theirs ahead of [LegacyAdapter], which encodes today's rules and is always last.
     */
    val chain: List<NotificationAdapter> = listOf(LegacyAdapter)

    /** Null only when every adapter failed; a failing adapter writes a breadcrumb and the next one runs. */
    fun adapt(n: NotificationSnapshot, ctx: AdapterContext, chain: List<NotificationAdapter> = this.chain): AdapterOutcome? =
        chain.firstNotNullOfOrNull { a ->
            if (!runCatching { a.accepts(n) }.getOrDefault(false)) {
                null
            } else {
                runCatching { a.adapt(n, ctx) }
                    .onFailure { CrashLog.breadcrumb("adapter", "${a.name} failed: ${it.javaClass.simpleName}") }
                    .getOrNull()
            }
        }
}
