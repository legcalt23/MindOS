package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 §18 C4 · Whether an exercise's measured signal needs a frame-locked timebase to be trustworthy —
 * an evidence-package-local, first-class enum with an EXPLICIT [UNKNOWN] member (never a nullable absence). It is
 * conceptually the SAME three-way distinction as
 * [com.mindos.app.features.mindboost.core.scoring.TimingCriticality] (`RT_CRITICAL`/`RT_SECONDARY`/
 * `RT_IRRELEVANT`, that type's "not declared" case modeled by nullable absence) — that type is core's scoring
 * authority and is deliberately NOT touched here; [RT_NON_CRITICAL] is this contract's own name for the
 * `RT_SECONDARY`/`RT_IRRELEVANT` idea ("this exercise's primary signal does not depend on RT"), collapsed to one
 * value because THIS contract only needs a binary "does the timebase matter" split once an exercise is actually
 * declared — the finer RT_SECONDARY/RT_IRRELEVANT distinction belongs to core.scoring's own gates.
 */
enum class RtCriticality { RT_CRITICAL, RT_NON_CRITICAL, UNKNOWN }

/** One dossier-declared (exerciseId, exerciseVersion) → [RtCriticality] entry. A List of small records, never a
 *  keyed/dictionary collection — the registry has at most a handful of entries, so a linear lookup is simplest
 *  and keeps this file free of an associative-collection type entirely. */
data class RtCriticalityDeclaration(
    val exerciseId: String,
    val exerciseVersion: String,
    val criticality: RtCriticality,
)

/**
 * SYSTEM-1 6R3 §18 C4 · The SINGLE authority for an exercise's [RtCriticality], keyed by the EXACT
 * (exerciseId, exerciseVersion) pair — a fail-closed lookup: any pair with no matching [declared] entry resolves
 * to [fallback], which defaults to [RtCriticality.UNKNOWN] (production posture). A CLASS (not an `object`) so a
 * test can inject its OWN [declared]/[fallback] — the seam this contract needs to exercise the
 * [RtCriticality.RT_NON_CRITICAL] waiver path and the [RtCriticality.RT_CRITICAL] timebase-gated path end to end,
 * not merely as a standalone pure-function unit test — while every real call site still gets the fail-closed
 * production registry via the constructor defaults.
 *
 * TODAY only ONE exercise/version pair is registered: speed E8 (Inhibición Rápida), because it is the ONLY speed
 * exercise that constructs a [com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun] at
 * all — E1/E2/E3/E4/E6/E7 have no wired run-emission path today, and E5 (though it declares its own content
 * version, `VisualScanRaceGenerator.CONFIG_VERSION`) likewise emits no run — so there is no REAL, verified
 * exerciseVersion this registry could honestly cite for them without fabricating one (forbidden: "no inventes
 * ids"). The real, sourced pair registered here:
 * - exerciseId `"E8"` = `com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId.E8_INHIBICION.code`
 *   (`speed/domain/SpeedEnums.kt:102`).
 * - exerciseVersion `"1.0.0-mc9d"` =
 *   `com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoVelocityGenerator.CONFIG_VERSION`
 *   (`speed/domain/exercise/GoNoGoVelocityGenerator.kt:133`).
 * - Both are threaded into the actual [com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun]
 *   at the ONE live call site, `speed/presentation/e8/GoNoGoSessionStateHolder.kt:326-327`
 *   (`exerciseId = SpeedExerciseId.E8_INHIBICION.code`, `exerciseVersion = GoNoGoVelocityGenerator.CONFIG_VERSION`).
 *
 * When a future E1–E7 dossier declares its own `measurement.timingContract` and that exercise starts constructing
 * a real run, add its (exerciseId, exerciseVersion, criticality) entry here AND update the catalog-pin test in
 * the SAME change — a psychometrician-approved, deliberate act, never a silent default.
 */
class RtCriticalityRegistry(
    private val declared: List<RtCriticalityDeclaration> = PRODUCTION_DECLARED,
    private val fallback: RtCriticality = RtCriticality.UNKNOWN,
) {

    /** The declared [RtCriticality] for one (exerciseId, exerciseVersion), or [fallback] if undeclared. */
    fun criticalityOf(exerciseId: String, exerciseVersion: String): RtCriticality =
        declared.firstOrNull { it.exerciseId == exerciseId && it.exerciseVersion == exerciseVersion }
            ?.criticality
            ?: fallback

    companion object {
        /** Bump when a dossier entry is added, removed, or its declared criticality changes. */
        const val VERSION: String = "rt-criticality-registry-v2"

        private const val SPEED_E8_EXERCISE_ID: String = "E8"
        private const val SPEED_E8_EXERCISE_VERSION: String = "1.0.0-mc9d"

        /** The production catalog pin: exactly one real, sourced entry today (see the class KDoc). */
        val PRODUCTION_DECLARED: List<RtCriticalityDeclaration> = listOf(
            RtCriticalityDeclaration(SPEED_E8_EXERCISE_ID, SPEED_E8_EXERCISE_VERSION, RtCriticality.RT_CRITICAL),
        )
    }
}
