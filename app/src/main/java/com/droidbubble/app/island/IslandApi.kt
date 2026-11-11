package com.droidbubble.app.island

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * What the island can use on this Android version (Decision D1: `minSdk` stays 29, so the newer
 * effects are gated here and each has a fallback).
 *
 * | Feature | Needs | Fallback |
 * |---|---|---|
 * | Squircle corners (anti-aliased path clip) | Android 13 | regular rounded corners |
 * | AGSL shaders (the liquid neck, epic D1) | Android 13 | today's drawn neck |
 * | Blur (`RenderEffect`) | Android 12 | content fades without blur |
 */
object IslandApi {
    /** Android 13+: arbitrary paths clip with anti-aliasing, so surfaces can be squircles. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val hasSmoothPathClip: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 13+: `RuntimeShader` (AGSL). Check this before creating any island shader. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val hasRuntimeShader: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 12+: `RenderEffect` blur on a layer. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val hasRenderEffect: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
