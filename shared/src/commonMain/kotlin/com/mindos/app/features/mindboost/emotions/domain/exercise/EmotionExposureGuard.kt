@file:Suppress("MaxLineLength") // Guard KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.domain.exercise

/**
 * MC8u-D §5 — cross-session exposure/repetition guard for the EM3/EM10 content bank. Because a valid session
 * advances calibration by COUNT (not by the item score), memorization never inflates the calibration COUNT — but
 * it CAN inflate the DISPLAYED 0–100 block score through false familiarity, so repeated sessions must not simply
 * replay the same trials. This pure guard deprioritizes recently-seen phrases so item selection prefers FRESH
 * content, only reusing seen items when there is not enough fresh content to fill the block (graceful fallback).
 *
 * It is deterministic: it applies a STABLE fresh-first partition to an already-seeded, already-shuffled pool, so
 * the same (seed, recent) inputs always yield the same selection (no new randomness). Difficulty/ambiguity shaping
 * is unchanged — the guard only reorders eligibility within each pool before the quota take.
 */
internal object EmotionExposureGuard {

    /**
     * Returns [pool] with items whose phrase is in [recent] moved AFTER the fresh items, preserving the relative
     * order within each group (a stable partition). With an empty [recent] the pool is returned unchanged, so
     * behavior is identical to pre-guard selection until real exposure history is supplied.
     */
    fun freshFirst(pool: List<EmotionContent.SeparatorItem>, recent: Set<String>): List<EmotionContent.SeparatorItem> {
        if (recent.isEmpty()) return pool
        val (seen, fresh) = pool.partition { it.phrase in recent }
        return fresh + seen
    }
}
