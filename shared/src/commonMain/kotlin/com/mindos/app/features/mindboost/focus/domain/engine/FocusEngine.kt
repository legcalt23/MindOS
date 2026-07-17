package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.core.session.ReproducibleSessionIdentity
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus

/**
 * What a session is allowed to be used for. Today there is exactly one mode, and it is the honest one: training,
 * and nothing else.
 *
 * The measurement and official-scoring gates are separate and both OFF, because the work they depend on is open:
 * the adaptive surface is not fully guarded (T3), the duration table is E1-calibrated (B1), E2 has no metric
 * policy (B2), and its renderer still carries a size pop-out (B3). A session run under this mode is real
 * training; it is not evidence.
 */
enum class FocusSessionMode {
    TRAINING_ONLY,
}

/** Whether the engine lets difficulty move during/after a session. */
enum class FocusAdaptationPolicy {
    /** Difficulty is pinned for the whole session; no rule may move it. */
    FIXED,
}

/**
 * Build a NEW session. The caller states what it wants trained — never a version, never a seed.
 *
 * Both are deliberately absent. Versions are resolved from [FocusFormContract], so a caller cannot pin a stale
 * one; the seed is minted by the engine's injected `SeedSource`, so a session's identity is created exactly once,
 * at the authority that also persists it. A caller that could supply either could create two sessions with the
 * same identity, or one whose recorded version is not the one that ran.
 */
data class CreateFocusSessionRequest(
    val exerciseId: ExerciseId,
    val difficulty: DifficultyVector,
    val spec: BlockSpec,
    val sessionTemplateVersion: Int,
    val mode: FocusSessionMode,
)

/**
 * Rebuild the form of an EXISTING session from its persisted identity.
 *
 * The identity is the authority here, not the current code: it names the versions that session ran at. If any of
 * them no longer matches what this build can produce, the engine fails closed rather than presenting a DIFFERENT
 * form under the same session id.
 *
 * There is deliberately NO `spec` parameter. An earlier version took one, and the caller could — and in the test,
 * did — pass a different trial count than the session ran with, silently producing a different form under the
 * same identity. The trial count is part of the identity; the engine derives the spec from it. If a caller can
 * vary an input that changes the form, the identity does not determine the form.
 */
data class RestoreFocusSessionRequest(
    val identity: ReproducibleSessionIdentity,
    val mode: FocusSessionMode,
)

/**
 * A built session: the form, the versions that produced it, and the identity that reproduces it.
 *
 * FORM_REPRODUCTION only (`ENG_CORE_AND_FOCUS_BLUEPRINT_V3.md` §7, the operative acceptance criterion). The
 * identity reproduces this potential stimulus sequence. It is NOT a session replay, which additionally needs the
 * trials actually presented, the responses, the lifecycle and relative monotonic timings. The seed alone
 * reproduces nothing.
 */
data class FocusSessionPlan(
    val exerciseId: ExerciseId,
    val identity: ReproducibleSessionIdentity,
    val stimuli: List<Stimulus>,
    val mode: FocusSessionMode,
    val adaptation: FocusAdaptationPolicy,
    val measurementEligible: Boolean,
    val officialScoringEligible: Boolean,
) {
    val generatorVersion: Int get() = identity.descriptor.generatorVersion
}

/**
 * The outcome of rebuilding a persisted session's form.
 *
 * Typed rather than thrown. A persisted identity is untrusted input — it can be corrupt, or written by a build
 * whose versions no longer exist — so "restore failed, present nothing" is an ordinary outcome of reading it, not
 * an exceptional one.
 *
 * Every rejection is a `data object` carrying no payload. That is deliberate: none of them can leak the seed, the
 * digests, the resolved difficulty or any other value from the row into whatever ends up logging the failure. A
 * corrupt row is not a reason to start emitting reproducibility material.
 */
sealed interface FocusRestoreResult {

    /** The form was rebuilt, and both the input digest and the output digest matched what was persisted. */
    data class Restored(val plan: FocusSessionPlan) : FocusRestoreResult

    /** Nothing is presented and nothing becomes eligible. The session stays exactly as it was recorded. */
    sealed interface Rejected : FocusRestoreResult {

        /** The identity schema is newer than this build understands. */
        data object UnsupportedSchema : Rejected

        /** The row names an exercise this build does not have. */
        data object UnknownExercise : Rejected

        /** The row names an exercise that is not (or no longer) wired to the engine. */
        data object UnsupportedHistoricalImplementation : Rejected

        /** The row names an RNG algorithm this build cannot reproduce. */
        data object UnsupportedRng : Rejected

        /** The row's generator version is not the one this build authorizes. */
        data object UnsupportedGeneratorVersion : Rejected

        /** Some other version (engine/exercise/mapping/response/content) drifted from what the row ran at. */
        data object VersionDrift : Rejected

        /** The row's fields do not satisfy the descriptor's own invariants. */
        data object InvalidDescriptor : Rejected

        /** The inputs rebuilt to a different definition digest than the row claims. */
        data object DefinitionDigestMismatch : Rejected

        /**
         * The inputs matched, but the regenerated stimulus sequence did not.
         *
         * This is the one that catches a generator whose output changed without its version being bumped — the
         * failure every version check is structurally blind to, because in exactly that case the inputs are
         * identical and so every input-derived digest agrees.
         */
        data object PlanDigestMismatch : Rejected
    }
}

/**
 * The single authority that turns a session request into a runnable form for MindBoost · Enfoque.
 *
 * It exists so the invariants live in ONE place a caller cannot step around: versions come from
 * [FocusFormContract], the seed is minted here (never in the UI), the identity is built here, and what a session
 * may be used for is decided by its mode rather than by whoever reads the result later.
 */
interface FocusEngine {
    /** Mints a seed, resolves versions, generates the form, and digests both the inputs and the output. */
    fun createSession(request: CreateFocusSessionRequest): FocusSessionPlan

    /**
     * Rebuilds an existing session's form from its identity. Fails closed on ANY drift: a restored session must
     * present what it originally presented, or not be restored at all.
     */
    fun restoreSession(request: RestoreFocusSessionRequest): FocusRestoreResult
}
