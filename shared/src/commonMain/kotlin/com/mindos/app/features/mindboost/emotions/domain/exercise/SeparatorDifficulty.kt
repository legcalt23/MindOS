@file:Suppress("MagicNumber", "MaxLineLength") // 0.0/1.0 bounds are the formula; KDoc/signatures read best whole.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure, deterministic difficulty shaping for EM3 (Separador). Difficulty changes the ACTUAL trials (MC8u-B §2),
 * not a decorative number:
 *  - [select] draws more deliberately-confusable (`ambiguity == 1`) items as the difficulty [level] rises (semantic
 *    ambiguity / distractor similarity), always returning DISTINCT training items;
 *  - [interStimulus] shortens the reset gap between items (less recovery time).
 * All content stays within the clean, leve-only bank — difficulty NEVER adds clinical or high-intensity material.
 *
 * **Ola 3 · R3 · P2 deleted `window`.** It tightened the response window as `CATEGORY_AMBIGUITY` rose, which put
 * two different manipulations behind one number. Three things were wrong with that at once:
 *  - R3 makes time pressure a MODE (calm against pressure), and a mode the difficulty axis also applies a little
 *    of, continuously, is not a mode;
 *  - the exercise's own risk note says difficulty must rise by semantic boundary and never by anything else;
 *  - the window was blind to how long the phrase was. Measured over the bank, an item is 2 to 7 words, so reading
 *    demand spans 780 ms to 1230 ms — at the tightened 1750 ms window that is 970 ms of spare time for the
 *    shortest item against 520 ms for the longest. Item length was a second difficulty dimension, uncontrolled,
 *    invisible to the engine, and it grew as the engine tightened the window.
 *
 * EM3 now serves the block's calm window unchanged. Its last consumer gone, `window` went with it rather than
 * waiting for the pressure mode: the length-scaled formula that mode needs is a different function, and keeping a
 * dead one warmed up is how a superseded design survives its own replacement.
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

    /**
     * Ola 3 · R3 · P3 — the response window for a trial served in PRESSURE mode, scaled by how much there is to
     * read: [PRESSURE_BASE_MS] plus [PRESSURE_PER_WORD_MS] per word.
     *
     * A constant pressure window would not be a pressure manipulation, it would be a reading-speed one. Items run
     * 2 to 7 words, so a single number gives the shortest phrase almost twice the spare time of the longest, and
     * whichever items a user happened to draw would decide how hard their block was. Scaling by length holds the
     * time-per-word roughly constant, which is what makes "the same pressure" mean the same thing across items.
     *
     * The range over today's bank is 780 ms to 1230 ms, against a calm window of 2500 ms.
     */
    fun pressureWindowMs(phrase: String): Long =
        PRESSURE_BASE_MS + PRESSURE_PER_WORD_MS * phrase.trim().split(' ').count { it.isNotBlank() }

    /** Reading-independent floor of a pressure trial: perceiving the phrase and reaching a button. */
    const val PRESSURE_BASE_MS: Long = 600L

    /** Added per word, so time-per-word is what stays constant across items rather than time-per-trial. */
    const val PRESSURE_PER_WORD_MS: Long = 90L

    /** Inter-stimulus gap in ms, shortened by [level]. */
    fun interStimulus(baseMs: Long, level: Double, span: Double, minMs: Long): Long =
        (baseMs * (1.0 - span * level)).toLong().coerceAtLeast(minMs)
}
