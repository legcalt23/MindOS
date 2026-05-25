package com.example.neuv3.features.social.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaContentResolverTest {

    @Test
    fun resolvePrioritizesMimeTypeOverExtension() {
        // MIME says image, Extension says video
        val result = MediaContentResolver.resolve("image/jpeg", "https://example.com/video.mp4")
        assertEquals(MediaContentType.Image, result)
    }

    @Test
    fun resolveFallbacksToExtensionIfMimeIsMissing() {
        val result = MediaContentResolver.resolve(null, "https://example.com/image.png")
        assertEquals(MediaContentType.Image, result)
    }

    @Test
    fun resolveHandlesSignedUrlsWithQueryParams() {
        val url = "https://firebasestorage.googleapis.com/v0/b/app.appspot.com/o/files%2Fdocument.pdf?alt=media&token=123-abc"
        val result = MediaContentResolver.resolve(null, url)
        assertEquals(MediaContentType.Pdf, result)
    }

    @Test
    fun resolveHandlesUrlsWithFragments() {
        val url = "https://example.com/archive.zip#inside/photo.jpg"
        // fragment should be ignored, so it looks at .zip (which is unsupported)
        val result = MediaContentResolver.resolve(null, url)
        assertEquals(MediaContentType.Unsupported, result)
    }

    @Test
    fun resolveDetectsVideoFormats() {
        assertEquals(MediaContentType.Video, MediaContentResolver.resolve("video/mp4", null))
        assertEquals(MediaContentType.Video, MediaContentResolver.resolve(null, "file.webm"))
    }

    @Test
    fun resolveDetectsWavAudioFormats() {
        assertEquals(MediaContentType.Audio, MediaContentResolver.resolve("audio/wav", null))
        assertEquals(MediaContentType.Audio, MediaContentResolver.resolve("audio/x-wav", null))
        assertEquals(MediaContentType.Audio, MediaContentResolver.resolve(null, "file.WAV"))
    }

    @Test
    fun resolveDetectsMobileAudioFormats() {
        assertEquals(MediaContentType.Audio, MediaContentResolver.resolve("audio/mp4", null))
        assertEquals(MediaContentType.Audio, MediaContentResolver.resolve(null, "recording.m4a"))
    }

    @Test
    fun resolveDetectsTextAndJson() {
        assertEquals(MediaContentType.Text, MediaContentResolver.resolve("text/plain", null))
        assertEquals(MediaContentType.Text, MediaContentResolver.resolve("application/json", null))
        assertEquals(MediaContentType.Text, MediaContentResolver.resolve(null, "config.json"))
    }

    @Test
    fun resolveReturnsUnsupportedForUnknownTypes() {
        val result = MediaContentResolver.resolve("application/octet-stream", "unknown.dat")
        assertEquals(MediaContentType.Unsupported, result)
    }

    @Test
    fun resolveReturnsUnknownForNullInputs() {
        val result = MediaContentResolver.resolve(null, null)
        assertEquals(MediaContentType.Unknown, result)
    }

    @Test
    fun extractCleanExtensionHandlesComplexPaths() {
        assertEquals("jpg", MediaContentResolver.extractCleanExtension("image.JPG"))
        assertEquals("pdf", MediaContentResolver.extractCleanExtension("/path/to/doc.pdf?query=1"))
        assertEquals("mp4", MediaContentResolver.extractCleanExtension("video.mp4#t=10"))
        assertEquals(null, MediaContentResolver.extractCleanExtension("no_extension"))
        assertEquals(null, MediaContentResolver.extractCleanExtension("trailing_slash/"))
    }

    @Test
    fun resolveHandlesInconsistentMimeAndExtensionDefensively() {
        // Aggressive test: multiple dots and noise
        val url = "https://cdn.example.com/get?file=my.photo.was.actually.a.pdf.jpg&signed=true"
        val result = MediaContentResolver.resolve("application/pdf", url) 
        // MIME priority
        assertEquals(MediaContentType.Pdf, result)
        
        // No MIME, must use last real extension
        val result2 = MediaContentResolver.resolve(null, url)
        assertEquals(MediaContentType.Image, result2) // .jpg
    }
}
