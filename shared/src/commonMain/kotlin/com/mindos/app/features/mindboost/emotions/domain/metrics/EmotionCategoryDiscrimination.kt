package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.core.scoring.Probit
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory

/**
 * One category's discrimination, read one-versus-rest.
 *
 * [dPrime] is `z(hit rate) − z(false-alarm rate)` where a hit is "this item WAS this category and the user said
 * so" and a false alarm is "it was NOT and the user said it was". That second term is the whole point: accuracy
 * alone rewards a user who answers `hecho` to everything on the items that happen to be facts, while d′ charges
 * them for every other item they also called a fact.
 *
 * [n] is how many items of this category were shown — the sample the estimate rests on, kept so a reader can
 * tell a confident zero from a category that barely appeared. [hits] and [falseAlarms] are kept for the same
 * reason: a zero produced because the user never used this label at all is a different fact from a zero produced
 * by answering it at chance, and only the raw counts tell them apart.
 */
data class CategoryDiscrimination(
    val category: EmotionCategory,
    val dPrime: Double,
    val n: Int,
    val hits: Int,
    val falseAlarms: Int,
) {
    /** The user never chose this label, for this category or any other. See [neverChosen] handling in d′. */
    val neverChosen: Boolean get() = hits == 0 && falseAlarms == 0
}

/**
 * Discrimination across all four categories, or a named reason there is none.
 *
 * The weakest category is exposed deliberately ([weakest]): in a four-way task a user is only as good as the
 * boundary they handle worst, and a mean across the four would let one strong category hide a boundary that is
 * being missed every time.
 */
sealed interface CategoryDiscriminationReading {
    /** Not enough trials, or a category that never appeared. Named so "no number" is explainable. */
    data class Absent(val reason: Reason) : CategoryDiscriminationReading

    data class Present(val perCategory: List<CategoryDiscrimination>) : CategoryDiscriminationReading {
        val weakest: CategoryDiscrimination get() = perCategory.minBy { it.dPrime }
    }

    enum class Reason {
        /** Fewer answered trials than the estimate needs. */
        TOO_FEW_TRIALS,

        /** At least one category never appeared, so its discrimination is unmeasured rather than zero. */
        CATEGORY_NOT_PRESENTED,
    }
}

/**
 * Ola 2 · R3 · P3 — the four-category discrimination the module was missing.
 *
 * What it replaces, and why both had to go:
 *  - `EMOTION_THOUGHT_SEPARATION` averaged accuracy over INTERNAL_STATE and THOUGHT only. In a four-way task
 *    that is a reading of half the task, presented under a name that sounds like all of it, and the two
 *    categories it left out — impulse and fact — are the pair users confuse most.
 *  - `GRANULARITY_INDEX` counted how many distinct categories the user got right at least once, divided by how
 *    many appeared. That is coverage, not granularity: one lucky correct answer per category scored 1.0.
 *
 * Neither was wrong arithmetic; both were honest numbers wearing a label that promised more. They are renamed
 * to what they measure ([EmotionMetricKey.STATE_THOUGHT_ACCURACY], [EmotionMetricKey.CATEGORY_COVERAGE]) and
 * demoted, and the metric a four-way classification actually needs is computed here.
 *
 * The log-linear correction (Hautus 1995: +0.5 to each count, +1.0 to each total) is applied unconditionally so
 * d′ stays finite at rates of 0 or 1. Its declared price, the same one the repo already accepts elsewhere: with
 * unbalanced cells the correction shrinks the larger cell less. One-versus-rest is unbalanced BY CONSTRUCTION —
 * in a four-way task the signal cell is a third of the noise cell — and that bias has one consequence sharp
 * enough to need handling rather than a footnote: a category the user NEVER chose sits at the ROC origin, where
 * hit rate and false-alarm rate are both zero and d′ is mathematically undefined. The correction resolves that
 * undefined point to roughly +0.46 purely because the smaller cell is nudged further, so a user who never once
 * pressed `impulso` would read as discriminating impulses.
 *
 * So that case is decided from the RAW counts instead of from the corrected statistic: never chosen ⇒ 0.0, no
 * demonstrated discrimination. It is not clamped anywhere else — a label used badly (hits 0, false alarms > 0)
 * still earns the negative d′ it deserves, and clamping that away would hide a real confusion.
 */
object EmotionCategoryDiscrimination {

    /** Answered trials required before any per-category d′ is reported. */
    const val MIN_TRIALS: Int = 16

    private const val CORRECTION_COUNT = 0.5
    private const val CORRECTION_TOTAL = 1.0

    /**
     * Reads discrimination for every category from the answered classification trials.
     *
     * [truthOf] and [answerOf] are supplied by the caller so this stays free of the trial types: `answerOf`
     * returns null for a trial the user did not answer, and those are excluded rather than counted as a wrong
     * category — an omission is not a confusion, and charging it as one would make a slow user look like a
     * confused one.
     */
    fun <T> read(
        trials: List<T>,
        truthOf: (T) -> EmotionCategory,
        answerOf: (T) -> EmotionCategory?,
    ): CategoryDiscriminationReading {
        val answered = trials.mapNotNull { trial -> answerOf(trial)?.let { truthOf(trial) to it } }
        val gap = when {
            answered.size < MIN_TRIALS -> CategoryDiscriminationReading.Reason.TOO_FEW_TRIALS
            EmotionCategory.entries.any { category -> answered.none { it.first == category } } ->
                CategoryDiscriminationReading.Reason.CATEGORY_NOT_PRESENTED
            else -> null
        }
        return gap?.let { CategoryDiscriminationReading.Absent(it) }
            ?: CategoryDiscriminationReading.Present(
                EmotionCategory.entries.map { category -> discriminationOf(category, answered) },
            )
    }

    private fun discriminationOf(
        category: EmotionCategory,
        answered: List<Pair<EmotionCategory, EmotionCategory>>,
    ): CategoryDiscrimination {
        val signal = answered.filter { it.first == category }
        val noise = answered.filter { it.first != category }
        val hits = signal.count { it.second == category }
        val falseAlarms = noise.count { it.second == category }
        val hitRate = (hits + CORRECTION_COUNT) / (signal.size + CORRECTION_TOTAL)
        val falseAlarmRate = (falseAlarms + CORRECTION_COUNT) / (noise.size + CORRECTION_TOTAL)
        val corrected = Probit.zScore(hitRate) - Probit.zScore(falseAlarmRate)
        return CategoryDiscrimination(
            category = category,
            dPrime = if (hits == 0 && falseAlarms == 0) 0.0 else corrected,
            n = signal.size,
            hits = hits,
            falseAlarms = falseAlarms,
        )
    }
}
