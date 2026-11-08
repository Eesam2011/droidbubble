package com.droidbubble.app.util

import androidx.tracing.Trace

/**
 * Labelled trace sections for island work, so a Perfetto trace shows notification parsing, media
 * refreshes and window updates next to the frames they delay. Costs about nothing when no trace
 * is being recorded. Labels start with `db.`; see `app/perfetto/island.pbtxt` for the recipe.
 */
internal inline fun <T> traced(label: String, block: () -> T): T {
    Trace.beginSection(label)
    try {
        return block()
    } finally {
        Trace.endSection()
    }
}

/** Counter track `db.root.compositions`: one step per composition of the island root. */
internal object IslandTraceCounters {
    private var rootCompositions = 0

    fun rootComposed() {
        if (!Trace.isEnabled()) return
        Trace.setCounter("db.root.compositions", ++rootCompositions)
    }
}
