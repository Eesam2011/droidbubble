package com.droidbubble.app.island.activity.text

/**
 * A4.2: text normalisation before any matching or number parsing.
 *
 * - Arabic-Indic (U+0660–0669) and Persian (U+06F0–06F9) digits become ASCII, so "٠٠:٤٢" parses
 *   like "00:42". Patterns then use `[0-9]`, never `\d` (the JVM's `\d` is ASCII, Android's ICU
 *   `\d` is Unicode, so tests wouldn't predict the phone).
 * - Arabic: tashkeel (harakat, tatweel) is stripped and the alef forms (أ إ آ ٱ) unify to ا, so
 *   "إلغاء" and "الغاء" match the same token.
 */
object Digits {

    fun normalizeDigits(s: String): String {
        if (s.none { it in '٠'..'٩' || it in '۰'..'۹' }) return s
        val out = StringBuilder(s.length)
        for (c in s) {
            out.append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    else -> c
                }
            )
        }
        return out.toString()
    }

    private val TASHKEEL = Regex("[ً-ٰٟـ]")

    /** Digits to ASCII, Arabic tashkeel stripped and alef forms unified. Case is left alone. */
    fun normalize(s: String): String {
        val digits = normalizeDigits(s)
        if (digits.none { it in '؀'..'ۿ' }) return digits
        return TASHKEEL.replace(digits, "")
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    }

    // "200 m", "1.2 km", "٢٠٠ م", "1,5 km" (units backed by the nav/ fixtures: m, km, م, كم)
    private val DISTANCE = Regex("""(?<![\p{L}\p{N}])([0-9]+(?:[.,][0-9]+)?)\s*(km|m|كم|م)(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

    /** The first distance in [s], in metres, or null. */
    fun parseMeters(s: String?): Int? {
        val m = DISTANCE.find(normalizeDigits(s ?: return null)) ?: return null
        val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = m.groupValues[2].lowercase()
        return if (unit == "km" || unit == "كم") (value * 1000).toInt() else value.toInt()
    }
}
