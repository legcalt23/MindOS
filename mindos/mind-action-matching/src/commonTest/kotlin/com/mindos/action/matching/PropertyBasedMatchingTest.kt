package com.mindos.action.matching

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.testkit.FuzzMutator
import com.mindos.testkit.PropertyRunner
import com.mindos.testkit.SeededRng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based suite (ZIP04 PROPERTY_BASED_TESTING): deterministic output, stable ranking,
 * valid-only, no-null, same input ⇒ same output. Failures report the exact seed + minimal input.
 */
class PropertyBasedMatchingTest {

    @Test
    fun outputIsDeterministicForGeneratedCommands() {
        val report = PropertyRunner.forAllStrings(
            seed = 1001, cases = 3000,
            generator = { rng -> Reliability.generator.generate(rng).text },
            property = { input ->
                val a = Reliability.matcher.understand(input)
                val b = Reliability.matcher.understand(input)
                check(a == b) { "non-deterministic for '$input'" }
            },
        )
        assertTrue(report.passed, report.describe())
    }

    @Test
    fun validCommandsResolveToExpectedIntent() {
        val rng = SeededRng(2002)
        repeat(3000) {
            val g = Reliability.generator.generate(rng)
            val r = Reliability.matcher.understand(g.text)
            assertTrue(r is ActionUnderstandingResult.Understood, "no match for '${g.text}'")
            assertEquals(g.expectedIntentId, (r as ActionUnderstandingResult.Understood).intent.id.value, "wrong intent for '${g.text}'")
        }
    }

    @Test
    fun rankingIsStableUnderSemanticPreservingMutation() {
        val mutator = FuzzMutator()
        val rng = SeededRng(3003)
        repeat(3000) {
            val g = Reliability.generator.generate(rng)
            val mutated = mutator.mutatePreserving(g.text, rng)
            val r = Reliability.matcher.understand(mutated.text)
            assertTrue(
                r is ActionUnderstandingResult.Understood,
                "preserving mutation broke match: '${g.text}' -> '${mutated.text}' (${mutated.kind})",
            )
            assertEquals(
                g.expectedIntentId,
                (r as ActionUnderstandingResult.Understood).intent.id.value,
                "intent changed under '${mutated.kind}': '${g.text}' -> '${mutated.text}'",
            )
        }
    }

    @Test
    fun understoodResultsCarryOnlyKnownIntents() {
        val known = setOf("open_settings", "open_home", "open_messages", "open_profile", "open_story", "send_message")
        val rng = SeededRng(4004)
        repeat(2000) {
            val g = Reliability.generator.generate(rng)
            val r = Reliability.matcher.understand(g.text)
            if (r is ActionUnderstandingResult.Understood) {
                assertTrue(r.intent.id.value in known, "unknown intent ${r.intent.id.value}")
            }
        }
    }
}
