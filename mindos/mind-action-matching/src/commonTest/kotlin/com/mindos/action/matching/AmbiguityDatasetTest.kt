package com.mindos.action.matching

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.testkit.AmbiguityDataset
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies every curated ambiguity case (ZIP11 AMBIGUITY_DATASET) resolves **deterministically** to
 * its documented expected outcome against the reference registry. Conflicting patterns, overlapping
 * aliases and uncertain entity matches must never be guessed.
 */
class AmbiguityDatasetTest {

    private fun outcome(result: ActionUnderstandingResult): String = when (result) {
        is ActionUnderstandingResult.Understood -> result.intent.id.value
        is ActionUnderstandingResult.Ambiguous -> "ambiguous"
        is ActionUnderstandingResult.NoMatch -> "no_match"
        is ActionUnderstandingResult.Rejected -> "no_match"
    }

    @Test
    fun allAmbiguityCasesResolveAsDocumented() {
        for (case in AmbiguityDataset.cases) {
            val result = Reliability.matcher.understand(case.input)
            assertEquals(case.expected, outcome(result), "ambiguity case <<${case.input}>>: ${case.description}")
            // deterministic
            assertEquals(result, Reliability.matcher.understand(case.input), "non-deterministic: <<${case.input}>>")
        }
    }
}
