@file:Suppress("MagicNumber") // The detection thresholds ARE the tuning data of this pure guard.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import kotlin.math.abs

/**
 * Minimal, honest validity guard (MC8u-B §1E). Given a block's classified trials, it decides whether the
 * PRIMARY accuracy-family score (ACCURACY / CATEGORY_ACCURACY / COMMISSION_RATE) can be TRUSTED, or whether the
 * block shows a coarse gaming / spam pattern that would otherwise fabricate a high score. It NEVER accuses the
 * user and never lowers a real value — it only flips the metric's `confident` flag to false (fail-closed), exactly
 * like an under-powered sample, so the UI shows "insufficient data" rather than a fake reading.
 *
 * Three coarse patterns are detected:
 *  - impossible reaction times (a large share of responses faster than [IMPOSSIBLE_RT_MS] — physiologically
 *    impossible, i.e. not a real decision);
 *  - same-answer spam (an identical response-key streak longer than [MAX_SAME_KEY_STREAK] over RESPONDED trials —
 *    "always tap the same button"); this is only meaningful for multi-alternative paradigms;
 *  - random-like fast guessing (accuracy within [NEAR_CHANCE_MARGIN] of the task's chance level AND a very low
 *    median response time — guessing quickly, not discriminating).
 *
 * Deliberately conservative: with fewer than [MIN_RESPONSES_FOR_GUARD] real responses it returns false (the
 * ordinary sample-size gates already withhold confidence), so it never punishes a small honest block.
 */
object EmotionGamingGuard {

    const val IMPOSSIBLE_RT_MS = 150.0
    const val MAX_IMPOSSIBLE_SHARE = 0.30
    const val MAX_SAME_KEY_STREAK = 8
    const val NEAR_CHANCE_MARGIN = 0.12
    const val FAST_GUESS_RT_MS = 250.0
    private const val MIN_RESPONSES_FOR_GUARD = 6

    /** Share of [rts] that are physiologically impossible (faster than [IMPOSSIBLE_RT_MS]); 0.0 for an empty list. */
    fun impossibleRtShare(rts: List<Double>): Double =
        if (rts.isEmpty()) 0.0 else rts.count { it < IMPOSSIBLE_RT_MS }.toDouble() / rts.size

    /** Longest run of an identical key in [keys] (order-sensitive); 0 for an empty list. */
    fun longestSameKeyStreak(keys: List<EmotionResponseKey>): Int {
        var best = 0
        var run = 0
        var previous: EmotionResponseKey? = null
        for (key in keys) {
            run = if (key == previous) run + 1 else 1
            previous = key
            if (run > best) best = run
        }
        return best
    }

    /**
     * True when the block looks gamed/spammed and its accuracy-family score must NOT be trusted. [chanceLevel] is
     * the task's chance accuracy (0.5 two-alt, 0.25 four-category) or null for go/no-go (which has no chance
     * accuracy — the hit-rate floor handles its "never respond" gaming instead). [checkSameKey] is true only for
     * multi-alternative paradigms (a single-tap go/no-go always uses one key, so a streak would be meaningless).
     */
    fun looksGamed(
        classified: List<ClassifiedEmotionTrial>,
        chanceLevel: Double?,
        checkSameKey: Boolean,
    ): Boolean {
        val responded = classified.filter { it.trial.responseKey != EmotionResponseKey.NONE }
        if (responded.size < MIN_RESPONSES_FOR_GUARD) return false
        val rts = responded.mapNotNull { it.trial.latencyMs?.toDouble() }
        val impossible = impossibleRtShare(rts) > MAX_IMPOSSIBLE_SHARE
        val spam = checkSameKey && longestSameKeyStreak(responded.map { it.trial.responseKey }) > MAX_SAME_KEY_STREAK
        return impossible || spam || randomLikeFastGuessing(classified, rts, chanceLevel)
    }

    private fun randomLikeFastGuessing(
        classified: List<ClassifiedEmotionTrial>,
        rts: List<Double>,
        chanceLevel: Double?,
    ): Boolean {
        if (chanceLevel == null || rts.isEmpty() || classified.isEmpty()) return false
        val accuracy = classified.count { it.outcome == EmotionTrialOutcome.CORRECT }.toDouble() / classified.size
        return abs(accuracy - chanceLevel) <= NEAR_CHANCE_MARGIN && EmotionRtStatistics.median(rts) <= FAST_GUESS_RT_MS
    }
}
