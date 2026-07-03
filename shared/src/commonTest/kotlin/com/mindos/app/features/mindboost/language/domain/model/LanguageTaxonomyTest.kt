package com.mindos.app.features.mindboost.language.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the language taxonomy cardinalities and identifier uniqueness to the v6 design
 * (§4.2 traits, §6.6 errors, §8.3 banks, §5.5 gates). An accidental add/remove of an enum
 * value is caught immediately, because every other layer references these closed sets.
 */
class LanguageTaxonomyTest {

    @Test
    fun nineteenTraits() {
        assertEquals(19, TraitCode.entries.size)
    }

    @Test
    fun fourteenErrorTypes() {
        assertEquals(14, ErrorType.entries.size)
    }

    @Test
    fun nineBanks() {
        assertEquals(9, BankType.entries.size)
    }

    @Test
    fun tenEvidenceGates() {
        assertEquals(10, EvidenceGate.entries.size)
    }

    @Test
    fun traitCodesAreUnique() {
        val codes = TraitCode.entries.map { it.code }
        assertEquals(TraitCode.entries.size, codes.toSet().size)
    }

    @Test
    fun errorCodesAreUnique() {
        val codes = ErrorType.entries.map { it.code }
        assertEquals(ErrorType.entries.size, codes.toSet().size)
    }

    @Test
    fun everyErrorMapsToAtLeastOnePrimaryTrait() {
        assertTrue(ErrorType.entries.all { it.primaryTraits.isNotEmpty() })
    }

    @Test
    fun onlyThreeResponseModesAreClosed() {
        assertEquals(3, ResponseMode.entries.count { it.isClosed })
    }

    @Test
    fun onlyDeterministicRulesAreClosedScorable() {
        // The offline spine may only auto-score deterministic rules; rubric rules need the
        // optional Evaluator OS.
        assertEquals(4, ScoringRule.entries.count { it.isDeterministic })
    }

    @Test
    fun userContentPrivacyClassesAreFlagged() {
        // Free user text / documents / audio / sensitive must be flagged as user content so
        // retention and redaction policies can key on it (§13).
        assertTrue(PrivacyClass.USER_GENERATED_TEXT.isUserContent)
        assertTrue(PrivacyClass.USER_AUDIO.isUserContent)
        assertTrue(!PrivacyClass.MINIMAL_TELEMETRY.isUserContent)
        assertTrue(!PrivacyClass.NON_SENSITIVE_ORIGINAL_CONTENT.isUserContent)
    }
}
