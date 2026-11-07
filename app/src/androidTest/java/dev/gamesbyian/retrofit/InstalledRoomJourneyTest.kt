package dev.gamesbyian.retrofit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Installed app + actual Android image decoding/storage and Compose UI.
 *
 * The source is seeded using a local file URI to keep the test deterministic. This exercises
 * the same RoomRepository.importPhoto that Photo Picker calls, but deliberately does NOT
 * claim to prove system Photo Picker interactions or a real external assistant handoff.
 */
@RunWith(AndroidJUnit4::class)
class InstalledRoomJourneyTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test fun exifQuarterAndHalfTurnsPreserveRawJpegAndOrientWorkingCanvas() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = RoomRepository(context)
        val width = 240
        val height = 120
        val red = Color.rgb(230, 25, 25)
        val green = Color.rgb(25, 230, 25)
        val blue = Color.rgb(25, 25, 230)
        val yellow = Color.rgb(230, 230, 25)
        // Source quadrant colors before EXIF metadata rotation:
        // red green
        // blue yellow
        val orientations = listOf(
            Triple(ExifInterface.ORIENTATION_ROTATE_90,
                listOf(blue, red, yellow, green), true),
            Triple(ExifInterface.ORIENTATION_ROTATE_180,
                listOf(yellow, blue, green, red), false),
            Triple(ExifInterface.ORIENTATION_ROTATE_270,
                listOf(green, yellow, red, blue), true)
        )
        for ((orientation, corners, quarterTurn) in orientations) {
            val raw = File(context.cacheDir, "retrofit-exif-${orientation}.jpg")
            val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                for (y in 0 until height) for (x in 0 until width) {
                    val color = if (y < height / 2) {
                        if (x < width / 2) red else green
                    } else {
                        if (x < width / 2) blue else yellow
                    }
                    source.setPixel(x, y, color)
                }
                raw.outputStream().use { stream ->
                    assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, stream))
                }
                val exif = ExifInterface(raw.absolutePath)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                exif.saveAttributes()
                assertEquals(orientation, ExifInterface(raw.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
                ))
                val originalBytes = raw.readBytes()
                val room = runBlocking { repository.importPhoto(Uri.fromFile(raw)) }
                val normalizedWidth = if (quarterTurn) height else width
                val normalizedHeight = if (quarterTurn) width else height
                assertEquals(normalizedWidth, room.width)
                assertEquals(normalizedHeight, room.height)
                val roomFolder = File(context.filesDir, "rooms/${room.id}")
                assertArrayEquals(originalBytes, File(roomFolder, "original.bin").readBytes())
                val oriented = BitmapFactory.decodeFile(File(roomFolder, "oriented.jpg").path)
                    ?: error("Oriented working photo did not decode")
                try {
                    assertEquals(normalizedWidth, oriented.width)
                    assertEquals(normalizedHeight, oriented.height)
                    val observed = listOf(
                        oriented.getPixel(20, 20),
                        oriented.getPixel(normalizedWidth - 20, 20),
                        oriented.getPixel(20, normalizedHeight - 20),
                        oriented.getPixel(normalizedWidth - 20, normalizedHeight - 20)
                    )
                    // JPEG source and oriented derivative are lossy, so assert
                    // source quadrant identity by channel tolerance, not exact RGB.
                    for (i in corners.indices) {
                        val actual = observed[i]
                        val expected = corners[i]
                        assertTrue("EXIF ${orientation} corner ${i} red wrong",
                            kotlin.math.abs(Color.red(actual) - Color.red(expected)) <= 45)
                        assertTrue("EXIF ${orientation} corner ${i} green wrong",
                            kotlin.math.abs(Color.green(actual) - Color.green(expected)) <= 45)
                        assertTrue("EXIF ${orientation} corner ${i} blue wrong",
                            kotlin.math.abs(Color.blue(actual) - Color.blue(expected)) <= 45)
                    }
                } finally {
                    oriented.recycle()
                }
            } finally {
                source.recycle()
                raw.delete()
            }
        }
    }

    @Test fun importedPhotoMarkerAndOriginalSurviveActivityRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = File(context.cacheDir, "retrofit-installed-fixture.png")
        val image = Bitmap.createBitmap(480, 240, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(Color.rgb(90, 120, 150))
            fixture.outputStream().use { output ->
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            image.recycle()
        }

        try {
            val repository = RoomRepository(context)
            val imported = runBlocking { repository.importPhoto(Uri.fromFile(fixture)) }
            assertEquals(480, imported.width)
            assertEquals(240, imported.height)
            assertTrue(imported.markers.isEmpty())
            assertArrayEquals(
                fixture.readBytes(),
                File(context.filesDir, "rooms/${imported.id}/original.bin").readBytes()
            )

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                composeRule.onNodeWithContentDescription("Equipment-room photograph")
                    .performTouchInput { click(center) }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule.onAllNodesWithText("A · modify").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithText("A · modify").performClick()
                composeRule.onNodeWithText("Marker A").assertExists()

                // Wait for the app's real asynchronous atomic persistence, then recreate Activity.
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    runCatching {
                        runBlocking { RoomRepository(context).load()?.markers?.size == 1 }
                    }.getOrDefault(false)
                }
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule.onAllNodesWithText("Saved locally").fetchSemanticsNodes().isNotEmpty()
                }
                scenario.recreate()
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule.onAllNodesWithText("A · modify").fetchSemanticsNodes().isNotEmpty()
                }
                composeRule.onNodeWithText("A · modify").performClick()
                composeRule.onNodeWithText("Marker A").assertExists()
            }

            val saved = runBlocking { RoomRepository(context).load() }!!
            assertEquals(imported.id, saved.id)
            assertEquals(imported.originalSha256, saved.originalSha256)
            assertEquals(imported.orientedSha256, saved.orientedSha256)
            assertEquals("A", saved.markers.single().label)
            assertArrayEquals(
                fixture.readBytes(),
                File(context.filesDir, "rooms/${imported.id}/original.bin").readBytes()
            )

            // A failed source replacement must not destroy the active draft.
            val bad = File(context.cacheDir, "retrofit-invalid-photo.txt")
            bad.writeText("Definitely not image data")
            try {
                runBlocking { repository.importPhoto(Uri.fromFile(bad)) }
                fail("Invalid replacement must fail")
            } catch (_: Exception) {
                assertEquals(saved, runBlocking { RoomRepository(context).load() })
            } finally {
                bad.delete()
            }
        } finally {
            fixture.delete()
        }
    }
}
