package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.random.Random

/**
 * Ola 4 · P2 — the held-out probe. Draws EM3/EM10 items the user has never been trained on, so "I got better"
 * can be checked against content that practice cannot have memorized.
 *
 * A reserved partition that is never administered proves nothing (`ItemPartition`: a holdout must be SERVED in
 * evaluation, not merely reserved). Until this existed, `isEval` items sat unused and the module had no way to
 * tell practice from progress.
 *
 * Rules this generator enforces so a probe stays interpretable:
 *  - **Balanced.** Exactly [ITEMS_PER_CATEGORY] fresh item per category. Chance is then exactly 1/categories,
 *    which is what the scoring correction assumes; an unbalanced probe would quietly break that assumption.
 *  - **Burned after use.** An item the user has already answered is no longer a holdout item for that user, so
 *    [draw] takes `servedItemIds` and never re-serves. Re-using one would measure memory of the probe.
 *  - **Fail-closed on exhaustion.** When any category has no fresh item left the draw returns [Draw.Exhausted]
 *    rather than a smaller or unbalanced probe. A short probe is not a weaker measurement, it is a different
 *    one, and silently swapping it in is how a number stops meaning what its label says.
 *  - **Deterministic.** Same (servedItemIds, seed) yields the same probe, including the presentation order.
 *
 * Order is shuffled: taking one item per category in declaration order would make the correct targets run
 * A, B, C, D every time, which is learnable without reading a single phrase.
 */
internal object EmotionHoldoutProbe {

    /**
     * Fresh items drawn per category. Equal counts keep chance at exactly 1/categories.
     *
     * Ola 2 · R3 · P2 raised this from 1 to 2. At 1 the probe held four items, which is below
     * `EmotionHoldoutEvidence.MIN_ITEMS_PER_PROBE_FOR_CHANGE`,
     * so every probe was too small to be one endpoint of a comparison and `practiceEffectRisk` refused for
     * every history the module could ever produce — the practice-versus-progress question was unanswerable by
     * construction, not by lack of data. Two per category makes a probe of eight, which meets that floor. The
     * bank had to grow first: at four held-out items per category the old bank could not have paid for this.
     */
    const val ITEMS_PER_CATEGORY: Int = 2

    /** Total trials in one probe administration. */
    val PROBE_SIZE: Int = EmotionCategory.entries.size * ITEMS_PER_CATEGORY

    /** Probability of a correct answer by guessing, given the four fixed category targets. */
    val CHANCE_LEVEL: Double = 1.0 / EmotionCategory.entries.size

    /** How many complete probes the authored holdout partition can ever supply (the measurement ceiling). */
    val MAX_PROBES: Int = EmotionSeparatorBank.holdoutItems
        .groupingBy { it.category }
        .eachCount()
        .values
        .minOrNull()
        ?.div(ITEMS_PER_CATEGORY)
        ?: 0

    sealed interface Draw {
        /** A complete, balanced probe of [PROBE_SIZE] never-served items, in presentation order. */
        data class Served(val items: List<EmotionContent.SeparatorItem>) : Draw

        /** No complete probe remains. [freshItemsLeft] is what is stranded — reported, not silently served. */
        data class Exhausted(val freshItemsLeft: Int) : Draw
    }

    /** Draws the next probe from items outside [servedItemIds], or [Draw.Exhausted] when one is not possible. */
    fun draw(servedItemIds: Set<String>, seed: Long): Draw {
        val rng = Random(seed)
        val freshByCategory = EmotionSeparatorBank.holdoutItems
            .filter { it.phrase !in servedItemIds }
            .groupBy { it.category }
        val complete = EmotionCategory.entries.all {
            (freshByCategory[it]?.size ?: 0) >= ITEMS_PER_CATEGORY
        }
        if (!complete) return Draw.Exhausted(freshByCategory.values.sumOf { it.size })
        val picked = EmotionCategory.entries.flatMap { category ->
            freshByCategory.getValue(category).shuffled(rng).take(ITEMS_PER_CATEGORY)
        }
        return Draw.Served(picked.shuffled(rng))
    }

    /**
     * Renders probe items as trials. `isEval = true` is what keeps them out of every training statistic
     * downstream; they use the same renderer as EM3/EM10 so the probe is not visually a different task.
     */
    fun stimuli(
        items: List<EmotionContent.SeparatorItem>,
        responseWindowMs: Long,
        interStimulusDelayMs: Long,
    ): List<EmotionStimulus> = items.mapIndexed { index, item ->
        EmotionStimulus(
            index = index,
            correctResponseKey = null,
            payload = EmotionStimulusPayload.Classification(item.phrase, item.category, isEval = true),
            interStimulusDelayMs = interStimulusDelayMs,
            responseWindowMs = responseWindowMs,
        )
    }
}
