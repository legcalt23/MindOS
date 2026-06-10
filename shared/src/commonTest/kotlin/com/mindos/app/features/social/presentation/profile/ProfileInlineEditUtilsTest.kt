package com.mindos.app.features.social.presentation.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileInlineEditUtilsTest {
    @Test
    fun sanitizePublicPhraseRemovesEmojiAndKeepsSpanishCharacters() {
        val sanitized = sanitizePublicPhrase("Hola 😀 qué tal, año 2026.")

        assertEquals("Hola  qué tal, año 2026.", sanitized)
    }

    @Test
    fun containsEmojiDetectsEmojiButNotAccents() {
        assertTrue(containsEmoji("MindOS 🚀"))
        assertFalse(containsEmoji("Canción pública número 2"))
    }
}
