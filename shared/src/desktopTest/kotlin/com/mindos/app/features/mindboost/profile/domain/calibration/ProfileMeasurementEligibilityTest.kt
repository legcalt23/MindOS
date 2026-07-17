package com.mindos.app.features.mindboost.profile.domain.calibration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 R3-FINAL D1 — the fail-closed measurement-eligibility RULE. A session may feed the live cognitive
 * profile ONLY with a sealed measurement/probe (or explicit CALIBRATION/INITIAL_ASSESSMENT) purpose AND a complete
 * versioned lineage. Everything else denies. There is no plan/subscription input, so Free and Elite are identical.
 */
class ProfileMeasurementEligibilityTest {

    private fun eligible(purpose: SessionMeasurementPurpose, lineageComplete: Boolean = true) =
        ProfileMeasurementEligibility.measurementEligible(purpose, lineageComplete)

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
    fun an_eligible_purpose_without_complete_lineage_still_denies() {
        // D2: without a complete versioned lineage/scale-key, comparability cannot be established, so even a
        // measurement purpose is denied — the current Focus/Logic production path (UNKNOWN + no lineage).
        assertFalse(eligible(SessionMeasurementPurpose.MEASUREMENT, lineageComplete = false))
        assertFalse(eligible(SessionMeasurementPurpose.CALIBRATION, lineageComplete = false))
    }

    @Test
    fun the_signature_has_no_plan_input_so_free_and_elite_are_identical() {
        // A structural guarantee, not a runtime one: measurementEligible takes only (purpose, lineageComplete);
        // there is nothing plan/subscription-shaped to read, so a paid plan can never raise scientific eligibility.
        SessionMeasurementPurpose.entries.forEach { purpose ->
            val once = eligible(purpose)
            val twice = eligible(purpose)
            assertTrue(once == twice, "$purpose eligibility must be plan-independent and deterministic")
        }
    }
}
