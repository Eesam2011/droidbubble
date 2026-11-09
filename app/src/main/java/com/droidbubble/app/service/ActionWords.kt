package com.droidbubble.app.service

import java.util.Locale

/**
 * Whole-word matching for notification button titles (B4.3): "Send", "Weekend" or "Recommend"
 * never count as "end". A word or phrase matches only where neither side touches a letter or
 * digit, which also works for multi-word phrases ("hang up") and non-Latin text.
 * Phase 7 A4 replaces the English word tables; it can keep delegating here.
 */
fun String.hasWord(vararg words: String): Boolean {
    val haystack = lowercase(Locale.ROOT)
    return words.any { haystack.containsWholeWord(it.lowercase(Locale.ROOT)) }
}

private fun String.containsWholeWord(needle: String): Boolean {
    if (needle.isEmpty()) return false
    var at = indexOf(needle)
    while (at >= 0) {
        if (!getOrNull(at - 1).isWordChar() && !getOrNull(at + needle.length).isWordChar()) return true
        at = indexOf(needle, at + 1)
    }
    return false
}

private fun Char?.isWordChar(): Boolean = this != null && (isLetter() || isDigit())
