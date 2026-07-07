@file:Suppress("MagicNumber", "MaxLineLength") // 0.0/1.0 bounds are the formula; KDoc/signatures read best whole.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure, deterministic difficulty shaping shared by EM3 (Separador) and EM10 (Separación bajo presión). Difficulty
 * changes the ACTUAL trials (MC8u-B §2), not a decorative number:
 *  - [select] draws more deliberately-confusable (`ambiguity == 1`) items as the difficulty [level] rises (semantic
 *    ambiguity / distractor similarity), always returning DISTINCT training items;
 *  - [window] tightens the response window (temporal pressure);
 *  - [interStimulus] shortens the reset gap between items (less recovery time).
 * All content stays within the clean, leve-only bank — difficulty NEVER adds clinical or high-intensity material.
 */
internal object SeparatorDifficulty {

    /**
     * Selects [target] DISTINCT training items, weighting toward ambiguous items proportionally to [level] (0 clear
     * only .. 1 as many ambiguous as available). Deterministic for a fixed [rng]. MC8u-D §5: within each pool, items
     * whose phrase is in [recent] are deprioritized (drawn only after fresh items) so repeated sessions draw fresher
     * content; when a pool lacks enough fresh items it gracefully falls back to seen ones. Always returns
     * min(target, available) distinct items — the exposure guard never shrinks the block.
     */
    fun select(
        training: List<EmotionContent.SeparatorItem>,
        target: Int,
        level: Double,
        rng: Random,
        recent: Set<String> = emptySet(),
    ): List<EmotionContent.SeparatorItem> {
        val want = target.coerceAtMost(training.size)
        val ambiguous = EmotionExposureGuard.freshFirst(training.filter { it.ambiguity == 1 }.shuffled(rng), recent)
        val clear = EmotionExposureGuard.freshFirst(training.filter { it.ambiguity == 0 }.shuffled(rng), recent)
        val ambiguousQuota = (want * level).roundToInt().coerceIn(0, minOf(ambiguous.size, want))
        val pickedAmbiguous = ambiguous.take(ambiguousQuota)
        val pickedClear = clear.take(want - pickedAmbiguous.size)
        val combined = pickedAmbiguous + pickedClear
        val filled = if (combined.size < want) {
            val used = combined.toSet()
            (combined + EmotionExposureGuard.freshFirst(training.filter { it !in used }.shuffled(rng), recent)).take(want)
        } else {
            combined
        }
        return filled.shuffled(rng)
    }

    /** Number of ambiguous (`ambiguity == 1`) items in [items] — used by tests to assert difficulty raises ambiguity. */
    fun ambiguousCount(items: List<EmotionContent.SeparatorItem>): Int = items.count { it.ambiguity == 1 }

    /** Response window in ms, tightened by [level]. [basePressure] applies even at level 0 (e.g. EM10's constant pressure). */
    fun window(baseMs: Long, level: Double, basePressure: Double, span: Double, minMs: Long): Long =
        (baseMs * (1.0 - basePressure - span * level)).toLong().coerceAtLeast(minMs)

    /** Inter-stimulus gap in ms, shortened by [level]. */
    fun interStimulus(baseMs: Long, level: Double, span: Double, minMs: Long): Long =
        (baseMs * (1.0 - span * level)).toLong().coerceAtLeast(minMs)
}
