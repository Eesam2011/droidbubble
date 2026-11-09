package com.droidbubble.app.service

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Album art at a bounded size (B10.1). A full-resolution decode of an art URI used to run on the
 * main thread, in the same frame as the pill's animation, and could run out of memory. Call these
 * off the main thread.
 */
object ArtDecoder {
    /** Big enough for the expanded card's art and its blur; the compact pill needs 256. */
    const val MAX_URI_PX = 512

    /** Metadata bitmaps above this are scaled down. */
    const val MAX_METADATA_PX = 1024

    /** The largest power-of-two sample size that keeps the longer side at or above [maxPx]. */
    fun sampleSizeFor(width: Int, height: Int, maxPx: Int): Int {
        val longer = maxOf(width, height)
        var sample = 1
        while (longer / (sample * 2) >= maxPx) sample *= 2
        return sample
    }

    /** Decodes [uri] sampled down towards [maxPx], or null if it can't be read. */
    fun decode(resolver: ContentResolver, uri: Uri, maxPx: Int = MAX_URI_PX): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxPx) }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?.let { scaleDown(it, maxPx) }
    }.getOrNull()

    /** [bitmap] with its longer side at most [maxPx], keeping the aspect ratio. */
    fun scaleDown(bitmap: Bitmap, maxPx: Int): Bitmap {
        val longer = maxOf(bitmap.width, bitmap.height)
        if (longer <= maxPx) return bitmap
        val scale = maxPx.toFloat() / longer
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}

/**
 * Which art decode may still apply (B10.1). Main thread only. A decode starts for a track key and
 * its result applies only if that key is still the latest one asked for, so skipping tracks quickly
 * never puts the previous track's art on the new one.
 */
class ArtRequests {
    private var latest: String? = null
    private var inFlight: String? = null
    private val failed = HashSet<String>()

    /** The track now on screen: a result for any other key is dropped. */
    fun current(key: String) {
        latest = key
    }

    /** True if a decode for [key] should start now (not already running and not failed before). */
    fun request(key: String): Boolean {
        latest = key
        if (key == inFlight || key in failed) return false
        inFlight = key
        return true
    }

    /** A decode for [key] finished. True if its result should be applied. */
    fun complete(key: String, success: Boolean): Boolean {
        if (inFlight == key) inFlight = null
        if (!success) {
            failed += key
            if (failed.size > 32) failed.clear()
        }
        return success && key == latest
    }
}
