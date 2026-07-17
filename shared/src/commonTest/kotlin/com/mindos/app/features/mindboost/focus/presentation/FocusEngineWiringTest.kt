package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.engine.DefaultFocusEngine
import com.mindos.app.features.mindboost.focus.domain.engine.FocusEngine
import com.mindos.app.features.mindboost.focus.domain.engine.FocusRestoreResult
import com.mindos.app.features.mindboost.focus.domain.engine.FocusSessionMode
import com.mindos.app.features.mindboost.focus.domain.engine.FocusSessionPlan
import com.mindos.app.features.mindboost.focus.domain.engine.CreateFocusSessionRequest
import com.mindos.app.features.mindboost.focus.domain.engine.RestoreFocusSessionRequest
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E2FiltroGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * ENG-01: the live E2 route must go through [FocusEngine], and must NOT go through the unversioned
 * `TrialGenerator.generate(Long)` overload.
 *
 * This test exists because the two barriers that came before it did not hold. `@Deprecated` on E2's override was
 * invisible: the holder resolves its generator as a [TrialGenerator], and deprecation is checked against the
 * STATIC type. The source scanner was blind for a different reason: it only inspects files that name
 * `E2FiltroGenerator`, and a polymorphic caller never names it. Both are defeated by the same fact — the call is
 * dispatched at runtime — so the barrier has to OBSERVE THE DISPATCH. That is what the spy generator below does:
 * it counts which overload actually executed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusEngineWiringTest {

    /**
     * Counts which overload the live holder actually invokes. It is a real [VersionedTrialGenerator] registered
     * under E2, so it is dispatched exactly as the production generator is.
     */
    private class SpyE2Generator(
        private val delegate: E2FiltroGenerator = E2FiltroGenerator(),
    ) : VersionedTrialGenerator {
        var versionedCalls = 0
        var lastSeed: SessionSeed? = null

        override val exerciseId: ExerciseId = ExerciseId.E2_FILTRO
        override val generatorVersion: Int = delegate.generatorVersion

        override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: SessionSeed): List<Stimulus> {
            versionedCalls++
            lastSeed = seed
            return delegate.generate(difficulty, spec, seed)
        }
    }

    /** Records every request the holder makes, then delegates to the real engine so the session can run. */
    private class SpyFocusEngine(private val delegate: FocusEngine) : FocusEngine {
        val requests = mutableListOf<CreateFocusSessionRequest>()
        val plans = mutableListOf<FocusSessionPlan>()

        override fun createSession(request: CreateFocusSessionRequest): FocusSessionPlan {
            requests += request
            return delegate.createSession(request).also { plans += it }
        }

        override fun restoreSession(request: RestoreFocusSessionRequest): FocusRestoreResult =
            delegate.restoreSession(request)
    }

    private val seed = SessionSeed(4242L, RngVersion.V1)

    private class Harness(
        val holder: FocusSessionStateHolder,
        val engine: SpyFocusEngine,
        val generator: SpyE2Generator,
        val history: InMemorySessionHistoryRepository,
    )

    private fun harness(scope: CoroutineScope, exerciseId: ExerciseId = ExerciseId.E2_FILTRO): Harness {
        val spyGenerator = SpyE2Generator()
        val history = InMemorySessionHistoryRepository()
        val base = FocusTestFixtures.focusGraph(
            profileRepository = InMemoryFocusProfileRepository(),
            historyRepository = history,
        )
        // Swap E2's generator for the spy, and build the engine over THAT registry, so the dispatch under test
        // is the real one — nothing here bypasses the production path.
        val versioned: Map<ExerciseId, VersionedTrialGenerator> =
            base.versionedGenerators + (ExerciseId.E2_FILTRO to spyGenerator)
        val spyEngine = SpyFocusEngine(DefaultFocusEngine(versioned, SeedSource { seed }))
        val graph = base.copy(versionedGenerators = versioned, focusEngine = spyEngine)
        return Harness(
            holder = FocusSessionStateHolder(
                graph = graph,
                exerciseId = exerciseId,
                userId = "u1",
                scope = scope,
            ),
            engine = spyEngine,
            generator = spyGenerator,
            history = history,
        )
    }

    @Test
    fun startingE2_invokesTheEngineExactlyOnce_withTheSessionSeedAndVersionTwo() = runTest {
        val h = harness(this)
        h.holder.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        assertEquals(1, h.engine.requests.size, "E2 must build its form through FocusEngine exactly once")
        val request = h.engine.requests.single()
        assertEquals(ExerciseId.E2_FILTRO, request.exerciseId)
        assertEquals(FocusSessionMode.TRAINING_ONLY, request.mode)

        // The request deliberately carries NEITHER a seed NOR a version — the engine resolves both. They are
        // asserted on the resulting identity, which is where they are now decided and recorded.
        val identity = h.engine.plans.single().identity
        assertEquals("splitmix64-v1", identity.descriptor.rngVersionId)
        assertEquals(
            seed.value.toULong().toString(16).padStart(16, '0'),
            identity.descriptor.seedHex,
            "the engine must record the seed its own SeedSource minted",
        )
        assertEquals(2, identity.descriptor.generatorVersion, "E2 is v2 since the move to the pinned RNG")
        coroutineContext.cancelChildren()
    }

    /**
     * The discriminating arm: it observes that the live route draws through the VERSIONED path, carrying the
     * session's own seed.
     *
     * This used to also assert `legacyCalls == 0` against a spy that implemented both overloads. That counter is
     * gone because the overload is gone: `VersionedTrialGenerator` no longer extends `TrialGenerator`, so a
     * versioned generator cannot declare a bare-`Long` `generate` at all — this spy would not compile if it
     * tried. The absence is now a property of the types rather than of a counter, and it is asserted directly in
     * `E2HasNoLegacyRouteTest`. What remains worth observing here is the positive half: that the seed the engine
     * minted is the seed the generator actually drew from.
     */
    @Test
    fun startingE2_drawsThroughTheVersionedPath_withTheSessionsOwnSeed() = runTest {
        val h = harness(this)
        h.holder.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        assertEquals(1, h.generator.versionedCalls, "E2 must draw its form from the versioned overload")
        assertEquals(seed, h.generator.lastSeed, "the versioned overload must receive the session's own seed")
        coroutineContext.cancelChildren()
    }

    /** The plan the engine returned is the form the session actually ran and persisted — not a parallel one. */
    @Test
    fun theEnginesPlanIsTheFormTheSessionRunsAndPersists() = runTest {
        val h = harness(this)
        h.holder.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        assertIs<FocusSessionUiState.Finished>(h.holder.uiState, "the E2 engine route must complete a session")
        val plan = h.engine.plans.single()
        assertTrue(plan.stimuli.isNotEmpty(), "a plan with no stimuli would make the assertions below vacuous")

        val persisted = h.history.recent("u1", limit = 5).single()
        val trials = persisted.blocks.single().trials
        assertEquals(
            plan.stimuli.size,
            trials.size,
            "every stimulus in the engine's plan must become a trial in the persisted session",
        )
        // The congruency sequence is derived from the stimulus payload by the assembler, so comparing it to the
        // plan's own payloads proves the persisted trials came from THIS form — not from a second, parallel one
        // generated somewhere else. Comparing the plan to itself would prove nothing.
        assertEquals(
            plan.stimuli.map { (it.payload as StimulusPayload.Flanker).congruent },
            trials.map { it.congruent },
            "the persisted trials must carry the engine plan's own congruency sequence",
        )
        assertEquals(ExerciseId.E2_FILTRO, persisted.exerciseFocus)
        coroutineContext.cancelChildren()
    }

    /**
     * The twelve unmigrated exercises must keep their existing route untouched: this slice migrates E2 only, so
     * a non-versioned exercise must never reach the engine (which would fail closed on it).
     */
    @Test
    fun anUnmigratedExercise_keepsItsLegacyRoute_andNeverTouchesTheEngine() = runTest {
        val h = harness(this, exerciseId = ExerciseId.E3_GATILLO)
        h.holder.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        assertIs<FocusSessionUiState.Finished>(h.holder.uiState, "E3 must still complete on its legacy route")
        assertTrue(h.engine.requests.isEmpty(), "an unmigrated exercise must not be routed through FocusEngine")
        coroutineContext.cancelChildren()
    }
}
