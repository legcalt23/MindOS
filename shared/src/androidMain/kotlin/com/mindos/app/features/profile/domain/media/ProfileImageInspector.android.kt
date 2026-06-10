package com.mindos.app.features.profile.domain.media

import android.graphics.BitmapFactory

/** Decodes only the bounds (no pixels) to read the image's intrinsic size. */
actual fun decodeImagePixelSize(bytes: ByteArray): ImagePixelSize? {
    if (bytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) ImagePixelSize(width, height) else null
}
