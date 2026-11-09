package com.droidbubble.app.util

import android.content.Context
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the stack trace of the last crash in app storage, so a crash on a real phone (which the
 * JVM tests can't reproduce) can be copied from the settings screen and fixed at its source.
 * The system's own crash handling still runs afterwards.
 *
 * B11.1: it also keeps the last [MAX_BREADCRUMBS] island events in memory and adds them to the
 * crash report, plus a rotated log of caught (non-fatal) errors. Breadcrumbs must never hold user
 * text (titles, message bodies): package names, kinds, key hashes and window specs only.
 */
object CrashLog {
    private const val FILE_NAME = "last_crash.txt"
    private const val NONFATAL_FILE = "nonfatal.txt"
    const val MAX_BREADCRUMBS = 64
    const val MAX_NONFATAL = 20
    private const val NONFATAL_SEPARATOR = "----\n"

    @Volatile private var installed = false
    @Volatile private var appContext: Context? = null
    private val crumbs = ArrayDeque<String>(MAX_BREADCRUMBS)

    /** Breadcrumb timestamps; a test seam. */
    @JvmField var clock: () -> Long = SystemClock::elapsedRealtime

    fun install(context: Context) {
        attach(context)
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { appContext?.let { write(it, thread, error) } }
            // B11.3: crash times and the last primary activity, for the crash-loop guard
            runCatching { appContext?.let { SafeMode.recordCrash(it, System.currentTimeMillis(), SafeMode.lastPrimary(breadcrumbs())) } }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Where [nonFatal] writes. [install] calls it; tests call it without the crash handler. */
    fun attach(context: Context) {
        appContext = context.applicationContext ?: context
    }

    /** Records an island event. Oldest first, at most [MAX_BREADCRUMBS]. */
    fun breadcrumb(tag: String, msg: String) {
        val line = "${clock()} $tag $msg"
        synchronized(crumbs) {
            if (crumbs.size >= MAX_BREADCRUMBS) crumbs.removeFirst()
            crumbs.addLast(line)
        }
    }

    fun breadcrumbs(): List<String> = synchronized(crumbs) { crumbs.toList() }

    /**
     * A caught error: a breadcrumb plus an entry in `nonfatal.txt` (the last [MAX_NONFATAL]).
     * The entry holds the error's class and top frames, not its message, which can quote content.
     */
    fun nonFatal(tag: String, error: Throwable) {
        breadcrumb(tag, "nonfatal ${error.javaClass.name}")
        val context = appContext ?: return
        runCatching {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = buildString {
                appendLine("$time $tag ${error.javaClass.name}")
                error.stackTrace.take(8).forEach { appendLine("    at $it") }
            }
            val file = File(context.filesDir, NONFATAL_FILE)
            val old = if (file.exists()) file.readText().split(NONFATAL_SEPARATOR).filter { it.isNotBlank() } else emptyList()
            val kept = (old + entry).takeLast(MAX_NONFATAL)
            file.writeText(kept.joinToString("") { it + NONFATAL_SEPARATOR })
        }
    }

    /** The logged non-fatal errors, oldest first. */
    fun readNonFatal(context: Context): List<String> = runCatching {
        File(context.filesDir, NONFATAL_FILE).takeIf { it.exists() }?.readText()
            ?.split(NONFATAL_SEPARATOR)?.filter { it.isNotBlank() }
    }.getOrNull().orEmpty()

    internal fun report(thread: Thread, error: Throwable, time: Date = Date()): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(time)
        return buildString {
            appendLine("Droidbubble crash at $stamp")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace)
            appendLine()
            appendLine("Breadcrumbs (oldest first, elapsedRealtime ms):")
            breadcrumbs().forEach { appendLine("  $it") }
        }
    }

    internal fun write(context: Context, thread: Thread, error: Throwable) {
        File(context.filesDir, FILE_NAME).writeText(report(thread, error))
    }

    /** The last crash report, or null if the app hasn't crashed since it was cleared. */
    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
        runCatching { File(context.filesDir, NONFATAL_FILE).delete() }
    }

    /** Short, non-reversible id for a notification key, so breadcrumbs can follow one card. */
    fun keyHash(key: String?): String = key?.let { Integer.toHexString(it.hashCode() and 0xffff) } ?: "-"

    @androidx.annotation.VisibleForTesting
    internal fun resetForTest() {
        synchronized(crumbs) { crumbs.clear() }
        appContext = null
    }
}
