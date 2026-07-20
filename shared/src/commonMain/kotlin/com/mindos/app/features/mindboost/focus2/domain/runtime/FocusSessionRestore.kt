package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.VectorFilterPlanner
import com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer
import kotlinx.serialization.json.Json

/**
 * Focus V5 · S3 · tarea (4) — restoring a session after process death, FAIL-CLOSED.
 *
 * The rule this file exists to enforce: a session may only be resumed when the build resuming it can reproduce
 * exactly what the build that started it was doing. Anything less and the second half of the session is measured
 * under a different instrument from the first half — and because both halves land in one record, nothing
 * downstream can ever tell them apart again. Refusing to resume costs the participant a few minutes; resuming
 * wrongly costs the measurement its meaning, silently and permanently.
 *
 * So every rejection is a TYPED FACT about the stored records, never a guess and never a repair. In particular
 * nothing here "migrates" or "fixes up" a record: a snapshot written by a newer build is rejected rather than
 * coerced, because the field this build does not know about is exactly the one that would have changed the
 * answer.
 */
object FocusSessionRestore {

    /**
     * `ignoreUnknownKeys = false` on purpose, and it is the setting that makes this file's promise true.
     *
     * With it on, a record written by a newer build that added a field would decode SILENTLY, dropping the very
     * field that might have changed the answer — while the KDoc above claimed such records are "rejected rather
     * than coerced". Off, the unknown field makes the decode fail, the decode failure maps to `*_UNREADABLE`,
     * and the promise holds. Note that a `schemaVersion` bump is the polite version of the same signal; this is
     * the one that still works when someone forgets to bump it.
     */
    private val json = Json { ignoreUnknownKeys = false }

    /**
     * Attempts to resume the session described by [descriptorJson] / [snapshotJson].
     *
     * Both are nullable and both are raw text on purpose: this IS the deserialization boundary, so "absent" and
     * "unreadable" have to be distinguishable here or they never will be. A caller that hands over pre-decoded
     * objects has already made the decision this function is supposed to make.
     */
    fun restore(
        descriptorJson: String?,
        snapshotJson: String?,
        current: EngineVersions = EngineVersions.CURRENT,
    ): RestoreOutcome {
        val descriptor = descriptorJson?.let { decode<FocusSessionDescriptor>(it) }
        val snapshot = snapshotJson?.let { decode<FocusRuntimeSnapshot>(it) }
        return when {
            descriptorJson == null -> RestoreOutcome.Rejected(RestoreRejection.DESCRIPTOR_ABSENT)
            descriptor == null -> RestoreOutcome.Rejected(RestoreRejection.DESCRIPTOR_UNREADABLE)
            snapshotJson == null -> RestoreOutcome.Rejected(RestoreRejection.SNAPSHOT_ABSENT)
            snapshot == null -> RestoreOutcome.Rejected(RestoreRejection.SNAPSHOT_UNREADABLE)
            else -> verdict(descriptor, snapshot, current)
        }
    }

    private fun verdict(
        descriptor: FocusSessionDescriptor,
        snapshot: FocusRuntimeSnapshot,
        current: EngineVersions,
    ): RestoreOutcome {
        val rejection = firstProblem(descriptor, snapshot, current)
        return when {
            rejection != null -> RestoreOutcome.Rejected(rejection)
            snapshot.state.isTerminal -> RestoreOutcome.NothingToResume(descriptor, snapshot)
            else -> RestoreOutcome.Restored(descriptor, resumable(snapshot))
        }
    }

    /**
     * Makes a snapshot safe to continue in THIS process.
     *
     * A window that was open when the process died cannot be resumed: the snapshot's anchors are elapsed
     * readings from a monotonic epoch that ended with the old process, and the new one starts again at zero.
     * Continuing would compute a latency against an anchor from a different clock — which, being larger than
     * any current reading, comes out NEGATIVE, lands under the anticipation floor, and records that the person
     * answered before perception was possible. A process death would have manufactured a behavioural fact.
     *
     * So the open trial is DISCARDED, exactly as it is for a pause or an interruption (REDTEAM #21). Process
     * death is the most extreme interruption there is; it would have been incoherent for it to be the one
     * interruption that kept its trial.
     */
    private fun resumable(snapshot: FocusRuntimeSnapshot): FocusRuntimeSnapshot =
        if (snapshot.hasOpenWindow) FocusSessionReducer.discardOpenTrial(snapshot) else snapshot

    /**
     * The checks, in order, first failure wins.
     *
     * Ordered deliberately from "the records disagree about what they are" to "the records are fine but this
     * build is different", so the reported reason is the most fundamental one rather than whichever check
     * happened to run first.
     */
    private fun firstProblem(
        descriptor: FocusSessionDescriptor,
        snapshot: FocusRuntimeSnapshot,
        current: EngineVersions,
    ): RestoreRejection? = when {
        descriptor.schemaVersion > current.descriptorSchemaVersion ||
            snapshot.schemaVersion > current.snapshotSchemaVersion -> RestoreRejection.SCHEMA_UNSUPPORTED

        descriptor.sessionId != snapshot.sessionId ||
            descriptor.exerciseId != current.exerciseId -> RestoreRejection.IDENTITY_MISMATCH

        descriptor.purpose == SessionMeasurementPurpose.UNKNOWN -> RestoreRejection.PURPOSE_NOT_SEALED

        descriptor.rngVersion == null -> RestoreRejection.RNG_VERSION_UNSUPPORTED

        descriptor.generatorVersion != current.generatorVersion ||
            descriptor.scorerVersion != current.scorerVersion -> RestoreRejection.ENGINE_VERSION_CHANGED

        incoherent(descriptor, snapshot) -> RestoreRejection.SNAPSHOT_INCOHERENT

        else -> null
    }

    /**
     * Internal contradictions that no legal sequence of reductions could have produced.
     *
     * A record that cannot have been written by this engine was written by something else — a corruption, a
     * hand-edit, or a bug — and resuming from it would mean trusting arithmetic we know is wrong.
     */
    private fun incoherent(descriptor: FocusSessionDescriptor, snapshot: FocusRuntimeSnapshot): Boolean {
        val accountedTrials = snapshot.resolvedTrials + snapshot.contaminatedCount
        val openWindowWithoutPresentation =
            snapshot.phase == TrialPhase.AWAITING_RESPONSE &&
                (snapshot.presentedAtElapsedNanos == null || snapshot.deadlineElapsedNanos == null)
        val unknownCondition = snapshot.tallies.any { it.conditionId == null }
        val negativeCount = snapshot.tallies.any { it.cell.totalTrials < 0 }
        return snapshot.trialIndex < 0 ||
            snapshot.resumeCount < 0 ||
            snapshot.contaminatedCount < 0 ||
            snapshot.trialIndex > descriptor.spec.totalTrials ||
            accountedTrials > descriptor.spec.totalTrials ||
            // More resolved trials than the index says were reached is arithmetically impossible: a trial has
            // to be presented before it can resolve.
            accountedTrials > snapshot.trialIndex ||
            unknownCondition ||
            negativeCount ||
            openWindowWithoutPresentation
    }

    private inline fun <reified T> decode(raw: String): T? = runCatching { json.decodeFromString<T>(raw) }.getOrNull()
}

/**
 * The versions the CURRENT build implements. A restore compares the seal against these.
 *
 * [exerciseId] is here rather than being trusted from the descriptor because "this record belongs to a different
 * exercise" is a real corruption mode: two exercises share the same store, and a key collision would otherwise
 * resume E01's snapshot inside E07's engine.
 */
data class EngineVersions(
    val exerciseId: String,
    val generatorVersion: Int,
    val scorerVersion: Int,
    val descriptorSchemaVersion: Int = FocusSessionDescriptor.SCHEMA_VERSION,
    val snapshotSchemaVersion: Int = FocusRuntimeSnapshot.SCHEMA_VERSION,
) {
    companion object {
        /** Read from the components themselves, so bumping a generator cannot forget to bump this. */
        val CURRENT = EngineVersions(
            exerciseId = VectorFilterPlanner.EXERCISE_ID,
            generatorVersion = VectorFilterPlanner.GENERATOR_VERSION,
            scorerVersion = DescriptiveScorer.SCORER_VERSION,
        )
    }
}

/**
 * Why a stored session could not be resumed. Every entry is a FACT about the records; not one is a guess.
 *
 * **Deviation from the plan, declared explicitly:** the slice text asks for "9 razones tipadas" and there are
 * TEN. The extra one is the absent/unreadable split, which the plan's count collapses. Keeping them apart is the
 * repo's standing rule that a missing record and a damaged one are different facts (learning 2026-07-05): one
 * says a session never really started, the other says storage lost something that was there. Collapsing them
 * would hide the second, which is the one that indicates a bug rather than an ordinary abandoned run.
 */
enum class RestoreRejection {
    /** No seal was ever written. A snapshot without its descriptor has no purpose, no seed and no spec. */
    DESCRIPTOR_ABSENT,

    /** The seal exists but could not be decoded. Distinct from absent: this is damage, not a missing start. */
    DESCRIPTOR_UNREADABLE,

    /** A seal exists but no progress was recorded. The session never got past its own beginning. */
    SNAPSHOT_ABSENT,

    /** Progress exists but could not be decoded. */
    SNAPSHOT_UNREADABLE,

    /** Written by a NEWER build. Rejected rather than coerced — the unknown field is the one that mattered. */
    SCHEMA_UNSUPPORTED,

    /** The seal and the snapshot disagree about which session or which exercise they belong to. */
    IDENTITY_MISMATCH,

    /** The purpose is unrecognised or absent, so nothing can be said about what this run was for. */
    PURPOSE_NOT_SEALED,

    /** This build cannot mint the algorithm the seed was drawn under, so the trial plan is not reproducible. */
    RNG_VERSION_UNSUPPORTED,

    /** The generator or the scorer changed since the seal. The second half would be a different instrument. */
    ENGINE_VERSION_CHANGED,

    /** The snapshot contradicts itself; no legal reduction could have produced it. */
    SNAPSHOT_INCOHERENT,
}

/** The three honest answers: resume it, there is nothing to resume, or here is exactly why we will not. */
sealed interface RestoreOutcome {

    data class Restored(
        val descriptor: FocusSessionDescriptor,
        val snapshot: FocusRuntimeSnapshot,
    ) : RestoreOutcome

    /**
     * The records are sound but the session already ended.
     *
     * Deliberately NOT a rejection: a completed session is a success, and reporting it as a failure would teach
     * every caller to treat a normal outcome as an error path.
     */
    data class NothingToResume(
        val descriptor: FocusSessionDescriptor,
        val snapshot: FocusRuntimeSnapshot,
    ) : RestoreOutcome

    data class Rejected(val reason: RestoreRejection) : RestoreOutcome
}
