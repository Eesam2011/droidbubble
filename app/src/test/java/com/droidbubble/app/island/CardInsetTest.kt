package com.droidbubble.app.island

import com.droidbubble.app.island.layout.CardGeometry
import com.droidbubble.app.model.IslandPriority
import org.junit.Assert.assertEquals
import org.junit.Test

/** V2.2: one margin system. Every card's inset comes from CardGeometry, and the measures use the same one. */
class CardInsetTest {

    @Test
    fun eachPriorityMapsToItsInset() {
        assertEquals(CardInsetKind.TIGHT, cardInsetKind(IslandPriority.FLASHLIGHT))
        for (p in IslandPriority.entries.filter { it != IslandPriority.FLASHLIGHT }) {
            assertEquals(p.name, CardInsetKind.ROOMY, cardInsetKind(p))
        }
    }

    @Test
    fun valuesFollowTheCardRadius() {
        // s 1.1: R = 42.9, roomy = R/2, tight = m
        assertEquals(21.45f, cardInsetDp(IslandPriority.MEDIA, 1.1f, 1f).value, 0.01f)
        assertEquals(15.4f, cardInsetDp(IslandPriority.FLASHLIGHT, 1.1f, 1f).value, 0.01f)
        // s 0.8 and 1.3, as in the plan's table
        assertEquals(CardGeometry(0.8f).R.value / 2f, cardInsetDp(IslandPriority.POP_NOTIFICATION, 0.8f, 1f).value, 0.01f)
        assertEquals(CardGeometry(1.3f).R.value / 2f, cardInsetDp(IslandPriority.PROGRESS, 1.3f, 1f).value, 0.01f)
        // Large text grows the card radius, and the inset with it, so nested corners stay concentric
        assertEquals(CardGeometry(1.1f, 1.5f).R.value / 2f, cardInsetDp(IslandPriority.MEDIA, 1.1f, 1.5f).value, 0.01f)
    }

    @Test
    fun theMeasuresUseTheSameInsetAsTheViews() {
        for (s in listOf(0.8f, 1.1f, 1.3f)) for (f in listOf(1f, 1.3f, 1.5f)) {
            val n = NotificationCardMetrics(s, f)
            assertEquals(cardInsetDp(IslandPriority.POP_NOTIFICATION, s, f), n.horizontalPadding)
            assertEquals(cardInsetDp(IslandPriority.POP_NOTIFICATION, s, f), n.verticalPadding)
            assertEquals(cardInsetDp(IslandPriority.PROGRESS, s, f), ProgressCardMetrics(s, f).verticalPadding)
        }
    }

    @Test
    fun changesStayWithinTheBudget() {
        // The shift from today's paddings (20·s horizontal, 18–20·s vertical, clamped) is at most 4.6 dp at font 1.0
        for (s in listOf(0.8f, 1.0f, 1.1f, 1.3f)) {
            val roomy = cardInsetDp(IslandPriority.MEDIA, s, 1f).value
            val oldH = (20f * s).coerceIn(14f, 24f)
            val oldNotifV = (18f * s).coerceIn(10f, 22f)
            assertEquals("media at $s", oldH, roomy, 4.6f)
            assertEquals("notification vertical at $s", oldNotifV, roomy, 4.6f)
        }
    }
}
