@file:Suppress("MagicNumber", "MaxLineLength") // Tuning constants + dense metric-builder call sites.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.SsrtCalculator
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import kotlin.math.abs

/**
 * Default metrics engine for the three v1 paradigms (dispatched by payload). Implements the design's
 * honesty rules: commission over the no-go denominator; RT central tendency on MAD-cleaned data while
 * extreme events (anticipations) count on raw; the conditional post-error-recovery composite (slowing
 * penalized ONLY when accuracy did not recover; post-error impulsivity penalized); chance-adjusted
 * separation; and `confident = n >= MIN_N` everywhere with `unavailable` instead of NaN.
 */
// One private function per derived metric, which is what keeps each honesty rule readable and separately
// testable. Ola 3 · R1 and R2 added the stop-signal and post-error families and carried it past the threshold.
// Splitting the class by family would put the shared trial partitioning behind a seam for no gain in clarity.
@Suppress("TooManyFunctions")
class DefaultEmotionMetricsEngine : EmotionMetricsEngine {

    override fun blockMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val first = classified.firstOrNull() ?: return emptyMap()
        return when (first.trial.stimulus.payload) {
            is EmotionStimulusPayload.AffectiveGoNoGo -> goNoGoMetrics(classified)
            is EmotionStimulusPayload.TwoAltPerceptual -> choiceMetrics(classified)
            is EmotionStimulusPayload.Classification -> classificationMetrics(classified)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod") // One cohesive paradigm computation; shared locals.
    private fun goNoGoMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        // `requiresWithhold`, not `frameOpen`: from Ola 3 · R1 the withhold demand can arrive AFTER onset as a
        // stop signal, and those trials open their frame. Splitting on the initial frame would file every stop
        // trial as a GO and leave the commission denominator empty.
        val noGo = classified.filter { frame(it).requiresWithhold }
        val falseAlarms = noGo.count { it.outcome == EmotionTrialOutcome.FALSE_ALARM }
        val commissionRate = if (noGo.isEmpty()) 0.0 else falseAlarms.toDouble() / noGo.size

        val goTrials = classified.filter { !frame(it).requiresWithhold }
        val hits = classified.filter { it.outcome == EmotionTrialOutcome.HIT }
        // MC8u-B §1A: the GO-trial hit rate — the engagement signal that keeps commission honest.
        val hitRate = if (goTrials.isEmpty()) 0.0 else hits.size.toDouble() / goTrials.size
        val responses = hits.size + falseAlarms
        val commissionConfident = isCommissionConfident(classified, noGo.size, hitRate, responses)

        val hitRtsRaw = hits.mapNotNull { it.trial.latencyMs?.toDouble() }
        val anticipations = hitRtsRaw.count { it < IMPULSIVE_FLOOR_MS }
        val cleanHitRts = EmotionRtStatistics.rejectOutliers(hitRtsRaw)
        val rtMedian = EmotionRtStatistics.median(cleanHitRts)

        val congRts = hits.filter { frame(it).congruent }.mapNotNull { it.trial.latencyMs?.toDouble() }
        val incongRts = hits.filter { !frame(it).congruent }.mapNotNull { it.trial.latencyMs?.toDouble() }
        val interferenceConfident = congRts.size >= MIN_CELL && incongRts.size >= MIN_CELL
        val congMed = EmotionRtStatistics.median(congRts)
        val cost = if (interferenceConfident && congMed > 0.0) {
            ((EmotionRtStatistics.median(incongRts) - congMed) / congMed).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        return mapOf(
            EmotionMetricKey.COMMISSION_RATE to
                gatedMetric(EmotionMetricKey.COMMISSION_RATE, commissionRate, noGo.size, commissionConfident, EmotionMetricUnit.RATIO),
            // MC8u-B §1A: HIT_RATE is emitted so the low-confidence of COMMISSION_RATE is EXPLAINABLE (a low hit rate
            // is why commission was not trusted) and so the adaptive engine can block a step-up on go-omissions.
            EmotionMetricKey.HIT_RATE to
                metric(EmotionMetricKey.HIT_RATE, hitRate, goTrials.size, MIN_GO, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.RT_MEDIAN to
                metric(EmotionMetricKey.RT_MEDIAN, rtMedian, cleanHitRts.size, MIN_RT, EmotionMetricUnit.MILLISECONDS),
            EmotionMetricKey.ANTICIPATIONS to
                // MC8q: the VALUE is the count of too-fast (sub-floor) hits; `confident` gates on the HIT sample the
                // count is drawn from (hits.size >= MIN_RT) — the correct denominator for trusting an anticipation
                // count. Consumed only via valueIfConfident (never a raw ungated read).
                metric(EmotionMetricKey.ANTICIPATIONS, anticipations.toDouble(), hits.size, MIN_RT, EmotionMetricUnit.COUNT),
            EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST to
                if (interferenceConfident) {
                    metric(
                        EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
                        1.0 - cost,
                        minOf(congRts.size, incongRts.size),
                        MIN_CELL,
                        EmotionMetricUnit.INDEX,
                    )
                } else {
                    EmotionMetricValue.unavailable(EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST, EmotionMetricUnit.INDEX)
                },
        ) + stopSignalMetrics(classified)
    }

    /**
     * Ola 3 · R1 — the stop-signal readings, or nothing at all.
     *
     * Emitted ONLY for a block that actually carried stop signals. A classic go/no-go decided at onset has no
     * stop-signal delay to integrate over, so publishing an SSRT key for it — even an `unavailable` one — would
     * assert that the exercise tried to measure a brake latency and failed, when it never asked the question.
     */
    private fun stopSignalMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val stops = classified.filter { frame(it).stopAfterMs != null }
        return if (stops.isEmpty()) {
            emptyMap()
        } else {
            mapOf(
                EmotionMetricKey.SSRT to ssrtOf(classified, EmotionMetricKey.SSRT, tone = null),
                EmotionMetricKey.AFFECTIVE_SSRT_DELTA to affectiveSsrtDelta(classified),
            )
        }
    }

    /**
     * SSRT by the integration method over the trials of one tone cell ([tone] null = the whole block).
     *
     * Go reaction times pool the responded GO trials with the responses that ESCAPED a stop signal, which is the
     * standard treatment: a failed stop is a go response that the signal did not catch in time, and dropping it
     * would trim exactly the fast tail the integration reads.
     */
    private fun ssrtOf(
        classified: List<ClassifiedEmotionTrial>,
        key: EmotionMetricKey,
        tone: Boolean?,
    ): EmotionMetricValue {
        val cell = if (tone == null) classified else classified.filter { frame(it).negativeWord == tone }
        val stops = cell.filter { frame(it).stopAfterMs != null }
        val goRts = cell.filter { frame(it).stopAfterMs == null && it.outcome == EmotionTrialOutcome.HIT }
            .mapNotNull { it.trial.latencyMs?.toDouble() } +
            stops.filter { it.outcome == EmotionTrialOutcome.FALSE_ALARM }
                .mapNotNull { it.trial.latencyMs?.toDouble() }
        val ssds = stops.mapNotNull { frame(it).stopAfterMs?.toDouble() }
        val result = SsrtCalculator.compute(
            SsrtCalculator.SsrtInput(
                goReactionTimes = goRts.map { it.toLong() },
                stopTrials = stops.size,
                stopFailures = stops.count { it.outcome == EmotionTrialOutcome.FALSE_ALARM },
                meanStopSignalDelayMs = if (ssds.isEmpty()) 0.0 else ssds.average(),
            ),
        )
        return when (result) {
            is SsrtCalculator.SsrtResult.NotComputable ->
                EmotionMetricValue.unavailable(key, EmotionMetricUnit.MILLISECONDS, stops.size)
            is SsrtCalculator.SsrtResult.Value -> EmotionMetricValue(
                key = key,
                value = result.ssrtMs,
                n = stops.size,
                // `trustworthy` is the calculator's own staircase-breakdown check: when p(respond|signal) has
                // drifted far from 0.5 the estimate is not an estimate of anything, whatever its sample size.
                confident = result.trustworthy && goRts.size >= MIN_RT && stops.size >= MIN_STOP_TRIALS,
                unit = EmotionMetricUnit.MILLISECONDS,
            )
        }
    }

    /**
     * The affective cost of stopping: SSRT after a negative word minus SSRT after a neutral one.
     *
     * A difference score is worth its SMALLER cell, so both sides must clear the floor and BOTH must be
     * trustworthy on their own — a delta between a real estimate and a broken one is not a smaller effect, it is
     * a different quantity. Below that, the reading is withheld rather than shrunk.
     */
    private fun affectiveSsrtDelta(classified: List<ClassifiedEmotionTrial>): EmotionMetricValue {
        val negative = ssrtOf(classified, EmotionMetricKey.SSRT, tone = true)
        val neutral = ssrtOf(classified, EmotionMetricKey.SSRT, tone = false)
        val cell = minOf(negative.n, neutral.n)
        val usable = negative.confident && neutral.confident && cell >= MIN_SSRT_CELL
        return if (usable) {
            EmotionMetricValue(
                key = EmotionMetricKey.AFFECTIVE_SSRT_DELTA,
                value = negative.value - neutral.value,
                n = cell,
                confident = true,
                unit = EmotionMetricUnit.MILLISECONDS,
            )
        } else {
            EmotionMetricValue.unavailable(EmotionMetricKey.AFFECTIVE_SSRT_DELTA, EmotionMetricUnit.MILLISECONDS, cell)
        }
    }

    private fun choiceMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val total = classified.size
        val correct = classified.count { it.outcome == EmotionTrialOutcome.CORRECT }
        val accuracy = if (total == 0) 0.0 else correct.toDouble() / total
        val correctRts = classified.filter { it.outcome == EmotionTrialOutcome.CORRECT }
            .mapNotNull { it.trial.latencyMs?.toDouble() }
        val cleanRts = EmotionRtStatistics.rejectOutliers(correctRts)
        val rtCv = EmotionRtStatistics.coefficientOfVariation(cleanRts)
        val abandonment = classified.count { it.outcome == EmotionTrialOutcome.NO_RESPONSE }
        // MC8u-B §1E: same-answer spam / impossible RT / random-fast guessing must not fabricate a high accuracy.
        val accuracyConfident = total >= MIN_TOTAL &&
            !EmotionGamingGuard.looksGamed(classified, CHANCE_2ALT, checkSameKey = true)

        return mapOf(
            EmotionMetricKey.ACCURACY to
                gatedMetric(EmotionMetricKey.ACCURACY, accuracy, total, accuracyConfident, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.RT_CV to
                metric(EmotionMetricKey.RT_CV, rtCv, cleanRts.size, MIN_RT, EmotionMetricUnit.RATIO),
            EmotionMetricKey.ABANDONMENT to
                // MC8q: route through metric() so confident = total >= MIN_TOTAL, uniform with every other metric's
                // sample-size gate (was an inline total>0 flag that marked a 1-trial block confident, breaking the
                // module's own "confident = n >= minN everywhere" invariant).
                metric(EmotionMetricKey.ABANDONMENT, abandonment.toDouble(), total, MIN_TOTAL, EmotionMetricUnit.COUNT),
        ) + postErrorMetrics(classified, accuracyConfident)
    }

    /**
     * Ola 3 · R2 — the post-error family of the fused recovery exercise.
     *
     * Every reading here is a COMPARISON between what the user did after a mistake and what they did after a
     * success, so each one is worth its smaller cell and none of them is emitted as a number until BOTH cells
     * clear [MIN_POST_ERROR_CASES]. That floor used to be 3. Three pairs is not a small sample of a recovery
     * effect, it is noise with a name: a single unlucky trial moves post-error accuracy by 33 points. The audit
     * named the old floor as the module's most deceptive number and this raises it to the 8 its design specifies.
     *
     * The paired split is also why the staircase in front of this exercise matters. Under planted near-threshold
     * trials the cells were filled by whoever happened to make mistakes, so the strongest users produced no pairs
     * and were told the reading was `unavailable` — which reads as honesty and was a failure to ask the question.
     * With the error rate held at its target, the cells fill for everyone.
     */
    private fun postErrorMetrics(classified: List<ClassifiedEmotionTrial>, notGamed: Boolean): EmotionMetricSet {
        val pairs = responsePairs(classified)
        val afterError = pairs.filter { it.first.outcome == EmotionTrialOutcome.INCORRECT }
        val afterCorrect = pairs.filter { it.first.outcome == EmotionTrialOutcome.CORRECT }
        val postError = afterError.map { it.second }
        val postCorrect = afterCorrect.map { it.second }
        val cell = minOf(postError.size, postCorrect.size)
        val paired = notGamed && postError.size >= MIN_POST_ERROR_CASES && postCorrect.size >= MIN_POST_ERROR_FOLLOWUPS

        val postErrorAcc = accuracyOf(postError)
        val postCorrectAcc = accuracyOf(postCorrect)
        val slowing = EmotionRtStatistics.median(correctRtsOf(postError)) -
            EmotionRtStatistics.median(correctRtsOf(postCorrect))
        val recovery = recoveryComposite(postError, postErrorAcc, postCorrectAcc, slowing)
        val persistence = strategyPersistence(afterError, afterCorrect)
        val postErrorAbandonment = postError.count { it.outcome == EmotionTrialOutcome.NO_RESPONSE }
            .toDouble() / postError.size.coerceAtLeast(1)

        return mapOf(
            EmotionMetricKey.POST_ERROR_ACCURACY to
                paired(EmotionMetricKey.POST_ERROR_ACCURACY, postErrorAcc, cell, paired, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.POST_CORRECT_ACCURACY to
                paired(EmotionMetricKey.POST_CORRECT_ACCURACY, postCorrectAcc, cell, paired, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.POST_ERROR_SLOWING to
                paired(EmotionMetricKey.POST_ERROR_SLOWING, slowing, cell, paired, EmotionMetricUnit.MILLISECONDS),
            EmotionMetricKey.POST_ERROR_RECOVERY to
                paired(EmotionMetricKey.POST_ERROR_RECOVERY, recovery, cell, paired, EmotionMetricUnit.INDEX),
            // Persistence needs the RESPONSE of both trials of a pair, not just its outcome, so it carries a
            // second gate: a user who let trials time out can clear the pair floor and still leave no answers to
            // compare. `null` there is the honest reading, never a persistence of 1.0 built from nothing.
            EmotionMetricKey.STRATEGY_PERSISTENCE to
                paired(EmotionMetricKey.STRATEGY_PERSISTENCE, persistence ?: 0.0, cell, paired && persistence != null, EmotionMetricUnit.INDEX),
            // Abandonment after an error is not a comparison, so it needs only its own cell — a user who stopped
            // answering after mistakes should be visible even when there is no post-correct baseline to read
            // recovery against.
            EmotionMetricKey.POST_ERROR_ABANDONMENT to
                paired(
                    EmotionMetricKey.POST_ERROR_ABANDONMENT,
                    postErrorAbandonment,
                    postError.size,
                    postError.size >= MIN_POST_ERROR_CASES,
                    EmotionMetricUnit.PROPORTION,
                ),
        )
    }

    /** Consecutive (previous, next) trials where the PREVIOUS one was actually answered. */
    private fun responsePairs(
        classified: List<ClassifiedEmotionTrial>,
    ): List<Pair<ClassifiedEmotionTrial, ClassifiedEmotionTrial>> =
        (0 until classified.size - 1)
            .map { classified[it] to classified[it + 1] }
            .filter {
                it.first.outcome == EmotionTrialOutcome.CORRECT || it.first.outcome == EmotionTrialOutcome.INCORRECT
            }

    /**
     * Conditional post-error composite (slowing penalized only when accuracy did not recover, post-error
     * impulsivity penalized). Kept from MC8u-B §1C as a SECONDARY summary; the primary reading is now the
     * post-error accuracy itself, which is a plain proportion the zone machinery and the block score can read.
     */
    private fun recoveryComposite(
        postError: List<ClassifiedEmotionTrial>,
        postErrorAcc: Double,
        postCorrectAcc: Double,
        slowingMs: Double,
    ): Double {
        val baseRt = EmotionRtStatistics.median(correctRtsOf(postError)) - slowingMs
        val normSlow = if (baseRt > 0.0) (slowingMs / baseRt).coerceIn(0.0, 1.0) else 0.0
        val impulsive = postError.count {
            it.outcome == EmotionTrialOutcome.INCORRECT &&
                (it.trial.latencyMs?.toDouble() ?: Double.MAX_VALUE) < POST_ERROR_IMPULSE_MS
        }.toDouble() / postError.size.coerceAtLeast(1)
        val penalty = if (postErrorAcc >= postCorrectAcc - ACC_TOL) 0.0 else SLOW_PENALTY * normSlow
        return (postErrorAcc - IMPULSE_WEIGHT * impulsive - penalty).coerceIn(0.0, 1.0)
    }

    /**
     * How much an error changed the WAY the user answers: 1 minus the gap between the side-switch rate after an
     * error and after a correct answer. Null when either side has no pair in which both trials carry a directional
     * answer — a switch rate needs two answers to compare, not two trials.
     */
    private fun strategyPersistence(
        afterError: List<Pair<ClassifiedEmotionTrial, ClassifiedEmotionTrial>>,
        afterCorrect: List<Pair<ClassifiedEmotionTrial, ClassifiedEmotionTrial>>,
    ): Double? {
        val errorShift = switchRate(afterError)
        val correctShift = switchRate(afterCorrect)
        return if (errorShift != null && correctShift != null) {
            (1.0 - abs(errorShift - correctShift)).coerceIn(0.0, 1.0)
        } else {
            null
        }
    }

    /** Share of pairs whose two ANSWERED sides differ; null when no pair has an answer on both trials. */
    private fun switchRate(pairs: List<Pair<ClassifiedEmotionTrial, ClassifiedEmotionTrial>>): Double? {
        val answered = pairs.filter {
            it.first.trial.responseKey.isDirectional && it.second.trial.responseKey.isDirectional
        }
        return if (answered.isEmpty()) {
            null
        } else {
            answered.count { it.first.trial.responseKey != it.second.trial.responseKey }.toDouble() / answered.size
        }
    }

    private fun accuracyOf(trials: List<ClassifiedEmotionTrial>): Double =
        if (trials.isEmpty()) 0.0 else trials.count { it.outcome == EmotionTrialOutcome.CORRECT }.toDouble() / trials.size

    /** A paired reading: the value when the comparison stands, an explicit absence when it does not. */
    private fun paired(
        key: EmotionMetricKey,
        value: Double,
        n: Int,
        available: Boolean,
        unit: EmotionMetricUnit,
    ): EmotionMetricValue =
        if (available) {
            EmotionMetricValue(key = key, value = value, n = n, confident = true, unit = unit)
        } else {
            EmotionMetricValue.unavailable(key, unit, n)
        }

    private fun classificationMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val total = classified.size
        val correct = classified.count { it.outcome == EmotionTrialOutcome.CORRECT }
        val accuracy = if (total == 0) 0.0 else correct.toDouble() / total

        val etTrials = classified.filter {
            val truth = (it.trial.stimulus.payload as EmotionStimulusPayload.Classification).truth
            truth == EmotionCategory.INTERNAL_STATE || truth == EmotionCategory.THOUGHT
        }
        val etCorrect = etTrials.count { it.outcome == EmotionTrialOutcome.CORRECT }
        val etAcc = if (etTrials.isEmpty()) 0.0 else etCorrect.toDouble() / etTrials.size
        val separation = ((etAcc - CHANCE_4WAY) / (1.0 - CHANCE_4WAY)).coerceIn(0.0, 1.0)

        // MC8u-B §1D: granularity is normalized by the categories PRESENTED this session, not the whole enum — a short
        // block that never showed a category must not be penalized for "missing" it. Confidence needs enough distinct
        // categories on screen (a 1-category block can never demonstrate granularity), and repeats never inflate it
        // (the numerator is DISTINCT correctly-labeled categories, so spamming one answer stays low).
        val presentedCategories = classified
            .map { (it.trial.stimulus.payload as EmotionStimulusPayload.Classification).truth }.toSet()
        val distinctCorrect = classified.filter { it.outcome == EmotionTrialOutcome.CORRECT }
            .map { (it.trial.stimulus.payload as EmotionStimulusPayload.Classification).truth }.toSet().size
        val granularity = if (presentedCategories.isEmpty()) 0.0 else distinctCorrect.toDouble() / presentedCategories.size
        val granularityConfident = total >= MIN_TOTAL && presentedCategories.size >= MIN_GRANULARITY_CATEGORIES

        val correctRts = classified.filter { it.outcome == EmotionTrialOutcome.CORRECT }
            .mapNotNull { it.trial.latencyMs?.toDouble() }
        val rtMedian = EmotionRtStatistics.median(EmotionRtStatistics.rejectOutliers(correctRts))
        val categoryAccuracyConfident = total >= MIN_TOTAL &&
            !EmotionGamingGuard.looksGamed(classified, CHANCE_4WAY, checkSameKey = true)

        // Ola 2 · R3 · P3 — the metric a four-way task actually needs. The two below it read half the task
        // (STATE_THOUGHT_ACCURACY) and mere coverage (CATEGORY_COVERAGE); they keep their numbers under names
        // that say what they are, and this one carries the claim.
        val discrimination = EmotionCategoryDiscrimination.read(
            trials = classified,
            truthOf = { (it.trial.stimulus.payload as EmotionStimulusPayload.Classification).truth },
            answerOf = { it.trial.chosenCategory },
        )
        val answered = classified.count { it.trial.chosenCategory != null }

        return mapOf(
            EmotionMetricKey.CATEGORY_ACCURACY to
                gatedMetric(EmotionMetricKey.CATEGORY_ACCURACY, accuracy, total, categoryAccuracyConfident, EmotionMetricUnit.PROPORTION),
            // The WEAKEST of the four one-vs-rest d' values, not their mean: in a four-way task a user is only as
            // good as the boundary they handle worst, and averaging lets one strong category hide a missed one.
            EmotionMetricKey.CATEGORY_DPRIME_MIN to when (discrimination) {
                is CategoryDiscriminationReading.Present ->
                    gatedMetric(EmotionMetricKey.CATEGORY_DPRIME_MIN, discrimination.weakest.dPrime, answered, categoryAccuracyConfident, EmotionMetricUnit.INDEX)
                is CategoryDiscriminationReading.Absent ->
                    EmotionMetricValue.unavailable(EmotionMetricKey.CATEGORY_DPRIME_MIN, EmotionMetricUnit.INDEX, answered)
            },
            EmotionMetricKey.STATE_THOUGHT_ACCURACY to
                metric(EmotionMetricKey.STATE_THOUGHT_ACCURACY, separation, etTrials.size, MIN_PAIR, EmotionMetricUnit.INDEX),
            EmotionMetricKey.CATEGORY_COVERAGE to
                gatedMetric(EmotionMetricKey.CATEGORY_COVERAGE, granularity, total, granularityConfident, EmotionMetricUnit.INDEX),
            EmotionMetricKey.RT_MEDIAN to
                metric(EmotionMetricKey.RT_MEDIAN, rtMedian, correctRts.size, MIN_RT, EmotionMetricUnit.MILLISECONDS),
        )
    }

    /**
     * MC8u-B §1A: commission is confident only with enough no-go samples, real GO engagement (hit rate above a floor)
     * and enough total responses — and never on an impossibly-fast (gamed) run. A "never respond" strategy makes
     * commission 0 look like flawless inhibition; this closes that (and, via a null valueIfConfident, blocks step-up).
     */
    private fun isCommissionConfident(classified: List<ClassifiedEmotionTrial>, noGoSize: Int, hitRate: Double, responses: Int): Boolean =
        noGoSize >= MIN_NOGO &&
            hitRate >= MIN_HIT_RATE_FOR_INHIBITION_CONFIDENCE &&
            responses >= MIN_RESPONSES_FOR_COMMISSION_CONFIDENCE &&
            !EmotionGamingGuard.looksGamed(classified, chanceLevel = null, checkSameKey = false)

    private fun correctRtsOf(trials: List<ClassifiedEmotionTrial>): List<Double> =
        trials.filter { it.outcome == EmotionTrialOutcome.CORRECT }.mapNotNull { it.trial.latencyMs?.toDouble() }

    private fun frame(c: ClassifiedEmotionTrial): EmotionStimulusPayload.AffectiveGoNoGo =
        c.trial.stimulus.payload as EmotionStimulusPayload.AffectiveGoNoGo

    private fun metric(key: EmotionMetricKey, value: Double, n: Int, minN: Int, unit: EmotionMetricUnit): EmotionMetricValue =
        EmotionMetricValue(key = key, value = value, n = n, confident = n >= minN, unit = unit)

    /** Builds a metric with an EXPLICIT confidence flag (for gates richer than a plain sample-size threshold). */
    private fun gatedMetric(key: EmotionMetricKey, value: Double, n: Int, confident: Boolean, unit: EmotionMetricUnit): EmotionMetricValue =
        EmotionMetricValue(key = key, value = value, n = n, confident = confident, unit = unit)

    companion object {
        private const val MIN_NOGO = 6
        private const val MIN_GO = 6
        private const val MIN_RT = 8
        private const val MIN_TOTAL = 8
        private const val MIN_CELL = 5
        private const val MIN_PAIR = 8
        private const val MIN_STOP_TRIALS = 8

        /**
         * Stop trials required per TONE cell before the affective delta is reported. The audit's R1 design fixes
         * this at 12: below it, the difference between two SSRT estimates is dominated by the noise of the two
         * estimates rather than by any affective load.
         */
        internal const val MIN_SSRT_CELL = 12
        /**
         * Ola 3 · R2 — post-error pairs, and post-correct baseline pairs, required before any paired reading is
         * emitted. Both were 3. The audit named that floor as the module`s most deceptive number: with three
         * pairs one unlucky trial moves post-error accuracy by 33 points, so the reading was noise wearing the
         * name of a recovery effect. Eight is the floor its design specifies, and the staircase in front of the
         * exercise is what makes eight reachable for a user who rarely makes mistakes.
         */
        internal const val MIN_POST_ERROR_CASES = 8
        internal const val MIN_POST_ERROR_FOLLOWUPS = 8
        private const val MIN_GRANULARITY_CATEGORIES = 3
        private const val MIN_HIT_RATE_FOR_INHIBITION_CONFIDENCE = 0.5
        private const val MIN_RESPONSES_FOR_COMMISSION_CONFIDENCE = 4
        private const val IMPULSIVE_FLOOR_MS = 350.0
        private const val POST_ERROR_IMPULSE_MS = 250.0
        private const val CHANCE_4WAY = 0.25
        private const val CHANCE_2ALT = 0.5
        private const val ACC_TOL = 0.05
        private const val SLOW_PENALTY = 0.5
        private const val IMPULSE_WEIGHT = 0.5
    }
}
