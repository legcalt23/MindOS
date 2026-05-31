package com.mindos.action.normalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NormalizationPipelineTest {

    private val default = NormalizationPipeline(NormalizationConfig.Default)
    private val spanish = NormalizationPipeline(NormalizationConfig.Spanish)

    @Test
    fun trimsCollapsesAndLowercasesDisplay() {
        val r = default.normalize("  Open   Settings  ")
        assertEquals("open settings", r.text)
        assertEquals("open settings", r.displayText)
        assertEquals("Open Settings", r.rawText)
        assertEquals(2, r.tokens.size)
        assertEquals("Open", r.tokens[0].raw)
        assertEquals("open", r.tokens[0].display)
    }

    @Test
    fun nonBreakingSpaceCollapsesToTokenBoundary() {
        // U+00A0 non-breaking space is whitespace -> token boundary.
        val r = default.normalize("open settings")
        assertEquals(2, r.tokens.size)
        assertEquals("open settings", r.text)
    }

    @Test
    fun zeroWidthAndControlCharsAreStripped() {
        // U+200B zero-width space (FORMAT) is removed, joining the token.
        val r = default.normalize("open​settings")
        assertEquals(1, r.tokens.size)
        assertEquals("opensettings", r.text)
    }

    @Test
    fun nfcComposesDecomposedAccents() {
        // "e" + U+0301 combining acute -> "é"
        val r = default.normalize("Café")
        assertEquals("café", r.tokens[0].display)
        assertEquals("café", r.tokens[0].normalized) // default config does not fold
    }

    @Test
    fun spanishConfigFoldsDiacriticsInNormalizedOnly() {
        val r = spanish.normalize("Café")
        assertEquals("Café", r.tokens[0].raw)        // raw preserves case + accent
        assertEquals("café", r.tokens[0].display)    // display lowercases, keeps accent
        assertEquals("cafe", r.tokens[0].normalized) // normalized folds the accent
        assertEquals("cafe", r.text)
    }

    @Test
    fun rawPreservesCaseForCaseSensitiveConsumers() {
        val r = default.normalize("aB3xZ9")
        assertEquals("aB3xZ9", r.tokens[0].raw)
        assertEquals("ab3xz9", r.tokens[0].normalized)
    }

    @Test
    fun blankInputProducesNoTokens() {
        val r = default.normalize("   \t  ")
        assertTrue(r.isBlank)
        assertEquals(0, r.tokens.size)
        assertEquals("", r.text)
    }

    @Test
    fun emptyInputIsBlank() {
        assertTrue(default.normalize("").isBlank)
    }

    @Test
    fun tokenSpansIndexIntoCasePreservedText() {
        val r = default.normalize("open ana")
        assertEquals(0, r.tokens[0].start)
        assertEquals(4, r.tokens[0].end)
        assertEquals(5, r.tokens[1].start)
        assertEquals(8, r.tokens[1].end)
    }

    @Test
    fun normalizationIsIdempotent() {
        val once = spanish.normalize("  Buenos   DIAS  café ").text
        val twice = spanish.normalize(once).text
        assertEquals(once, twice)
    }

    @Test
    fun freeTextRawDiffersFromFoldedNormalized() {
        val es = spanish.normalize("café")
        assertFalse(es.tokens[0].raw == es.tokens[0].normalized)
    }
}
