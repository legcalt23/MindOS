package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome

/** 4×4 confusion matrix for EM3 (truth → chosen → count). The assembler owns it as scoring authority. */
data class EmotionConfusionMatrix(val counts: Map<EmotionCategory, Map<EmotionCategory, Int>>) {
    fun correct(): Int = counts.entries.sumOf { (truth, row) -> row[truth] ?: 0 }
    fun total(): Int = counts.values.sumOf { row -> row.values.sum() }
}

/** The assembled result of one block: ordered outcomes, computed metrics, and (EM3) confusion matrix. */
data class EmotionBlockResult(
    val exerciseId: EmotionExerciseId,
    val outcomes: List<EmotionTrialOutcome>,
    val metrics: EmotionMetricSet,
    val confusion: EmotionConfusionMatrix?,
)
