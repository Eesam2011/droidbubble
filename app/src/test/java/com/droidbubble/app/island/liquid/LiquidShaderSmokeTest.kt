package com.droidbubble.app.island.liquid

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D1.1: the AGSL compiles and takes every uniform [LiquidBodyShader.update] sets (`RuntimeShader`
 * throws on a compile error or an unknown uniform). Drawing needs hardware rendering, which
 * Robolectric's software canvas refuses, so the drawn result is covered by [LiquidSdfTest]'s
 * mirror of the same maths and checked on a device in D1.2.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidShaderSmokeTest {

    @Test
    fun compilesAndTakesItsUniforms() {
        val h = 18f
        val shader = LiquidBodyShader()
        shader.update(
            pill = LiquidSdf.Body(100f, 40f, 90f, h, h),
            bubble = LiquidSdf.Body(211f, 40f, h, h, h),
            k = 10f,
            bulge = 3f,
            color = Color.Black,
            outlineWidth = 3f,
            outlineColor = Color.White.copy(alpha = 0.55f),
        )
        assertNotNull(shader.brush.createShader(Size(260f, 80f)))
    }

    /** Proves the check above is real: the compiler runs here and rejects bad AGSL and unknown uniforms. */
    @Test
    fun theCompilerReallyRuns() {
        assertThrows(IllegalArgumentException::class.java) {
            android.graphics.RuntimeShader("half4 main(float2 xy) { return notDeclared; }")
        }
        assertThrows(IllegalArgumentException::class.java) {
            android.graphics.RuntimeShader(LiquidBodyShader.SRC).setFloatUniform("uNoSuchUniform", 1f)
        }
    }
}
