@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.coach.domain

/**
 * MC8u-F — the app-wide AI Coach contract, reusable by all 8 MindBoost blocks. Each block contributes a PII-FREE,
 * structured [CoachContribution] (coarse training patterns + calibration only — never raw trials, self-report values,
 * free text, email/name, or any clinical/medical datum). The coach aggregates contributions into a [CoachSummary],
 * which is the ONLY thing that ever reaches a prompt/model. Mirrors the per-module profile contract
 * (`CognitiveModuleProfileSource`) so the same discipline applies to coaching.
 */
enum class CoachCalibrationState { CALIBRATING, PROVISIONAL, ESTABLISHED }

enum class CoachConfidenceLevel { LOW, MEDIUM, HIGH }

enum class CoachRecommendationType { PRACTICE_MORE, INCREASE_DIFFICULTY, DIVERSIFY, REST }

/** A coarse, non-clinical training recommendation. `reason` is a short training rationale, never a diagnosis/claim. */
data class CoachRecommendation(
    val type: CoachRecommendationType,
    val reason: String,
    val targetBlockId: String,
    val targetExerciseId: String? = null,
    val durationMinutes: Int? = null,
)

/**
 * One block's PII-free contribution to the coach. Everything is a COARSE label or count derived from calibration/
 * training patterns. `strengths`/`weaknesses` describe TASK PERFORMANCE patterns ("consistent on category sorting"),
 * never person-level traits or emotional/clinical readings. `cautions` carry the block's interpretation limits.
 */
data class CoachContribution(
    val blockId: String,
    val calibrationState: CoachCalibrationState,
    val confidence: CoachConfidenceLevel,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val patterns: List<String> = emptyList(),
    val recommendations: List<CoachRecommendation> = emptyList(),
    val cautions: List<String> = emptyList(),
    val schemaVersion: Int = SCHEMA_VERSION,
    val freshnessMillis: Long? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/** Each block implements this to feed the coach. Returns null when the block has no usable (calibrated-enough) data. */
interface CoachContributionSource {
    val blockId: String
    suspend fun contribute(userId: String): CoachContribution?
}

/** The aggregated, PII-free view the coach reasons over and persists as memory. */
data class CoachSummary(
    val contributions: List<CoachContribution>,
    val overallConfidence: CoachConfidenceLevel,
    val schemaVersion: Int = CoachContribution.SCHEMA_VERSION,
) {
    val isEmpty: Boolean get() = contributions.isEmpty()
}

/**
 * A coach-proposed plan change. It is a SUGGESTION only — applying it REQUIRES explicit user confirmation (the coach
 * never mutates the plan directly). The real plan applier is out of scope here (plan lives in the profile/planner,
 * adjacent to foreign-WIP), so [PlanApplier] is a boundary with a pending default + a release guard.
 */
data class CoachPlanSuggestion(
    val recommendation: CoachRecommendation,
    val explanation: String,
)

/** Result of a confirmed plan apply. */
enum class PlanApplyOutcome { APPLIED, REJECTED, PENDING_NOT_WIRED }

/** Boundary the real adaptive planner implements. Default is a no-op that reports the wiring is pending. */
interface PlanApplier {
    /** Applies [suggestion] ONLY when [confirmed] is true; a coach can never mutate the plan without confirmation. */
    suspend fun apply(userId: String, suggestion: CoachPlanSuggestion, confirmed: Boolean): PlanApplyOutcome
}

/** Charter default: no real planner wired → applying is impossible (never silently "applied"). */
object PendingPlanApplier : PlanApplier {
    override suspend fun apply(userId: String, suggestion: CoachPlanSuggestion, confirmed: Boolean): PlanApplyOutcome =
        PlanApplyOutcome.PENDING_NOT_WIRED
}

/** MC8u-F release guard: a real plan applier must be wired before "recommendation_applied" can be claimed at release. */
object PlanApplierPolicy {
    fun requireRealApplierForApply(applier: PlanApplier, isReleaseBuild: Boolean) {
        require(!(isReleaseBuild && applier === PendingPlanApplier)) {
            "AI Coach: applying a recommendation to the plan requires a real PlanApplier in a release build."
        }
    }
}
