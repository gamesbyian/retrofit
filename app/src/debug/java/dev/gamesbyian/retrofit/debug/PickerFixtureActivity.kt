package dev.gamesbyian.retrofit.debug

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Disposable REAL Android Photo Picker source, never part of the release APK.
 *
 * This test fixture publishes exactly one synthetic JPEG to MediaStore on an emulator.
 * Cleanup deletes only the exact MediaStore URI and name recorded by this Activity.
 * It refuses real handsets even if a caller starts the exported debug Activity manually.
 */
class PickerFixtureActivity : Activity() {
    private val receipt get() = File(filesDir, "retrofit-picker-fixture.json")
    private val source get() = File(filesDir, "retrofit-picker-fixture-original.jpg")
    private val cleanupReceipt get() = File(filesDir, "retrofit-picker-cleanup.json")
    private val displayName = "RETROFIT-EMULATOR-PICKER-SOURCE.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            require(
                Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                    Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
                    Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                    Build.MODEL.contains("sdk_gphone", ignoreCase = true)
            ) { "Fixture refuses non-emulator hardware" }

            if (intent.getBooleanExtra("cleanup", false)) {
                val data = JSONObject(receipt.readText())
                require(data.getString("status") == "READY")
                require(data.getString("displayName") == displayName)
                val uri = Uri.parse(data.getString("sourceUri"))
                require(uri.scheme == "content" && uri.authority == "media")
                require(uri.path?.startsWith("/external/images/media/") == true)
                val deleted = contentResolver.delete(uri, null, null)
                cleanupReceipt.writeText(JSONObject()
                    .put("status", "DONE")
                    .put("deleted", deleted)
                    .put("sourceSHA256", data.getString("sourceSHA256"))
                    .put("deletedOnlyOwnFixture", true)
                    .toString())
                return
            }

            require(!receipt.exists()) { "Fixture already published; refusing duplicate MediaStore source" }
            val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
            val bytes = try {
                bitmap.eraseColor(Color.rgb(37, 92, 166))
                for (y in 0 until 320) for (x in 0 until 240) {
                    if ((x + 2 * y) % 37 < 9) {
                        bitmap.setPixel(x, y, Color.rgb(238, 191, 61))
                    }
                }
                ByteArrayOutputStream().also {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it))
                }.toByteArray()
            } finally {
                bitmap.recycle()
            }
            source.writeBytes(bytes)
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RetrofitPickerQA/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )) { "Synthetic MediaStore insert failed" }
            try {
                requireNotNull(contentResolver.openOutputStream(uri, "w")).use {
                    it.write(bytes)
                }
                check(contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) == 1) { "Synthetic image did not publish" }
            } catch (error: Exception) {
                contentResolver.delete(uri, null, null)
                throw error
            }
            receipt.writeText(JSONObject()
                .put("status", "READY")
                .put("sourceUri", uri.toString())
                .put("sourceSHA256", sha)
                .put("sourceBytes", bytes.size)
                .put("sourceWidth", 240)
                .put("sourceHeight", 320)
                .put("displayName", displayName)
                .put("emulatorOnly", true)
                .toString())
        } catch (error: Exception) {
            if (!intent.getBooleanExtra("cleanup", false)) {
                receipt.writeText(JSONObject()
                    .put("status", "ERROR")
                    .put("reason", error.javaClass.simpleName)
                    .toString())
            } else {
                cleanupReceipt.writeText(JSONObject()
                    .put("status", "ERROR")
                    .put("reason", error.javaClass.simpleName)
                    .toString())
            }
        } finally {
            finish()
        }
    }
}
