@file:Suppress("MagicNumber") // The trial counts and d' values ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 2 · R3 · P3 — the four-category discrimination.
 *
 * The tests that matter here are the ones the metrics it replaces could not pass: a user who labels everything
 * `hecho` scored well on the fact items under plain accuracy and scored a perfect 1.0 under the coverage index
 * as soon as they got one item of each category right by luck. d' charges the false alarms, so both strategies
 * collapse — and an honest user must still come out high, or the fix would just be a harsher number.
 */
class EmotionCategoryDiscriminationTest {

    private data class Trial(val truth: EmotionCategory, val answer: EmotionCategory?)

    private fun read(trials: List<Trial>) =
        EmotionCategoryDiscrimination.read(trials, { it.truth }, { it.answer })

    /** [n] balanced trials per category, answered correctly unless [confuse] maps the truth elsewhere. */
    private fun block(n: Int, confuse: Map<EmotionCategory, EmotionCategory> = emptyMap()): List<Trial> =
        EmotionCategory.entries.flatMap { truth ->
            List(n) { Trial(truth, confuse[truth] ?: truth) }
        }

    @Test
    fun an_honest_block_reads_high_on_every_category() {
        val reading = read(block(n = 8)) as CategoryDiscriminationReading.Present
        assertEquals(EmotionCategory.entries.size, reading.perCategory.size, "all four categories must be read")
        assertTrue(reading.perCategory.all { it.dPrime > 3.0 }, "perfect answers must read high: $reading")
        assertTrue(reading.perCategory.all { it.falseAlarms == 0 })
    }

    @Test
    fun answering_the_same_category_to_everything_cannot_look_like_discrimination() {
        // The strategy the old metrics rewarded. Under plain accuracy this scores 25 %, which at least looked
        // like chance; under the coverage index it could reach 1.0 the moment one item of each category landed.
        val allFact = EmotionCategory.entries.flatMap { truth ->
            List(8) { Trial(truth, EmotionCategory.FACT) }
        }
        val reading = read(allFact) as CategoryDiscriminationReading.Present
        assertTrue(
            reading.perCategory.all { it.dPrime <= 0.0 },
            "a single-answer strategy must not read as discrimination: $reading",
        )
        assertTrue(reading.weakest.dPrime <= 0.0)
    }

    @Test
    fun one_missed_boundary_shows_up_in_the_weakest_category_and_not_in_the_others() {
        // THE reading the module was missing: a user who is fine everywhere except that they call urges facts.
        // Overall accuracy stays at 75 %, which reads as a good session. The weakest category tells the truth.
        val reading = read(
            block(n = 8, confuse = mapOf(EmotionCategory.IMPULSE to EmotionCategory.FACT)),
        ) as CategoryDiscriminationReading.Present

        assertEquals(EmotionCategory.IMPULSE, reading.weakest.category, "the missed boundary must surface")
        assertTrue(reading.weakest.dPrime <= 0.0, "a category never identified cannot read positive")
        val others = reading.perCategory.filter { it.category != EmotionCategory.IMPULSE }
        assertTrue(
            others.filter { it.category != EmotionCategory.FACT }.all { it.dPrime > 3.0 },
            "the categories the user handles well must stay high: $others",
        )
        // FACT is where the misses landed, so it collects the false alarms — that is the point of one-vs-rest.
        val fact = reading.perCategory.first { it.category == EmotionCategory.FACT }
        assertEquals(8, fact.falseAlarms, "every misread urge is a false alarm on fact")
    }

    @Test
    fun an_unanswered_trial_is_excluded_rather_than_counted_as_a_confusion() {
        // An omission is not a confusion. Charging it as one would make a slow user look like a confused one.
        val withOmissions = block(n = 8).mapIndexed { index, trial ->
            if (index % 8 == 0) trial.copy(answer = null) else trial
        }
        val reading = read(withOmissions) as CategoryDiscriminationReading.Present
        assertTrue(reading.perCategory.all { it.falseAlarms == 0 }, "omissions must not become false alarms")
        assertTrue(reading.perCategory.all { it.n == 7 }, "an unanswered item is not part of the sample")
    }

    @Test
    fun a_short_block_reports_no_number_instead_of_a_noisy_one() {
        val reading = read(block(n = 2))
        assertEquals(
            CategoryDiscriminationReading.Absent(CategoryDiscriminationReading.Reason.TOO_FEW_TRIALS),
            reading,
        )
    }

    @Test
    fun a_category_that_never_appeared_is_unmeasured_not_zero() {
        // Dropping the missing category and reading the other three would report a discrimination the block
        // cannot support; a zero would be worse still, since it would look like a failure the user never had.
        val threeCategories = EmotionCategory.entries
            .filter { it != EmotionCategory.FACT }
            .flatMap { truth -> List(8) { Trial(truth, truth) } }
        assertEquals(
            CategoryDiscriminationReading.Absent(CategoryDiscriminationReading.Reason.CATEGORY_NOT_PRESENTED),
            read(threeCategories),
        )
    }

    @Test
    fun a_label_the_user_never_pressed_reads_as_no_discrimination_not_as_a_positive() {
        // One-versus-rest is unbalanced by construction (signal is a third of noise), so the log-linear
        // correction nudges the small cell further than the large one. At the ROC origin — never pressed, so
        // hits and false alarms are both zero and d' is undefined — that nudge alone resolves to about +0.46.
        // Without the raw-count guard a user who never once said `impulso` would read as discriminating impulses.
        val neverImpulse = read(block(n = 8, confuse = mapOf(EmotionCategory.IMPULSE to EmotionCategory.FACT)))
        val impulse = (neverImpulse as CategoryDiscriminationReading.Present)
            .perCategory.first { it.category == EmotionCategory.IMPULSE }

        assertTrue(impulse.neverChosen, "fixture must reach the degenerate corner or this test proves nothing")
        assertEquals(0.0, impulse.dPrime, "an undefined ROC point must not be resolved into a positive reading")
    }

    @Test
    fun a_label_used_badly_still_earns_its_negative_reading() {
        // The discriminating half of the guard above: the zero is for a label never pressed, NOT a clamp on bad
        // performance. A user who presses `impulso` only on items that are not impulses must read below zero —
        // clamping that to zero would hide the exact confusion this metric exists to surface.
        val onlyWrong = EmotionCategory.entries.flatMap { truth ->
            val answer =
                if (truth == EmotionCategory.IMPULSE) EmotionCategory.FACT else EmotionCategory.IMPULSE
            List(8) { Trial(truth, answer) }
        }
        val impulse = (read(onlyWrong) as CategoryDiscriminationReading.Present)
            .perCategory.first { it.category == EmotionCategory.IMPULSE }

        assertEquals(0, impulse.hits)
        assertTrue(impulse.falseAlarms > 0, "the label was pressed, just never correctly")
        assertTrue(impulse.dPrime < 0.0, "a label used only on the wrong items must read negative: $impulse")
    }

    @Test
    fun the_correction_keeps_d_prime_finite_at_a_perfect_and_an_empty_hit_rate() {
        // Without the log-linear correction a perfect category divides by zero and the whole reading is NaN.
        val perfect = read(block(n = 8)) as CategoryDiscriminationReading.Present
        assertTrue(perfect.perCategory.all { it.dPrime.isFinite() }, "a perfect block must not produce NaN")

        val neverRight = read(
            block(n = 8, confuse = EmotionCategory.entries.associateWith { EmotionCategory.THOUGHT }),
        ) as CategoryDiscriminationReading.Present
        assertTrue(neverRight.perCategory.all { it.dPrime.isFinite() }, "a zero hit rate must not produce NaN")
    }
}
