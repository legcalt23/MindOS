package com.mindos.app.features.profile.domain.media

/** Pixel dimensions of a decoded image. */
data class ImagePixelSize(val width: Int, val height: Int) {
    val isSquare: Boolean get() = width > 0 && width == height
}

/**
 * Decodes only the header of [bytes] to read an image's pixel dimensions, used
 * to enforce the 1:1 requirement for custom-link icons. Returns null when the
 * platform can't decode (callers then accept the image but cannot prove the
 * aspect ratio — surfaced as a soft warning, not a hard rejection).
 */
expect fun decodeImagePixelSize(bytes: ByteArray): ImagePixelSize?
