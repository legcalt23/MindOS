package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.SsrtCalculator
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import com.mindos.app.features.mindboost.focus.domain.model.isResponse
import kotlin.math.max

/**
 * Default, scientifically-honest implementation of [MetricsEngine].
 *
 * Key choices (see 03_metrics_engine_plan.md):
 * - RT median (not mean) and RT-CV (SD/median) are the primary speed/stability metrics.
 * - Only HIT reaction times feed RT stats, after MAD-based outlier rejection and
 *   anticipation filtering.
 * - Every value carries its sample size and a confidence flag.
 *
 * Suppressions: this is a computation-dense engine. MagicNumber covers the inline
 * statistical/formula constants (each documented at its use site); TooManyFunctions
 * reflects the many small, single-purpose metric helpers that read better separated
 * than merged into one method.
 */
@Suppress("TooManyFunctions", "MagicNumber")
class DefaultMetricsEngine : MetricsEngine {

    override fun blockMetrics(
        trials: List<TrialResult>,
        definition: ExerciseDefinition,
        thresholds: IndividualThresholds,
    ): MetricSet {
        val result = LinkedHashMap<MetricKey, MetricValue>()
        // Cells for the metrics whose measurement sample is NOT their `n`: a difference score is limited by
        // its weaker cell, and only the helper that splits the cells knows how big each one is.
        val samples = LinkedHashMap<MetricKey, MeasurementSample>()
        val validTrials = trials.filter { it.outcome != TrialOutcome.ANTICIPATION }
        val hits = trials.filter { it.outcome == TrialOutcome.HIT }

        // ── Reaction-time cleaning (anticipations already excluded as a category) ──
        val rawHitRts = hits.mapNotNull { it.reactionTime?.value }
            .filter { it >= thresholds.anticipationFloorMs }
            .map { it.toDouble() }
        val cleanRts = RtStatistics.rejectOutliers(rawHitRts)

        // ── Accuracy ──
        val correct = trials.count {
            it.outcome == TrialOutcome.HIT || it.outcome == TrialOutcome.CORRECT_REJECTION
        }
        val accuracyN = validTrials.size
        // FOC-P0-04 (`MB-FOCUS-0091`): accuracy measures the user only if the user RESPONDED. A block with zero
        // responses still scores every non-target as a correct rejection, so a rare-target schedule reads 0.94
        // for someone who never touched the screen. That is an absence — typed as such — not a value.
        val responses = trials.count { it.outcome.isResponse }
        result[MetricKey.ACCURACY] = when {
            accuracyN == 0 -> MetricValue.unavailable(MetricKey.ACCURACY, MetricUnit.PROPORTION)
            responses == 0 -> MetricValue.unavailable(
                MetricKey.ACCURACY, MetricUnit.PROPORTION, accuracyN, MetricAbsence.NO_RESPONSES,
            )
            else -> MetricValue(
                key = MetricKey.ACCURACY,
                value = correct.toDouble() / accuracyN,
                n = accuracyN,
                confident = accuracyN >= MIN_N_ACCURACY,
                unit = MetricUnit.PROPORTION,
            )
        }

        // ── RT median ──
        val rtMedian = RtStatistics.median(cleanRts)
        result[MetricKey.RT_MEDIAN] = metric(
            MetricKey.RT_MEDIAN, rtMedian, cleanRts.size, MetricUnit.MILLISECONDS, MIN_N_RT,
        )

        // ── RT-CV = SD / median (NOT SD/mean) ──
        val sd = RtStatistics.standardDeviation(cleanRts)
        val rtCv = if (rtMedian > 0.0) sd / rtMedian else 0.0
        result[MetricKey.RT_CV] = metric(
            MetricKey.RT_CV, rtCv, cleanRts.size, MetricUnit.RATIO, MIN_N_RT,
        )

        // ── Lapses (RT above the individual threshold) ──
        // Counted on rawHitRts (anticipation-filtered but NOT MAD-filtered): a lapse
        // is a genuinely slow response, not a measurement artifact, so it must not be
        // removed by the outlier rejection that protects the central-tendency metrics.
        val lapses = rawHitRts.count { it > thresholds.lapseRtMs }
        result[MetricKey.LAPSES] = metric(
            MetricKey.LAPSES, lapses.toDouble(), rawHitRts.size, MetricUnit.COUNT, MIN_N_RT,
        )

        // ── Error tallies ──
        addErrorTallies(trials, result)

        // ── Post-error slowing & post-error accuracy ──
        addPostErrorMetrics(trials, result, samples)

        // ── Efficiency composites ──
        addEfficiencyMetrics(
            accuracy = result[MetricKey.ACCURACY],
            rtMedian = rtMedian,
            cleanRts = cleanRts,
            totalValidTrials = validTrials.size,
            out = result,
        )

        // ── Vigilance decrement slope (RT trend over time-on-task) ──
        result[MetricKey.VIGILANCE_DECREMENT_SLOPE] = vigilanceDecrement(hits, thresholds)

        // ── Block stability (1 - normalized RT-CV; higher = steadier) ──
        result[MetricKey.BLOCK_STABILITY] = blockStability(rtCv, cleanRts.size)

        // ── High-level indices for reporting ──
        addFocusIndices(result, rtCv, lapses, rawHitRts.size, cleanRts.size)

        // ── Paradigm-specific metrics (only for exercises that declare them) ──
        addParadigmMetrics(trials, definition, result, samples)

        stampMeasurementSufficiency(result, samples)
        return result
    }

    override fun sessionMetrics(blocks: List<BlockResult>): MetricSet {
        if (blocks.isEmpty()) return emptyMap()
        val allTrials = blocks.flatMap { it.trials }
        val result = LinkedHashMap<MetricKey, MetricValue>()

        // Trial-weighted average of accuracy and RT median across blocks.
        result[MetricKey.ACCURACY] = weightedAverage(blocks, MetricKey.ACCURACY, MetricUnit.PROPORTION)
        result[MetricKey.RT_MEDIAN] = weightedAverage(blocks, MetricKey.RT_MEDIAN, MetricUnit.MILLISECONDS)
        result[MetricKey.RT_CV] = weightedAverage(blocks, MetricKey.RT_CV, MetricUnit.RATIO)

        // Lapses are RT-threshold dependent, so sum the per-block lapse counts
        // rather than re-deriving them without each block's thresholds.
        val lapseCells = blocks.mapNotNull { it.metrics[MetricKey.LAPSES] }
        // The cell is what those lapses were counted over -- the caught signals each block measured -- and
        // not every trial in the session: a lapse is a SLOW HIT, so a trial nobody responded to is not part
        // of its sample. Same rule as [weightedAverage], reached by a different route.
        val lapseCell = lapseCells.sumOf { it.n }
        result[MetricKey.LAPSES] = MetricValue(
            MetricKey.LAPSES, lapseCells.sumOf { it.value }, lapseCell, lapseCell >= MIN_N_RT, MetricUnit.COUNT,
        )
        result[MetricKey.FALSE_ALARMS] = countMetric(
            MetricKey.FALSE_ALARMS, allTrials.count { it.outcome == TrialOutcome.FALSE_ALARM }, allTrials.size,
        )

        // Intra-session fatigue: RT-CV trend from first third to last third of blocks.
        result[MetricKey.FATIGUE_INDEX] = sessionFatigue(blocks)

        stampMeasurementSufficiency(result, samples = emptyMap())
        return result
    }

    /**
     * Attaches the SECOND sample verdict — measurement-grade, from the central `MinSampleGate` — to every
     * metric that has a ratified floor. Done in one pass so a metric cannot be added and silently left
     * undeclared; the ones without a floor keep `null`, which means "the question does not apply".
     *
     * Metrics absent from [samples] are judged on their own `n`: it IS their cell. Only difference scores
     * need an entry, because their `n` is the SUM of two cells and a claim is limited by the smaller one.
     */
    private fun stampMeasurementSufficiency(
        out: MutableMap<MetricKey, MetricValue>,
        samples: Map<MetricKey, MeasurementSample>,
    ) {
        for (key in out.keys.toList()) {
            val value = out.getValue(key)
            val sample = samples[key] ?: MeasurementSample(value.n, cellsPresent = value.n > 0)
            out[key] = value.copy(measurementSufficient = MeasurementSufficiency.evaluate(key, sample))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun metric(
        key: MetricKey,
        value: Double,
        n: Int,
        unit: MetricUnit,
        minN: Int,
    ): MetricValue = MetricValue(
        key = key,
        value = if (value.isFinite()) value else 0.0,
        n = n,
        confident = n >= minN && value.isFinite(),
        unit = unit,
    )

    private fun countMetric(key: MetricKey, count: Int, n: Int): MetricValue = MetricValue(
        key = key,
        value = count.toDouble(),
        n = n,
        confident = n > 0,
        unit = MetricUnit.COUNT,
    )

    private fun addErrorTallies(trials: List<TrialResult>, out: MutableMap<MetricKey, MetricValue>) {
        out[MetricKey.FALSE_ALARMS] = countMetric(
            MetricKey.FALSE_ALARMS, trials.count { it.outcome == TrialOutcome.FALSE_ALARM }, trials.size,
        )
        out[MetricKey.OMISSIONS] = countMetric(
            MetricKey.OMISSIONS, trials.count { it.outcome == TrialOutcome.MISS }, trials.size,
        )
        out[MetricKey.ANTICIPATIONS] = countMetric(
            MetricKey.ANTICIPATIONS, trials.count { it.outcome == TrialOutcome.ANTICIPATION }, trials.size,
        )
    }

    // Sequential pairwise scan: complexity comes from the post-error vs post-correct
    // case split, which is clearest expressed inline rather than fragmented.
    @Suppress("CyclomaticComplexMethod")
    private fun addPostErrorMetrics(
        trials: List<TrialResult>,
        out: MutableMap<MetricKey, MetricValue>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ) {
        val postErrorRts = mutableListOf<Double>()
        val postCorrectRts = mutableListOf<Double>()
        var postErrorCorrect = 0
        var postErrorTotal = 0
        for (i in 1 until trials.size) {
            val prev = trials[i - 1]
            val cur = trials[i]
            val prevWasError = prev.outcome == TrialOutcome.FALSE_ALARM || prev.outcome == TrialOutcome.MISS
            val curRt = cur.reactionTime?.value?.toDouble()
            if (prevWasError) {
                postErrorTotal++
                if (cur.outcome == TrialOutcome.HIT || cur.outcome == TrialOutcome.CORRECT_REJECTION) postErrorCorrect++
                if (cur.outcome == TrialOutcome.HIT && curRt != null) postErrorRts.add(curRt)
            } else if (prev.outcome == TrialOutcome.HIT && cur.outcome == TrialOutcome.HIT && curRt != null) {
                postCorrectRts.add(curRt)
            }
        }
        val pes = if (postErrorRts.isNotEmpty() && postCorrectRts.isNotEmpty()) {
            postErrorRts.average() - postCorrectRts.average()
        } else {
            0.0
        }
        // Post-error slowing compares two RT sets, so its measurement sample is the smaller of them.
        samples[MetricKey.POST_ERROR_SLOWING] = cellsOf(postErrorRts.size, postCorrectRts.size)
        out[MetricKey.POST_ERROR_SLOWING] = MetricValue(
            key = MetricKey.POST_ERROR_SLOWING,
            value = pes,
            n = postErrorRts.size,
            confident = postErrorRts.size >= MIN_N_POST_ERROR && postCorrectRts.size >= MIN_N_POST_ERROR,
            unit = MetricUnit.MILLISECONDS,
        )
        out[MetricKey.POST_ERROR_ACCURACY] = MetricValue(
            key = MetricKey.POST_ERROR_ACCURACY,
            value = if (postErrorTotal > 0) postErrorCorrect.toDouble() / postErrorTotal else 0.0,
            n = postErrorTotal,
            confident = postErrorTotal >= MIN_N_POST_ERROR,
            unit = MetricUnit.PROPORTION,
        )
    }

    private fun addEfficiencyMetrics(
        accuracy: MetricValue?,
        rtMedian: Double,
        cleanRts: List<Double>,
        totalValidTrials: Int,
        out: MutableMap<MetricKey, MetricValue>,
    ) {
        val acc = accuracy?.value ?: 0.0
        // IES = RT median / accuracy (ms per unit of correct). Undefined at accuracy 0.
        out[MetricKey.IES] = if (acc <= 0.0 || cleanRts.size < MIN_N_RT) {
            MetricValue.unavailable(MetricKey.IES, MetricUnit.MILLISECONDS, cleanRts.size)
        } else {
            MetricValue(MetricKey.IES, rtMedian / acc, cleanRts.size, true, MetricUnit.MILLISECONDS)
        }
        // Rate-correct score = correct responses per second of total RT.
        val totalRtSeconds = cleanRts.sum() / 1000.0
        out[MetricKey.RATE_CORRECT_SCORE] = if (totalRtSeconds <= 0.0 || cleanRts.size < MIN_N_RT) {
            MetricValue.unavailable(MetricKey.RATE_CORRECT_SCORE, MetricUnit.RATIO, cleanRts.size)
        } else {
            MetricValue(
                MetricKey.RATE_CORRECT_SCORE,
                cleanRts.size.toDouble() / totalRtSeconds,
                cleanRts.size, true, MetricUnit.RATIO,
            )
        }
        // LISAS = mean RT + (SD_RT / SD_PE) * PE. Falls back to unavailable when SD_PE is 0.
        out[MetricKey.LISAS] = computeLisas(cleanRts, acc, totalValidTrials)
    }

    private fun computeLisas(cleanRts: List<Double>, accuracy: Double, totalValidTrials: Int): MetricValue {
        if (cleanRts.size < MIN_N_RT || totalValidTrials == 0) {
            return MetricValue.unavailable(MetricKey.LISAS, MetricUnit.MILLISECONDS, cleanRts.size)
        }
        val meanRt = cleanRts.average()
        val sdRt = RtStatistics.standardDeviation(cleanRts)
        val proportionErrors = 1.0 - accuracy
        // SD of a Bernoulli error process across trials.
        val sdPe = kotlin.math.sqrt(max(proportionErrors * (1.0 - proportionErrors), 0.0))
        // With no error variance (sdPe == 0) LISAS reduces to the mean RT.
        val lisas = if (sdPe == 0.0) meanRt else meanRt + (sdRt / sdPe) * proportionErrors
        return MetricValue(MetricKey.LISAS, lisas, cleanRts.size, true, MetricUnit.MILLISECONDS)
    }

    private fun vigilanceDecrement(hits: List<TrialResult>, thresholds: IndividualThresholds): MetricValue {
        val points = hits.mapIndexedNotNull { index, t ->
            val rt = t.reactionTime?.value?.toDouble() ?: return@mapIndexedNotNull null
            if (rt < thresholds.anticipationFloorMs) null else index.toDouble() to rt
        }
        if (points.size < MIN_N_SLOPE) {
            return MetricValue.unavailable(MetricKey.VIGILANCE_DECREMENT_SLOPE, MetricUnit.SLOPE, points.size)
        }
        val slope = RtStatistics.linearSlope(points.map { it.first }, points.map { it.second })
        return MetricValue(MetricKey.VIGILANCE_DECREMENT_SLOPE, slope, points.size, true, MetricUnit.SLOPE)
    }

    private fun addParadigmMetrics(
        trials: List<TrialResult>,
        definition: ExerciseDefinition,
        out: MutableMap<MetricKey, MetricValue>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ) {
        val primary = definition.primaryMetrics
        if (MetricKey.CONGRUENCY_EFFECT in primary) out[MetricKey.CONGRUENCY_EFFECT] = congruencyEffect(trials, samples)
        if (MetricKey.SSRT in primary) out[MetricKey.SSRT] = stopSignalReactionTime(trials)
        if (MetricKey.SWITCH_COST in primary) out[MetricKey.SWITCH_COST] = switchCost(trials, samples)
        if (MetricKey.SET_SIZE_SLOPE in primary) out[MetricKey.SET_SIZE_SLOPE] = setSizeSlope(trials)
        if (MetricKey.DUAL_TASK_COST in primary) out[MetricKey.DUAL_TASK_COST] = dualTaskCost(trials, samples)
        if (MetricKey.DISTRACTION_RESISTANCE_INDEX in primary) {
            out[MetricKey.DISTRACTION_RESISTANCE_INDEX] = distractionResistance(trials)
        }
        if (MetricKey.PRESSURE_DELTA in primary) out[MetricKey.PRESSURE_DELTA] = pressureDelta(trials, samples)
    }

    /**
     * The measurement sample of a difference score: its SMALLER cell, and whether both cells exist at all.
     * The total ([MetricValue.n]) is deliberately not it — 40 trials in one condition and 2 in the other is a
     * sample of 42 that cannot support the comparison.
     */
    private fun cellsOf(a: Int, b: Int) =
        MeasurementSample(nEffective = kotlin.math.min(a, b), cellsPresent = a > 0 && b > 0)

    /** E6: RT(switch) − RT(repeat) over correct responses. */
    private fun switchCost(
        trials: List<TrialResult>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ): MetricValue {
        fun hitRts(switch: Boolean) = trials
            .filter { it.outcome == TrialOutcome.HIT && it.isSwitch == switch }
            .mapNotNull { it.reactionTime?.value?.toDouble() }
        val switchRts = hitRts(switch = true)
        val repeatRts = hitRts(switch = false)
        val n = switchRts.size + repeatRts.size
        samples[MetricKey.SWITCH_COST] = cellsOf(switchRts.size, repeatRts.size)
        if (switchRts.size < MIN_N_CONGRUENCY || repeatRts.size < MIN_N_CONGRUENCY) {
            return MetricValue.unavailable(MetricKey.SWITCH_COST, MetricUnit.MILLISECONDS, n)
        }
        val cost = RtStatistics.median(switchRts) - RtStatistics.median(repeatRts)
        return MetricValue(MetricKey.SWITCH_COST, cost, n, confident = true, unit = MetricUnit.MILLISECONDS)
    }

    /** E7: slope of RT vs set size over correct responses (ms per item). */
    private fun setSizeSlope(trials: List<TrialResult>): MetricValue {
        val points = trials
            .filter { it.outcome == TrialOutcome.HIT && it.setSize != null && it.reactionTime != null }
            .map { it.setSize!!.toDouble() to it.reactionTime!!.value.toDouble() }
        val distinctSizes = points.map { it.first }.distinct().size
        if (points.size < MIN_N_SLOPE || distinctSizes < 2) {
            return MetricValue.unavailable(MetricKey.SET_SIZE_SLOPE, MetricUnit.SLOPE, points.size)
        }
        val slope = RtStatistics.linearSlope(points.map { it.first }, points.map { it.second })
        return MetricValue(MetricKey.SET_SIZE_SLOPE, slope, points.size, confident = true, unit = MetricUnit.SLOPE)
    }

    /** E8: accuracy(single) − accuracy(dual). Positive = a cost from sharing attention. */
    private fun dualTaskCost(
        trials: List<TrialResult>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ): MetricValue {
        val singleTrials = trials.filter { it.dualTask == false }
        val dualTrials = trials.filter { it.dualTask == true }
        val single = accuracyOf(singleTrials)
        val dual = accuracyOf(dualTrials)
        val n = trials.count { it.dualTask != null }
        samples[MetricKey.DUAL_TASK_COST] = cellsOf(validCount(singleTrials), validCount(dualTrials))
        if (single == null || dual == null) {
            return MetricValue.unavailable(MetricKey.DUAL_TASK_COST, MetricUnit.PROPORTION, n)
        }
        return MetricValue(MetricKey.DUAL_TASK_COST, single - dual, n, confident = true, unit = MetricUnit.PROPORTION)
    }

    /** E11: accuracy(during intrusion) / accuracy(baseline). 1.0 = fully resistant. */
    private fun distractionResistance(trials: List<TrialResult>): MetricValue {
        val baseline = accuracyOf(trials.filter { it.duringIntrusion == false })
        val intruded = accuracyOf(trials.filter { it.duringIntrusion == true })
        val n = trials.count { it.duringIntrusion != null }
        if (baseline == null || intruded == null || baseline == 0.0) {
            return MetricValue.unavailable(MetricKey.DISTRACTION_RESISTANCE_INDEX, MetricUnit.INDEX, n)
        }
        val resistance = (intruded / baseline).coerceIn(0.0, 1.0)
        return MetricValue(
            MetricKey.DISTRACTION_RESISTANCE_INDEX, resistance, n, confident = true, unit = MetricUnit.INDEX,
        )
    }

    /** E13: accuracy(calm) − accuracy(deadline). Positive = pressure hurts accuracy. */
    private fun pressureDelta(
        trials: List<TrialResult>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ): MetricValue {
        val calmTrials = trials.filter { it.underPressure == false }
        val pressuredTrials = trials.filter { it.underPressure == true }
        val calm = accuracyOf(calmTrials)
        val pressured = accuracyOf(pressuredTrials)
        val n = trials.count { it.underPressure != null }
        samples[MetricKey.PRESSURE_DELTA] = cellsOf(validCount(calmTrials), validCount(pressuredTrials))
        if (calm == null || pressured == null) {
            return MetricValue.unavailable(MetricKey.PRESSURE_DELTA, MetricUnit.PROPORTION, n)
        }
        return MetricValue(
            MetricKey.PRESSURE_DELTA, calm - pressured, n, confident = true, unit = MetricUnit.PROPORTION,
        )
    }

    /** Trials of a cell that count towards accuracy (anticipations are not a response to the condition). */
    private fun validCount(trials: List<TrialResult>): Int =
        trials.count { it.outcome != TrialOutcome.ANTICIPATION }

    /**
     * Accuracy over a trial subset, or null when the subset is too small to trust — or when nobody responded in
     * it: a cell of pure withholding is not a measurement, and two such cells would otherwise compare as a
     * confident "no cost" / "fully resistant" (`MB-FOCUS-0091`).
     */
    private fun accuracyOf(trials: List<TrialResult>): Double? {
        val valid = trials.filter { it.outcome != TrialOutcome.ANTICIPATION }
        val responded = trials.any { it.outcome.isResponse }
        return if (valid.size < MIN_N_ACCURACY || !responded) {
            null
        } else {
            val correct = valid.count {
                it.outcome == TrialOutcome.HIT || it.outcome == TrialOutcome.CORRECT_REJECTION
            }
            correct.toDouble() / valid.size
        }
    }

    /** E2/E5: RT(incongruent) − RT(congruent) over correct responses. Positive = more interference. */
    private fun congruencyEffect(
        trials: List<TrialResult>,
        samples: MutableMap<MetricKey, MeasurementSample>,
    ): MetricValue {
        fun hitRts(congruent: Boolean) = trials
            .filter { it.outcome == TrialOutcome.HIT && it.congruent == congruent }
            .mapNotNull { it.reactionTime?.value?.toDouble() }
        val congruentRts = hitRts(congruent = true)
        val incongruentRts = hitRts(congruent = false)
        val n = congruentRts.size + incongruentRts.size
        samples[MetricKey.CONGRUENCY_EFFECT] = cellsOf(congruentRts.size, incongruentRts.size)
        if (congruentRts.size < MIN_N_CONGRUENCY || incongruentRts.size < MIN_N_CONGRUENCY) {
            return MetricValue.unavailable(MetricKey.CONGRUENCY_EFFECT, MetricUnit.MILLISECONDS, n)
        }
        val effect = RtStatistics.median(incongruentRts) - RtStatistics.median(congruentRts)
        return MetricValue(MetricKey.CONGRUENCY_EFFECT, effect, n, confident = true, unit = MetricUnit.MILLISECONDS)
    }

    /** E4: SSRT via the integration method, computed from a block's stop/go trials. */
    private fun stopSignalReactionTime(trials: List<TrialResult>): MetricValue {
        val stopTrials = trials.filter { it.requiredStop }
        if (stopTrials.isEmpty()) return MetricValue.unavailable(MetricKey.SSRT, MetricUnit.MILLISECONDS)
        val goRts = trials
            .filter { !it.requiredStop && it.outcome == TrialOutcome.HIT }
            .mapNotNull { it.reactionTime?.value } +
            stopTrials // failed-stop responses count as go-RTs (Verbruggen)
                .filter { it.outcome == TrialOutcome.FALSE_ALARM }
                .mapNotNull { it.reactionTime?.value }
        val ssds = stopTrials.mapNotNull { it.stopSignalDelayMs }
        val meanSsd = if (ssds.isEmpty()) 0.0 else ssds.average()
        val result = SsrtCalculator.compute(
            SsrtCalculator.SsrtInput(
                goReactionTimes = goRts,
                stopTrials = stopTrials.size,
                stopFailures = stopTrials.count { it.outcome == TrialOutcome.FALSE_ALARM },
                meanStopSignalDelayMs = meanSsd,
            ),
        )
        return when (result) {
            is SsrtCalculator.SsrtResult.NotComputable ->
                MetricValue.unavailable(MetricKey.SSRT, MetricUnit.MILLISECONDS, stopTrials.size)
            is SsrtCalculator.SsrtResult.Value -> MetricValue(
                key = MetricKey.SSRT,
                value = result.ssrtMs,
                n = stopTrials.size,
                // Not confident when the staircase broke down (gaming) — see SsrtCalculator.
                confident = result.trustworthy && goRts.size >= MIN_N_RT,
                unit = MetricUnit.MILLISECONDS,
            )
        }
    }

    /**
     * Steadiness of the block, as `1 - RT-CV`.
     *
     * With no clean RTs there is no dispersion to measure: the incoming [rtCv] is 0.0 there only because
     * `median([])` is 0.0, and `1 - 0` would report PERFECT steadiness for a user who never responded. Absent
     * data is reported absent, never as a flawless block.
     */
    private fun blockStability(rtCv: Double, cleanRtCount: Int): MetricValue =
        if (cleanRtCount == 0) {
            MetricValue.unavailable(MetricKey.BLOCK_STABILITY, MetricUnit.INDEX)
        } else {
            MetricValue(
                key = MetricKey.BLOCK_STABILITY,
                value = (1.0 - rtCv).coerceIn(0.0, 1.0),
                n = cleanRtCount,
                confident = cleanRtCount >= MIN_N_RT,
                unit = MetricUnit.INDEX,
            )
        }

    /**
     * Sustained focus: penalize variability and lapses. 0..100.
     *
     * [cleanRtCount] is required, not derived: with no clean RTs the incoming [rtCv] is 0.0 purely because
     * `median([])` is 0.0, so the formula would return a PERFECT 100 for a user who produced no data at all.
     * Both the raw-hit count and the clean-RT count must be non-empty for the index to mean anything; when
     * either is empty the index is reported unavailable rather than fabricated.
     */
    private fun addFocusIndices(
        out: MutableMap<MetricKey, MetricValue>,
        rtCv: Double,
        lapses: Int,
        n: Int,
        cleanRtCount: Int,
    ) {
        if (n == 0 || cleanRtCount == 0) {
            out[MetricKey.SUSTAINED_FOCUS_INDEX] =
                MetricValue.unavailable(MetricKey.SUSTAINED_FOCUS_INDEX, MetricUnit.INDEX, n)
            return
        }
        val lapseRate = lapses.toDouble() / n
        val focus = (100.0 * (1.0 - rtCv).coerceIn(0.0, 1.0) * (1.0 - lapseRate).coerceIn(0.0, 1.0))
        out[MetricKey.SUSTAINED_FOCUS_INDEX] = MetricValue(
            key = MetricKey.SUSTAINED_FOCUS_INDEX,
            value = focus,
            n = n,
            confident = n >= MIN_N_RT,
            unit = MetricUnit.INDEX,
        )
    }

    /**
     * Trial-weighted mean of [key] across blocks, stamped with the CELL the claim rests on.
     *
     * Two different numbers, and this used to conflate them. A block trial count is the WEIGHT (how much
     * that block counts towards the mean); [MetricValue.n] is the SAMPLE. For accuracy they coincide,
     * because its cell IS the trials. For a reaction-time metric they do not: its cell is the caught
     * signals, which on the reserved probe is about a fifth of them. Stamping the weight as the sample
     * turned a cell below the ratified floor into one above it, and `measurementSufficient` is derived
     * from exactly this `n` -- so the aggregate handed back the sufficiency the block had honestly refused.
     */
    private fun weightedAverage(blocks: List<BlockResult>, key: MetricKey, unit: MetricUnit): MetricValue {
        val contributions = blocks.mapNotNull { block ->
            block.metrics[key]?.takeIf { it.confident }?.let { it to block.trials.size }
        }
        val weight = contributions.sumOf { it.second }
        return if (weight == 0) {
            MetricValue.unavailable(key, unit)
        } else {
            val weightedSum = contributions.sumOf { it.first.value * it.second }
            MetricValue(key, weightedSum / weight, contributions.sumOf { it.first.n }, true, unit)
        }
    }

    private fun sessionFatigue(blocks: List<BlockResult>): MetricValue {
        val cvs = blocks.mapNotNull { it.metrics[MetricKey.RT_CV]?.takeIf { v -> v.confident }?.value }
        if (cvs.size < 2) return MetricValue.unavailable(MetricKey.FATIGUE_INDEX, MetricUnit.INDEX, cvs.size)
        val third = max(1, cvs.size / 3)
        val firstThird = cvs.take(third).average()
        val lastThird = cvs.takeLast(third).average()
        // Positive = RT-CV grew over the session = fatigue signal. Normalize to 0..1.
        val raw = (lastThird - firstThird).coerceIn(-1.0, 1.0)
        return MetricValue(
            key = MetricKey.FATIGUE_INDEX,
            value = ((raw + 1.0) / 2.0),
            n = cvs.size,
            confident = true,
            unit = MetricUnit.INDEX,
        )
    }

    private companion object {
        const val MIN_N_ACCURACY = 5
        const val MIN_N_RT = 5
        const val MIN_N_POST_ERROR = 3
        const val MIN_N_SLOPE = 6
        const val MIN_N_CONGRUENCY = 3
    }
}
