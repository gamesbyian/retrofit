package dev.gamesbyian.retrofit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Installed native Android bitmap experiment only. No production editor, external generator,
 * customer media, AYA recipient, camera registration, or manufacturer geometry is exercised.
 */
@RunWith(AndroidJUnit4::class)
class LocalPixelProtectionInstrumentedTest {

    @Test fun preservesAllOpaqueOriginalPixelsOutsideBinaryMaskAndAfterPngRoundtrip() {
        val width = 196
        val height = 128
        val original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val candidate = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mask = ByteArray(width * height)
        val expected = IntArray(width * height)
        try {
            for (y in 0 until height) for (x in 0 until width) {
                val i = y * width + x
                val red = (x * 13 + y * 7) and 0xff
                val color = Color.rgb(red, 110, 170)
                original.setPixel(x, y, color)
                expected[i] = color
                candidate.setPixel(x, y, Color.rgb(255 - red, 20, 90))
                if (x in 33..109 && y in 24..96) mask[i] = 255.toByte()
            }
            val evidence = LocalPixelProtection.composite(
                original, candidate, mask, sameOrientedCameraFrameVerified = true
            )
            val output = evidence.bitmap
            try {
                assertEquals(0, evidence.evidence.driftedProtectedOutputPixels)
                assertTrue(evidence.evidence.driftedProtectedCandidatePixels > 0)
                assertTrue(evidence.evidence.protectedPixels > 0)
                assertTrue(evidence.evidence.allowedPixels > 0)
                assertEquals(width * height,
                    evidence.evidence.protectedPixels + evidence.evidence.allowedPixels)
                val encoded = ByteArrayOutputStream().also {
                    assertTrue(output.compress(Bitmap.CompressFormat.PNG, 100, it))
                }.toByteArray()
                val reloaded = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
                    ?: error("PNG re-decode failed")
                try {
                    for (y in 0 until height) for (x in 0 until width) {
                        val i = y * width + x
                        val actual = reloaded.getPixel(x, y)
                        if (mask[i].toInt() == 0) {
                            assertEquals("protected pixel $i", expected[i], actual)
                        } else {
                            assertEquals("allowed pixel $i", candidate.getPixel(x, y), actual)
                        }
                        assertEquals("original mutated at $i", expected[i], original.getPixel(x, y))
                    }
                } finally {
                    reloaded.recycle()
                }
            } finally {
                output.recycle()
            }
        } finally {
            original.recycle()
            candidate.recycle()
        }
    }

    @Test fun invalidFrameMaskOrTransparencyCannotSilentlyGenerateAResult() {
        val original = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val candidate = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val different = Bitmap.createBitmap(9, 8, Bitmap.Config.ARGB_8888)
        try {
            original.eraseColor(Color.rgb(100, 110, 120))
            candidate.eraseColor(Color.rgb(130, 140, 150))
            different.eraseColor(Color.rgb(130, 140, 150))
            val validMask = ByteArray(64) { if (it < 8) 255.toByte() else 0 }
            fun rejects(block: () -> Unit) {
                try {
                    block()
                    fail("Expected invalid registered-image input to be rejected")
                } catch (_: IllegalArgumentException) {
                    // Expected refusal.
                }
            }
            rejects { LocalPixelProtection.composite(original, candidate, validMask, false) }
            rejects { LocalPixelProtection.composite(original, different, validMask, true) }
            rejects { LocalPixelProtection.composite(original, candidate, ByteArray(63), true) }
            rejects { LocalPixelProtection.composite(original, candidate, ByteArray(64), true) }
            rejects {
                LocalPixelProtection.composite(
                    original, candidate, ByteArray(64) { 255.toByte() }, true
                )
            }
            val softMask = validMask.copyOf().also { it[3] = 127 }
            rejects { LocalPixelProtection.composite(original, candidate, softMask, true) }
            candidate.setPixel(1, 1, Color.argb(64, 80, 100, 120))
            rejects { LocalPixelProtection.composite(original, candidate, validMask, true) }
        } finally {
            original.recycle()
            candidate.recycle()
            different.recycle()
        }
    }
}
