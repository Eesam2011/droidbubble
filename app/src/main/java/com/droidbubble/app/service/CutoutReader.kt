package com.droidbubble.app.service

import android.content.Context
import android.graphics.Point
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import com.droidbubble.app.island.layout.CameraGeometry
import com.droidbubble.app.island.layout.CutoutCalibration
import com.droidbubble.app.island.layout.CutoutRect

/**
 * Reads the phone's camera cutout (V1.1) and hands it to [CutoutCalibration]. It goes through
 * [DisplayManager]'s default display, which works from a Service context on every supported API
 * level (a window context isn't needed). Any failure means "no cutout": the island then keeps
 * its manual placement.
 */
object CutoutReader {
    private const val TAG = "CutoutReader"

    fun read(context: Context): CameraGeometry = runCatching {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return CameraGeometry.None
        val cutout = display.cutout ?: return CameraGeometry.None
        val size = Point().also {
            @Suppress("DEPRECATION")
            display.getRealSize(it)
        }
        val rects = cutout.boundingRects.map { CutoutRect(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) }
        val pathBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cutout.cutoutPath?.let { path ->
                val bounds = RectF()
                @Suppress("DEPRECATION")
                path.computeBounds(bounds, true)
                if (bounds.isEmpty) null else CutoutRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
        } else null
        val density = context.resources.displayMetrics.density
        CutoutCalibration.fromRects(rects, pathBounds, size.x, size.y, density, display.rotation)
    }.getOrElse {
        Log.w(TAG, "cutout read failed", it)
        CameraGeometry.None
    }
}
