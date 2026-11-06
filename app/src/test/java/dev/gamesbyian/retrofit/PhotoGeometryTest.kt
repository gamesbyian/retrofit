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
