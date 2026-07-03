package com.mindos.app.features.arena

import com.mindos.app.features.arena.session.explanation.InternalReasonCode
import com.mindos.app.features.arena.session.explanation.UserSafeExplanationMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 04 / P4.3: User-safe explanation mapping reveals no thresholds/algorithms/anti-cheat. */
class UserSafeExplanationMapperTest {

    // Tokens that would leak thresholds, algorithm internals, or anti-cheat signals.
    private val leakTokens = listOf(
        "sem", "sigma", "bayesian", "latency", "jitter", "threshold", "algorithm",
        "reliability", "integrity", "smurf", "boost", "collusion", "cheat", "multiplier",
    )

    @Test
    fun everyCodeHasNonEmptyCopy() {
        for (code in InternalReasonCode.entries) {
            assertTrue(UserSafeExplanationMapper.copyFor(code).isNotBlank(), "blank copy for $code")
        }
    }

    @Test
    fun copyNeverLeaksNumbersOrTechnicalInternals() {
        for (code in InternalReasonCode.entries) {
            val copy = UserSafeExplanationMapper.copyFor(code)
            assertTrue(copy.none { it.isDigit() }, "copy for $code leaks a number: $copy")
            val lower = copy.lowercase()
            for (token in leakTokens) {
                assertTrue(!lower.contains(token), "copy for $code leaks '$token': $copy")
            }
        }
    }

    @Test
    fun rawCodesFromEveryLayerMapToSafeCategories() {
        assertEquals(
            InternalReasonCode.NOT_COMPARABLE,
            InternalReasonCode.fromRaw("NOT_COMPARABLE:LATENCY+NETWORK"),
        )
        assertEquals(
            InternalReasonCode.INSUFFICIENT_MEASUREMENT_CONFIDENCE,
            InternalReasonCode.fromRaw("SEM_ABOVE_MAX"),
        )
        assertEquals(
            InternalReasonCode.INTEGRITY_NOT_VERIFIED,
            InternalReasonCode.fromRaw("INTEGRITY_NOT_VERIFIED"),
        )
        assertEquals(InternalReasonCode.SUFFICIENT, InternalReasonCode.fromRaw("COMPARABLE:"))
        assertEquals(InternalReasonCode.UNKNOWN, InternalReasonCode.fromRaw("something_internal_47"))
    }

    @Test
    fun explainBridgesRawCodeToCopy() {
        assertEquals(
            UserSafeExplanationMapper.copyFor(InternalReasonCode.NOT_COMPARABLE),
            UserSafeExplanationMapper.explain("NOT_COMPARABLE:LATENCY"),
        )
    }
}
