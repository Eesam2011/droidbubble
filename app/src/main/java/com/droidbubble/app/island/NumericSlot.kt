package com.droidbubble.app.island

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

/** [template] with every digit replaced by "0": the widest a value of that shape gets with tabular digits. */
internal fun slotTemplate(template: String): String = template.map { if (it.isDigit()) '0' else it }.joinToString("")

/** The width a [NumericSlot] reserves: the widest of [template] (digits as "0") and [alsoFits]. */
@Composable
fun rememberNumericSlotWidth(template: String, style: TextStyle, alsoFits: List<String> = emptyList()): Dp {
    val measurer = rememberTextMeasurer()
    val zeros = slotTemplate(template)
    return with(LocalDensity.current) {
        remember(zeros, style, alsoFits) {
            (listOf(zeros) + alsoFits).maxOf { measurer.measure(it, style).size.width }.toDp()
        }
    }
}

/**
 * A live number in a fixed-width slot (F4.4). The slot is as wide as [template] with its digits
 * set to "0" (for example "100%", "00:00"), so the layout around it never shifts while the
 * value changes. [style] should use tabular digits (`tnum`).
 *
 * @param alsoFits other texts the slot may show, such as a "--:--" placeholder
 * @param content draws the value; leave it out for a plain [Text]. Pass a `RollingNumberText`
 * to roll the digits.
 */
@Composable
fun NumericSlot(
    text: String,
    style: TextStyle,
    template: String,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal = Alignment.End,
    color: Color = Color.Unspecified,
    alsoFits: List<String> = emptyList(),
    content: (@Composable (String) -> Unit)? = null,
) {
    val width = rememberNumericSlotWidth(template, style, alsoFits)
    if (content == null) {
        Text(
            text = text,
            color = color,
            style = style,
            maxLines = 1,
            textAlign = when (align) {
                Alignment.Start -> TextAlign.Start
                Alignment.CenterHorizontally -> TextAlign.Center
                else -> TextAlign.End
            },
            modifier = modifier.width(width)
        )
    } else {
        val boxAlignment = when (align) {
            Alignment.Start -> Alignment.CenterStart
            Alignment.CenterHorizontally -> Alignment.Center
            else -> Alignment.CenterEnd
        }
        Box(modifier = modifier.width(width), contentAlignment = boxAlignment) { content(text) }
    }
}
