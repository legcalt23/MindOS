package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
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

/**
 * Focus V5 · S3 · tarea (4) — process-death restore, fail-closed.
 *
 * The suite is organised around one question per test: "what fact about the stored records makes resuming
 * dishonest?" Every branch of `firstProblem` has a case here, and the final test asserts that the enum has no
 * member without one — so adding a rejection reason without a test turns the file red.
 */
class FocusSessionRestoreTest {

    private val current = EngineVersions.CURRENT

    private fun descriptor(
        purpose: SessionMeasurementPurpose = SessionMeasurementPurpose.MEASUREMENT,
    ) = FocusSessionDescriptor.seal(
        sessionId = SESSION_ID,
        exerciseId = VectorFilterPlanner.EXERCISE_ID,
        purpose = purpose,
        seedSource = SeedSource { SessionSeed(11L, RngVersion.V1) },
        generatorVersion = VectorFilterPlanner.GENERATOR_VERSION,
        scorerVersion = DescriptiveScorer.SCORER_VERSION,
        constructModelVersion = 1,
        spec = VectorFilterSpec(),
        inputModality = InputModality.TOUCH_DIRECT,
        screenReaderActive = false,
    )

    /** A snapshot mid-session: one resolved trial, one open window. */
    private fun snapshot(state: FocusSessionState = FocusSessionState.RUNNING) = FocusRuntimeSnapshot(
        sessionId = SESSION_ID,
        stateName = state.name,
        phaseName = TrialPhase.AWAITING_RESPONSE.name,
        trialIndex = 1,
        presentedAtElapsedNanos = 500L,
        deadlineElapsedNanos = 1_500L,
        tallies = listOf(ConditionTally(ConditionId.CONGRUENT.name, SdtCell(hits = 1))),
    )

    /** The same session, but between trials — no window open. */
    private fun settled() = snapshot().copy(
        phaseName = TrialPhase.INTER_STIMULUS.name,
        presentedAtElapsedNanos = null,
        deadlineElapsedNanos = null,
    )

    private fun restore(
        d: FocusSessionDescriptor? = descriptor(),
        s: FocusRuntimeSnapshot? = snapshot(),
        versions: EngineVersions = current,
    ) = FocusSessionRestore.restore(d?.let { Json.encodeToString(it) }, s?.let { Json.encodeToString(it) }, versions)

    private fun rejectionOf(
        d: FocusSessionDescriptor? = descriptor(),
        s: FocusRuntimeSnapshot? = snapshot(),
        versions: EngineVersions = current,
    ): RestoreRejection = assertIs<RestoreOutcome.Rejected>(restore(d, s, versions)).reason

    // --- the two non-rejections ----------------------------------------------------------------------------

    @Test
    fun a_sound_mid_session_record_resumes_with_its_counts_intact() {
        val outcome = assertIs<RestoreOutcome.Restored>(restore(s = settled()))
        assertEquals(1, outcome.snapshot.trialIndex)
        assertEquals(1, outcome.snapshot.resolvedTrials, "the tallies survive the round trip")
        assertEquals(SessionMeasurementPurpose.MEASUREMENT, outcome.descriptor.purpose)
    }

    /**
     * The fix for the efficacy gate's first blocker.
     *
     * A snapshot whose window was open carries elapsed anchors from a monotonic epoch that died with the old
     * process. Resuming it would compute a latency against a clock that no longer exists — a negative value,
     * which lands under the anticipation floor and records that the person answered before perception was
     * possible. So the open trial is discarded, exactly as it is for a pause or an interruption: process death
     * is the most extreme interruption there is, and it would be incoherent for it to be the one that kept its
     * trial.
     */
    @Test
    fun an_open_window_is_discarded_on_restore_rather_than_resumed_across_a_dead_clock() {
        val outcome = assertIs<RestoreOutcome.Restored>(restore()) // the default fixture IS mid-window
        assertEquals(null, outcome.snapshot.deadlineElapsedNanos, "the stale window must not be resumable")
        assertEquals(null, outcome.snapshot.presentedAtElapsedNanos, "nor its stale presentation anchor")
        assertEquals(1, outcome.snapshot.contaminatedCount, "the discard is recorded, not silent")
        assertEquals(2, outcome.snapshot.trialIndex, "the discarded trial is not re-presented")

        // The discriminating half: a snapshot with no open window is returned untouched.
        val calm = assertIs<RestoreOutcome.Restored>(restore(s = settled()))
        assertEquals(0, calm.snapshot.contaminatedCount)
        assertEquals(1, calm.snapshot.trialIndex)
    }

    @Test
    fun a_finished_session_is_nothing_to_resume_rather_than_a_failure() {
        val finished = snapshot(FocusSessionState.COMPLETED)
            .copy(phaseName = TrialPhase.IDLE.name, presentedAtElapsedNanos = null, deadlineElapsedNanos = null)
        assertIs<RestoreOutcome.NothingToResume>(restore(s = finished))
    }

    // --- one test per rejection ----------------------------------------------------------------------------

    @Test
    fun a_missing_seal_and_a_damaged_seal_are_different_facts() {
        assertEquals(RestoreRejection.DESCRIPTOR_ABSENT, rejectionOf(d = null))
        assertEquals(
            RestoreRejection.DESCRIPTOR_UNREADABLE,
            assertIs<RestoreOutcome.Rejected>(
                FocusSessionRestore.restore("{not json", Json.encodeToString(snapshot()), current),
            ).reason,
        )
    }

    /**
     * S4 · `MB-FOCUS-0040` follow-up (privacy exit gate): the widened `spec` serialises POLYMORPHICALLY, so a
     * spec TYPE a newer build wrote must fail the decode fail-CLOSED, not resolve to a wrong default. Observed
     * here — mutate the discriminator to an unregistered subtype — rather than inferred from library semantics.
     * `spec` has no default in the descriptor, so an undecodable spec cannot quietly vanish either.
     */
    @Test
    fun a_spec_of_an_unknown_exercise_type_is_unreadable_not_coerced() {
        val tampered = Json.encodeToString(descriptor()).replace("focus2.VectorFilterSpec", "focus2.SomeFutureSpec")
        assertEquals(
            RestoreRejection.DESCRIPTOR_UNREADABLE,
            assertIs<RestoreOutcome.Rejected>(
                FocusSessionRestore.restore(tampered, Json.encodeToString(snapshot()), current),
            ).reason,
        )
    }

    @Test
    fun a_missing_snapshot_and_a_damaged_snapshot_are_different_facts() {
        assertEquals(RestoreRejection.SNAPSHOT_ABSENT, rejectionOf(s = null))
        assertEquals(
            RestoreRejection.SNAPSHOT_UNREADABLE,
            assertIs<RestoreOutcome.Rejected>(
                FocusSessionRestore.restore(Json.encodeToString(descriptor()), "{not json", current),
            ).reason,
        )
    }

    @Test
    fun a_record_written_by_a_newer_build_is_refused_rather_than_coerced() {
        assertEquals(
            RestoreRejection.SCHEMA_UNSUPPORTED,
            rejectionOf(d = descriptor().copy(schemaVersion = FocusSessionDescriptor.SCHEMA_VERSION + 1)),
        )
        assertEquals(
            RestoreRejection.SCHEMA_UNSUPPORTED,
            rejectionOf(s = settled().copy(schemaVersion = FocusRuntimeSnapshot.SCHEMA_VERSION + 1)),
        )
    }

    @Test
    fun records_that_disagree_about_which_session_or_exercise_they_are_do_not_resume() {
        assertEquals(RestoreRejection.IDENTITY_MISMATCH, rejectionOf(s = settled().copy(sessionId = "other")))
        assertEquals(
            RestoreRejection.IDENTITY_MISMATCH,
            rejectionOf(versions = current.copy(exerciseId = "foc2-e01-centinela")),
        )
    }

    @Test
    fun an_unsealed_purpose_stops_the_resume_instead_of_defaulting_to_practice() {
        assertEquals(
            RestoreRejection.PURPOSE_NOT_SEALED,
            rejectionOf(d = descriptor().copy(purposeName = "NOT_A_PURPOSE")),
        )
    }

    @Test
    fun a_seed_whose_algorithm_this_build_cannot_mint_is_not_reproducible() {
        assertEquals(
            RestoreRejection.RNG_VERSION_UNSUPPORTED,
            rejectionOf(d = descriptor().copy(rngVersionId = "v-future")),
        )
    }

    /**
     * The plan's golden: "mutación de versión ⇒ restore rechazado". Both halves, because a generator change and
     * a scorer change are different instruments failing in the same way.
     */
    @Test
    fun a_generator_or_scorer_version_change_refuses_the_resume() {
        assertEquals(
            RestoreRejection.ENGINE_VERSION_CHANGED,
            rejectionOf(versions = current.copy(generatorVersion = current.generatorVersion + 1)),
        )
        assertEquals(
            RestoreRejection.ENGINE_VERSION_CHANGED,
            rejectionOf(versions = current.copy(scorerVersion = current.scorerVersion + 1)),
        )
    }

    @Test
    fun a_snapshot_that_contradicts_itself_is_refused() {
        val impossible = listOf(
            settled().copy(trialIndex = -1),
            settled().copy(resumeCount = -1),
            settled().copy(contaminatedCount = -1),
            settled().copy(trialIndex = VectorFilterSpec().totalTrials + 1),
            snapshot().copy(presentedAtElapsedNanos = null), // window open, nothing presented
            // More trials resolved than were ever reached: a trial must be presented before it can resolve.
            settled().copy(tallies = listOf(ConditionTally(ConditionId.CONGRUENT.name, SdtCell(hits = 9)))),
            // A condition name this build cannot resolve would silently vanish from the scoring map.
            settled().copy(tallies = listOf(ConditionTally("SOME_FUTURE_CONDITION", SdtCell(hits = 1)))),
        )
        impossible.forEachIndexed { i, s ->
            assertEquals(RestoreRejection.SNAPSHOT_INCOHERENT, rejectionOf(s = s), "case #$i")
        }
    }

    /**
     * Anti-vacuity: every rejection reason must be reachable by a test in this file.
     *
     * Without this, adding an eleventh reason and never exercising it would leave a fail-closed path that has
     * never once been observed failing closed.
     */
    @Test
    fun every_rejection_reason_is_covered_by_a_case_in_this_file() {
        val exercised = setOf(
            rejectionOf(d = null),
            RestoreRejection.DESCRIPTOR_UNREADABLE,
            rejectionOf(s = null),
            RestoreRejection.SNAPSHOT_UNREADABLE,
            rejectionOf(d = descriptor().copy(schemaVersion = 99)),
            rejectionOf(s = settled().copy(sessionId = "other")),
            rejectionOf(d = descriptor().copy(purposeName = "NOT_A_PURPOSE")),
            rejectionOf(d = descriptor().copy(rngVersionId = "v-future")),
            rejectionOf(versions = current.copy(generatorVersion = 99)),
            rejectionOf(s = settled().copy(trialIndex = -1)),
        )
        assertEquals(RestoreRejection.entries.toSet(), exercised)
    }

    private companion object {
        const val SESSION_ID = "s-restore"
    }
}
