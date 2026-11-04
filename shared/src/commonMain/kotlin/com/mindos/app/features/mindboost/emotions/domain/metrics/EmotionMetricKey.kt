package com.mindos.app.features.mindboost.emotions.domain.metrics

/**
 * Catalogue of every metric the emotions module can compute. Emotions OWNS this enum — it must NOT
 * import focus's `MetricKey` (a closed enum with no emotion keys; a compile test pins the absence of
 * that import). Honest by construction: every [EmotionMetricValue] reports its sample size and a
 * confidence flag so the UI never presents an under-powered estimate as a hard fact.
 */
enum class EmotionMetricKey {
    POST_ERROR_RECOVERY,
    COMMISSION_RATE,
    PAUSE_BEFORE_ACTION_RATE,
    CATEGORY_ACCURACY,

    /**
     * Ola 2 · R3 · P3. The WEAKEST of the four one-vs-rest d' values for a classification block, so the reading
     * covers all four categories and charges false alarms. [CATEGORY_COVERAGE] and [STATE_THOUGHT_ACCURACY]
     * below kept their arithmetic but were renamed to what they actually measure: coverage, and accuracy over
     * two of the four categories.
     */
    CATEGORY_DPRIME_MIN,
    CATEGORY_COVERAGE,
    STATE_THOUGHT_ACCURACY,

    /**
     * Ola 3 · R1. Stop-signal reaction time in ms — how long the brake takes, estimated by integration over a
     * staircase held near p(respond|signal) = 0.5. It is the PRIMARY reading of the fused inhibition exercise
     * and deliberately NOT its governing metric: it is a latency, so the zone machinery (which compares against
     * thresholds expressed as goodness in [0,1]) cannot read it without inventing a comparison, and
     * [com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionBlockScore] has no honest 0-100 mapping
     * for it. Its direction is INFORMATIONAL_ONLY for exactly that reason.
     */
    SSRT,

    /**
     * Ola 3 · R1. SSRT after a negative word MINUS SSRT after a neutral one, in ms: whether affective load
     * degrades the brake. A difference score, so it is judged on its SMALLER cell and withheld entirely below
     * the floor — a delta over five stop trials is noise wearing the name of an affective effect.
     */
    AFFECTIVE_SSRT_DELTA,
    AFFECTIVE_INTERFERENCE_COST,
    CALIBRATION_ERROR,
    FRUSTRATION_TOLERANCE_INDEX,
    AFFECTIVE_DECISION_QUALITY,
    SAFE_PERSISTENCE_INDEX,
    REGULATION_STABILITY,
    ACCURACY,
    // MC8u-B §1A: the GO-trial hit rate for a go/no-go block. Its floor gates COMMISSION_RATE confidence so a
    // "never respond" strategy (commission = 0) can never look like perfect inhibition, and blocks difficulty step-up.
    HIT_RATE,
    RT_MEDIAN,
    RT_CV,
    FALSE_ALARMS,
    OMISSIONS,
    ANTICIPATIONS,
    /**
     * Ola 3 · R2. Accuracy on the trials that FOLLOW an error — the primary reading of the fused recovery
     * exercise, and the one it is governed on. A proportion, so unlike R1's SSRT it can be ranked by the zone
     * machinery and mapped to a block score honestly. It is confident only when [POST_CORRECT_ACCURACY] is too:
     * post-error accuracy without the baseline it is supposed to be compared against is a number with no
     * question attached.
     */
    POST_ERROR_ACCURACY,

    /**
     * Ola 3 · R2. Accuracy on the trials that follow a CORRECT one — the baseline [POST_ERROR_ACCURACY] is read
     * against.
     */
    POST_CORRECT_ACCURACY,

    /**
     * Ola 3 · R2. Whether an error changes HOW the user answers, not just how well: 1 minus the gap between the
     * rate of switching sides after an error and after a correct answer. In a task whose correct side is drawn at
     * random, a user with no post-error strategy shift switches equally often either way, so 1.0 means the error
     * left their approach intact and low values mean it did not.
     */
    STRATEGY_PERSISTENCE,

    /**
     * Ola 3 · R2. Share of post-error trials left unanswered — disengaging after a mistake, counted apart from
     * overall [ABANDONMENT].
     */
    POST_ERROR_ABANDONMENT,
    POST_ERROR_SLOWING,
    IMPULSIVITY,
    LAPSES,
    ABANDONMENT,
}

/** The unit an [EmotionMetricValue] is expressed in (drives formatting in the UI). */
enum class EmotionMetricUnit { PROPORTION, MILLISECONDS, RATIO, COUNT, INDEX }

/**
 * A single computed metric plus the metadata that keeps reporting honest.
 *
 * @param value finite numeric value (callers pass a finite number; use [confident] = false when the
 *   underlying sample is too small — never NaN).
 * @param n sample size that supports this value.
 * @param confident false when [n] is below the trust threshold; the UI must then show "datos
 *   insuficientes", never a hard number, and the engine/safety must NOT act on it.
 */
data class EmotionMetricValue(
    val key: EmotionMetricKey,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: EmotionMetricUnit,
) {
    companion object {
        /** A value the system could not compute with confidence (e.g. zero post-error pairs). */
        fun unavailable(key: EmotionMetricKey, unit: EmotionMetricUnit, n: Int = 0): EmotionMetricValue =
            EmotionMetricValue(key = key, value = 0.0, n = n, confident = false, unit = unit)
    }
}

/** A keyed bundle of computed metrics for one block / session. */
typealias EmotionMetricSet = Map<EmotionMetricKey, EmotionMetricValue>

/**
 * Confidence-SAFE accessor: returns the value ONLY when present AND confident, else null. Every
 * adaptive/escalation/safety branch MUST use this (never a "default = 0.0" accessor, which would let
 * an under-powered estimate masquerade as a real low value — the phantom-zero footgun).
 */
fun EmotionMetricSet.valueIfConfident(key: EmotionMetricKey): Double? {
    val metric = this[key] ?: return null
    return if (metric.confident) metric.value else null
}

/** Raw value regardless of confidence, or [default] when absent. For DISPLAY only, never for escalation. */
fun EmotionMetricSet.rawValueOf(key: EmotionMetricKey, default: Double = 0.0): Double =
    this[key]?.value ?: default
