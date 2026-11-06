package dev.gamesbyian.retrofit

import kotlin.math.min

/** Image coordinates, not zoomed viewport coordinates. */
data class UnitPoint(val x: Float, val y: Float)

data class FittedPhoto(val left: Float, val top: Float, val width: Float, val height: Float)

fun fitPhoto(viewWidth: Float, viewHeight: Float, photoWidth: Int, photoHeight: Int): FittedPhoto {
    require(viewWidth > 0f && viewHeight > 0f && photoWidth > 0 && photoHeight > 0)
    val ratio = min(viewWidth / photoWidth, viewHeight / photoHeight)
    val width = photoWidth * ratio
    val height = photoHeight * ratio
    return FittedPhoto((viewWidth - width) / 2f, (viewHeight - height) / 2f, width, height)
}

/** Null for the letterbox margin, which must never add a marker. */
fun toUnitPoint(viewX: Float, viewY: Float, photo: FittedPhoto): UnitPoint? {
    if (!viewX.isFinite() || !viewY.isFinite() ||
        viewX < photo.left || viewY < photo.top ||
        viewX > photo.left + photo.width || viewY > photo.top + photo.height
    ) return null
    return UnitPoint(
        ((viewX - photo.left) / photo.width).coerceIn(0f, 1f),
        ((viewY - photo.top) / photo.height).coerceIn(0f, 1f)
    )
}

fun toViewPoint(point: UnitPoint, photo: FittedPhoto): Pair<Float, Float> =
    (photo.left + point.x * photo.width) to (photo.top + point.y * photo.height)
