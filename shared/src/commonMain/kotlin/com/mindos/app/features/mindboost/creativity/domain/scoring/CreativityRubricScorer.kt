package com.mindos.app.features.mindboost.creativity.domain.scoring

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseDefinition
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityResponse
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityRubricResult

/**
 * Deterministic, local, non-clinical rubric over a (validated) response.
 *
 * Produces practice telemetry only — `fluency` (distinct SUBSTANTIVE-idea count, so single-character
 * gibberish never inflates it) and `elaboration` (average words per idea) proxies framed against the
 * user's own session. Never a diagnosis, a graded or clinical score. No AI:
 * [CreativityRubricResult.aiDerived] stays false.
 */
object CreativityRubricScorer {

    const val RUBRIC_VERSION = 1
    const val SIGNAL_FLUENCY = "fluency"
    const val SIGNAL_ELABORATION = "elaboration"

    /**
     * Minimum characters for an idea to count as substantive. A single character ("a", "b", …) is not a
     * creative idea, so it never counts toward fluency — this closes the trivial "a,b,c,d,e" gaming.
     */
    const val MIN_IDEA_CHARS = 2

    fun score(
        definition: CreativityExerciseDefinition,
        response: CreativityResponse,
    ): CreativityRubricResult {
        val ideas = response.normalizedIdeas.distinct().filter { it.length >= MIN_IDEA_CHARS }
        val fluency = ideas.size.toDouble()
        val elaboration = if (ideas.isEmpty()) {
            0.0
        } else {
            ideas.sumOf { wordCount(it) }.toDouble() / ideas.size
        }
        return CreativityRubricResult(
            rubricVersion = RUBRIC_VERSION,
            signals = mapOf(SIGNAL_FLUENCY to fluency, SIGNAL_ELABORATION to elaboration),
            summary = labelFor(ideas.size, definition),
        )
    }

    private fun labelFor(
        distinctIdeas: Int,
        definition: CreativityExerciseDefinition,
    ): CreativityPracticeLabel {
        val midpoint = (definition.minIdeas + definition.targetIdeas) / 2
        return when {
            distinctIdeas >= definition.targetIdeas -> CreativityPracticeLabel.STRONG_LOCAL_SESSION
            distinctIdeas >= midpoint -> CreativityPracticeLabel.STEADY_PRACTICE
            else -> CreativityPracticeLabel.KEEP_PRACTICING
        }
    }

    private fun wordCount(idea: String): Int = idea.split(WHITESPACE).count { it.isNotBlank() }

    private val WHITESPACE = Regex("\\s+")
}
