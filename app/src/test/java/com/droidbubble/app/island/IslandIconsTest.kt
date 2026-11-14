package com.droidbubble.app.island

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V7.1 / V7.3: one icon family (Material Symbols Rounded), reached only through [IslandIcons],
 * with every glyph at optical size 24 and, in [IslandIcons.Compact], at 20.
 */
class IslandIconsTest {

    private fun entriesOf(owner: Any): Map<String, ImageVector> =
        owner::class.java.declaredMethods
            .filter { it.returnType == ImageVector::class.java && it.parameterCount == 0 }
            .associate { it.name.removePrefix("get") to it.invoke(owner) as ImageVector }

    private val entries = entriesOf(IslandIcons)
    private val compact = entriesOf(IslandIcons.Compact)

    @Test
    fun catalogueIsNotEmpty() {
        assertTrue("found ${entries.size} entries", entries.size >= 25)
    }

    @Test
    fun everyEntryIsAMaterialSymbol() {
        val other = entries.filterValues { !it.name.startsWith("Symbols.") || it.name.endsWith(".20") }.mapValues { it.value.name }
        assertEquals("card entries outside Material Symbols at optical size 24", emptyMap<String, String>(), other)
        val otherCompact = compact.filterValues { !(it.name.startsWith("Symbols.") && it.name.endsWith(".20")) }.mapValues { it.value.name }
        assertEquals("compact entries outside Material Symbols at optical size 20", emptyMap<String, String>(), otherCompact)
    }

    @Test
    fun compactMirrorsTheCatalogue() {
        assertEquals(entries.keys, compact.keys)
        entries.forEach { (k, v) -> assertEquals(k, v.name + ".20", compact.getValue(k).name) }
    }

    @Test
    fun islandCodeUsesTheCatalogueOnly() {
        val src = File("src/main/java/com/droidbubble/app/island")
        val pattern = Regex("""\bIcons\.(Default|Filled|Rounded|Outlined|Sharp|TwoTone|AutoMirrored)\b|material\.icons|\bIslandGlyphs\b""")
        val hits = src.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "IslandIcons.kt" }
            .flatMap { f -> f.readLines().mapIndexedNotNull { i, line -> if (pattern.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null } }
            .toList()
        assertEquals("use IslandIcons instead of Icons.* or IslandGlyphs directly", emptyList<String>(), hits)
    }
}
