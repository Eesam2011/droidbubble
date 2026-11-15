package com.droidbubble.app.island.liquid

import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb

/**
 * The pill, the bubble and the neck between them as one black body (epic D1.1): an AGSL
 * signed-distance smooth union. Android 13+ only; check `IslandApi.hasRuntimeShader` first and
 * keep today's drawn neck otherwise. Not wired yet (D1.2).
 *
 * One instance per window: [update] only sets uniforms, it allocates nothing per frame. The maths
 * is mirrored in [LiquidSdf].
 */
@RequiresApi(33)
class LiquidBodyShader {
    private val shader = RuntimeShader(SRC)

    /** Draw it with `drawRect(brush)` behind the pill and bubble content. */
    val brush: ShaderBrush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader = shader
    }

    fun update(
        pill: LiquidSdf.Body,
        bubble: LiquidSdf.Body,
        k: Float,
        bulge: Float,
        color: Color,
        outlineWidth: Float = 0f,
        outlineColor: Color = Color.Transparent,
    ) {
        shader.setFloatUniform("uPill", pill.cx, pill.cy, pill.halfW, pill.halfH)
        shader.setFloatUniform("uPillR", pill.r)
        shader.setFloatUniform("uBubble", bubble.cx, bubble.cy, bubble.halfW, bubble.halfH)
        shader.setFloatUniform("uBubbleR", bubble.r)
        shader.setFloatUniform("uK", k)
        shader.setFloatUniform("uBulge", bulge)
        shader.setFloatUniform("uAA", LiquidSdf.AA_PX)
        shader.setFloatUniform("uOutlineW", outlineWidth)
        shader.setColorUniform("uColor", color.toArgb())
        shader.setColorUniform("uOutlineColor", outlineColor.toArgb())
    }

    companion object {
        /** Keep in step with [LiquidSdf]. */
        const val SRC = """
uniform float4 uPill;
uniform float uPillR;
uniform float4 uBubble;
uniform float uBubbleR;
uniform float uK;
uniform float uBulge;
uniform float uAA;
uniform float uOutlineW;
layout(color) uniform half4 uColor;
layout(color) uniform half4 uOutlineColor;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float smin(float a, float b, float k) {
    if (k <= 0.0) return min(a, b);
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

half4 main(float2 xy) {
    float dP = sdRoundBox(xy - uPill.xy, uPill.zw + float2(uBulge, 0.0), uPillR);
    float dB = sdRoundBox(xy - uBubble.xy, uBubble.zw, uBubbleR);
    float d = smin(dP, dB, uK);
    float fill = 1.0 - smoothstep(-uAA, uAA, d);
    float ring = (uOutlineW > 0.0) ? 1.0 - smoothstep(-uAA, uAA, abs(d + uOutlineW * 0.5) - uOutlineW * 0.5) : 0.0;
    return mix(uColor * fill, uOutlineColor, ring * uOutlineColor.a);
}
"""
    }
}
