package com.mindos.app.features.mindboost.language.domain.adaptation

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import kotlinx.serialization.Serializable

/**
 * Adaptation model (v6 §6.4). The engine selects the next item by an explainable utility, not
 * by streaks or time-in-app (§3.4). These are the lean inputs/outputs of that decision; a
 * `TaskPassport.toCandidate()` mapper (later) projects content into an [ItemCandidate] so the
 * engine stays decoupled from the full passport.
 */

/**
 * A lean projection of a candidate item, carrying only what the utility needs. [repairTargets]
 * are the error patterns this item is designed to repair (boosts repair value); the dose-derived
 * [transferDistance]/[fatigueCost] are normalized 0.0..1.0.
 */
@Serializable
data class ItemCandidate(
    val taskId: TaskId,
    val primaryTrait: TraitCode,
    val bankType: BankType,
    val transferDistance: Double,
    val fatigueCost: Double,
    val priorKnowledgeDependency: DependencyLevel = DependencyLevel.LOW,
    val variantBiasRisk: Boolean = false,
    val repairTargets: Set<ErrorType> = emptySet(),
)

/**
 * Immutable per-task exposure counts (v6 §8.6 rotation, §5.6 repeated-item gate). Pure value
 * carrier: [record] returns a new snapshot; persistence of it is the repository slice.
 */
@Serializable
data class ExposureSnapshot(val counts: Map<TaskId, Int> = emptyMap()) {
    fun timesSeen(taskId: TaskId): Int = counts[taskId] ?: 0

    fun record(taskId: TaskId): ExposureSnapshot =
        ExposureSnapshot(counts + (taskId to timesSeen(taskId) + 1))
}

/**
 * Everything the utility needs that is not part of a candidate: the user's profile (for
 * expected information), the patterns under active repair, the user's goal traits (construct
 * priority), current fatigue and the exposure history.
 */
@Serializable
data class AdaptationContext(
    val profile: UserLanguageProfile,
    val activeRepairTargets: Set<ErrorType> = emptySet(),
    val goalTraits: Set<TraitCode> = emptySet(),
    val fatigue: FatigueState = FatigueState.FRESH,
    val exposure: ExposureSnapshot = ExposureSnapshot(),
)

/** A candidate with its computed utility and a per-term breakdown (for explainability). */
@Serializable
data class ScoredCandidate(
    val candidate: ItemCandidate,
    val utility: Double,
    val components: Map<String, Double>,
)

/**
 * The adaptation outcome: the [selected] item (highest utility, or `null` if no candidates),
 * the full [ranked] list (descending), and a claim-safe [rationale] of why.
 */
@Serializable
data class AdaptationDecision(
    val selected: ItemCandidate?,
    val ranked: List<ScoredCandidate>,
    val rationale: String,
)
