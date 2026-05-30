package com.example.mindos.features.social.presentation.media

import com.example.mindos.features.social.shared.util.MediaContentType

actual object MediaPreloadManager {
    actual fun preload(url: String, type: MediaContentType) {
        // No-op en escritorio por ahora para simplificar y asegurar compilación
    }
}
