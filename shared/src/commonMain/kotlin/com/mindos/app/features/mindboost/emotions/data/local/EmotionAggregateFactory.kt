@file:Suppress("MagicNumber") // Calibration thresholds/norms ARE the data of this pure formula file.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.core.calibration.BlockPredictionReader
import com.mindos.app.features.mindboost.core.calibration.CalibrationKernel
import com.mindos.app.features.mindboost.core.calibration.CalibrationReading
import com.mindos.app.features.mindboost.core.scoring.normalization.InterSessionConfidence
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.RobustnessDto
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionHoldoutEvidence
import com.mindos.app.features.mindboost.emotions.domain.metrics.HoldoutProbeOutcome
import com.mindos.app.features.mindboost.emotions.domain.metrics.HoldoutSessionEvidence
import com.mindos.app.features.mindboost.emotions.domain.metrics.PressureCells
import com.mindos.app.features.mindboost.emotions.domain.metrics.SeparatorRobustness
import com.mindos.app.features.mindboost.emotions.domain.metrics.SeparatorRobustnessIndex
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence

/**
 * The SINGLE, pure authority for the emotion aggregate (the calibration profile) derived from persisted
 * summaries. Both the live terminal (holder) and the durable-terminal reconciler build the aggregate through
 * here (MC8h), so a completed session recovered by pending-retry advances the aggregate EXACTLY as a live
 * completion would — never a second, drifting formula.
 *
 * HONEST calibration semantics (rigor charter): a session counts ONLY when it is a real completion
 * (endReason-authoritative via [EmotionTerminalSemantics.countsForAggregate]) AND has a RELIABLE identity
 * (MC8m — [EmotionTerminalSemantics.hasReliableSessionIdentity]) AND at least [EmotionTerminalSemantics.
 * MIN_SESSION_DURATION_MS] of active training — the PRD "valid session", enforced via [EmotionTerminalSemantics.
 * isValidForAggregate]. A SAFETY_STOP/ABANDONED session, a short (<120s) session, or a legacy/null/corrupt-identity
 * row, never advances calibration (read tolerance ≠ scoring tolerance). Returns null when nothing qualifies, so
 * callers never overwrite an existing aggregate with a fabricated zero (`null ≠ 0`).
 *
 * PRD calibration (MC8n/MC8r): [OFFICIAL_SESSIONS] = 5 valid sessions unlock full sufficiency and the STABLE
 * (official) tier. This is the SCORING authority — NOT `SessionRecord`. A `SessionRecord.status == FINALIZED`
 * (which a SAFETY_STOP also reaches) must NEVER be read as "a completed session for scoring/calibration": the
 * record is a motor-agnostic write-ahead lifecycle log with no score, and the aggregate here is derived only
 * from qualifying summaries.
 */
// Ola 3 · R3 · P4 pushed this object to the `TooManyFunctions` threshold. Every member here derives exactly one
// aggregate field from the persisted summaries, so the count tracks how many fields the aggregate has rather than
// any tangling — and splitting it would give the "single, pure authority" above two homes, which is the failure
// this file exists to prevent. Suppressed locally, deliberately, rather than by moving members out of the object.
@Suppress("TooManyFunctions")
object EmotionAggregateFactory {
    // MC8n/MC8r · PRD: 5 VALID sessions unlock full sufficiency / official range. STABLE == official calibration.
    private const val OFFICIAL_SESSIONS = 5.0
    private const val RECENT_SAFETY_WINDOW = 5
    private const val SAFETY_LOAD_NORM = 6.0 // ~6 safety events across the recent window ⇒ full safetyLoad (1.0)

    // Ola 0 · S4 — a direction needs enough comparable points to be more than two sessions of noise, and a move
    // smaller than this on the 0–100 block score is within ordinary session-to-session variation.
    private const val MIN_TREND_SESSIONS = 4
    private const val TREND_DELTA_POINTS = 5.0

    /** The aggregate implied by [summaries] at [now], or null if no VALID completed session exists yet. */
    fun fromSummaries(summaries: List<EmotionSessionSummaryDto>, now: Long): EmotionAggregateDto? {
        val completed = summaries.filter { EmotionTerminalSemantics.isValidForAggregate(it) }
        if (completed.isEmpty()) return null
        val sessions = completed.size
        val sufficiency = (sessions.toDouble() / OFFICIAL_SESSIONS).coerceAtMost(1.0)
        return EmotionAggregateDto(
            sessionsCompleted = sessions,
            tier = tierFor(sessions).name,
            dataSufficiency = sufficiency,
            safetyLoad = safetyLoadOf(summaries),
            transferConfidence = transferConfidenceOf(completed).name,
            robustness = robustnessOf(completed),
            confidence = confidenceOf(completed),
            trend = trendOf(completed).name,
            updatedAtMillis = now,
        )
    }

    /**
     * Ola 3 · R3 · P4 — accuracy under pressure against accuracy in calm, pooled across every session that
     * contributed cells.
     *
     * Pooling is where this reading has to live. The ratified floor for a pressure comparison is 20 per cell and a
     * session's calm cell is one block, so the per-session numbers are shares of an index rather than an index;
     * summing them here is the first point at which the question can be answered at all. The refusal travels with
     * the reason, because "pressure has never been served" and "keep training" are different things to tell a
     * user, and collapsing both into an absent value would make the second look like the first.
     */
    private fun robustnessOf(completed: List<EmotionSessionSummaryDto>): RobustnessDto {
        val pooled = completed
            .mapNotNull { it.pressureCells }
            .fold(PressureCells.EMPTY) { acc, dto ->
                acc + PressureCells(dto.calmCorrect, dto.calmTotal, dto.pressureCorrect, dto.pressureTotal)
            }
        return when (val reading = SeparatorRobustnessIndex.evaluate(pooled)) {
            is SeparatorRobustness.Measured -> RobustnessDto(
                index = reading.index,
                intervalLow = reading.interval.low,
                intervalHigh = reading.interval.high,
                verdict = reading.verdict.name,
            )
            is SeparatorRobustness.Unavailable -> RobustnessDto(gap = reading.reason.name)
        }
    }

    /**
     * Ola 0 · S4 — `confidence` is how STABLE the reading is; `dataSufficiency` is how MUCH of it there is. They
     * are orthogonal by charter, and this used to assign the same number to both: a user who had simply shown up
     * five times was reported as maximally confident even if every session disagreed wildly with the last.
     *
     * Stability is computed by the shared [InterSessionConfidence] kernel, which already fails closed on fewer
     * than two points and on a non-positive mean, so a single session reports 0.0 — genuinely "not established
     * yet" — rather than a fabricated fraction.
     */
    private fun confidenceOf(completed: List<EmotionSessionSummaryDto>): Double =
        InterSessionConfidence.interSessionConfidence(comparableScoreSeries(completed))

    /**
     * Ola 0 · S4 — the direction of travel, or [ProfileTrend.UNKNOWN] when the evidence cannot support one. It
     * used to be the literal UNKNOWN, which is indistinguishable from "we looked and could not tell".
     *
     * Fails closed below [MIN_TREND_SESSIONS] comparable points, and the comparison is oldest-half against
     * newest-half so the sign cannot be inverted by the storage order. A move smaller than [TREND_DELTA_POINTS] on
     * the 0–100 block score is reported as STEADY rather than dressed up as progress.
     */
    private fun trendOf(completed: List<EmotionSessionSummaryDto>): ProfileTrend {
        val series = comparableScoreSeries(completed)
        if (series.size < MIN_TREND_SESSIONS) return ProfileTrend.UNKNOWN
        val half = series.size / 2
        val delta = series.takeLast(half).average() - series.take(half).average()
        return when {
            delta >= TREND_DELTA_POINTS -> ProfileTrend.IMPROVING
            delta <= -TREND_DELTA_POINTS -> ProfileTrend.DECLINING
            else -> ProfileTrend.STEADY
        }
    }

    /**
     * The longest series of block scores that are genuinely COMPARABLE with one another, oldest first.
     *
     * Comparability is the whole difficulty here. Each exercise is adapted on its OWN governing metric, so scores
     * from different exercises are different measurements on different scales and pooling them would manufacture
     * both a stability number and a direction out of nothing but the order the user happened to pick exercises in.
     * The series is therefore taken from a SINGLE exercise — the one with the most scored sessions.
     *
     * `normalizedScore` is nullable and null on rows written before it existed, which is exactly what makes this
     * safe: those rows are dropped instead of entering the series as a measured zero and dragging every statistic
     * toward a value nobody ever scored.
     */
    private fun comparableScoreSeries(completed: List<EmotionSessionSummaryDto>): List<Double> =
        completed
            .filter { it.normalizedScore != null }
            .groupBy { it.exerciseId }
            .values
            .maxByOrNull { it.size }
            ?.sortedBy { it.startedAtMillis }
            ?.mapNotNull { it.normalizedScore?.toDouble() }
            .orEmpty()

    /**
     * Ola 4 — the transfer claim is now DERIVED from the held-out probes instead of asserted as a constant.
     *
     * The value is unchanged in practice today ([TransferConfidence.NOT_DEMONSTRATED]), and that is the point:
     * it holds because the evidence says so, checked every time, rather than because a literal says so. It is a
     * CEILING — [EmotionHoldoutEvidence.maxTransferConfidence] can only withhold a stronger claim, never grant
     * one — and it is what gives the persisted probes a live reader, so a probe that stops being served or
     * stops being recorded shows up here instead of passing unnoticed.
     */
    private fun transferConfidenceOf(completed: List<EmotionSessionSummaryDto>): TransferConfidence {
        val probed = completed
            .sortedBy { it.startedAtMillis }
            .mapNotNull { summary ->
                summary.holdoutProbe?.let {
                    HoldoutSessionEvidence(
                        probe = HoldoutProbeOutcome(correct = it.correct, total = it.total),
                        trainedAccuracy = summary.accuracy,
                    )
                }
            }
        return EmotionHoldoutEvidence.maxTransferConfidence(
            separation = EmotionHoldoutEvidence.separation(probed.map { it.probe }),
            practiceEffectRisk = EmotionHoldoutEvidence.practiceEffectRisk(probed),
        )
    }

    /**
     * Ola 1 · N1 — the cross-session calibration reading, and the point at which `CALIBRATION_ERROR` stops being a
     * metric nothing can produce. It is produced HERE, at the aggregate, and deliberately not per block: a single
     * block cannot carry the [CalibrationKernel.judgmentsRequired] judgments the reading needs, so a per-block
     * version would either be permanently absent or would have to lower the bar to say something. EM4 and EM14,
     * which govern on it per block, therefore stay correctly unwired.
     *
     * Returns [CalibrationReading.Absent] with the real counts until the floor is met — never a Brier computed from
     * a handful of judgments and presented as a reading.
     */
    fun calibrationOf(summaries: List<EmotionSessionSummaryDto>): CalibrationReading =
        CalibrationKernel.read(EmotionCalibrationSummaries.judgmentsOf(interpretableRows(summaries)))

    /** Ola 1 · N1 — the cross-session block-prediction reading over the same interpretable rows. */
    fun blockForecastsOf(summaries: List<EmotionSessionSummaryDto>): BlockPredictionReader.Reading =
        BlockPredictionReader.read(EmotionCalibrationSummaries.forecastsOf(interpretableRows(summaries)))

    /**
     * Rows whose contents can be believed, which is a DIFFERENT question from [EmotionTerminalSemantics.
     * isValidForAggregate] and the difference is deliberate.
     *
     * The training aggregate asks "did this session measure enough training to move a calibration tier?", so it
     * demands a real completion and 120s. A confidence judgment is not a training statistic: the user made that
     * metacognitive act, and it stayed true, whether or not the session later ran long enough or was stopped for
     * safety. Requiring completion here would silently discard the evidence of exactly the users who stop most.
     *
     * What IS required is that the row be interpretable and not gamed: a reliable identity, a coherent terminal
     * (so a contradictory row cannot contribute), and no anomaly flag — an impossibly-fast session is a gaming
     * signal, and its self-ratings have no more claim to being honest than its response times.
     */
    private fun interpretableRows(summaries: List<EmotionSessionSummaryDto>): List<EmotionSessionSummaryDto> =
        summaries.filter {
            EmotionTerminalSemantics.hasReliableSessionIdentity(it) &&
                EmotionTerminalSemantics.targetStatusOrNull(it) != null &&
                it.anomalyFlags.isEmpty()
        }

    /**
     * Safety PRESSURE (not calibration): the total safety-event count over the most recent up-to-[RECENT_SAFETY_WINDOW]
     * summaries that have a RELIABLE identity AND a COHERENT terminal ([EmotionTerminalSemantics.targetStatusOrNull]
     * != null) — INCLUDING SAFETY_STOP sessions (a safety stop IS safety pressure). It deliberately does NOT require
     * `countsForAggregate`, so a safety stop still contributes the load it caused; but it DOES exclude
     * corrupt/invalid-identity AND contradictory/unknown-terminal rows, so a corrupt row can never inflate safety.
     * Normalized by [SAFETY_LOAD_NORM] and capped at 1.0.
     */
    fun safetyLoadOf(summaries: List<EmotionSessionSummaryDto>): Double {
        val recentReliable = summaries
            .filter {
                EmotionTerminalSemantics.hasReliableSessionIdentity(it) &&
                    EmotionTerminalSemantics.targetStatusOrNull(it) != null
            }
            .sortedByDescending { it.startedAtMillis }
            .take(RECENT_SAFETY_WINDOW)
        val recentSafety = recentReliable.sumOf { it.safetyEvents.size }
        return (recentSafety.toDouble() / SAFETY_LOAD_NORM).coerceAtMost(1.0)
    }

    /** Coarse calibration tier from the number of VALID completed sessions (MC8n/MC8r · PRD: STABLE at 5). */
    fun tierFor(sessions: Int): ProfileTier = when {
        sessions <= 1 -> ProfileTier.EXPLORATORY
        sessions <= 3 -> ProfileTier.PRELIMINARY
        sessions <= 4 -> ProfileTier.INITIAL_TREND
        else -> ProfileTier.STABLE
    }
}
