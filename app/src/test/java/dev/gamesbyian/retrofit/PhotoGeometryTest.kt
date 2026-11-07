package dev.gamesbyian.retrofit

import org.junit.Assert.*
import org.junit.Test

class PhotoGeometryTest {
    @Test fun portraitLetterboxRejectsMargin() {
        val rect = fitPhoto(400f, 400f, 400, 200)
        assertEquals(100f, rect.top, 0.001f)
        assertNull(toUnitPoint(200f, 20f, rect))
        assertEquals(0.5f, toUnitPoint(200f, 200f, rect)!!.x, 0.001f)
        assertEquals(0.5f, toUnitPoint(200f, 200f, rect)!!.y, 0.001f)
    }

    @Test fun coordinateIsIndependentOfViewportSize() {
        val point = UnitPoint(.22f, .81f)
        listOf(320f to 240f, 412f to 892f, 1200f to 480f).forEach { (w, h) ->
            val bounds = fitPhoto(w, h, 4000, 3000)
            val (screenX, screenY) = toViewPoint(point, bounds)
            val roundTrip = toUnitPoint(screenX, screenY, bounds)!!
            assertEquals(point.x, roundTrip.x, 0.0001f)
            assertEquals(point.y, roundTrip.y, 0.0001f)
        }
    }

    @Test fun invalidInputAndLetterboxAreRejected() {
        val bounds = fitPhoto(300f, 400f, 900, 100)
        assertNull(toUnitPoint(Float.NaN, 200f, bounds))
        assertNull(toUnitPoint(0f, 0f, bounds))
        assertThrows(IllegalArgumentException::class.java) {
            fitPhoto(0f, 100f, 900, 100)
        }
    }

    @Test fun orientedPortraitLandscapeAndSquareRoundTripWithoutViewportAssumptions() {
        val viewports = listOf(320f to 700f, 412f to 892f, 960f to 540f, 600f to 600f)
        val orientedSources = listOf(4000 to 3000, 3000 to 4000, 4000 to 4000, 8192 to 128)
        val positions = listOf(
            UnitPoint(0f, 0f),
            UnitPoint(1f, 0f),
            UnitPoint(0f, 1f),
            UnitPoint(1f, 1f),
            UnitPoint(0.13f, 0.79f),
            UnitPoint(0.999f, 0.001f)
        )
        for ((vw, vh) in viewports) for ((sw, sh) in orientedSources) {
            val fitted = fitPhoto(vw, vh, sw, sh)
            assertTrue("fitted photo spills beyond view", fitted.left >= -0.01f &&
                fitted.top >= -0.01f &&
                fitted.left + fitted.width <= vw + 0.01f &&
                fitted.top + fitted.height <= vh + 0.01f)
            for (position in positions) {
                val (x, y) = toViewPoint(position, fitted)
                val returned = toUnitPoint(x, y, fitted)
                    ?: throw AssertionError("Image corner or interior was rejected")
                assertEquals(position.x, returned.x, 0.0001f)
                assertEquals(position.y, returned.y, 0.0001f)
            }
            // Always outside the view or photo, independent of letterbox orientation.
            assertNull(toUnitPoint(fitted.left - 1f, fitted.top, fitted))
            assertNull(toUnitPoint(fitted.left, fitted.top - 1f, fitted))
            assertNull(toUnitPoint(fitted.left + fitted.width + 1f, fitted.top, fitted))
            assertNull(toUnitPoint(fitted.left, fitted.top + fitted.height + 1f, fitted))
        }
    }

    @Test fun rejectNonfinitePointerCoordinatesEvenOnAnOtherwiseValidPhoto() {
        val fitted = fitPhoto(412f, 892f, 4000, 3000)
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
            assertNull(toUnitPoint(value, 300f, fitted))
            assertNull(toUnitPoint(200f, value, fitted))
        }
    }

    @Test fun labelsRemainUniquePastAlphabet() {
        assertEquals("A", markerLabel(0))
        assertEquals("Z", markerLabel(25))
        assertEquals("AA", markerLabel(26))
        assertEquals("AB", markerLabel(27))
        assertEquals("AZ", markerLabel(51))
        assertEquals("BA", markerLabel(52))
    }

    @Test fun markerCoordinatesAndIdentifiersStayStableAcrossEdit() {
        val room = RoomDraft("r1", 400, 300, "raw", "oriented").mark(UnitPoint(.2f, .7f))
        val marker = room.markers.single()
        val edited = room.copy(markers = listOf(marker.copy(detail = "Clean stain")))
        assertEquals(marker.id, edited.markers.single().id)
        assertEquals(marker.x, edited.markers.single().x)
        assertEquals(marker.y, edited.markers.single().y)
        assertEquals("B", edited.mark(UnitPoint(.6f, .2f)).markers.last().label)
    }
}
