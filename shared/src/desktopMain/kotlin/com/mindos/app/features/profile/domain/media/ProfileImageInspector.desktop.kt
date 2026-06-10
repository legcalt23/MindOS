package com.mindos.app.features.profile.domain.media

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** Reads image dimensions via ImageIO on the JVM desktop target. */
actual fun decodeImagePixelSize(bytes: ByteArray): ImagePixelSize? {
    if (bytes.isEmpty()) return null
    return runCatching {
        ByteArrayInputStream(bytes).use { stream ->
            val image = ImageIO.read(stream) ?: return null
            ImagePixelSize(image.width, image.height)
        }
    }.getOrNull()
}
