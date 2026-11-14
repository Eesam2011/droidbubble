package com.droidbubble.app.island

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V7.1: one icon family (Material Rounded), reached only through [IslandIcons]. */
class IslandIconsTest {

    private val entries: Map<String, ImageVector> =
        IslandIcons::class.java.declaredMethods
            .filter { it.returnType == ImageVector::class.java && it.parameterCount == 0 }
            .associate { it.name.removePrefix("get") to it.invoke(IslandIcons) as ImageVector }

    @Test
    fun catalogueIsNotEmpty() {
        assertTrue("found ${entries.size} entries", entries.size >= 25)
    }

    @Test
    fun everyEntryIsRounded() {
        val notRounded = entries.filterValues { v ->
            !(v.name.startsWith("Rounded.") || v.name.startsWith("AutoMirrored.Rounded."))
        }.mapValues { it.value.name }
        assertEquals("entries outside the Rounded family", emptyMap<String, String>(), notRounded)
    }

    @Test
    fun islandCodeUsesTheCatalogueOnly() {
        val src = File("src/main/java/com/droidbubble/app/island")
        val pattern = Regex("""\bIcons\.(Default|Filled|Rounded|Outlined|Sharp|TwoTone|AutoMirrored)\b|material\.icons\.filled""")
        val hits = src.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "IslandIcons.kt" }
            .flatMap { f -> f.readLines().mapIndexedNotNull { i, line -> if (pattern.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null } }
            .toList()
        assertEquals("use IslandIcons instead of Icons.* directly", emptyList<String>(), hits)
    }
}
