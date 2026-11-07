package dev.gamesbyian.retrofit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
