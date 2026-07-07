@file:Suppress("MagicNumber", "MaxLineLength") // Fixture values are the data under test.

package com.mindos.app.features.mindboost.emotions.data.dto

import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u-B+ §D — the persisted Emociones session contract. Verifies the durable [EmotionSessionSummaryDto] covers the
 * APPLICABLE session-data fields (identity, durationActual, difficultyEnd, accuracy/RT via metricsSummary, quality,
 * versions, terminal, calibration eligibility). Fields that are N/A BY CHARTER are documented, not faked:
 *  - rawScore / normalizedScore / scoreDelta / xpEarned: Emociones has NO per-session cognitive score (the "loaded gun"
 *    was removed in MC8u-A); calibration progress (valid-session count) is the only progression signal.
 *  - platform / syncStatus: local-only; no remote sync exists yet (documented P2 debt).
 *  - blockId / durationSelected / maxLevelReached / explicit answer counts: not modeled in v1 (documented P2 debt).
 */
class EmotionSessionContractTest {

    private fun summary() = EmotionSessionSummaryDto(
        exerciseId = "EM3_SEPARADOR",
        startedAtMillis = 1_000,
        durationMs = 130_000,
        difficultySummary = DifficultySummaryDto("CATEGORY_AMBIGUITY", 0.6, 2),
        metricsSummary = listOf(
            MetricSummaryDto(EmotionMetricKey.CATEGORY_ACCURACY.name, 0.85, 20, true, "PROPORTION"),
            MetricSummaryDto(EmotionMetricKey.RT_MEDIAN.name, 640.0, 18, true, "MILLISECONDS"),
            MetricSummaryDto(EmotionMetricKey.RT_CV.name, 0.22, 18, true, "RATIO"),
        ),
        completed = true,
        abandoned = false,
        sessionId = "sess_contract1",
        endReason = EmotionTerminalSemantics.COMPLETED,
        governingConfident = true,
        anomalyFlags = emptyList(),
        scoringVersion = EmotionScoringVersions.SCORING,
        exerciseVersion = EmotionScoringVersions.EXERCISE,
    )

    @Test
    fun applicable_session_contract_fields_are_present() {
        val s = summary()
        assertTrue(s.sessionId != null, "sessionId (identity)")
        assertTrue(s.exerciseId.isNotBlank(), "exerciseId")
        assertTrue(s.durationMs > 0, "durationActual")
        assertTrue(s.difficultySummary.level in 0.0..1.0, "difficultyEnd")
        assertTrue(s.difficultySummary.intensity in 1..5, "intensity")
        assertTrue(s.endReason != null, "terminal reason (completed/abandoned/safety)")
        assertTrue(s.scoringVersion >= EmotionScoringVersions.SCORING, "scoringVersion")
        assertTrue(s.exerciseVersion >= EmotionScoringVersions.EXERCISE, "exerciseVersion")
    }

    @Test
    fun accuracy_and_reaction_time_are_recoverable_from_metrics_summary() {
        val metrics = summary().metricsSummary.associateBy { it.key }
        assertTrue(metrics.containsKey(EmotionMetricKey.CATEGORY_ACCURACY.name), "accuracy is persisted")
        assertTrue(metrics.containsKey(EmotionMetricKey.RT_MEDIAN.name), "reaction time is persisted")
        assertTrue(metrics.containsKey(EmotionMetricKey.RT_CV.name), "reaction-time variability is persisted")
    }

    @Test
    fun quality_signals_gate_calibration_eligibility() {
        val valid = summary()
        assertTrue(EmotionTerminalSemantics.isValidForAggregate(valid), "a confident, non-anomalous completed session is eligible")
        val gamed = valid.copy(governingConfident = false)
        assertTrue(!EmotionTerminalSemantics.isValidForAggregate(gamed), "a low-confidence session must be ineligible")
    }

    // MC8u-C §3: explicit contract fields are first-class on the DTO, not silently re-derived from metricsSummary.
    @Test
    fun explicit_contract_fields_are_first_class_on_the_summary() {
        val s = summary().copy(
            difficultyStart = 0.3, totalAnswers = 20, correctAnswers = 17, incorrectAnswers = 2, omissions = 1,
            accuracy = 0.85, averageReactionTimeMs = 640.0, bestReactionTimeMs = 410.0, worstReactionTimeMs = 1200.0,
            reactionTimeVariability = 0.22, normalizedScore = 85,
        )
        assertTrue(s.totalAnswers == 20 && s.correctAnswers == 17 && s.omissions == 1)
        assertTrue(s.accuracy > 0.0 && s.bestReactionTimeMs > 0.0 && s.worstReactionTimeMs >= s.bestReactionTimeMs)
        assertTrue(s.difficultyStart in 0.0..1.0)
        assertTrue(s.normalizedScore == 85, "the 0–100 training-performance block score is persisted")
    }

    // MC8u-C §3: fields the PRD lists that are N/A for Emociones are documented IN CODE, never faked.
    @Test
    fun not_applicable_fields_are_documented_by_charter() {
        val na = EmotionSessionContract.NOT_APPLICABLE_BY_CHARTER
        listOf("rawScore", "scoreDelta", "xpEarned", "durationSelected", "blockId", "maxLevelReached", "platform", "syncStatus")
            .forEach { assertTrue(it in na, "$it must be documented NotApplicableByCharter, not silently omitted") }
    }
}
