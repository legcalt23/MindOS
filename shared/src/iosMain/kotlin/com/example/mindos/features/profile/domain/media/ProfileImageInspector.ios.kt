package com.example.mindos.features.profile.domain.media

/**
 * iOS image-dimension inspection is deferred (verification requires Xcode/macOS,
 * unavailable on the author's machine). Returning null means the 1:1 aspect
 * check cannot be proven on iOS, so callers accept the icon with a soft warning
 * rather than a hard rejection — consistent with the expect contract. A real
 * `UIImage(data:)` size read can replace this once iOS tooling is available.
 */
actual fun decodeImagePixelSize(bytes: ByteArray): ImagePixelSize? = null
