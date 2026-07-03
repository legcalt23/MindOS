package com.mindos.app.features.mindboost.emotions.domain.model

/**
 * Logical response keys for the emotion exercises. CATEGORY_A..D are the four fixed Separador (EM3)
 * buttons — distinct keys (no "PRIMARY + index" fallback). NONE = withhold / timeout.
 */
enum class EmotionResponseKey {
    PRIMARY,
    LEFT,
    RIGHT,
    CATEGORY_A,
    CATEGORY_B,
    CATEGORY_C,
    CATEGORY_D,
    NONE,
    ;

    /** Two-choice directional answers (EM2): scored by WHICH side was chosen. */
    val isDirectional: Boolean get() = this == LEFT || this == RIGHT

    /** Four-category classification answers (EM3). */
    val isCategory: Boolean get() = this in CATEGORY_KEYS

    companion object {
        private val CATEGORY_KEYS = setOf(CATEGORY_A, CATEGORY_B, CATEGORY_C, CATEGORY_D)
    }
}

/**
 * The four separation categories trained by EM3. The renderer keeps a FIXED quadrant→category mapping
 * for the whole session; the recorded trial carries the resolved [EmotionCategory] directly.
 */
enum class EmotionCategory { INTERNAL_STATE, THOUGHT, IMPULSE, FACT }

/**
 * Outcome of classifying a recorded trial. The [com.mindos.app.features.mindboost.emotions] session
 * assembler is the SOLE classification authority; live UI counters are display-only.
 */
enum class EmotionTrialOutcome {
    HIT,
    MISS,
    FALSE_ALARM,
    CORRECT_REJECTION,
    CORRECT,
    INCORRECT,
    NO_RESPONSE,
}

/**
 * Everything the assembler needs to classify a trial — captured at response time. Carries the chosen
 * side/category and latency so classification never depends on live UI state.
 *
 * @param latencyMs null when no response landed within the window.
 * @param chosenCategory non-null only for EM3 classification trials.
 */
data class RecordedEmotionTrial(
    val stimulus: EmotionStimulus,
    val latencyMs: Long?,
    val responseKey: EmotionResponseKey,
    val chosenCategory: EmotionCategory? = null,
)
