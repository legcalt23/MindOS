package com.mindos.app.features.mindboost.language.domain.repair

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlinx.serialization.Serializable

/**
 * Repair domain model (v6 §6.5 Repair Protocol, §6.6 error taxonomy). A reparable pattern is
 * detected from *recurring* errors; a [RepairPlan] turns it into the standard protocol
 * (Detectar → Nombrar → Contrastar → Microexplicar → Reintentar → Transferir → Mantener).
 */

/** How serious/entrenched a detected pattern is (drives escalation). */
@Serializable
enum class RepairSeverity { LOW, MEDIUM, HIGH }

/** What to do after a repair retry, depending on whether the user improved. */
@Serializable
enum class RepairNextAction {
    RETRY_ISOMORPHIC,
    ESCALATE_MICROLESSON,
    RECONSOLIDATE,
    ADVANCE_TO_TRANSFER,
    SCHEDULE_MAINTENANCE,
}

/**
 * A recurring error pattern detected for a user (v6 §6.6). [frequency] is counted over RAW
 * error observations — never deduplicated — because the decision depends on repetition
 * (project rule: count frequency on raw events, dedup only for reporting).
 */
@Serializable
data class ErrorPattern(
    val errorType: ErrorType,
    val frequency: Int,
    val primaryTrait: TraitCode,
    val severity: RepairSeverity,
) {
    init {
        require(frequency >= 1) { "ErrorPattern frequency must be >= 1 (was $frequency)." }
    }
}

/**
 * The standard repair protocol for one pattern. Every field is mandatory (§6.5): an
 * [explanation] that names the pattern, a [microLesson] rule, an [isomorphicRetry] to re-test
 * the same operation on fresh material, a [verificationCriterion], and the branch actions.
 */
@Serializable
data class RepairPlan(
    val errorType: ErrorType,
    val explanation: String,
    val microLesson: String,
    val isomorphicRetry: String,
    val verificationCriterion: String,
    val nextIfFail: RepairNextAction,
    val nextIfImprove: RepairNextAction,
)

/** Result of running a repair plan (v6 RepairOutcome). */
@Serializable
data class RepairOutcome(
    val errorType: ErrorType,
    val improved: Boolean,
    val nextAction: RepairNextAction,
)
