package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.IndeterminateReason
import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.SentinelSpec
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterSpec
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Focus V5 · S3 · tarea (4)+(5) — the pre-trial seal. */
class FocusSessionDescriptorTest {

    private val fixedSeed = SeedSource { SessionSeed(0x0123456789ABCDEFL, RngVersion.V1) }

    private fun descriptor(
        purpose: SessionMeasurementPurpose = SessionMeasurementPurpose.MEASUREMENT,
        screenReader: Boolean = false,
        modality: InputModality = InputModality.TOUCH_DIRECT,
    ) = FocusSessionDescriptor.seal(
        sessionId = "s-1",
        exerciseId = "foc2-e07-filtro-vectorial",
        purpose = purpose,
        seedSource = fixedSeed,
        generatorVersion = 1,
        scorerVersion = 1,
        constructModelVersion = 1,
        spec = VectorFilterSpec(),
        inputModality = modality,
        screenReaderActive = screenReader,
    )

    @Test
    fun the_seed_comes_from_the_injected_source_and_survives_a_round_trip_unchanged() {
        val sealed = descriptor()
        assertEquals(SessionSeed(0x0123456789ABCDEFL, RngVersion.V1), sealed.seed)
        assertEquals("0123456789abcdef", sealed.seedHex)

        val decoded = Json.decodeFromString<FocusSessionDescriptor>(Json.encodeToString(sealed))
        assertEquals(sealed, decoded, "the descriptor must survive persistence byte-for-byte")
        assertEquals(sealed.seedHex, decoded.seedHex, "REDTEAM #26b: the persisted seed must not change")
    }

    @Test
    fun a_purpose_this_build_does_not_recognise_denies_instead_of_crashing() {
        val alien = descriptor().copy(purposeName = "SOME_FUTURE_PURPOSE")
        assertEquals(SessionMeasurementPurpose.UNKNOWN, alien.purpose)
        assertEquals(
            ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.PURPOSE_NOT_SEALED),
            alien.evidenceDisposition(),
        )
    }

    @Test
    fun an_unknown_rng_version_makes_the_lineage_incomplete_rather_than_defaulting_to_one() {
        val alien = descriptor().copy(rngVersionId = "v99")
        assertNull(alien.rngVersion, "an unknown algorithm must not resolve to a default")
        assertNull(alien.seed, "a seed without its algorithm is not a seed")
        assertFalse(alien.lineageComplete)
        assertEquals(
            ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.LINEAGE_INCOMPLETE),
            alien.evidenceDisposition(),
        )
    }

    /** Task (5): a warm-up run can never become evidence, and the reason is its PURPOSE, not its quality. */
    @Test
    fun tutorial_and_familiarization_are_never_evidence_while_measurement_is() {
        listOf(SessionMeasurementPurpose.TUTORIAL, SessionMeasurementPurpose.FAMILIARIZATION).forEach { purpose ->
            assertEquals(
                ProfileEligibilityDisposition.NotEligibleByPurpose(purpose),
                descriptor(purpose = purpose).evidenceDisposition(),
                "$purpose must never be evidence",
            )
        }
        // The discriminating half: without it, a bug that denied EVERYTHING would pass the assertions above.
        assertEquals(
            ProfileEligibilityDisposition.Eligible,
            descriptor(purpose = SessionMeasurementPurpose.MEASUREMENT).evidenceDisposition(),
        )
    }

    @Test
    fun an_active_screen_reader_withdraws_rt_from_evidence_and_nothing_else() {
        val assisted = descriptor(screenReader = true)
        assertFalse(assisted.rtEvidenceAdmissible, "REDTEAM #33: the RT construct changes with a screen reader")
        assertEquals(
            ProfileEligibilityDisposition.Eligible,
            assisted.evidenceDisposition(),
            "accuracy stays OFFICIAL and the person is never excluded from the exercise",
        )
        assertTrue(descriptor(screenReader = false).rtEvidenceAdmissible, "discriminating half")
    }

    @Test
    fun switch_access_implies_the_sequential_modality_and_is_recorded_as_a_distinct_one() {
        assertEquals(
            InputModality.SWITCH_SEQUENTIAL,
            AccessibilityContext(switchAccessActive = true).impliedModality,
        )
        // Fail-CLOSED: nothing detected and nothing declared is UNKNOWN, not TOUCH_DIRECT. An undetected
        // assistive input must not be admitted as direct touch by default.
        assertEquals(InputModality.UNKNOWN, AccessibilityContext.NONE.impliedModality)
        assertEquals(
            InputModality.TOUCH_DIRECT,
            AccessibilityContext(directTouchDeclared = true).impliedModality,
            "only a positive declaration of direct touch selects it",
        )
        assertFalse(
            descriptor(modality = InputModality.UNKNOWN).rtEvidenceAdmissible,
            "an unconfirmed modality withholds RT rather than admitting it",
        )
        assertEquals(
            InputModality.SWITCH_SEQUENTIAL,
            descriptor(modality = InputModality.SWITCH_SEQUENTIAL).inputModality,
        )
    }

    @Test
    fun a_modality_written_by_a_newer_build_does_not_masquerade_as_touch() {
        assertEquals(InputModality.UNKNOWN, descriptor().copy(inputModalityName = "EYE_TRACKING").inputModality)
    }

    /**
     * S4 · `MB-FOCUS-0040`: the descriptor's `spec` widened to the neutral [FocusExerciseSpec], so a session of
     * a DIFFERENT exercise (the vigilance tracer) can be sealed without fabricating flanker parameters. It must
     * survive the polymorphic round trip, and the engine must still reach its two scalars through the interface.
     */
    @Test
    fun a_descriptor_sealed_with_a_different_exercise_spec_round_trips_polymorphically() {
        val sealed = FocusSessionDescriptor.seal(
            sessionId = "s-2",
            exerciseId = "foc2-e01-centinela",
            purpose = SessionMeasurementPurpose.MEASUREMENT,
            seedSource = fixedSeed,
            generatorVersion = 1,
            scorerVersion = 1,
            constructModelVersion = 1,
            spec = SentinelSpec(),
            inputModality = InputModality.TOUCH_DIRECT,
            screenReaderActive = false,
        )
        val decoded = Json.decodeFromString<FocusSessionDescriptor>(Json.encodeToString(sealed))
        assertEquals(sealed, decoded, "a vigilance spec must survive the polymorphic round trip")
        assertIs<SentinelSpec>(decoded.spec, "the concrete spec type must be preserved, not collapsed")
        assertEquals(SentinelSpec.DEFAULT_RESPONSE_WINDOW_MS, decoded.spec.responseWindowMs)
        assertEquals(SentinelSpec().totalTrials, decoded.spec.totalTrials)
    }
}
