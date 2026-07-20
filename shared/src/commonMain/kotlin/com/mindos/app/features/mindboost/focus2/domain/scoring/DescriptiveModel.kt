package com.mindos.app.features.mindboost.focus2.domain.scoring

/**
 * Focus V5 · S3 · tarea (1) + (3) — the descriptive measurement vocabulary of the Focus v2 tracers.
 *
 * PROMOTED VERBATIM from the S2 simulation (`focus2.simulation.CandidateDescriptiveScorer`), which the plan
 * required to be exercised against adversarial profiles BEFORE any engine was committed: "si A1-A6 no pasan EN
 * SIMULACION, la formula candidata se corrige AQUI, no en S3". A1-A6 passed there, so this is the promotion S3
 * owes, and the S2 oracle now runs against THIS code rather than a private copy — a second implementation of a
 * ratified formula is a drift waiting to happen, and the copy that drifts is always the one nobody re-reads.
 *
 * What lives here is a VOCABULARY, not a score. There is deliberately no scalar capability number anywhere in
 * this package: `ADR-FOCUS-SCORING` is `BLOCKED_BY_DESIGN` and nothing here may anticipate it.
 */

import kotlinx.serialization.Serializable

/** Which experimental condition a trial belonged to. */
enum class ConditionId { CONGRUENT, INCONGRUENT, NEUTRAL, VIGILANCE }

/** What the person did. NONE means the response window closed with nothing. */
enum class Response { LEFT, RIGHT, TAP, NONE }

/**
 * Latency, kept in a SEPARATE type on purpose.
 *
 * This is the structural half of ratified cross-cutting rule 2 ("RT secundario en C0") and of assertion A5.
 * The primary path never destructures this beyond comparing against the anticipation floor, and because the
 * timing lives behind its own type rather than as fields on [TrialRecord], a future edit that starts feeding
 * latency into d' has to go out of its way to do it. A convention can be forgotten; a type has to be fought.
 */
data class SecondaryTiming(val rtMs: Long, val windowMs: Long)

/** One trial. Carries no aggregate, no RT_CV, no lapse count, and no `MetricSet`. */
data class TrialRecord(
    val index: Int,
    val conditionId: ConditionId,
    val isSignal: Boolean,
    val response: Response,
    val timing: SecondaryTiming?,
)

/** What makes one task's classification differ from another's. */
data class TaskSpec(
    val taskId: String,
    val conditions: List<ConditionId>,
    /** The response that means "yes, signal". LEFT for the flanker (by convention), TAP for vigilance. */
    val yesResponse: Response,
    /** The response that means "no, noise". RIGHT for the flanker; NONE for go-only vigilance. */
    val noResponse: Response,
    /** Null where the paradigm has no well-defined guessing probability. Never fabricate one. */
    val chanceLevel: Double?,
    /** The condition whose d' the dossier names as the exercise's primary statistic. */
    val primaryCondition: ConditionId,
    val anticipationFloorMs: Long = DEFAULT_ANTICIPATION_FLOOR_MS,
) {
    /**
     * The classification rule, and the ONLY one in the engine.
     *
     * It lives on the spec because the spec is what DEFINES it: which response means yes, which means no, and
     * below what latency a response cannot have evaluated anything. It takes loose facts rather than a
     * [TrialRecord] because the runtime classifies a trial the moment it resolves and then keeps only the
     * counts — `ADR-FOCUS-DATA` option (i) forbids persisting the trial itself. Two implementations of this
     * rule, one for scoring and one for the runtime, would be a formula with two versions, and the one that
     * drifts is always the one nobody re-reads.
     *
     * [rtMs] is the only place latency touches the primary path, and it is a FLOOR, not a measure: a response
     * faster than any possible perception did not evaluate the stimulus. `null` means "no latency available"
     * and can never produce an anticipation — a missing measurement must not be able to manufacture a
     * behavioural fact about the person.
     */
    fun outcomeOf(isSignal: Boolean, response: Response, rtMs: Long?): TrialOutcome = when {
        rtMs != null && rtMs < anticipationFloorMs -> TrialOutcome.ANTICIPATION
        isSignal && response == yesResponse -> TrialOutcome.HIT
        isSignal && response == noResponse -> TrialOutcome.MISS
        !isSignal && response == yesResponse -> TrialOutcome.FALSE_ALARM
        !isSignal && response == noResponse -> TrialOutcome.CORRECT_REJECTION
        // Its own event. Never a miss, never a false alarm: an absent response in a task that REQUIRES one is
        // a different fact from choosing wrongly.
        else -> TrialOutcome.OMISSION
    }

    companion object {
        const val DEFAULT_ANTICIPATION_FLOOR_MS: Long = 150L
    }
}

/**
 * A quantity, or the reason there isn't one.
 *
 * Deliberately not `MetricValue`: that type's `value` is a non-nullable `Double` and its `unavailable(...)`
 * helper encodes absence as `value = 0.0` — the very sentinel the charter forbids — while
 * `MetricSet.rawValueOf` reads `value` and ignores `confident`, so any consumer would read an unavailable
 * d' as a real zero. `MetricKey` also has no per-condition variants and `MetricSet` is a flat map, so it
 * structurally cannot hold per-condition SDT even if the sentinel were fixed.
 */
sealed interface Descriptive<out T> {
    data class Present<T>(val value: T, val n: Int) : Descriptive<T>
    data class Absent(val reason: AbsenceReason) : Descriptive<Nothing>
}

enum class AbsenceReason {
    INSUFFICIENT_N,
    AT_OR_BELOW_CHANCE,
    NO_SIGNAL_TRIALS,
    NO_NOISE_TRIALS,
    NOT_APPLICABLE,
}

/**
 * What one trial turned out to be. The six mutually exclusive outcomes of the 2x2 plus its two escapes.
 *
 * It exists as a named type because the runtime classifies a trial the MOMENT it resolves and then keeps only
 * the running counts — ADR-FOCUS-DATA option (i) forbids persisting the trial itself. Without this type the
 * runtime would have to re-implement the classification rule, and a formula with two implementations has two
 * versions the day one of them is edited.
 */
enum class TrialOutcome { HIT, MISS, FALSE_ALARM, CORRECT_REJECTION, OMISSION, ANTICIPATION }

/**
 * Raw counts, never collapsed. Everything downstream is derivable from these, and is derived from them.
 *
 * This is also the unit of PERSISTENCE for a running session: it is a summary, so a session can survive
 * process death without any per-trial record ever reaching disk. `signalTrials` and `noiseTrials` are derived
 * rather than stored, so they cannot disagree with the counts they summarise.
 */
@Serializable
data class SdtCell(
    val hits: Int = 0,
    val misses: Int = 0,
    val falseAlarms: Int = 0,
    val correctRejections: Int = 0,
    val omissions: Int = 0,
    val anticipations: Int = 0,
) {
    val signalTrials: Int get() = hits + misses
    val noiseTrials: Int get() = falseAlarms + correctRejections

    /** Every count this cell holds, including the two that sit outside the 2x2. */
    val totalTrials: Int get() = signalTrials + noiseTrials + omissions + anticipations

    /** Accumulates one classified trial. The runtime's only mutation. */
    operator fun plus(outcome: TrialOutcome): SdtCell = when (outcome) {
        TrialOutcome.HIT -> copy(hits = hits + 1)
        TrialOutcome.MISS -> copy(misses = misses + 1)
        TrialOutcome.FALSE_ALARM -> copy(falseAlarms = falseAlarms + 1)
        TrialOutcome.CORRECT_REJECTION -> copy(correctRejections = correctRejections + 1)
        TrialOutcome.OMISSION -> copy(omissions = omissions + 1)
        TrialOutcome.ANTICIPATION -> copy(anticipations = anticipations + 1)
    }

    companion object {
        val EMPTY = SdtCell()
    }
}

data class ConditionResult(
    val conditionId: ConditionId,
    val cell: SdtCell,
    val accuracyRaw: Descriptive<Double>,
    val accuracyChanceCorrected: Descriptive<Double>,
    val dPrime: Descriptive<Double>,
    val criterion: Descriptive<Double>,
    /** Never optional. A d' without its uncertainty is a number pretending to be a finding. */
    val dPrimeStandardError: Descriptive<Double>,
)

enum class InvalidationReason {
    /** No condition's 95% interval cleared zero: nothing was told apart from anything. */
    NO_DISCRIMINATION,

    /**
     * Exactly one distinct response across every scorable trial.
     *
     * Note what this is not: it is not "we caught you cheating". A person tapping everything and a person
     * who misunderstood the instructions produce the identical record, and this verdict says the same true
     * thing about both — the session cannot support a measurement. Telling those two apart is the
     * anti-gaming layer's job, and that layer is not wired.
     */
    DEGENERATE_RESPONDING,
}

sealed interface Verdict {
    data object Valid : Verdict
    data object InsufficientData : Verdict
    data class Invalidated(val reason: InvalidationReason) : Verdict
}

/**
 * The session's descriptive result. Note what is absent: any scalar capability score.
 *
 * [primaryStatistic] is a within-condition LEVEL (a d'), never a between-condition subtraction — the
 * ratified rule 1 retires difference scores as governing metrics. It exists so ordering assertions have a
 * comparable quantity; it is not a subscore and C0 shows nothing.
 */
data class SessionDescriptive(
    val perCondition: Map<ConditionId, ConditionResult>,
    val verdict: Verdict,
    val primaryStatistic: Descriptive<Double>,
)
