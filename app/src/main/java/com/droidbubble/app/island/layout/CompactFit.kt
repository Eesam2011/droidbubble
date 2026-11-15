package com.droidbubble.app.island.layout

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em

/**
 * Text in the compact pill never ends in "…" (V3.2). When it doesn't fit its side, in order:
 * 1. squeeze: 92% width with slightly tighter letters (no further: it would distort the strokes);
 * 2. abbreviate: a shorter known form, or a name's first word;
 * 3. drop: the text goes and the icon stays.
 */
object CompactFit {
    const val SQUEEZE = 0.92f
    val SQUEEZE_LETTER_SPACING = (-0.01).em

    /** [text] null means dropped. */
    data class Result(val text: String?, val scaleX: Float = 1f) {
        val dropped: Boolean get() = text == null
    }

    /** Known shorter forms. A form of null means: drop rather than shorten. */
    private val ABBREVIATIONS: Map<String, String?> = mapOf(
        "Google Play" to "Play",
        "Google Play Store" to "Play",
        "Download" to null,
        "Downloading" to null,
        "Notification" to null,
    )

    /** Shorter forms to try, best first. Numbers are never shortened. */
    fun shorterForms(text: String): List<String> {
        if (text.any { it.isDigit() }) return emptyList()
        val out = mutableListOf<String>()
        if (ABBREVIATIONS.containsKey(text)) {
            ABBREVIATIONS[text]?.let(out::add)
            return out
        }
        val words = text.trim().split(Regex("\\s+"))
        if (words.size > 1) out += words.first()
        return out
    }

    /** [width] gives a text's width in dp, plain or squeezed. */
    fun fit(text: String, available: Float, width: (String, Boolean) -> Float): Result {
        for (candidate in listOf(text) + shorterForms(text)) {
            if (width(candidate, false) <= available) return Result(candidate)
            if (width(candidate, true) <= available) return Result(candidate, SQUEEZE)
        }
        return Result(null)
    }

    /** A style with the squeeze applied, for a [Result] with scaleX below 1. */
    fun squeezed(style: TextStyle, result: Result): TextStyle =
        if (result.scaleX >= 1f) style
        else style.merge(TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = result.scaleX), letterSpacing = SQUEEZE_LETTER_SPACING))

    fun measure(measurer: TextMeasurer, density: Density, text: String, style: TextStyle, squeeze: Boolean): Float = with(density) {
        val s = if (squeeze) squeezed(style, Result(text, SQUEEZE)) else style
        measurer.measure(text, s, maxLines = 1, softWrap = false).size.width.toDp().value
    }
}

/** [CompactFit.fit] for [text] in [style] (merged onto the island's base style), within [available]. */
@Composable
fun rememberCompactFit(text: String?, available: Dp, style: TextStyle): CompactFit.Result {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val density = LocalDensity.current
    return remember(text, available, style, base, density) {
        if (text == null) CompactFit.Result(null) else {
            val full = base.merge(style)
            CompactFit.fit(text, available.value) { t, squeeze -> CompactFit.measure(measurer, density, t, full, squeeze) }
        }
    }
}
