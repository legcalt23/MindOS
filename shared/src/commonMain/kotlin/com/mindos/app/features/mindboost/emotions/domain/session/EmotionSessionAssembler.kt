package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionCapacity
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.metrics.ClassifiedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial

/**
 * The SOLE classification authority for a block: it classifies every recorded trial (never the live UI
 * counters), delegates numeric metrics to the [EmotionMetricsEngine], and builds the EM3 confusion
 * matrix. Live holder counters are display-only and must never feed metrics.
 */
class EmotionSessionAssembler(private val metricsEngine: EmotionMetricsEngine) {

    fun assembleBlock(
        trials: List<RecordedEmotionTrial>,
        definition: EmotionExerciseDefinition,
    ): EmotionBlockResult {
        val classified = trials.map { ClassifiedEmotionTrial(it, EmotionTrialClassifier.classify(it)) }
        val metrics = metricsEngine.blockMetrics(classified)
        val confusion = if (definition.capacity == EmotionCapacity.EMOTION_THOUGHT_SEPARATION) {
            confusionMatrix(classified)
        } else {
            null
        }
        return EmotionBlockResult(
            exerciseId = definition.id,
            outcomes = classified.map { it.outcome },
            metrics = metrics,
            confusion = confusion,
        )
    }

    private fun confusionMatrix(classified: List<ClassifiedEmotionTrial>): EmotionConfusionMatrix {
        val counts = LinkedHashMap<EmotionCategory, MutableMap<EmotionCategory, Int>>()
        EmotionCategory.entries.forEach { truth -> counts[truth] = linkedMapOf() }
        classified.forEach { item ->
            val payload = item.trial.stimulus.payload
            val chosen = item.trial.chosenCategory
            if (payload is EmotionStimulusPayload.Classification && chosen != null) {
                val row = counts.getValue(payload.truth)
                row[chosen] = (row[chosen] ?: 0) + 1
            }
        }
        return EmotionConfusionMatrix(counts.mapValues { (_, row) -> row.toMap() })
    }
}
