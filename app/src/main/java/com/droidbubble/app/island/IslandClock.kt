package com.droidbubble.app.island

/**
 * The wall clock the island UI reads for live timers (countdowns, stopwatches, the clock hands).
 * Production uses the system clock. Screenshot tests freeze it, so timer text is identical run to
 * run.
 */
object IslandClock {
    @Volatile
    @JvmField
    var wallMs: () -> Long = System::currentTimeMillis
}
