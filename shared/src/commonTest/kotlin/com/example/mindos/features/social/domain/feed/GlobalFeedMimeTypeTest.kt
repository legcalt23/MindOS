package com.example.mindos.features.social.domain.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlobalFeedMimeTypeTest {

    // ── normalizedGlobalFeedMimeType ────────────────────────────────

    @Test
    fun normalize_lowercases() {
        assertEquals("image/jpeg", "IMAGE/JPEG".normalizedGlobalFeedMimeType())
    }

    @Test
    fun normalize_trims() {
        assertEquals("image/png", "  image/png  ".normalizedGlobalFeedMimeType())
    }

    @Test
    fun normalize_stripsCharset() {
        assertEquals("text/plain", "text/plain; charset=utf-8".normalizedGlobalFeedMimeType())
    }

    @Test
    fun normalize_mapsWavAliasesToCanonical() {
        // Various WAV aliases all map to "audio/wav"
        assertEquals("audio/wav", "audio/x-wav".normalizedGlobalFeedMimeType())
        assertEquals("audio/wav", "audio/wave".normalizedGlobalFeedMimeType())
        assertEquals("audio/wav", "audio/vnd.wave".normalizedGlobalFeedMimeType())
    }

    // ── globalFeedAttachmentExtension ───────────────────────────────

    @Test
    fun extension_imageMappings() {
        assertEquals("jpg", globalFeedAttachmentExtension("image/jpeg"))
        assertEquals("png", globalFeedAttachmentExtension("image/png"))
        assertEquals("webp", globalFeedAttachmentExtension("image/webp"))
        assertEquals("gif", globalFeedAttachmentExtension("image/gif"))
    }

    @Test
    fun extension_videoMappings() {
        assertEquals("mp4", globalFeedAttachmentExtension("video/mp4"))
        assertEquals("webm", globalFeedAttachmentExtension("video/webm"))
    }

    @Test
    fun extension_audioMappings() {
        assertEquals("m4a", globalFeedAttachmentExtension("audio/mp4"))
        assertEquals("aac", globalFeedAttachmentExtension("audio/aac"))
        assertEquals("mp3", globalFeedAttachmentExtension("audio/mpeg"))
        assertEquals("wav", globalFeedAttachmentExtension("audio/wav"))
    }

    @Test
    fun extension_documentMappings() {
        assertEquals("pdf", globalFeedAttachmentExtension("application/pdf"))
        assertEquals("txt", globalFeedAttachmentExtension("text/plain"))
        assertEquals("json", globalFeedAttachmentExtension("application/json"))
    }

    @Test
    fun extension_unknownMimeReturnsNull() {
        assertNull(globalFeedAttachmentExtension("application/x-unknown"))
        assertNull(globalFeedAttachmentExtension(""))
        assertNull(globalFeedAttachmentExtension("garbage"))
    }

    @Test
    fun extension_normalizesBeforeMapping() {
        // Uppercase + charset should still map
        assertEquals("jpg", globalFeedAttachmentExtension("IMAGE/JPEG"))
        assertEquals("txt", globalFeedAttachmentExtension("text/plain; charset=utf-8"))
        // WAV alias maps to canonical + extension
        assertEquals("wav", globalFeedAttachmentExtension("audio/x-wav"))
    }
}
