package com.mindos.app.features.social.presentation.media

import com.mindos.app.features.social.shared.util.MediaContentType
import platform.AVFoundation.*
import platform.Foundation.*

actual object MediaPreloadManager {
    private val preloadedAssets = mutableMapOf<String, AVURLAsset>()

    actual fun preload(url: String, type: MediaContentType) {
        if (type != MediaContentType.Video) return

        val nsUrl = NSURL.URLWithString(url) ?: return
        if (preloadedAssets.containsKey(url)) return

        val asset = AVURLAsset.assetWithURL(nsUrl)
        preloadedAssets[url] = asset

        // "Calentar" el asset cargando valores básicos asíncronamente
        asset.loadValuesAsynchronouslyForKeys(listOf("duration", "playable")) {
            // No necesitamos hacer nada con el resultado, el objetivo es que AVFoundation 
            // ya tenga estos datos (y posiblemente los primeros bytes de red) en caché interna.
        }
        
        // Mantener un cache pequeño para evitar fugas; eliminamos el más antiguo si supera 20
        if (preloadedAssets.size > 20) {
            val firstKey = preloadedAssets.keys.first()
            preloadedAssets.remove(firstKey)
        }
    }

    fun getPreloadedAsset(url: String): AVURLAsset? = preloadedAssets[url]
}
