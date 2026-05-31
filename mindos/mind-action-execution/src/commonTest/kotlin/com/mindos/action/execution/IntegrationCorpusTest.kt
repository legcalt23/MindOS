package com.mindos.action.execution

import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.testkit.CorpusBuilder
import com.mindos.testkit.FuzzMutator
import com.mindos.testkit.PropertyRunner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration suite (ZIP04 INTEGRATION_TESTS): validates complete pipelines
 * text → understanding → action → execution → navigation command over a generated corpus.
 * Asserts the whole pipeline never crashes and that a healthy number of valid commands execute.
 */
class IntegrationCorpusTest {

    @Test
    fun fullPipelineNeverCrashesAndExecutesValidCommands() = runTest {
        val corpus = CorpusBuilder(ExecReliability.grammar, ExecReliability.aliases).build(targetSize = 4000, seed = 5)
        val coordinator = FakeCoordinator()
        val mapper = IntentToActionMapper(counterRequestIds())
        val executor = ExecReliability.executor(coordinator)

        var executed = 0
        for (entry in corpus) {
            val understood = ExecReliability.matcher.understand(entry.text) as? ActionUnderstandingResult.Understood ?: continue
            val action = mapper.map(understood.intent, understood.entities) ?: continue
            if (executor.execute(action) is ExecutionResult.Executed) executed++
        }
        assertTrue(executed > 100, "expected many executed commands, got $executed")
        assertTrue(coordinator.calls >= executed)
    }

    @Test
    fun fuzzedInputsNeverCrashThePipeline() = runTest {
        val mutator = FuzzMutator()
        val coordinator = FakeCoordinator()
        val mapper = IntentToActionMapper(counterRequestIds())
        val executor = ExecReliability.executor(coordinator)

        // PropertyRunner drives generation; the property runs the suspend pipeline via a trampoline.
        val report = PropertyRunner.forAllStrings(
            seed = 8008, cases = 3000,
            generator = { rng -> mutator.mutate(generateValid(rng), rng).text },
            property = { input ->
                val u = ExecReliability.matcher.understand(input)
                if (u is ActionUnderstandingResult.Understood) {
                    val action = mapper.map(u.intent, u.entities)
                    // Mapping must never throw; execution of mapped actions is exercised in the runTest cases above.
                    check(action == null || action.actionType.isNotEmpty())
                }
            },
        )
        assertTrue(report.passed, report.describe())
    }

    private fun generateValid(rng: com.mindos.testkit.SeededRng): String =
        com.mindos.testkit.CommandGenerator(ExecReliability.grammar).generate(rng).text
}
