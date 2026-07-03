package com.mindos.app.features.mindboost.decisiones.domain.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structure + content-boundary guard for the CANDIDATE reversibility bank. It does NOT validate the labels
 * (inter-rater validation needs humans) — it pins that the content is real, balanced, distinct, and stays
 * within the everyday non-sensitive domains the product allows.
 */
class ReversibilityScenarioBankTest {

    private val all = ReversibilityScenarioBank.candidates

    @Test
    fun bankIsThirtyScenariosBalancedBetweenTheTwoTypes() {
        assertEquals(30, all.size)
        assertEquals(15, all.count { it.isReversible }, "15 reversible (Type-2)")
        assertEquals(15, all.count { !it.isReversible }, "15 irreversible (Type-1)")
    }

    @Test
    fun idsAndPromptsAreUnique() {
        assertEquals(all.size, all.map { it.id }.toSet().size, "every id must be distinct")
        assertEquals(all.size, all.map { it.prompt }.toSet().size, "every prompt must be distinct")
    }

    @Test
    fun promptsAreRealJudgeableSentences_notTemplatePlaceholders() {
        // The whole point vs the degenerate generator ("Reversibility triage level N"): real, contentful cards.
        for (scenario in all) {
            assertTrue(scenario.prompt.isNotBlank(), "${scenario.id} prompt must not be blank")
            assertTrue(scenario.prompt.length >= MIN_PROMPT_LENGTH, "${scenario.id} prompt too short to carry a cue")
            assertTrue(scenario.prompt.trimEnd().endsWith("."), "${scenario.id} prompt should read as a sentence")
        }
    }

    @Test
    fun everyAllowedDomainIsRepresented() {
        assertEquals(ReversibilityDomain.entries.toSet(), all.map { it.domain }.toSet())
    }

    @Test
    fun noPromptStraysIntoExcludedSensitiveDomains() {
        // The `excluded` stems below reject the sensitive real-world domains the product must avoid; the
        // build copy-safety scanner additionally enforces the forbidden lexicon across this test file.
        val excluded = listOf("dinero", "invers", "financ", "abogad", "demanda", "suicid", "autolesi", "enferm")
        for (scenario in all) {
            val text = scenario.prompt.lowercase()
            for (term in excluded) {
                assertTrue(term !in text, "${scenario.id} strays into an excluded domain (matched '$term')")
            }
        }
    }

    private companion object {
        const val MIN_PROMPT_LENGTH = 20
    }
}
