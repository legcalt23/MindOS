package com.mindos.action.matching

import com.mindos.testkit.FuzzMutator
import com.mindos.testkit.PropertyRunner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Fuzz suite (ZIP04 FUZZING_STRATEGY): random spacing, unicode, emoji, malformed and typo inputs.
 * Objective — **no crashes** and stable (deterministic) outputs. The matcher must always return a
 * typed result, never throw.
 */
class FuzzMatchingTest {

    private val mutator = FuzzMutator()

    @Test
    fun fuzzedCommandsNeverCrash() {
        val report = PropertyRunner.forAllStrings(
            seed = 5005, cases = 6000,
            generator = { rng -> mutator.mutate(Reliability.generator.generate(rng).text, rng).text },
            property = { input -> Reliability.matcher.understand(input) }, // must not throw
        )
        assertTrue(report.passed, report.describe())
    }

    @Test
    fun pureRandomStringsNeverCrash() {
        val report = PropertyRunner.forAllStrings(
            seed = 6006, cases = 6000,
            generator = { rng ->
                buildString {
                    repeat(rng.nextInt(24)) { append(Char(rng.nextIntInRange(32, 0x2100))) }
                }
            },
            property = { input -> Reliability.matcher.understand(input) },
        )
        assertTrue(report.passed, report.describe())
    }

    @Test
    fun fuzzedOutputsAreDeterministic() {
        val report = PropertyRunner.forAllStrings(
            seed = 7007, cases = 6000,
            generator = { rng -> mutator.mutate(Reliability.generator.generate(rng).text, rng).text },
            property = { input ->
                check(Reliability.matcher.understand(input) == Reliability.matcher.understand(input)) {
                    "non-deterministic under fuzz: '$input'"
                }
            },
        )
        assertTrue(report.passed, report.describe())
    }
}
