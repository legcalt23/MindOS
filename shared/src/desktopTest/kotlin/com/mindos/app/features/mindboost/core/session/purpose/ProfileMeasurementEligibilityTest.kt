package com.mindos.app.features.mindboost.core.session.purpose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 R3-FINAL D1, re-sealed by Focus V5 · S1 · ADR-FOCUS-PURPOSE — the fail-closed measurement-eligibility
 * RULE and its three-way disposition. A session may feed the live cognitive profile ONLY with a sealed
 * measurement/probe (or explicit CALIBRATION/INITIAL_ASSESSMENT) purpose AND a complete versioned lineage.
 * Everything else denies. There is no plan/subscription input, so Free and Elite are identical.
 */
class ProfileMeasurementEligibilityTest {

    private fun eligible(purpose: SessionMeasurementPurpose, lineageComplete: Boolean = true) =
        ProfileMeasurementEligibility.measurementEligible(purpose, lineageComplete)

    private fun disposition(purpose: SessionMeasurementPurpose, lineageComplete: Boolean = true) =
        ProfileMeasurementEligibility.disposition(purpose, lineageComplete)

    @Test
    fun sealed_measurement_purposes_with_complete_lineage_are_eligible() {
        assertTrue(eligible(SessionMeasurementPurpose.MEASUREMENT))
        assertTrue(eligible(SessionMeasurementPurpose.CALIBRATION))
        assertTrue(eligible(SessionMeasurementPurpose.INITIAL_ASSESSMENT))
    }

    @Test
    fun training_practice_debug_unknown_all_fail_closed_even_with_complete_lineage() {
        assertFalse(eligible(SessionMeasurementPurpose.TRAINING_FREE), "TRAINING_FREE never feeds the profile")
        assertFalse(eligible(SessionMeasurementPurpose.TRAINING_RECOMMENDED))
        assertFalse(eligible(SessionMeasurementPurpose.PRACTICE))
        assertFalse(eligible(SessionMeasurementPurpose.DEBUG))
        assertFalse(eligible(SessionMeasurementPurpose.UNKNOWN), "absent/unknown purpose fails closed")
    }

    @Test
    fun the_three_purposes_S1_added_are_all_structurally_excluded() {
        // TUTORIAL and FAMILIARIZATION exist so a warm-up can be RECORDED as such, never so it can count: the
        // standing prohibition is that tutorial/familiarization never feed a score. RESEARCH_ONLY is data gathered
        // under a study protocol, which stays out of the live profile no matter how clean it is.
        listOf(
            SessionMeasurementPurpose.TUTORIAL,
            SessionMeasurementPurpose.FAMILIARIZATION,
            SessionMeasurementPurpose.RESEARCH_ONLY,
        ).forEach { purpose ->
            assertFalse(eligible(purpose), "$purpose must never feed the profile")
            assertEquals(
                ProfileEligibilityDisposition.NotEligibleByPurpose(purpose),
                disposition(purpose),
                "$purpose is a DEFINITE no, not an Indeterminate — we know exactly what it was.",
            )
        }
    }

    @Test
    fun an_eligible_purpose_without_complete_lineage_still_denies() {
        // D2: without a complete versioned lineage/scale-key, comparability cannot be established, so even a
        // measurement purpose is denied — the current Focus/Logic production path (UNKNOWN + no lineage).
        assertFalse(eligible(SessionMeasurementPurpose.MEASUREMENT, lineageComplete = false))
        assertFalse(eligible(SessionMeasurementPurpose.CALIBRATION, lineageComplete = false))
    }

    @Test
    fun a_denial_says_WHY_it_denied_and_the_two_kinds_are_never_confused() {
        // The whole point of the sealed 3-way: "not allowed" and "cannot tell" are different facts. Collapsing them
        // into a single false hides the one that means the sealing path is broken.
        assertEquals(
            ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.PURPOSE_NOT_SEALED),
            disposition(SessionMeasurementPurpose.UNKNOWN),
            "UNKNOWN is 'not sealed', NEVER a way to spell 'not applicable'.",
        )
        assertEquals(
            ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.LINEAGE_INCOMPLETE),
            disposition(SessionMeasurementPurpose.MEASUREMENT, lineageComplete = false),
            "A measurement purpose with broken lineage is indeterminate, not 'wrong purpose'.",
        )
        assertEquals(
            ProfileEligibilityDisposition.NotEligibleByPurpose(SessionMeasurementPurpose.TRAINING_FREE),
            disposition(SessionMeasurementPurpose.TRAINING_FREE),
            "A known training purpose is a definite no, not an indeterminate.",
        )
    }

    @Test
    fun every_purpose_resolves_and_exactly_three_of_eleven_can_ever_be_eligible() {
        // Totality + a real cardinality claim. If someone adds a member and routes it to Eligible, or widens the
        // eligible set, this fails: the count is asserted, not merely re-read from the implementation.
        val eligibleOnes = SessionMeasurementPurpose.entries.filter { eligible(it) }
        assertEquals(
            listOf(
                SessionMeasurementPurpose.MEASUREMENT,
                SessionMeasurementPurpose.CALIBRATION,
                SessionMeasurementPurpose.INITIAL_ASSESSMENT,
            ),
            eligibleOnes,
            "Exactly these three purposes may ever feed the profile.",
        )
        assertEquals(11, SessionMeasurementPurpose.entries.size, "The sealed purpose vocabulary is 11 members.")
        SessionMeasurementPurpose.entries.forEach { purpose ->
            // With lineage broken, NOTHING is eligible — the lineage gate is an independent AND, not a tie-breaker.
            assertFalse(eligible(purpose, lineageComplete = false), "$purpose must deny without complete lineage")
        }
    }

    @Test
    fun an_unrecognised_or_absent_wire_value_deserialises_to_a_denial_instead_of_throwing() {
        // A row written by a NEWER build must not crash this one and must not be guessed into a neighbour: it is
        // "not sealed", which denies. Same for an absent field.
        assertEquals(SessionMeasurementPurpose.UNKNOWN, SessionMeasurementPurpose.fromWire("SOMETHING_FUTURE"))
        assertEquals(SessionMeasurementPurpose.UNKNOWN, SessionMeasurementPurpose.fromWire(null))
        assertEquals(SessionMeasurementPurpose.UNKNOWN, SessionMeasurementPurpose.fromWire(""))
        assertEquals(
            SessionMeasurementPurpose.MEASUREMENT,
            SessionMeasurementPurpose.fromWire("MEASUREMENT"),
            "Discriminating half: a KNOWN name must still round-trip, or the check above proves nothing.",
        )
        assertIs<ProfileEligibilityDisposition.Indeterminate>(
            disposition(SessionMeasurementPurpose.fromWire("SOMETHING_FUTURE")),
        )
    }

    // DELIBERATELY NOT A TEST: "a paid plan cannot raise scientific eligibility".
    //
    // The guarantee is real and it is enforced by the SIGNATURE — disposition(purpose, lineageComplete) has
    // nothing plan- or subscription-shaped to read, so the compiler already refuses the failure mode. A test
    // here previously asserted `assertEquals(disposition(p), disposition(p))`, comparing a pure function to
    // itself: it passed against every possible implementation, including one that read a plan flag. It was
    // removed rather than weakened-in-place, because a test that cannot fail is worse than no test — it
    // reports coverage where there is none (learning 2026-07-15).
}
