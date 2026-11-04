package com.mindos.app.features.mindboost.emotions.domain.model

/**
 * Render-agnostic payload of a stimulus, sealed by paradigm family. Render is the UI layer's concern;
 * the domain only carries the logical content. The three v1 paradigms are honest by design: the
 * correct answer never depends on the affective content.
 */
sealed interface EmotionStimulusPayload {

    /**
     * EM1 Pausa (affective go/no-go, and from Ola 3 · R1 the affective STOP-SIGNAL). The outcome is decided
     * ONLY by the frame, never by the affective content: [affectiveWord] is a mild distractor and [congruent]
     * is whether its tone matches the frame (it drives the affective-interference-cost secondary metric).
     *
     * Two shapes share this payload because they share a renderer, a classifier and a metric family:
     *  - **Decided at onset** ([stopAfterMs] null): open = GO, closed = NO-GO. The classic go/no-go.
     *  - **Stop-signal** ([stopAfterMs] set): the frame opens — so the trial STARTS as a go and the prepotent
     *    response is already under way — and closes [stopAfterMs] later. The correct answer is then to abort.
     *    The delay is decided ONLINE by the staircase, never at generation, so a generator leaves it null and
     *    the runtime stamps the value it actually presented. That stamped value is what the SSRT estimate
     *    integrates over, which is why it travels on the payload rather than living in the holder.
     *
     * [negativeWord] is the tone CELL of the affective contrast, kept as its own fact rather than re-derived
     * from `frameOpen != congruent`: that identity holds today, and a difference score whose cell assignment
     * depends on a double negative is one refactor away from a silent sign error.
     */
    data class AffectiveGoNoGo(
        val frameOpen: Boolean,
        val affectiveWord: String,
        val congruent: Boolean,
        val stopAfterMs: Long? = null,
        val negativeWord: Boolean = false,
    ) : EmotionStimulusPayload {
        /**
         * True when the correct response is to WITHHOLD: a frame closed at onset, or a stop signal on the way.
         * Every consumer that used to test `!frameOpen` must test this instead — a stop trial opens its frame,
         * so reading `frameOpen` alone would score a failed inhibition as a hit.
         */
        val requiresWithhold: Boolean get() = !frameOpen || stopAfterMs != null
    }

    /**
     * EM2 Recuperación (2-alternative perceptual). Respond [correct] (LEFT/RIGHT). [discriminability]
     * 1.0 = easy .. 0.0 = near-threshold, resolved ONLINE by the exercise`s accuracy staircase.
     *
     * Ola 3 · R2 deleted the companion `harder` flag. It marked a deliberately planted near-threshold trial, and
     * planting only makes an error LIKELY — the users who never took the bait produced no post-error pairs at all
     * and were shown an unavailable reading. A staircase holds the error RATE at a target instead, so the flag`s
     * last reader disappeared with it; leaving a field that is only ever written is how a dead design survives.
     */
    data class TwoAltPerceptual(
        val correct: EmotionResponseKey,
        val discriminability: Double,
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
