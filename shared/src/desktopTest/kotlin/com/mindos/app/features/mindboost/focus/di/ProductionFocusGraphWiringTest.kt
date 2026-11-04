package com.mindos.app.features.mindboost.focus.di

import com.mindos.app.features.mindboost.core.runtime.PlatformSeedSource
import com.mindos.app.features.mindboost.focus.domain.engine.DefaultFocusEngine
import com.mindos.app.features.mindboost.focus.domain.engine.FocusFormContract
import com.mindos.app.features.mindboost.focus.domain.exercise.E2FiltroGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The PRODUCTION composition, not a fixture.
 *
 * `FocusEngineWiringTest` proves the holder dispatches through the engine, but it does so with a spy engine over
 * a swapped registry — it could pass while the real graph handed production something else entirely. This test
 * asserts what production actually assembles.
 *
 * `rememberFocusGraph` is `@Composable` and cannot be invoked here, so this asserts the two things that decide
 * the production wiring: the defaults `FocusGraph` applies when a caller does not override them (which is what
 * `rememberFocusGraph` does — it passes no engine and no seed source), and the real registry's contents.
 */
class ProductionFocusGraphWiringTest {

    /** A graph built the way production builds it: registry in, everything else defaulted. */
    private fun productionGraph() = FocusTestGraphFactory.withProductionDefaults()

    /**
     * The load-bearing assumption of every other test here: `rememberFocusGraph` genuinely relies on the
     * defaults asserted below. Without this arm those tests would describe a graph production does not build —
     * someone could pass a fake engine in `rememberFocusGraph` and they would all still pass.
     */
    @Test
    fun rememberFocusGraph_does_not_override_the_engine_or_the_seed_source() {
        val source = java.io.File(
            "src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/di/FocusGraph.kt",
        ).let { if (it.exists()) it else java.io.File("shared", it.path) }
        assertTrue(source.exists(), "could not locate FocusGraph.kt — this test's scan path is wrong")
        val factory = source.readText().substringAfter("fun rememberFocusGraph")
        assertTrue(
            !factory.contains("focusEngine ="),
            "rememberFocusGraph passes its own focusEngine, so the default asserted below is not what " +
                "production gets — update these tests to assert the real one",
        )
        assertTrue(
            !factory.contains("seedSource ="),
            "rememberFocusGraph passes its own seedSource, so the default asserted below is not production's",
        )
    }

    @Test
    fun the_default_focus_engine_is_a_real_engine_never_a_fake_or_no_op() {
        val engine = productionGraph().focusEngine
        assertIs<DefaultFocusEngine>(
            engine,
            "production must get the real engine — a fake/no-op default would make every wiring test vacuous",
        )
    }

    @Test
    fun the_default_seed_source_is_the_platform_one() {
        assertEquals(
            PlatformSeedSource,
            productionGraph().seedSource,
            "production must mint from the platform source, not a fixed test seed",
        )
    }

    /**
     * The engine's default is built over THIS graph's own generators. If it were built over some other registry,
     * the engine could produce forms from generators the graph does not expose.
     */
    @Test
    fun the_default_engine_is_built_over_the_graphs_own_registry() {
        val graph = productionGraph()
        assertEquals(
            DefaultFocusEngine(graph.versionedGenerators, graph.seedSource),
            graph.focusEngine,
            "the default engine must be built from the graph's own generators and seed source",
        )
    }

    @Test
    fun e2_in_the_real_registry_is_versioned_and_matches_the_canonical_contract() {
        val generator = assertNotNull(
            FocusExerciseGenerators.versioned[ExerciseId.E2_FILTRO],
            "E2 must be in the production VERSIONED registry",
        )
        val authorized = assertNotNull(
            FocusFormContract.versionsFor(ExerciseId.E2_FILTRO),
            "E2 must be authorized by the canonical contract",
        )
        assertEquals(
            authorized.generatorVersion,
            generator.generatorVersion,
            "the generator's own version and the canonical contract's version are declared INDEPENDENTLY and " +
                "must agree — this is the check that is vacuous if either derives from the other",
        )
        assertEquals(3, E2FiltroGenerator.E2_GENERATOR_VERSION, "E2 is v3 since the congruency split became a quota")
    }

    /**
     * ENG-01 migrates E2 ONLY. The other twelve must stay on the legacy route: wiring one of them into the
     * contract without a versioned generator would make the engine fail closed on a live exercise.
     */
    @Test
    fun the_contract_wires_exactly_the_exercises_that_have_versioned_generators() {
        val wired = FocusFormContract.wiredExercises()
        assertEquals(setOf(ExerciseId.E2_FILTRO), wired, "ENG-01 migrates E2 only")
        wired.forEach { id ->
            assertNotNull(
                FocusExerciseGenerators.versioned[id],
                "$id is wired to the engine but has no versioned generator — the live route would fail closed",
            )
        }
        val unwired = FocusExerciseGenerators.wiredExerciseIds - wired
        assertTrue(unwired.isNotEmpty(), "sanity: the twelve unmigrated exercises must still exist")
        assertTrue(
            unwired.none { FocusFormContract.versionsFor(it) != null },
            "an unmigrated exercise must have no authorized versions",
        )
    }
}

/** Builds a [FocusGraph] with production's own defaults for the engine and seed source. */
private object FocusTestGraphFactory {
    fun withProductionDefaults(): FocusGraph = FocusGraph(
        generators = FocusExerciseGenerators.legacy,
            versionedGenerators = FocusExerciseGenerators.versioned,
        metricsEngine = com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine(),
        adaptiveEngine = com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine(),
        assembler = com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler(
            com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine(),
        ),
        monotonicClock = com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock { 0L },
        epochClock = com.mindos.app.core.time.EpochClock { 0L },
        profileEvolution = com.mindos.app.features.mindboost.focus.domain.personalization.DefaultProfileEvolution(
            com.mindos.app.features.mindboost.focus.domain.personalization.DefaultArchetypeInference(),
            com.mindos.app.core.time.EpochClock { 0L },
        ),
        overtrainingGuard =
            com.mindos.app.features.mindboost.focus.domain.personalization.DefaultOvertrainingGuard(),
        personalizedScorer =
            com.mindos.app.features.mindboost.focus.domain.personalization.DefaultPersonalizedSessionScorer(),
        calibrationEngine = com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine(),
        baselineProtocol = com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols.INITIAL,
        baselineAssembler = com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler(
            com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine(),
        ),
        evaluationService = com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService(),
        profileRepository = com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository(),
        historyRepository = com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository(),
        baselineRepository = com.mindos.app.features.mindboost.focus.data.InMemoryBaselineRepository(),
        probeBankRepository = com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository(),
        // seedSource and focusEngine deliberately NOT passed — exactly as rememberFocusGraph leaves them.
    )
}
