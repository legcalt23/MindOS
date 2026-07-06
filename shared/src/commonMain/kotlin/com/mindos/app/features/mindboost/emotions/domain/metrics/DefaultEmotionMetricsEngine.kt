@file:Suppress("MagicNumber", "MaxLineLength") // Tuning constants + dense metric-builder call sites.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome

/**
 * Default metrics engine for the three v1 paradigms (dispatched by payload). Implements the design's
 * honesty rules: commission over the no-go denominator; RT central tendency on MAD-cleaned data while
 * extreme events (anticipations) count on raw; the conditional post-error-recovery composite (slowing
 * penalized ONLY when accuracy did not recover; post-error impulsivity penalized); chance-adjusted
 * separation; and `confident = n >= MIN_N` everywhere with `unavailable` instead of NaN.
 */
class DefaultEmotionMetricsEngine : EmotionMetricsEngine {

    override fun blockMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val first = classified.firstOrNull() ?: return emptyMap()
        return when (first.trial.stimulus.payload) {
            is EmotionStimulusPayload.AffectiveGoNoGo -> goNoGoMetrics(classified)
            is EmotionStimulusPayload.TwoAltPerceptual -> choiceMetrics(classified)
            is EmotionStimulusPayload.Classification -> classificationMetrics(classified)
        }
    }

    @Suppress("LongMethod") // One cohesive paradigm computation; splitting would scatter shared locals.
    private fun goNoGoMetrics(classified: List<ClassifiedEmotionTrial>): EmotionMetricSet {
        val noGo = classified.filter { !frame(it).frameOpen }
        val falseAlarms = noGo.count { it.outcome == EmotionTrialOutcome.FALSE_ALARM }
        val commissionRate = if (noGo.isEmpty()) 0.0 else falseAlarms.toDouble() / noGo.size

        val hits = classified.filter { it.outcome == EmotionTrialOutcome.HIT }
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
                metric(EmotionMetricKey.COMMISSION_RATE, commissionRate, noGo.size, MIN_NOGO, EmotionMetricUnit.RATIO),
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
        )
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
        val recovery = postErrorRecovery(classified)

        return mapOf(
            EmotionMetricKey.ACCURACY to
                metric(EmotionMetricKey.ACCURACY, accuracy, total, MIN_TOTAL, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.RT_CV to
                metric(EmotionMetricKey.RT_CV, rtCv, cleanRts.size, MIN_RT, EmotionMetricUnit.RATIO),
            EmotionMetricKey.ABANDONMENT to
                // MC8q: route through metric() so confident = total >= MIN_TOTAL, uniform with every other metric's
                // sample-size gate (was an inline total>0 flag that marked a 1-trial block confident, breaking the
                // module's own "confident = n >= minN everywhere" invariant).
                metric(EmotionMetricKey.ABANDONMENT, abandonment.toDouble(), total, MIN_TOTAL, EmotionMetricUnit.COUNT),
            EmotionMetricKey.POST_ERROR_RECOVERY to
                if (recovery.pairs >= MIN_POST_ERROR) {
                    metric(EmotionMetricKey.POST_ERROR_RECOVERY, recovery.score, recovery.pairs, MIN_POST_ERROR, EmotionMetricUnit.INDEX)
                } else {
                    EmotionMetricValue.unavailable(EmotionMetricKey.POST_ERROR_RECOVERY, EmotionMetricUnit.INDEX, recovery.pairs)
                },
            EmotionMetricKey.POST_ERROR_SLOWING to
                if (recovery.pairs >= MIN_POST_ERROR) {
                    metric(EmotionMetricKey.POST_ERROR_SLOWING, recovery.slowingMs, recovery.pairs, MIN_POST_ERROR, EmotionMetricUnit.MILLISECONDS)
                } else {
                    EmotionMetricValue.unavailable(EmotionMetricKey.POST_ERROR_SLOWING, EmotionMetricUnit.MILLISECONDS, recovery.pairs)
                },
        )
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

        val distinctCorrect = classified.filter { it.outcome == EmotionTrialOutcome.CORRECT }
            .map { (it.trial.stimulus.payload as EmotionStimulusPayload.Classification).truth }.toSet().size
        val granularity = distinctCorrect.toDouble() / EmotionCategory.entries.size

        val correctRts = classified.filter { it.outcome == EmotionTrialOutcome.CORRECT }
            .mapNotNull { it.trial.latencyMs?.toDouble() }
        val rtMedian = EmotionRtStatistics.median(EmotionRtStatistics.rejectOutliers(correctRts))

        return mapOf(
            EmotionMetricKey.CATEGORY_ACCURACY to
                metric(EmotionMetricKey.CATEGORY_ACCURACY, accuracy, total, MIN_TOTAL, EmotionMetricUnit.PROPORTION),
            EmotionMetricKey.EMOTION_THOUGHT_SEPARATION to
                metric(EmotionMetricKey.EMOTION_THOUGHT_SEPARATION, separation, etTrials.size, MIN_PAIR, EmotionMetricUnit.INDEX),
            EmotionMetricKey.GRANULARITY_INDEX to
                metric(EmotionMetricKey.GRANULARITY_INDEX, granularity, total, MIN_TOTAL, EmotionMetricUnit.INDEX),
            EmotionMetricKey.RT_MEDIAN to
                metric(EmotionMetricKey.RT_MEDIAN, rtMedian, correctRts.size, MIN_RT, EmotionMetricUnit.MILLISECONDS),
        )
    }

    /** Conditional post-error composite (slowing penalized only when accuracy did not recover). */
    private fun postErrorRecovery(classified: List<ClassifiedEmotionTrial>): PostError {
        val postError = mutableListOf<ClassifiedEmotionTrial>()
        val postCorrect = mutableListOf<ClassifiedEmotionTrial>()
        for (i in 0 until classified.size - 1) {
            val responded = classified[i].outcome == EmotionTrialOutcome.CORRECT ||
                classified[i].outcome == EmotionTrialOutcome.INCORRECT
            if (!responded) continue
            val next = classified[i + 1]
            if (classified[i].outcome == EmotionTrialOutcome.CORRECT) postCorrect.add(next) else postError.add(next)
        }
        if (postError.size < MIN_POST_ERROR) return PostError(0.0, 0.0, postError.size)

        val postErrAcc = postError.count { it.outcome == EmotionTrialOutcome.CORRECT }.toDouble() / postError.size
        val baseAcc = if (postCorrect.isEmpty()) {
            postErrAcc
        } else {
            postCorrect.count { it.outcome == EmotionTrialOutcome.CORRECT }.toDouble() / postCorrect.size
        }
        val postErrRt = EmotionRtStatistics.median(correctRtsOf(postError))
        val baseRt = EmotionRtStatistics.median(correctRtsOf(postCorrect))
        val slowing = postErrRt - baseRt
        val normSlow = if (baseRt > 0.0) (slowing / baseRt).coerceIn(0.0, 1.0) else 0.0
        val accRecovered = postErrAcc >= baseAcc - ACC_TOL
        val impulsive = postError.count {
            it.outcome == EmotionTrialOutcome.INCORRECT && (it.trial.latencyMs?.toDouble() ?: Double.MAX_VALUE) < POST_ERROR_IMPULSE_MS
        }.toDouble() / postError.size
        val penalty = if (accRecovered) 0.0 else SLOW_PENALTY * normSlow
        val score = (postErrAcc - IMPULSE_WEIGHT * impulsive - penalty).coerceIn(0.0, 1.0)
        return PostError(score, slowing, postError.size)
    }

    private fun correctRtsOf(trials: List<ClassifiedEmotionTrial>): List<Double> =
        trials.filter { it.outcome == EmotionTrialOutcome.CORRECT }.mapNotNull { it.trial.latencyMs?.toDouble() }

    private fun frame(c: ClassifiedEmotionTrial): EmotionStimulusPayload.AffectiveGoNoGo =
        c.trial.stimulus.payload as EmotionStimulusPayload.AffectiveGoNoGo

    private fun metric(key: EmotionMetricKey, value: Double, n: Int, minN: Int, unit: EmotionMetricUnit): EmotionMetricValue =
        EmotionMetricValue(key = key, value = value, n = n, confident = n >= minN, unit = unit)

    private data class PostError(val score: Double, val slowingMs: Double, val pairs: Int)

    companion object {
        private const val MIN_NOGO = 6
        private const val MIN_RT = 8
        private const val MIN_TOTAL = 8
        private const val MIN_CELL = 5
        private const val MIN_PAIR = 8
        private const val MIN_POST_ERROR = 3
        private const val IMPULSIVE_FLOOR_MS = 350.0
        private const val POST_ERROR_IMPULSE_MS = 250.0
        private const val CHANCE_4WAY = 0.25
        private const val ACC_TOL = 0.05
        private const val SLOW_PENALTY = 0.5
        private const val IMPULSE_WEIGHT = 0.5
    }
}
