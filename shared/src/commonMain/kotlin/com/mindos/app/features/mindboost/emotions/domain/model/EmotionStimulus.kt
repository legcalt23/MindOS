package com.mindos.app.features.mindboost.emotions.domain.model

/**
 * Render-agnostic payload of a stimulus, sealed by paradigm family. Render is the UI layer's concern;
 * the domain only carries the logical content. The three v1 paradigms are honest by design: the
 * correct answer never depends on the affective content.
 */
sealed interface EmotionStimulusPayload {

    /**
     * EM1 Pausa (affective go/no-go). The GO/NO-GO outcome is decided ONLY by [frameOpen]
     * (open = GO, closed = NO-GO — the rule). [affectiveWord] is a mild distractor; [congruent] is
     * whether its tone matches the frame (drives the affective-interference-cost secondary metric).
     * The tone is NEVER the answer.
     */
    data class AffectiveGoNoGo(
        val frameOpen: Boolean,
        val affectiveWord: String,
        val congruent: Boolean,
    ) : EmotionStimulusPayload

    /**
     * EM2 Recuperación (2-alternative perceptual). Respond [correct] (LEFT/RIGHT). [discriminability]
     * 1.0 = easy .. 0.0 = near-threshold; [harder] flags a deliberately planted near-threshold
     * "error-likely" trial used to guarantee measurable post-error sequences without unfair induction.
     */
    data class TwoAltPerceptual(
        val correct: EmotionResponseKey,
        val discriminability: Double,
        val harder: Boolean,
    ) : EmotionStimulusPayload

    /**
     * EM3 Separador (4-category classification). Classify the neutral [phrase] as its [truth] category.
     * [isEval] marks a held-out evaluation item (kept disjoint from training items for the holdout
     * scaffold). The phrase is scanner-clean and neutral (no sensitive or intimate content).
     */
    data class Classification(
        val phrase: String,
        val truth: EmotionCategory,
        val isEval: Boolean,
    ) : EmotionStimulusPayload
}

/**
 * A single stimulus in a block's generated sequence. Generators are deterministic (seeded); timing
 * fields are stamped by the runtime at presentation, not by the generator.
 */
data class EmotionStimulus(
    val index: Int,
    val correctResponseKey: EmotionResponseKey?,
    val payload: EmotionStimulusPayload,
    /** Inter-stimulus interval before this stimulus is shown (ms). */
    val interStimulusDelayMs: Long,
    /** Response window length once shown (ms). */
    val responseWindowMs: Long,
)
