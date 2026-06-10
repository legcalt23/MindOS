package com.mindos.app.features.social.presentation.media

import com.mindos.app.features.social.shared.util.MediaContentType

actual object MediaPreloadManager {
    actual fun preload(url: String, type: MediaContentType) {
        // No-op en escritorio por ahora para simplificar y asegurar compilación
    }
}
