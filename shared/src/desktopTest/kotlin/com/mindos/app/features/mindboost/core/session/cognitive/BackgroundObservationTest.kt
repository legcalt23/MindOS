package com.mindos.app.features.mindboost.core.session.cognitive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * FOC-P0-03 · `backgroundedDuringSession: Boolean` was a field NO producer ever observed (every writer hardcoded
 * `false`), so "not watched" read as "kept the foreground". It is now a tri-state: the policy denies only what was
 * OBSERVED, `NotObserved` stays visible and undecided, and the schema-1 wire is read back honestly.
 */
class BackgroundObservationTest {

    private fun evaluate(background: BackgroundObservation) = SessionEligibilityPolicy.evaluate(
        CognitiveSessionRunFixtures.validInputs(
            timing = CognitiveSessionRunFixtures.cleanTiming().copy(background = background),
        ),
    )

    // ---- policy -------------------------------------------------------------------------------------------

    @Test
    fun an_observed_foreground_loss_denies_every_lane_with_its_own_reason() {
        val result = evaluate(BackgroundObservation.Observed(backgrounded = true))

        assertFalse(result.eligibility.profileEligible)
        assertFalse(result.eligibility.calibrationEligible)
        assertFalse(result.eligibility.rankingEligible)
        assertFalse(result.trace.integrityEligible)
        assertTrue(SessionEligibilityPolicy.REASON_BACKGROUNDED in result.trace.rejectionReasons)
        // The clock itself was fine: the timing-integrity reason must NOT be borrowed to explain a lifecycle fact.
        assertFalse(SessionEligibilityPolicy.REASON_TIMING_INTEGRITY in result.trace.rejectionReasons)
    }

    @Test
    fun a_session_observed_to_keep_the_foreground_is_not_denied_for_it() {
        val result = evaluate(BackgroundObservation.Observed(backgrounded = false))

        assertTrue(result.eligibility.profileEligible)
        assertTrue(result.trace.integrityEligible)
        assertFalse(SessionEligibilityPolicy.REASON_BACKGROUNDED in result.trace.rejectionReasons)
    }

    @Test
    fun not_observed_is_neither_a_pass_nor_a_denial_and_stays_visible_on_the_snapshot() {
        val result = evaluate(BackgroundObservation.NotObserved)

        // Declared ceiling: a producer without a sensor cannot be denied for what it could not see...
        assertTrue(result.eligibility.profileEligible)
        assertFalse(SessionEligibilityPolicy.REASON_BACKGROUNDED in result.trace.rejectionReasons)
        // ...but the absence travels with the eligibility, typed, instead of collapsing into "kept the foreground".
        assertEquals(BackgroundObservation.NotObserved, result.eligibility.timingIntegrity.background)
        assertNotEquals<BackgroundObservation>(
            BackgroundObservation.Observed(backgrounded = false),
            result.eligibility.timingIntegrity.background,
        )
    }

    // ---- wire ---------------------------------------------------------------------------------------------

    private fun runWith(background: BackgroundObservation): CognitiveSessionRun {
        val run = CognitiveSessionRunFixtures.validRun()
        return run.copy(
            eligibility = run.eligibility.copy(
                timingIntegrity = run.eligibility.timingIntegrity.copy(background = background),
            ),
        )
    }

    private fun schema2Json(): String {
        val json = encodeRun(cognitiveRunJson, runWith(BackgroundObservation.Observed(backgrounded = false)))
        assertTrue("\"schemaVersion\":2" in json, "fixture must write schema 2: $json")
        assertTrue(KEPT_ON_WIRE in json, "fixture must write the observation on the wire: $json")
        assertTrue(LEGACY_NULL in json, "schema 2 never writes the legacy Boolean: $json")
        return json
    }

    /** Rewrites a schema-2 row into the exact shape a schema-1 writer produced (Boolean only, no `background`). */
    private fun legacyJson(backgrounded: Boolean): String = schema2Json()
        .replace("\"schemaVersion\":2", "\"schemaVersion\":1")
        .replace(LEGACY_NULL, "\"backgroundedDuringSession\":$backgrounded")
        .replace("$KEPT_ON_WIRE,", "")
        .also { assertFalse("\"background\"" in it, "legacy row must not carry the schema-2 field: $it") }

    @Test
    fun every_observation_survives_a_schema_2_round_trip() {
        listOf(
            BackgroundObservation.Observed(backgrounded = true),
            BackgroundObservation.Observed(backgrounded = false),
            BackgroundObservation.NotObserved,
        ).forEach { background ->
            val decoded = decodeRun(cognitiveRunJson, encodeRun(cognitiveRunJson, runWith(background)))
            assertEquals(background, decoded.eligibility.timingIntegrity.background)
        }
    }

    @Test
    fun a_legacy_false_is_read_as_not_observed_never_as_kept_the_foreground() {
        val decoded = decodeRun(cognitiveRunJson, legacyJson(backgrounded = false))

        assertEquals(BackgroundObservation.NotObserved, decoded.eligibility.timingIntegrity.background)
    }

    @Test
    fun a_legacy_true_keeps_the_observation_it_claims() {
        val decoded = decodeRun(cognitiveRunJson, legacyJson(backgrounded = true))

        assertEquals(
            BackgroundObservation.Observed(backgrounded = true),
            decoded.eligibility.timingIntegrity.background,
        )
    }

    @Test
    fun a_schema_2_row_that_is_silent_about_the_background_is_rejected() {
        val silent = schema2Json().replace("$KEPT_ON_WIRE,", "")
        assertFalse("\"background\"" in silent)

        assertFailsWith<IllegalArgumentException> { decodeRun(cognitiveRunJson, silent) }
    }

    @Test
    fun an_unknown_wire_value_is_rejected_not_defaulted() {
        val newer = schema2Json().replace("OBSERVED_FOREGROUND", "OBSERVED_SOMETHING_NEWER")

        assertFailsWith<IllegalArgumentException> { decodeRun(cognitiveRunJson, newer) }
    }

    private companion object {
        const val KEPT_ON_WIRE = "\"background\":\"OBSERVED_FOREGROUND\""
        const val LEGACY_NULL = "\"backgroundedDuringSession\":null"
    }
}
