@file:Suppress("MagicNumber") // Catalogue size (15) is the spec under test.

package com.mindos.app.features.mindboost.emotions.domain.catalog

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the catalogue ↔ registry ↔ implemented relationship so a finished exercise can never be
 * silently unreachable, and an exposed id can never lack a runnable generator. In Ola 0 `implemented`
 * and the registry are empty; Ola 1 step 1 sets both to [EM1, EM2, EM3] in the same change.
 */
class EmotionExerciseCatalogConsistencyTest {

    @Test
    fun catalogue_covers_15_ids_without_duplicates() {
        assertEquals(15, EmotionExerciseId.entries.size)
        assertEquals(EmotionExerciseId.entries.size, EmotionExerciseCatalog.all.size)
        assertEquals(EmotionExerciseId.entries.toSet(), EmotionExerciseCatalog.all.keys)
    }

    @Test
    fun implemented_equals_registered_generators() {
        assertEquals(
            EmotionExerciseCatalog.implemented.toSet(),
            EmotionExerciseGenerators.registry.keys,
            "Every selectable exercise must have a registered generator and vice versa.",
        )
    }

    @Test
    fun registered_generator_id_matches_its_key() {
        EmotionExerciseGenerators.registry.forEach { (id, generator) ->
            assertEquals(id, generator.exerciseId, "Generator registered under the wrong id.")
        }
    }

    @Test
    fun catalog_only_ids_have_no_generator() {
        val catalogOnly = EmotionExerciseCatalog.all.keys - EmotionExerciseCatalog.implemented.toSet()
        catalogOnly.forEach { id ->
            assertTrue(id !in EmotionExerciseGenerators.registry.keys, "$id is catalog-only but has a generator.")
        }
    }

    @Test
    fun no_duplicate_ids_in_implemented() {
        assertEquals(
            EmotionExerciseCatalog.implemented.size,
            EmotionExerciseCatalog.implemented.toSet().size,
            "Duplicate id in EmotionExerciseCatalog.implemented.",
        )
    }

    @Test
    fun domain_ready_partitions_into_implemented_and_pending() {
        val union = EmotionExerciseCatalog.implemented.toSet() + EmotionExerciseCatalog.pendingUiIntegration.toSet()
        assertEquals(EmotionExerciseCatalog.domainReady.toSet(), union, "domainReady must equal implemented ∪ pending.")
        assertTrue(
            EmotionExerciseCatalog.implemented.none { it in EmotionExerciseCatalog.pendingUiIntegration },
            "An id cannot be both implemented and pending UI integration.",
        )
    }

    @Test
    fun governing_metric_is_a_primary_metric_for_every_definition() {
        EmotionExerciseCatalog.all.values.forEach { def ->
            assertTrue(def.governingMetric in def.primaryMetrics, "${def.id} governing metric not in primaries.")
        }
    }

    @Test
    fun the_unbounded_discrimination_metric_never_becomes_a_governing_metric() {
        // Ola 2 · R3 · P3 leaves a live trap behind it, so the trap is pinned rather than described in a comment.
        // `EmotionBlockScore.of` maps its input to 0–100 with `value.coerceIn(0.0, 1.0)` — it assumes a metric
        // already expressed as a proportion. d′ is not one: it runs roughly −4..+4, so a healthy 3.6 would clamp
        // to a perfect 100 and a genuinely weak 0.9 would read as 90. Promoting it to `governingMetric` would
        // also hand the adaptive staircase a value it compares against 0..1 floors and ceilings.
        //
        // It is a PRIMARY metric of EM3/EM10 (it carries the four-way claim) and must stay one; what it must not
        // become is the metric those two are scored and adapted on until the 0–100 mapping is given a real scale.
        EmotionExerciseCatalog.all.values.forEach { def ->
            assertTrue(
                def.governingMetric != EmotionMetricKey.CATEGORY_DPRIME_MIN,
                "${def.id} governs on d′, which EmotionBlockScore would clamp into a fabricated 0–100.",
            )
        }
        assertTrue(
            EmotionMetricKey.CATEGORY_DPRIME_MIN in EmotionExerciseCatalog.EM3_SEPARADOR.primaryMetrics,
            "the discriminating half: d′ must still be a primary metric, or this test would pass by deleting it",
        )
    }

    @Test
    fun transfer_labels_are_the_honest_pins() {
        assertEquals(TransferLabel.NOT_DEMONSTRATED, EmotionExerciseCatalog.EM3_SEPARADOR.transferLabel)
        assertEquals(TransferLabel.TASK_ONLY, EmotionExerciseCatalog.EM1_PAUSA.transferLabel)
        // Ola 3 · R2 dropped this from PLAUSIBLE. Post-error slowing and post-error accuracy adjustment are real,
        // replicated constructs; that they carry over to recovering from consequential mistakes is not
        // demonstrated, and PLAUSIBLE claimed the second from evidence for the first.
        assertEquals(TransferLabel.NOT_DEMONSTRATED, EmotionExerciseCatalog.EM2_RECUPERACION.transferLabel)
        // Ola 3 · P4 dropped the three remaining inflated labels. EM11 is EM2's own host, so it carries EM2's
        // reason verbatim; EM8 and EM10 are pinned below against the exercise they duplicate rather than against a
        // literal.
        assertEquals(TransferLabel.NOT_DEMONSTRATED, EmotionExerciseCatalog.EM11_FEEDBACK_NEG.transferLabel)
    }

    /**
     * Ola 3 · P4 — the invariant the three inflated labels actually violated.
     *
     * EM8 was PLAUSIBLE while EM1 — the same go/no-go paradigm with a different no-go share — was TASK_ONLY, and
     * EM10 was PLAUSIBLE while EM3 — the same bank, the same judgement, minus the hurry — was NOT_DEMONSTRATED.
     * One of each pair had to be dishonest, and it was the optimistic one both times.
     *
     * Pinning the two literals would have closed today's gap and left the mechanism that opened it: someone can
     * raise EM1 or EM3 later and the duplicate silently stops matching again. Asserting EQUALITY between the pair
     * instead means a label can only move for a paradigm as a whole, which is the property the transfer enum is
     * supposed to guarantee — it is the module's only claim-containment mechanism, so an internally inconsistent
     * one contains nothing.
     */
    @Test
    fun a_duplicated_paradigm_cannot_carry_a_higher_transfer_label_than_the_exercise_it_duplicates() {
        assertEquals(
            EmotionExerciseCatalog.EM1_PAUSA.transferLabel,
            EmotionExerciseCatalog.EM8_ANTIIMPULSO.transferLabel,
            "EM8 is EM1's paradigm with a different no-go share; two labels for one paradigm means one of them " +
                "is not backed by evidence",
        )
        assertEquals(
            EmotionExerciseCatalog.EM3_SEPARADOR.transferLabel,
            EmotionExerciseCatalog.EM10_SEP_PRESION.transferLabel,
            "EM10 is EM3's bank and judgement with a time floor added; hurry is a confound, not evidence of " +
                "transfer, so it cannot buy a higher label",
        )
    }
}
