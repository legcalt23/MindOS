package com.mindos.app.features.mindboost.logic.domain.model

import kotlinx.serialization.Serializable

/** Stable taxonomy for the 28 logic subskills defined by the Mindboost Logic V4 specification. */
@Serializable
enum class LogicSkill {
    NECESSARY_DEDUCTION,
    INDETERMINACY,
    CONDITIONAL_REASONING,
    NECESSARY_SUFFICIENT,
    QUANTIFIERS,
    CONSISTENCY,
    FALSIFYING_INDUCTION,
    GENERATIVE_RULE,
    RELATIONAL_ANALOGY,
    ABDUCTION,
    COUNTEREXAMPLE,
    PROOF_REFUTATION,
    LOGICAL_REPAIR,
    VERBAL_FORMALIZATION,
    EXTERNAL_REPRESENTATION,
    GRAPHICAL_CAUSALITY,
    BASE_RATES,
    VALUE_OF_INFORMATION,
    CONFIDENCE_CALIBRATION,
    BELIEF_REVISION,
    ARGUMENT_ANATOMY,
    HIDDEN_ASSUMPTION,
    PRECISE_FALLACY,
    ARGUMENT_STRENGTH,
    MULTISTEP_CONSTRAINTS,
    STRATEGIC_FLEXIBILITY,
    FUNCTIONAL_TRANSFER,
    ADVERSARIAL_RESISTANCE,
}

@Serializable
enum class LogicExerciseType {
    DEDUCTION_FROM_PREMISES,
    CONDITIONAL_TEST,
    NECESSARY_SUFFICIENT,
    CONTRADICTION_DETECTION,
    COUNTEREXAMPLE,
}

@Serializable
@Suppress("MagicNumber") // Enum values are the stable difficulty scale itself.
enum class LogicDifficulty(val level: Int) {
    FOUNDATION(1),
    DEVELOPING(2),
    PRACTICED(3),
    ADVANCED(4),
    ELITE(5);

    fun increase(): LogicDifficulty = entries.getOrElse(ordinal + 1) { this }
    fun decrease(): LogicDifficulty = entries.getOrElse(ordinal - 1) { this }
}

@Serializable
enum class SurfaceForm {
    ABSTRACT,
    EVERYDAY,
    SOFTWARE,
    BUSINESS,
    STUDY,
}

@Serializable
enum class TransferLevel {
    TRAINED,
    T1_ISOMORPHIC,
    T2_NEAR,
}

@Serializable
enum class ReasoningRule {
    MODUS_PONENS,
    MODUS_TOLLENS,
    CONDITIONAL_FALSIFICATION,
    NECESSARY_SUFFICIENT_CLASSIFICATION,
    CONSISTENCY_CHECK,
    COUNTEREXAMPLE_CONSTRUCTION,
}

@Serializable
enum class DistractorType {
    AFFIRMING_CONSEQUENT,
    DENYING_ANTECEDENT,
    BELIEF_BIAS,
    OVERINFERENCE,
    SURFACE_MATCH,
    CONFIRMATION_BIAS,
    FALSE_CONTRADICTION,
    MISSING_PREMISE,
}

@Serializable
enum class LogicErrorTag {
    AFFIRMING_CONSEQUENT,
    DENYING_ANTECEDENT,
    BELIEF_BIAS,
    OVERINFERENCE,
    SURFACE_MATCH,
    CONFIRMATION_BIAS,
    FALSE_CONTRADICTION,
    MISSING_PREMISE,
    IMPULSIVE_RESPONSE,
    OVERCONFIDENT_ERROR,
}

@Serializable
@Suppress("MagicNumber") // Probabilities are the calibrated values represented by each enum entry.
enum class LogicConfidence(val probability: Double) {
    LOW(0.35),
    MEDIUM(0.65),
    HIGH(0.9),
}

@Serializable
enum class JustificationClaim {
    NOT_PROVIDED,
    CONCLUSION_FOLLOWS,
    CONCLUSION_DOES_NOT_FOLLOW,
    RULE_IS_REFUTED,
    CONDITIONS_CLASSIFIED,
    SET_IS_CONSISTENT,
    SET_IS_INCONSISTENT,
    COUNTEREXAMPLE_INVALIDATES,
}

@Serializable
data class LogicPremise(
    val id: String,
    val text: String,
)

@Serializable
data class CounterexampleCase(
    val premisesRemainTrue: Boolean,
    val conclusionBecomesFalse: Boolean,
)

@Serializable
data class AnswerOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val distractorType: DistractorType? = null,
    val feedback: String,
    val counterexampleCase: CounterexampleCase? = null,
)

@Serializable
data class LogicItem(
    val id: String,
    val exerciseType: LogicExerciseType,
    val targetSkills: List<LogicSkill>,
    val difficultyLevel: LogicDifficulty,
    val logicalStructureId: String,
    val surfaceVariantId: String,
    val surfaceForm: SurfaceForm,
    val prompt: String,
    val premises: List<LogicPremise>,
    val answerOptions: List<AnswerOption>,
    val correctAnswerId: String,
    val validReasoningRules: List<ReasoningRule>,
    val requiredPremiseIds: List<String>,
    val expectedJustificationClaim: JustificationClaim,
    val explanation: String,
    val commonErrorTags: List<LogicErrorTag>,
    val isIsomorphicProbe: Boolean,
    val transferLevel: TransferLevel,
    val hints: List<String>,
    val minimumReasoningTimeMs: Long = DEFAULT_MIN_REASONING_TIME_MS,
)

@Serializable
data class LogicResponse(
    val selectedAnswerId: String,
    val selectedPremiseIds: List<String>,
    val reasoningRule: ReasoningRule,
    val justificationInput: JustificationClaim,
    val confidence: LogicConfidence,
    val hintsUsed: Int,
    val timeSpentMs: Long,
)

@Serializable
data class ReasoningScore(
    val finalAnswerScore: Double,
    val premiseIdentificationScore: Double,
    val ruleApplicationScore: Double,
    val justificationScore: Double,
    val confidenceCalibrationScore: Double,
    val independenceFromHintsScore: Double,
    val transferConsistencyScore: Double?,
    val impulsivityPenalty: Double,
    val conceptualErrorPenalty: Double,
    val totalIcr: Double,
)

@Serializable
data class LogicAttempt(
    val itemId: String,
    val exerciseType: LogicExerciseType,
    val targetSkills: List<LogicSkill>,
    val difficulty: LogicDifficulty,
    val surfaceForm: SurfaceForm,
    val logicalStructureId: String,
    val selectedAnswer: String,
    val isCorrect: Boolean,
    val premisesSelected: List<String>,
    val reasoningRule: ReasoningRule,
    val justificationInput: JustificationClaim,
    val confidence: LogicConfidence,
    val hintsUsed: Int,
    val timeSpentMs: Long,
    val scoreBreakdown: ReasoningScore,
    val errorTags: List<LogicErrorTag>,
    val transferTag: TransferLevel?,
    val isIsomorphicProbe: Boolean,
    val createdAt: Long,
)

@Serializable
enum class FatigueSignal {
    POSSIBLE_IMPULSIVITY,
}

@Serializable
data class SkillState(
    val skillId: LogicSkill,
    val thetaEstimate: Double = 0.0,
    val uncertainty: Double = 1.0,
    val attemptsCount: Int = 0,
    val recentIcr: Double? = null,
    val recentAccuracy: Double? = null,
    val recentConceptualErrors: List<LogicErrorTag> = emptyList(),
    val recentFatigueSignal: FatigueSignal? = null,
)

/**
 * Which version of the ICR equation produced a score. Bumped whenever a change makes new scores
 * incomparable with previously stored ones, so a consumer can refuse to compare across the boundary
 * instead of reading our own instrument change as a change in the user.
 */
object LogicScoringVersion {
    /**
     * Scores written before the penalty block was parenthesised. The `else` branch absorbed the rest
     * of the sum, so an answer flagged as a conceptual error was credited a tenth of that penalty on
     * its own, and none at all when the answer was also impulsive. Every deviation inflated the score.
     */
    const val LEGACY_PRECEDENCE_BUG = 1

    /** Both penalties additive and at full weight. */
    const val CURRENT = 2
}

@Serializable
data class LogicSessionResult(
    val averageIcr: Double,
    val inferentialAccuracy: Double,
    val averageJustification: Double,
    val averageConfidenceCalibration: Double,
    val transferConsistency: Double?,
    val topErrorTags: List<LogicErrorTag>,
    val recommendation: String,
    /**
     * Defaults to [LogicScoringVersion.LEGACY_PRECEDENCE_BUG] on purpose, in both directions.
     *
     * Fail-closed: a stored session carrying no version is, by definition, older than the version
     * marker itself. Defaulting to CURRENT would let every inflated session declare itself current
     * and leave the guard decorative.
     *
     * Backwards compatible: the default is also what keeps already-written JSON readable. Without it
     * `decodeFromString` would reject every stored aggregate, `readParsed` would quarantine the key,
     * and the user's history would be destroyed by the very change meant to keep it honest.
     */
    val scoringVersion: Int = LogicScoringVersion.LEGACY_PRECEDENCE_BUG,
)

@Serializable
data class LogicSession(
    val id: String,
    val userId: String,
    val startedAt: Long,
    val endedAt: Long,
    val attempts: List<LogicAttempt>,
    val result: LogicSessionResult,
)

@Serializable
data class LogicConfiguration(
    val unlockedExerciseTypes: List<LogicExerciseType> = LogicExerciseType.entries,
    val sessionItemCount: Int = 6,
)

@Serializable
data class LogicFeedback(
    val title: String,
    val diagnostic: String,
    val nextStep: String,
    val errorTag: LogicErrorTag? = null,
    val revealSolution: Boolean = false,
)

private const val DEFAULT_MIN_REASONING_TIME_MS = 2_000L
