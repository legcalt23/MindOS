package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluationAsOfTest {

    @Test
    fun a_non_positive_epoch_is_rejected() {
        assertFailsWith<IllegalArgumentException> { EvaluationAsOf.authoritative(0L) }
        assertFailsWith<IllegalArgumentException> { EvaluationAsOf.bestEffort(-1L) }
    }

    @Test
    fun the_factories_tag_provenance() {
        assertEquals(ClockProvenance.AUTHORITATIVE_INJECTED, EvaluationAsOf.authoritative(1_000L).provenance)
        assertEquals(ClockProvenance.BEST_EFFORT_MONOTONIC, EvaluationAsOf.bestEffort(1_000L).provenance)
        assertEquals(1_000L, EvaluationAsOf.authoritative(1_000L).epochMs)
    }
}
