package com.example.neuv3.features.social.shared.util

enum class MediaContentType {
    Image,
    Video,
    Audio,
    Pdf,
    Text,
    Unsupported,
    Unknown
}

object MediaContentResolver {

    private val imageMimes = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    private val videoMimes = setOf("video/mp4", "video/webm", "video/quicktime")
    private val audioMimes = setOf(
        "audio/mp4",
        "audio/aac",
        "audio/mpeg",
        "audio/wav",
        "audio/x-wav",
        "audio/wave",
        "audio/vnd.wave",
    )
    private val pdfMimes = setOf("application/pdf")
    private val textMimes = setOf("text/plain", "application/json")

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val videoExtensions = setOf("mp4", "webm", "mov")
    private val audioExtensions = setOf("m4a", "aac", "mp3", "wav")
    private val pdfExtensions = setOf("pdf")
    private val textExtensions = setOf("txt", "json")

    fun resolve(mimeType: String?, url: String?): MediaContentType {
        if (!mimeType.isNullOrBlank()) {
            val normalizedMime = mimeType.lowercase().substringBefore(";").trim()
            when {
                normalizedMime in imageMimes -> return MediaContentType.Image
                normalizedMime in videoMimes -> return MediaContentType.Video
                normalizedMime in audioMimes -> return MediaContentType.Audio
                normalizedMime in pdfMimes -> return MediaContentType.Pdf
                normalizedMime in textMimes || normalizedMime.startsWith("text/") -> return MediaContentType.Text
            }
        }

        extractCleanExtension(url)?.let { extension ->
            when {
                extension in imageExtensions -> return MediaContentType.Image
                extension in videoExtensions -> return MediaContentType.Video
                extension in audioExtensions -> return MediaContentType.Audio
                extension in pdfExtensions -> return MediaContentType.Pdf
                extension in textExtensions -> return MediaContentType.Text
            }
        }

        return if (url.isNullOrBlank() && mimeType.isNullOrBlank()) {
            MediaContentType.Unknown
        } else {
            MediaContentType.Unsupported
        }
    }

    fun extractCleanExtension(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val withoutFragment = url.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        extensionFromPathSegment(withoutQuery)?.let { return it }

        val query = withoutFragment.substringAfter('?', missingDelimiterValue = "")
        if (query.isBlank()) return null

        return query
            .split('&')
            .asSequence()
            .map { it.substringAfter('=', missingDelimiterValue = it) }
            .mapNotNull { extensionFromPathSegment(it) }
            .firstOrNull()
    }

    private fun extensionFromPathSegment(value: String): String? {
        val decodedSeparators = value.replace("%2F", "/", ignoreCase = true)
        val lastSegment = decodedSeparators.substringAfterLast('/')
        if (lastSegment.isBlank()) return null
        return lastSegment
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}
