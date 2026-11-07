package dev.gamesbyian.retrofit

import java.util.UUID

data class RoomMarker(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val action: String = "modify",
    val detail: String = ""
)

data class RoomDraft(
    val id: String,
    val width: Int,
    val height: Int,
    val originalSha256: String,
    val orientedSha256: String,
    val nextLabelIndex: Int = 0,
    val instructions: String = "",
    val markers: List<RoomMarker> = emptyList()
) {
    fun mark(point: UnitPoint): RoomDraft {
        require(point.x.isFinite() && point.y.isFinite() &&
            point.x in 0f..1f && point.y in 0f..1f)
        val next = RoomMarker(
            id = "mark_" + UUID.randomUUID().toString().replace("-", ""),
            label = markerLabel(nextLabelIndex),
            x = point.x,
            y = point.y
        )
        return copy(markers = markers + next, nextLabelIndex = nextLabelIndex + 1)
    }
}

fun markerLabel(index: Int): String {
    require(index >= 0)
    var remaining = index + 1
    var label = ""
    while (remaining > 0) {
        remaining -= 1
        label = ('A' + remaining % 26).toString() + label
        remaining /= 26
    }
    return label
}
