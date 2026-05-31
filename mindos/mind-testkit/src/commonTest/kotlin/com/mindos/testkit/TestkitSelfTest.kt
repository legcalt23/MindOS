package com.mindos.testkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestkitSelfTest {

    @Test
    fun seededRngIsDeterministic() {
        val a = SeededRng(42)
        val b = SeededRng(42)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun seededRngBoundsAreRespected() {
        val rng = SeededRng(7)
        repeat(1000) {
            val v = rng.nextInt(5)
            assertTrue(v in 0 until 5)
        }
    }

    @Test
    fun generatorFillsTemplatesFromGrammar() {
        val grammar = CommandGrammar(
            templates = listOf(CommandTemplate("open_profile", "open {target}")),
            slots = mapOf("target" to listOf("ana")),
        )
        val g = CommandGenerator(grammar).generate(SeededRng(1))
        assertEquals("open ana", g.text)
        assertEquals("open_profile", g.expectedIntentId)
        assertEquals("ana", g.slotValues["target"])
    }

    @Test
    fun preservingMutationsAreFlaggedPreserving() {
        val mutator = FuzzMutator()
        val rng = SeededRng(3)
        repeat(200) {
            val m = mutator.mutatePreserving("open ana", rng)
            assertTrue(m.kind.semanticsPreserving)
        }
    }

    @Test
    fun corpusReachesTargetSizeDeterministically() {
        val grammar = CommandGrammar(
            templates = listOf(CommandTemplate("open_profile", "open {target}")),
            slots = mapOf("target" to listOf("ana", "bob")),
        )
        val builder = CorpusBuilder(grammar, aliases = listOf("home" to "open_home"))
        val a = builder.build(500, seed = 1)
        val b = builder.build(500, seed = 1)
        assertTrue(a.size >= 500)
        assertEquals(a, b) // deterministic
    }

    @Test
    fun propertyRunnerPassesWhenInvariantHolds() {
        val report = PropertyRunner.forAllStrings(
            seed = 1, cases = 500,
            generator = { rng -> "x".repeat(rng.nextInt(5)) },
            property = { /* always holds */ },
        )
        assertTrue(report.passed, report.describe())
    }

    @Test
    fun propertyRunnerDetectsAndShrinksFailure() {
        val report = PropertyRunner.forAllStrings(
            seed = 9, cases = 1000,
            generator = { rng -> buildString { repeat(rng.nextIntInRange(1, 8)) { append(('a' + rng.nextInt(26))) } } },
            property = { input -> if (input.contains('z')) error("found z") },
        )
        assertFalse(report.passed)
        val failure = report.failures.first()
        assertTrue(failure.input.contains('z'))
        assertEquals("z", failure.input) // shrunk to the minimal reproducer
    }

    @Test
    fun benchmarkComputesOrderedPercentiles() {
        val result = BenchmarkHarness.measure("noop", warmup = 10, iterations = 200) { /* trivial */ }
        assertTrue(result.p50Micros <= result.p99Micros)
        assertEquals(200, result.samples)
    }

    @Test
    fun benchmarkRegressionDetectsBudgetBreach() {
        val result = BenchmarkResult("x", 100, 1.0, 5.0, 100.0)
        assertTrue(BenchmarkHarness.regression(result, baselineP99Micros = 10.0, tolerancePct = 10.0).regressed)
        assertFalse(BenchmarkHarness.regression(result, baselineP99Micros = 1000.0, tolerancePct = 10.0).regressed)
    }
}
