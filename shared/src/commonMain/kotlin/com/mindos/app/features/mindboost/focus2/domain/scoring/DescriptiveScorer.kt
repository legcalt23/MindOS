package com.mindos.app.features.mindboost.focus2.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.Probit
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import kotlin.math.sqrt

/**
 * Focus V5 · S3 · tarea (3) — the DESCRIPTIVE scorer of the Focus v2 tracers, promoted from the S2 simulation.
 *
 * S2 ran this formula against five adversarial profiles before any engine existed, which is the only reason it
 * is allowed to exist in `commonMain` now. Two defects were found and fixed THERE rather than here, and both
 * are load-bearing enough that removing either would silently restore a false-progress path:
 *
 *  - `MB-FOCUS-0015`: the loglinear correction biases d' when the signal and noise cells differ in size, so a
 *    pure yes-sayer on a 24/96 vigilance stream lands at d' ~ -0.51, not 0. Any near-zero band on d' would have
 *    called that session VALID. The degenerate-responding rule below derives the verdict from RESPONSE COUNTS
 *    instead, which no correction can bias.
 *  - `MB-FOCUS-0016`: point estimates carried no uncertainty, so a deadline-rider choosing sides by coin flip
 *    produced d' = 0.50 and was called VALID when the standard error was ~0.37. The verdict now requires the
 *    lower bound of the 95% interval to clear zero.
 *
 * What it computes, per ratified cross-cutting rule 1: accuracy, d' and criterion **PER CONDITION**, with
 * chance correction. What it must never compute: a scalar capability score.
 *
 * The v1 defect it replaces is specific: accuracy as a raw proportion with no chance floor
 * (`DefaultMetricsEngine.kt:43-57`), which hands a coin-flipper the same 0.50 it hands someone who genuinely
 * discriminates half the time.
 */
object DescriptiveScorer {

    /**
     * The version of the SCORING RULE, sealed into every session before its first trial.
     *
     * It is not decoration: a session interrupted under one scoring rule and resumed under another would have
     * its two halves measured by different instruments, so `FocusSessionRestore` refuses that resume. Bump this
     * whenever a change would alter the numbers a stored session produces.
     */
    const val SCORER_VERSION: Int = 1

    /**
     * Per-cell floor, read from the repo's central authority — NOT re-declared.
     *
     * The S2 candidate duplicated this as a literal `20` because a simulation importing a production gate could
     * be mistaken for having wired one. That reason expires on promotion: this IS the production gate's caller,
     * so it asks the gate. A duplicated floor is a floor that can drift away from the authority that owns it.
     */
    val minNPerCell: Int = MinSampleGate.specFor(ScoreMetricId.DPRIME).nMinPerCell

    /**
     * Scores one session. Pure: no clock, no store, no flag.
     *
     * [trials] carries no aggregate. Every count below is derived here from the stream, which is the whole
     * point of the S2 rework — hand-written aggregates were the defect.
     */
    fun score(trials: List<TrialRecord>, spec: TaskSpec): SessionDescriptive =
        scoreCells(spec.conditions.associateWith { cellOf(trials.filter { t -> t.conditionId == it }, spec) }, spec)

    /**
     * Scores from per-condition COUNTS rather than from a trial stream.
     *
     * This is the entry point the runtime uses, and it exists because of ADR-FOCUS-DATA option (i): the engine
     * may not persist raw per-trial data, so a session that survives process death carries its tallies, not
     * its trials. [score] delegates here, so both paths compute identical numbers by construction — the S2
     * oracle keeps exercising the trial-stream signature it ratified, and the runtime cannot drift away from
     * it without turning that oracle red.
     */
    fun scoreCells(cells: Map<ConditionId, SdtCell>, spec: TaskSpec): SessionDescriptive {
        val perCondition = spec.conditions.associateWith { condition ->
            scoreCell(cells[condition] ?: SdtCell.EMPTY, condition, spec)
        }
        val primary = perCondition[spec.primaryCondition]?.dPrime
            ?: Descriptive.Absent(AbsenceReason.NO_SIGNAL_TRIALS)
        return SessionDescriptive(
            perCondition = perCondition,
            verdict = verdictOf(perCondition, cells),
            primaryStatistic = primary,
        )
    }

    private fun scoreCell(
        cell: SdtCell,
        condition: ConditionId,
        spec: TaskSpec,
    ): ConditionResult {
        return ConditionResult(
            conditionId = condition,
            cell = cell,
            accuracyRaw = AccuracyMeasures.raw(cell, minNPerCell),
            accuracyChanceCorrected = AccuracyMeasures.chanceCorrected(cell, spec.chanceLevel, minNPerCell),
            dPrime = sdt(cell) { h, f -> Probit.zScore(h) - Probit.zScore(f) },
            criterion = sdt(cell) { h, f -> -HALF * (Probit.zScore(h) + Probit.zScore(f)) },
            dPrimeStandardError = sdt(cell) { h, f -> dPrimeSe(h, f, cell) },
        )
    }

    /**
     * Standard error of d', so a point estimate is never mistaken for a demonstrated difference.
     *
     * `var(d') = H(1-H)/(Ns*phi(zH)^2) + F(1-F)/(Nn*phi(zF)^2)` — the standard result, phi being the normal
     * density. `MinSampleGate.specFor(DPRIME).requiresConfidenceInterval` is already `true` in this repo: the
     * central gate demanded an interval and the first draft of this scorer did not produce one.
     */
    private fun dPrimeSe(h: Double, f: Double, cell: SdtCell): Double {
        val phiH = Probit.density(Probit.zScore(h))
        val phiF = Probit.density(Probit.zScore(f))
        val varianceH = h * (1.0 - h) / (cell.signalTrials * phiH * phiH)
        val varianceF = f * (1.0 - f) / (cell.noiseTrials * phiF * phiF)
        return sqrt(varianceH + varianceF)
    }

    /**
     * Did this condition demonstrate discrimination, as opposed to landing above zero by chance?
     *
     * The lower bound of the 95% interval must clear zero. A point estimate on its own cannot answer this,
     * which is the whole reason the standard error exists above.
     */
    fun discriminated(result: ConditionResult): Boolean {
        val d = result.dPrime as? Descriptive.Present
        val se = result.dPrimeStandardError as? Descriptive.Present
        return d != null && se != null && d.value - Z_95 * se.value > 0.0
    }

    /**
     * Classifies one condition's trials into the 2x2 plus its two escape categories.
     *
     * The classification is driven entirely by [TaskSpec.yesResponse] / [TaskSpec.noResponse], which is what
     * lets ONE function serve both tracers honestly. For the flanker, "no" is RIGHT, so a missing response
     * falls through to OMISSION. For vigilance, "no" IS the absence of a response, so silence on a
     * non-target is a correct rejection — as it must be, because in that paradigm not responding is the norm.
     * Collapsing those two would either invent omissions in vigilance or swallow them in the flanker.
     */
    private fun cellOf(trials: List<TrialRecord>, spec: TaskSpec): SdtCell =
        trials.fold(SdtCell.EMPTY) { cell, trial ->
            cell + spec.outcomeOf(trial.isSignal, trial.response, trial.timing?.rtMs)
        }

    /**
     * d' and criterion share their guards and their correction, so they share this.
     *
     * **Hautus (1995) loglinear correction, applied ALWAYS — to every cell, extreme or not.**
     *
     * Rates of exactly 0 or 1 send z to infinity, so some correction is unavoidable. Two rivals exist:
     * replacing only the extreme rates with `1/(2N)` and `1 - 1/(2N)`, and dropping extreme cells outright.
     * Both are CONDITIONAL — they fire only where the extreme occurs — which makes the estimator
     * discontinuous and biases d' upward precisely for the highest performers, the people whose cells hit
     * ceiling. The loglinear correction adds 0.5 to every count and 1 to every total unconditionally, so it
     * is one smooth estimator with no branch. It shrinks d' toward zero slightly for everyone, uniformly: a
     * known, disclosed, direction-consistent bias, which is what an honesty charter should prefer over a
     * bias that appears only for the best performers and flatters them.
     *
     * Because it is unconditional, d' is ALWAYS finite. There is no non-finite path to fall through, which
     * closes the fabricated-zero trap at its source — `DefaultMetricsEngine.kt:147` silently maps non-finite
     * to 0.0, and nothing here routes through that.
     */
    private fun sdt(cell: SdtCell, compute: (h: Double, f: Double) -> Double): Descriptive<Double> {
        val absence = insufficiency(cell)
        if (absence != null) return Descriptive.Absent(absence)
        val h = (cell.hits + LOGLINEAR_COUNT) / (cell.signalTrials + LOGLINEAR_TOTAL)
        val f = (cell.falseAlarms + LOGLINEAR_COUNT) / (cell.noiseTrials + LOGLINEAR_TOTAL)
        val value = compute(h, f)
        // If this ever fires it is a bug in the correction, not a data condition. Data cannot reach here.
        check(value.isFinite()) { "non-finite SDT statistic from h=$h f=$f — the correction failed" }
        return Descriptive.Present(value, n = cell.signalTrials + cell.noiseTrials)
    }

    private fun insufficiency(cell: SdtCell): AbsenceReason? = when {
        cell.signalTrials == 0 -> AbsenceReason.NO_SIGNAL_TRIALS
        cell.noiseTrials == 0 -> AbsenceReason.NO_NOISE_TRIALS
        cell.signalTrials < minNPerCell || cell.noiseTrials < minNPerCell -> AbsenceReason.INSUFFICIENT_N
        else -> null
    }



    /**
     * Everything here is derived from RESPONSE COUNTS ONLY.
     *
     * Deliberately NOT from latency uniformity, however tempting: a key-masher's near-constant RT is the
     * most obvious signature available, and reading it would violate the ratified rule that RT is secondary.
     * Assertion A5 pins that it is not read.
     */
    private fun verdictOf(
        perCondition: Map<ConditionId, ConditionResult>,
        cells: Map<ConditionId, SdtCell>,
    ): Verdict {
        // A person who emitted exactly ONE distinct response across every scorable trial discriminated
        // nothing, by construction — there was no second answer available for them to discriminate WITH.
        // See MB-FOCUS-0015 in the KDoc above for why a band on the magnitude of d' cannot replace this.
        //
        // Derived from COUNTS, not from a trial list: a "yes" is a hit or a false alarm, a "no" is a miss or a
        // correct rejection, and exactly one distinct response means one of those totals is zero while the
        // other is not. Identical answer, no raw stream required.
        val saidYes = cells.values.sumOf { it.hits + it.falseAlarms }
        val saidNo = cells.values.sumOf { it.misses + it.correctRejections }
        val scorable = saidYes + saidNo
        // An ORDERED ladder, evaluated top to bottom. The order is the semantics: "we could not measure" must
        // be reported before "the measurement says nothing", or an under-sampled session would be labelled as
        // a failure to discriminate — a verdict about the person rather than about the data.
        return when {
            perCondition.values.any { it.dPrime is Descriptive.Absent } -> Verdict.InsufficientData

            scorable >= minNPerCell && (saidYes == 0 || saidNo == 0) ->
                Verdict.Invalidated(InvalidationReason.DEGENERATE_RESPONDING)

            // The interval, not the point. A condition counts only if the lower bound of its 95% interval
            // clears zero; otherwise the estimate is indistinguishable from no discrimination at all, however
            // comfortably positive the point value looks.
            perCondition.values.none { discriminated(it) } ->
                Verdict.Invalidated(InvalidationReason.NO_DISCRIMINATION)

            else -> Verdict.Valid
        }
    }

    private const val HALF = 0.5
    private const val LOGLINEAR_COUNT = 0.5
    private const val LOGLINEAR_TOTAL = 1.0

    /** Two-sided 95%. Pinned as a literal rather than derived, so the interval's width is auditable. */
    private const val Z_95 = 1.959963984540054
}

/**
 * Accuracy, split out of [DescriptiveScorer] so that object stays within its complexity budget.
 *
 * Extracted rather than suppressed: these two are a coherent pair (a raw proportion and the same proportion
 * rescaled against chance) and they read better together than interleaved with the signal-detection maths.
 */
private object AccuracyMeasures {

    fun raw(cell: SdtCell, minNPerCell: Int): Descriptive<Double> {
        val scorable = cell.signalTrials + cell.noiseTrials
        if (scorable < minNPerCell) return Descriptive.Absent(AbsenceReason.INSUFFICIENT_N)
        return Descriptive.Present((cell.hits + cell.correctRejections).toDouble() / scorable, scorable)
    }

    /**
     * The direct repair of the v1 defect.
     *
     * `corrected = (pRaw - pChance) / (1 - pChance)` rescales so that chance maps to 0 and perfect maps to 1.
     * At or below chance the quantity is not a small positive number, it is NOT A MEASUREMENT — so it
     * returns a typed absence and NEVER a clamped 0.0. A clamp would be the same lie in the other
     * direction: it would report "zero ability measured" where the truth is "nothing was measured".
     *
     * [chanceLevel] of null means the paradigm has no well-defined guessing probability — a go-only
     * vigilance task being the case in point, where the probability of a correct "no" depends on how
     * willing the person is to respond at all, which is what criterion measures. Fabricating a chance level
     * there would invent a denominator. The safe sentinel is the one that structurally disables the branch.
     */
    fun chanceCorrected(cell: SdtCell, chanceLevel: Double?, minNPerCell: Int): Descriptive<Double> {
        if (chanceLevel == null) return Descriptive.Absent(AbsenceReason.NOT_APPLICABLE)
        val raw = raw(cell, minNPerCell)
        return when {
            raw !is Descriptive.Present<Double> -> raw
            raw.value <= chanceLevel -> Descriptive.Absent(AbsenceReason.AT_OR_BELOW_CHANCE)
            else -> Descriptive.Present((raw.value - chanceLevel) / (1.0 - chanceLevel), raw.n)
        }
    }
}
