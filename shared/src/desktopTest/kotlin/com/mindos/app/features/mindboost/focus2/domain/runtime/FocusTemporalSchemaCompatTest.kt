package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.core.timing.ClockDomain
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterPlanner
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterSpec
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import com.mindos.app.features.mindboost.focus2.domain.scoring.SdtCell
import com.mindos.app.features.mindboost.focus2.domain.session.FocusSessionState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §13 [R2-HIST] — the N-1 fixture: a v1 record (schema 1, no temporal fields)
 * decodes under the v2 reader, restores, and resolves to NOT CAPTURED — never to a plausible
 * capability and never to an Eligible/Excluded temporal verdict. No migration invents quality.
 */
class FocusTemporalSchemaCompatTest {

    private fun v1Descriptor() = FocusSessionDescriptor.seal(
        sessionId = "s5-v1compat",
        exerciseId = VectorFilterPlanner.EXERCISE_ID,
        purpose = SessionMeasurementPurpose.MEASUREMENT,
        seedSource = SeedSource { SessionSeed(11L, RngVersion.V1) },
        generatorVersion = VectorFilterPlanner.GENERATOR_VERSION,
        scorerVersion = DescriptiveScorer.SCORER_VERSION,
        constructModelVersion = 1,
        spec = VectorFilterSpec(),
        inputModality = InputModality.TOUCH_DIRECT,
        screenReaderActive = false,
        // No temporal capture arguments: exactly the shape a v1 writer produced.
    ).copy(schemaVersion = 1)

    private fun v1Snapshot() = FocusRuntimeSnapshot(
        schemaVersion = 1,
        sessionId = "s5-v1compat",
        stateName = FocusSessionState.RUNNING.name,
        phaseName = TrialPhase.INTER_STIMULUS.name,
        trialIndex = 1,
        tallies = listOf(ConditionTally(ConditionId.CONGRUENT.name, SdtCell(hits = 1))),
    )

    @Test
    fun v1Record_restoresUnderV2_andTemporalFactsReadNotCaptured() {
        val outcome = FocusSessionRestore.restore(
            Json.encodeToString(v1Descriptor()),
            Json.encodeToString(v1Snapshot()),
        )
        val restored = assertIs<RestoreOutcome.Restored>(outcome)

        assertNull(restored.descriptor.probeCapability, "a v1 record never captured a capability")
        assertEquals(ClockDomain.UNKNOWN, restored.descriptor.clockDomain)
        assertEquals(0, restored.descriptor.temporalObservationVersion)

        val evaluation = FocusTemporalEligibility.evaluate(
            probeCapability = restored.descriptor.probeCapability,
            clockDomain = restored.descriptor.clockDomain,
            snapshot = restored.snapshot,
        )
        assertIs<FocusTemporalEligibility.Verdict.NotCaptured>(
            evaluation.rtShadowVerdict,
            "empty temporal cells are ABSENCE — never 0-bad (Eligible) nor 0-good (Excluded)",
        )
    }

    @Test
    fun futureSchema_staysRejected_failClosed() {
        val outcome = FocusSessionRestore.restore(
            Json.encodeToString(v1Descriptor().copy(schemaVersion = FocusSessionDescriptor.SCHEMA_VERSION + 1)),
            Json.encodeToString(v1Snapshot()),
        )
        assertEquals(
            RestoreRejection.SCHEMA_UNSUPPORTED,
            assertIs<RestoreOutcome.Rejected>(outcome).reason,
        )
    }
}
