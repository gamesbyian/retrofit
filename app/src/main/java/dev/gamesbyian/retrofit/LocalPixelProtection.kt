package dev.gamesbyian.retrofit

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Experimental local-only bitmap primitive. This is not connected to Retrofit's editor yet.
 *
 * Input bitmaps must already be oriented, geometrically registered, opaque ARGB_8888 and
 * the same size. A 0-byte mask value protects the original, 255 permits candidate pixels.
 * No interpolation, auto-registration, resizing, provider alpha-mask conversion or AI call.
 *
 * This deliberately limits working size to 2 MP until tiled processing and device memory
 * are measured; the independent host experiment can test larger frames separately.
 */
internal object LocalPixelProtection {
    private const val MAX_PIXELS = 2_000_000

    data class Evidence(
        val protectedPixels: Int,
        val allowedPixels: Int,
        val driftedProtectedCandidatePixels: Int,
        val driftedProtectedOutputPixels: Int
    )

    data class Result(val bitmap: Bitmap, val evidence: Evidence)

    fun composite(
        original: Bitmap,
        candidate: Bitmap,
        allowChange: ByteArray,
        sameOrientedCameraFrameVerified: Boolean
    ): Result {
        require(sameOrientedCameraFrameVerified) {
            "Identical dimensions are not camera registration; verify oriented frame first"
        }
        require(!original.isRecycled && !candidate.isRecycled) { "Bitmap has been recycled" }
        require(original.config == Bitmap.Config.ARGB_8888 && candidate.config == Bitmap.Config.ARGB_8888) {
            "Use explicit opaque ARGB_8888 working images"
        }
        require(original.width == candidate.width && original.height == candidate.height) {
            "Refusing implicit crop, resize or registration"
        }
        val pixelsLong = original.width.toLong() * original.height.toLong()
        require(pixelsLong in 1..MAX_PIXELS.toLong()) { "Exceeds bounded experimental pixel budget" }
        val count = pixelsLong.toInt()
        require(allowChange.size == count) { "Mask does not fit the oriented source image" }

        val originalPixels = IntArray(count)
        val candidatePixels = IntArray(count)
        original.getPixels(originalPixels, 0, original.width, 0, 0, original.width, original.height)
        candidate.getPixels(candidatePixels, 0, candidate.width, 0, 0, candidate.width, candidate.height)
        val resultPixels = IntArray(count)
        var protectedCount = 0
        var allowedCount = 0
        var driftBefore = 0
        for (i in 0 until count) {
            val originalColor = originalPixels[i]
            val candidateColor = candidatePixels[i]
            require(Color.alpha(originalColor) == 255 && Color.alpha(candidateColor) == 255) {
                "Opaque photo sources only; semitransparent blending requires a separately verified contract"
            }
            when (allowChange[i].toInt() and 0xff) {
                0 -> {
                    resultPixels[i] = originalColor
                    protectedCount++
                    if (originalColor != candidateColor) driftBefore++
                }
                255 -> {
                    resultPixels[i] = candidateColor
                    allowedCount++
                }
                else -> throw IllegalArgumentException(
                    "Only 0 or 255 mask values accepted; feathering is an independently reviewed feature"
                )
            }
        }
        require(protectedCount > 0 && allowedCount > 0) {
            "An edit mask must contain protected and editable pixels"
        }

        val bitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(resultPixels, 0, original.width, 0, 0, original.width, original.height)
        val readback = IntArray(count)
        bitmap.getPixels(readback, 0, original.width, 0, 0, original.width, original.height)
        var driftAfter = 0
        for (i in 0 until count) {
            if ((allowChange[i].toInt() and 0xff) == 0 && readback[i] != originalPixels[i]) {
                driftAfter++
            }
        }
        check(driftAfter == 0) { "Native compositing altered protected decoded pixels" }
        return Result(bitmap, Evidence(protectedCount, allowedCount, driftBefore, driftAfter))
    }
}
