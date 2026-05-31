package com.mindos.core

import com.mindos.contracts.OpenProfileActionDto
import com.mindos.contracts.ValidValidationReportDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MatcherExtractorValidatorTest {

    @Test
    fun normalizationPipelineIsDeterministic() {
        val pipeline = NormalizationPipelineImpl()

        val a = pipeline.normalize("  Open   Settings  ")
        val b = pipeline.normalize("Open Settings")

        assertEquals(b.text, a.text)
        assertEquals(listOf("open", "settings"), a.tokens)
    }

    @Test
    fun aliasMatcherSupportsLocalizedAndUndFallback() {
        val matcher = AliasMatcherImpl(CoreRouterRegistry.default().aliases)

        assertEquals("open_settings", matcher.match("ajustes", "es")?.actionType)
        assertEquals("open_messages", matcher.match("home", "fr")?.actionType)
    }

    @Test
    fun patternMatcherReturnsEntitiesWithoutExposingInternalTypes() {
        val matcher = PatternMatcherImpl()

        val result = matcher.match(com.mindos.contracts.RouterRequestDto("req-1", "open ana"))

        val matched = assertIs<CoreMatchResult.Matched>(result)
        assertEquals("open_profile", matched.actionType)
        assertEquals("ana", matched.entities["target"])
    }

    @Test
    fun entityExtractorSupportsCoreEntityShapes() {
        val extractor = EntityExtractorImpl()

        assertEquals("ana", assertIs<CoreEntityExtractionResult.Extracted>(extractor.extract("@Ana", "mention")).value)
        assertEquals("tag", assertIs<CoreEntityExtractionResult.Extracted>(extractor.extract("#Tag", "hashtag")).value)
        assertEquals("2026-05-31", assertIs<CoreEntityExtractionResult.Extracted>(extractor.extract("2026-05-31", "date")).value)
        assertIs<CoreEntityExtractionResult.Rejected>(extractor.extract("2026-99-31", "date"))
    }

    @Test
    fun actionValidatorImplDelegatesContractValidation() = runTest {
        val validator = ActionValidatorImpl()

        val report = validator.validate(OpenProfileActionDto("req-2", "ana"))

        assertIs<ValidValidationReportDto>(report)
    }

    @Test
    fun routerErrorMapsToTypedFailureDto() {
        val failure = RouterError.InvalidEntity("target", "bad username").toFailureDto()

        val validation = assertIs<com.mindos.contracts.ValidationFailedFailureDto>(failure)
        assertEquals("target", validation.failures.single().field)
    }
}
