package com.droidbubble.app.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.TreeMap

/**
 * F2.1: the "no new literals" gate (PLAN §6). Counts hard-coded colors, sp sizes, springs, tweens
 * and haptic calls in every file under `island/`, and compares them with
 * `src/test/resources/island-literal-baseline.properties`.
 *
 * - A count that goes **up** fails: put the value in `IslandTokens` (or `IslandMotion`,
 *   `IslandType`, `IslandHaptics`, `IslandTimings`) instead.
 * - A count that goes **down** fails too, until the baseline is lowered **in the same commit**.
 *   That is the ratchet: a number that was removed can't quietly come back.
 *
 * Rewrite the baseline from the current tree:
 * `./gradlew :app:testDebugUnitTest --tests "*IslandLiteralRatchetTest" -Pisland.record=true`
 *
 * `dp` is not counted: layout dp inside a card is legitimate.
 */
class IslandLiteralRatchetTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // Unit tests run from the module directory (app/)
    private val src = File("src/main/java/com/droidbubble/app/island")
    private val baselineFile = File("src/test/resources/island-literal-baseline.properties")

    @Test
    fun countsNeverGrow() {
        assertTrue("island sources not found at ${src.absolutePath}", src.isDirectory)
        val current = LiteralRatchet.count(src)
        if (System.getProperty("island.record") == "true") {
            baselineFile.parentFile.mkdirs()
            baselineFile.writeText(LiteralRatchet.format(current))
            println("Recorded ${current.size} literal counts into ${baselineFile.absolutePath}")
            return
        }
        val problems = LiteralRatchet.compare(LiteralRatchet.parse(baselineFile.readText()), current)
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun selfTest_aNewLiteralFails_andNamesTheFileAndRule() {
        val dir = tmp.newFolder("island")
        val view = File(dir, "Sample.kt")
        view.writeText(
            """
            val a = Color(0xFF000000)
            val b = 12.sp
            val c = spring<Float>(dampingRatio = 0.8f)
            """.trimIndent()
        )
        val baseline = LiteralRatchet.count(dir)
        assertEquals(mapOf("Sample.kt.hex" to 1, "Sample.kt.sp" to 1, "Sample.kt.spring" to 1), baseline)
        assertTrue(LiteralRatchet.compare(baseline, LiteralRatchet.count(dir)).isEmpty())

        view.appendText("\nval d = Color(0xFFFFFFFF)\n")
        val grown = LiteralRatchet.compare(baseline, LiteralRatchet.count(dir))
        assertEquals(1, grown.size)
        assertTrue(grown[0], "Sample.kt" in grown[0] && "hex" in grown[0] && "1 -> 2" in grown[0])

        view.writeText("val b = 12.sp\n")
        val shrunk = LiteralRatchet.compare(baseline, LiteralRatchet.count(dir))
        assertEquals("a lower count asks for a lower baseline", 2, shrunk.size)
        assertTrue(shrunk.all { "lower the baseline" in it })
    }

    @Test
    fun selfTest_rules() {
        val dir = tmp.newFolder("rules")
        File(dir, "sub").mkdirs()
        File(dir, "sub/Deep.kt").writeText(
            """
            val size = (13 * scale).coerceIn(11.5f, 15f).sp
            val t = tween<Float>(120)
            val u = tween(durationMillis = 90)
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val spec = 4.5.sp
            val notALiteral = fontSize.sp
            val springy = springBack()
            val routed = motion.spring(motion.press) + motion.tween(MotionTween.ROLL)
            """.trimIndent()
        )
        // Token files may hold literals: that is where they belong
        File(dir, "IslandTokens.kt").writeText("val x = Color(0xFF112233)\nval y = 10.sp\n")
        assertEquals(
            mapOf("sub/Deep.kt.haptic" to 1, "sub/Deep.kt.sp" to 2, "sub/Deep.kt.tween" to 2),
            LiteralRatchet.count(dir)
        )
    }

    @Test
    fun selfTest_formatRoundTrips() {
        val counts = mapOf("A.kt.hex" to 3, "b/C.kt.sp" to 1)
        assertEquals(counts, LiteralRatchet.parse(LiteralRatchet.format(counts)))
    }
}

/** The counting behind [IslandLiteralRatchetTest]. Keys are `<path under island/>.<rule>`. */
object LiteralRatchet {
    private val rules = linkedMapOf(
        "hex" to Regex("""Color\(0x"""),
        "sp" to Regex("""\b\d+(\.\d+)?\.sp\b|\(\s*\d+(\.\d+)?f?\s*\*\s*scale\s*\)[^\n]*\.sp"""),
        // Calls on the motion object (`motion.spring(motion.press)`) are the token route: not counted
        "spring" to Regex("""(?<![.\w])spring(<[^>]*>)?\("""),
        "tween" to Regex("""(?<![.\w])tween(<[^>]*>)?\("""),
        "haptic" to Regex("""performHapticFeedback|VibrationEffect|Vibrator"""),
    )

    /** Files that define the tokens, so they hold the literals. */
    private val exempt = setOf("IslandTokens.kt", "IslandMotion.kt", "IslandType.kt", "IslandHaptics.kt", "IslandTimings.kt")

    fun count(root: File): Map<String, Int> {
        val out = TreeMap<String, Int>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name !in exempt }.forEach { file ->
            val text = file.readText()
            val path = file.relativeTo(root).invariantSeparatorsPath
            for ((rule, regex) in rules) {
                val n = regex.findAll(text).count()
                if (n > 0) out["$path.$rule"] = n
            }
        }
        return out
    }

    fun compare(baseline: Map<String, Int>, current: Map<String, Int>): List<String> =
        (baseline.keys + current.keys).toSortedSet().mapNotNull { key ->
            val was = baseline[key] ?: 0
            val now = current[key] ?: 0
            val file = key.substringBeforeLast('.')
            val rule = key.substringAfterLast('.')
            when {
                now > was -> "$file: $rule literals went up, $was -> $now. Use a token instead (IslandTokens, IslandMotion, IslandType, IslandHaptics)."
                now < was -> "$file: $rule literals went down, $was -> $now. Good: lower the baseline to $now in island-literal-baseline.properties."
                else -> null
            }
        }

    fun format(counts: Map<String, Int>): String =
        "# Literal counts per island file (F2.1). The numbers only go down. See IslandLiteralRatchetTest.\n" +
            counts.toSortedMap().entries.joinToString("") { (k, v) -> "$k=$v\n" }

    fun parse(text: String): Map<String, Int> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { it.substringBeforeLast('=') to it.substringAfterLast('=').toInt() }
}
