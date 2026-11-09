package com.droidbubble.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** B10.1: album art is decoded at a bounded size, and a stale decode never lands on a new track. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtDecoderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun pngUri(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val file = File(context.cacheDir, "art_${width}x$height.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun hugeUriArt_decodesToAtMost512() {
        val art = ArtDecoder.decode(context.contentResolver, pngUri(4000, 4000))
        assertNotNull(art)
        assertTrue("decoded ${art!!.width}x${art.height}", art.width <= 512 && art.height <= 512)
        assertTrue(art.width >= 256)
    }

    @Test
    fun wideArt_keepsItsAspect() {
        val art = ArtDecoder.decode(context.contentResolver, pngUri(3000, 1000))!!
        assertEquals(512, art.width)
        assertEquals(170, art.height)
    }

    @Test
    fun unreadableUri_isNull() {
        assertNull(ArtDecoder.decode(context.contentResolver, Uri.fromFile(File(context.cacheDir, "missing.png"))))
    }

    @Test
    fun sampleSize_isTheLargestPowerOfTwoAboveTheLimit() {
        assertEquals(1, ArtDecoder.sampleSizeFor(500, 500, 512))
        assertEquals(1, ArtDecoder.sampleSizeFor(1000, 1000, 512))
        assertEquals(2, ArtDecoder.sampleSizeFor(1024, 1024, 512))
        assertEquals(4, ArtDecoder.sampleSizeFor(4000, 4000, 512))
    }

    @Test
    fun oversizedMetadataBitmap_scalesTo1024() {
        val big = Bitmap.createBitmap(2048, 1536, Bitmap.Config.ARGB_8888)
        val scaled = ArtDecoder.scaleDown(big, ArtDecoder.MAX_METADATA_PX)
        assertEquals(1024, scaled.width)
        assertEquals(768, scaled.height)
        val small = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        assertTrue(ArtDecoder.scaleDown(small, ArtDecoder.MAX_METADATA_PX) === small)
    }

    @Test
    fun decodesFinishingOutOfOrder_onlyTheCurrentTrackApplies() {
        val requests = ArtRequests()
        assertTrue(requests.request("A"))
        assertFalse("no second decode for the same track", requests.request("A"))
        requests.current("B")
        assertTrue(requests.request("B"))
        // B finishes first, then the slow A: A must not replace B's art
        assertTrue(requests.complete("B", success = true))
        assertFalse(requests.complete("A", success = true))
    }

    @Test
    fun failedDecode_isNotRetriedForTheSameTrack() {
        val requests = ArtRequests()
        assertTrue(requests.request("A"))
        assertFalse(requests.complete("A", success = false))
        assertFalse(requests.request("A"))
    }
}
