package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubjectScopedSessionBatchTest {

    @Test
    fun a_blank_subject_token_is_rejected() {
        assertFailsWith<IllegalArgumentException> { SubjectScopedSessionBatch.of("  ", emptyList()) }
    }

    @Test
    fun of_defensively_copies_so_a_later_mutation_of_the_source_does_not_leak_in() {
        val source = mutableListOf(CognitiveSessionRunFixtures.validRun(sessionId = "a"))
        val batch = SubjectScopedSessionBatch.of("subject", source)
        source.add(CognitiveSessionRunFixtures.validRun(sessionId = "b"))
        assertEquals(1, batch.runs.size, "the batch snapshot is immutable to later source mutation")
        assertEquals("subject", batch.subjectToken)
    }
}
