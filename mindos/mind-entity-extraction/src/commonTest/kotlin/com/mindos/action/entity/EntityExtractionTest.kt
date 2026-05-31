package com.mindos.action.entity

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.normalization.NormalizationPipeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntityExtractionTest {

    private val pipeline = NormalizationPipeline(NormalizationConfig.Default)
    private val registry = EntityExtractorRegistry.default()

    private fun extract(type: EntityType, input: String, from: Int = 0, until: Int = 1): EntityExtractionResult {
        val norm = pipeline.normalize(input)
        return registry.forType(type)!!.extract(norm, from, until)
    }

    private fun extractedValue(r: EntityExtractionResult): String =
        (r as EntityExtractionResult.Extracted).entity.value

    @Test
    fun registryDefaultSupportsAllV1Types() {
        assertEquals(EntityType.all.toSet(), registry.supportedTypes)
        assertEquals(9, registry.supportedTypes.size)
    }

    @Test
    fun usernamePlain() {
        val r = extract(EntityType.Username, "ana_99")
        assertEquals("ana_99", extractedValue(r))
        assertEquals(DeterministicConfidence.HIGH, (r as EntityExtractionResult.Extracted).entity.confidence)
    }

    @Test
    fun usernameWithSigilIsExact() {
        val r = extract(EntityType.Username, "@Ana") as EntityExtractionResult.Extracted
        assertEquals("ana", r.entity.value)
        assertEquals(DeterministicConfidence.EXACT, r.entity.confidence)
    }

    @Test
    fun usernameTooShortRejected() {
        val r = extract(EntityType.Username, "a")
        assertTrue(r is EntityExtractionResult.Rejected)
        assertTrue((r as EntityExtractionResult.Rejected).reason is EntityRejectionReason.TooShort)
    }

    @Test
    fun usernameBadCharsRejected() {
        val r = extract(EntityType.Username, "an\$a")
        assertTrue(r is EntityExtractionResult.Rejected)
    }

    @Test
    fun usernameEdgeDotRejected() {
        val r = extract(EntityType.Username, ".ana")
        assertTrue(r is EntityExtractionResult.Rejected)
    }

    @Test
    fun mentionRequiresSigil() {
        assertTrue(extract(EntityType.Mention, "ana") is EntityExtractionResult.Rejected)
        assertEquals("ana", extractedValue(extract(EntityType.Mention, "@ana")))
    }

    @Test
    fun hashtagRequiresHash() {
        assertTrue(extract(EntityType.Hashtag, "fun") is EntityExtractionResult.Rejected)
        assertEquals("fun", extractedValue(extract(EntityType.Hashtag, "#Fun")))
    }

    @Test
    fun opaqueIdPreservesCase() {
        val r = extract(EntityType.UserId, "aB3xZ9kQ") as EntityExtractionResult.Extracted
        assertEquals("aB3xZ9kQ", r.entity.value) // case preserved (case-sensitive id)
    }

    @Test
    fun opaqueIdRejectsIllegalChars() {
        assertTrue(extract(EntityType.StoryId, "ab\$cd") is EntityExtractionResult.Rejected)
    }

    @Test
    fun numericValidAndInvalid() {
        assertEquals("12345", extractedValue(extract(EntityType.Numeric, "12345")))
        assertTrue(extract(EntityType.Numeric, "12a") is EntityExtractionResult.Rejected)
    }

    @Test
    fun freeTextGreedyPreservesCaseAndAccents() {
        val norm = NormalizationPipeline(NormalizationConfig.Spanish).normalize("Buenos Días Café")
        val r = registry.forType(EntityType.FreeText)!!.extract(norm, 0, 3) as EntityExtractionResult.Extracted
        assertEquals("Buenos Días Café", r.entity.value)
        assertEquals(DeterministicConfidence.MEDIUM, r.entity.confidence)
    }

    @Test
    fun singleTokenExtractorRejectsWrongSpan() {
        val norm = pipeline.normalize("a b")
        val r = registry.forType(EntityType.Username)!!.extract(norm, 0, 2)
        assertTrue((r as EntityExtractionResult.Rejected).reason is EntityRejectionReason.WrongTokenCount)
    }

    @Test
    fun ambiguityResolverPrefersSigilsThenNumeric() {
        val resolver = EntityAmbiguityResolver()
        assertEquals(EntityType.Mention, resolvedType(resolver, "@ana"))
        assertEquals(EntityType.Hashtag, resolvedType(resolver, "#tag"))
        assertEquals(EntityType.Numeric, resolvedType(resolver, "123"))
        assertEquals(EntityType.Username, resolvedType(resolver, "ana"))
    }

    private fun resolvedType(resolver: EntityAmbiguityResolver, input: String): EntityType {
        val norm = pipeline.normalize(input)
        val r = resolver.resolveSingleToken(registry, norm, 0) as EntityExtractionResult.Extracted
        return r.entity.type
    }
}
