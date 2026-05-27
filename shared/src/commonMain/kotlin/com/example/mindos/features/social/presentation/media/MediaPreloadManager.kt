package com.example.mindos.features.social.presentation.media

import com.example.mindos.features.social.shared.util.MediaContentType

/**
 * Coordinador de pre-carga de medios (best-effort).
 * Provee una capa ligera para calentar cachés y preparar recursos antes de ser abiertos.
 */
expect object MediaPreloadManager {
    /**
     * Inicia una pre-carga ligera del recurso.
     * La implementación es específica de cada plataforma y debe ser no-bloqueante.
     */
    fun preload(url: String, type: MediaContentType)
}
