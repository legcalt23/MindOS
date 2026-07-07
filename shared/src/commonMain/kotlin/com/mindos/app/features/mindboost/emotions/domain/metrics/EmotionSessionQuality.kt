@file:Suppress("MaxLineLength") // Doc/const lines read best whole.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey

/**
 * The per-session quality signal (MC8u-B+ §B/§E). Distinct from the aggregate: it says whether THIS session produced
 * a trustworthy governing reading and whether it shows an extreme timing anomaly. It is the runtime bridge between the
 * anti-gaming guard and CALIBRATION eligibility — a completed, long-enough session that is low-confidence or anomalous
 * must NOT count toward the 5-valid calibration (that would let a gamed run unlock the official range).
 *
 * It never accuses the user: it only lowers the session's validity/confidence. `governingConfident` mirrors the
 * governing metric's own confidence (already fail-closed by the guard); [anomalyFlags] carries human-neutral reason
 * codes for observability (analytics/coach), never a person-level label.
 */
data class EmotionSessionQuality(
    val governingConfident: Boolean,
    val anomalyFlags: List<String>,
)

/** Pure evaluator: derives [EmotionSessionQuality] from the classified trials and the computed block metrics. */
object EmotionSessionQualityEvaluator {

    /** The governing metric was not confident (under-powered or gamed): the session is not trustworthy calibration. */
    const val ANOMALY_LOW_CONFIDENCE = "low_confidence"

    /** A large share of responses is physiologically impossible: an extreme timing anomaly, blocking for calibration. */
    const val ANOMALY_IMPOSSIBLE_TIMING = "impossible_timing"

    fun evaluate(
        classified: List<ClassifiedEmotionTrial>,
        metrics: EmotionMetricSet,
        governingKey: EmotionMetricKey,
    ): EmotionSessionQuality {
        val governingConfident = metrics[governingKey]?.confident == true
        val rts = classified
            .filter { it.trial.responseKey != EmotionResponseKey.NONE }
            .mapNotNull { it.trial.latencyMs?.toDouble() }
        val flags = buildList {
            if (!governingConfident) add(ANOMALY_LOW_CONFIDENCE)
            if (EmotionGamingGuard.impossibleRtShare(rts) > EmotionGamingGuard.MAX_IMPOSSIBLE_SHARE) {
                add(ANOMALY_IMPOSSIBLE_TIMING)
            }
        }
        return EmotionSessionQuality(governingConfident, flags)
    }
}
