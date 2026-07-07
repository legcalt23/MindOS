package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.speed.SpeedTestFixtures
import com.mindos.app.features.mindboost.speed.data.persistence.InMemorySpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.DefaultStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.AdaptiveStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.StaircaseDecision
import com.mindos.app.features.mindboost.speed.domain.StaircaseDirective
import com.mindos.app.features.mindboost.speed.domain.StaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.exercise.MultiviaGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.opposite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Conduce el [SpeedSessionStateHolder] a través de una corrida E6 adaptativa multi-bloque
 * (MVP-1), simulando los callbacks que normalmente emite la pantalla. Verifica la
 * estructura de la corrida, la regla "respuesta tardía ⇒ incorrecta" y la persistencia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedSessionStateHolderTest {

    private val ms = SpeedTestFixtures.MS

    private class FakeClock(start: Long = 1_700_000_000_000L) : EpochClock {
        private var t = start
        override fun nowMillis(): Long = t.also { t += 10L }
    }

    private fun holder(
        store: InMemorySpeedMeasurementStore,
        staircase: StaircaseEngine?,
        blockCount: Int,
        trialsPerBlock: Int,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = SpeedSessionStateHolder(
        scope = scope,
        store = store,
        clock = FakeClock(),
        userId = "u1",
        exerciseId = SpeedExerciseId.E6_MULTIVIA,
        context = SpeedTestFixtures.context,
        apiLevel = 34,
        deviceModel = "Pixel-Test",
        osVersion = "Android 14",
        trialsPerBlock = trialsPerBlock,
        blockCount = blockCount,
        staircase = staircase,
        promptGenerator = MultiviaGenerator(),
    )

    /** Recorre una corrida completa respondiendo cada trial; devuelve nada (lee el state). */
    private fun drive(h: SpeedSessionStateHolder, correct: Boolean, rtNanos: Long) {
        h.startSession()
        var stimulus = 1_000_000_000L
        var guard = 0
        while (h.state.value.phase != SpeedPhase.Finished && guard < MAX_STEPS) {
            guard++
            when (h.state.value.phase) {
                SpeedPhase.Foreperiod -> h.onForeperiodElapsed()
                SpeedPhase.Stimulus -> {
                    h.onStimulusShown(stimulus)
                    val target = h.state.value.prompt?.correctChoice
                    val choice = if (correct) target else target?.opposite()
                    h.onResponse(stimulus + rtNanos, choice)
                }
                SpeedPhase.Feedback -> {
                    h.beginNextTrial()
                    stimulus += 3_000_000_000L
                }
                else -> Unit
            }
        }
    }

    @Test
    fun adaptiveE6_runsAllBlocks_persistsRun_correctFastResponses() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val staircase = DefaultStaircaseEngine(initialCce = 0.5)
        val h = holder(store, staircase, blockCount = 3, trialsPerBlock = 4, scope = this)

        // RT corto (200 ms) < deadline de cualquier bloque ⇒ rápido y correcto.
        drive(h, correct = true, rtNanos = 200 * ms)
        advanceUntilIdle()

        val state = h.state.value
        assertEquals(SpeedPhase.Finished, state.phase)
        assertTrue(state.sessionPersisted)

        val run = assertNotNull(state.lastSession)
        assertEquals(3, run.blocks.size)
        assertEquals(12, run.trials.size)
        assertTrue(run.trials.all { it.isCorrect }, "respuestas rápidas y al lado correcto")
        // pc alta ⇒ el staircase sube la dificultad respecto al inicio.
        assertTrue(staircase.currentCce() > 0.5, "cce=${staircase.currentCce()}")

        // Persistencia íntegra.
        val loaded = assertNotNull(store.loadRun(run.session.id))
        assertEquals(12, loaded.trials.size)
        assertEquals(3, loaded.blocks.size)
        coroutineContext.cancelChildren()
    }

    @Test
    fun adaptiveE6_lateResponsesCountAsIncorrect_andEaseDifficulty() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val staircase = DefaultStaircaseEngine(initialCce = 0.5)
        val h = holder(store, staircase, blockCount = 2, trialsPerBlock = 5, scope = this)

        // RT enorme (3 s) > cualquier deadline ⇒ todas tardías ⇒ incorrectas.
        drive(h, correct = true, rtNanos = 3_000 * ms)
        advanceUntilIdle()

        val run = assertNotNull(h.state.value.lastSession)
        assertTrue(run.trials.none { it.isCorrect }, "todas las respuestas tardías son incorrectas")
        // pc=0 ⇒ el staircase baja la dificultad respecto al inicio.
        assertTrue(staircase.currentCce() < 0.5, "cce=${staircase.currentCce()}")
        coroutineContext.cancelChildren()
    }

    @Test
    fun adaptiveE6_cooldownDirective_endsSessionEarly() = runTest {
        val store = InMemorySpeedMeasurementStore()
        // Staircase falso que pide COOLDOWN al evaluar el 2º bloque.
        val staircase = CooldownAtBlock(cooldownAtBlock = 2)
        val h = holder(store, staircase, blockCount = 5, trialsPerBlock = 3, scope = this)

        drive(h, correct = true, rtNanos = 200 * ms)
        advanceUntilIdle()

        val run = assertNotNull(h.state.value.lastSession)
        // Aunque blockCount=5, la corrida termina tras el bloque que disparó COOLDOWN.
        assertEquals(2, run.blocks.size, "COOLDOWN debe terminar la corrida antes de tiempo")
        assertEquals(SpeedPhase.Finished, h.state.value.phase)
        coroutineContext.cancelChildren()
    }

    // --- MC9-B-R §1: a fresh measurement session id per run ---

    @Test
    fun repeatedRuns_fromSameHolder_getDistinctSessionIds_andDoNotOverwrite() = runTest {
        val store = InMemorySpeedMeasurementStore()
        val staircase = DefaultStaircaseEngine(initialCce = 0.5)
        val h = holder(store, staircase, blockCount = 2, trialsPerBlock = 3, scope = this)

        drive(h, correct = true, rtNanos = 200 * ms)
        advanceUntilIdle()
        val run1 = assertNotNull(h.state.value.lastSession)
        val id1 = run1.session.id

        drive(h, correct = true, rtNanos = 200 * ms) // "Run Again" on the SAME holder instance
        advanceUntilIdle()
        val run2 = assertNotNull(h.state.value.lastSession)
        val id2 = run2.session.id

        assertNotEquals(id1, id2, "each startSession must mint a new measurement session id")
        // Both runs survive independently — the repeat did NOT overwrite/contaminate the first.
        assertNotNull(store.loadRun(id1), "run 1 still retrievable after the repeat")
        assertNotNull(store.loadRun(id2), "run 2 persisted under its own id")
        // Every id in run2 belongs to the ACTIVE (run2) session id; run1's records never leaked into it.
        assertEquals(id2, assertNotNull(run2.sessionMetrics).sessionId, "session metrics carry the active session id")
        assertEquals(id2, run2.versionSnapshot.sessionId, "version snapshot id == session record id")
        assertTrue(run2.blocks.all { it.sessionId == id2 }, "block records carry the active session id")
        assertTrue(run2.blocks.all { it.id.startsWith("$id2:b") }, "block ids derive from the active session id")
        assertTrue(run2.qualityLogs.all { it.sessionId == id2 }, "quality log carries the active session id")
        assertTrue(run2.qualityLogs.all { it.id.startsWith("$id2:q") }, "quality log id derives from the session id")
        assertTrue(run1.blocks.all { it.sessionId == id1 }, "run1 records keep run1 id (stable within a run)")
        coroutineContext.cancelChildren()
    }

    @Test
    fun repeatedRuns_underAFixedClock_stillGetDistinctIds() = runTest {
        // A constant clock repeats the "$user:speed:<millis>" prefix; only the run counter can disambiguate.
        val store = InMemorySpeedMeasurementStore()
        val fixedClock = EpochClock { 1_000L }
        val h = SpeedSessionStateHolder(
            scope = this,
            store = store,
            clock = fixedClock,
            userId = "u1",
            exerciseId = SpeedExerciseId.E1_DESTELLO,
            context = SpeedTestFixtures.context,
            apiLevel = 34,
            deviceModel = "Pixel-Test",
            osVersion = "Android 14",
            trialsPerBlock = 2,
        )
        drive(h, correct = true, rtNanos = 200 * ms)
        advanceUntilIdle()
        val id1 = assertNotNull(h.state.value.lastSession).session.id
        drive(h, correct = true, rtNanos = 200 * ms)
        advanceUntilIdle()
        val id2 = assertNotNull(h.state.value.lastSession).session.id
        assertNotEquals(id1, id2, "a fixed clock must not collapse two runs to the same id")
        coroutineContext.cancelChildren()
    }

    /** Staircase de prueba que devuelve COOLDOWN a partir del bloque [cooldownAtBlock]. */
    private class CooldownAtBlock(private val cooldownAtBlock: Int) : AdaptiveStaircaseEngine {
        private var calls = 0
        private var directive = StaircaseDirective.HOLD

        override fun evaluate(metrics: BlockMetrics): StaircaseDecision {
            calls += 1
            directive = if (calls >= cooldownAtBlock) StaircaseDirective.COOLDOWN else StaircaseDirective.HOLD
            return StaircaseDecision(currentCce(), directive)
        }

        override fun updateDifficulty(metrics: BlockMetrics): Double = evaluate(metrics).cce
        override fun lastDirective(): StaircaseDirective = directive
        override fun getNextDeadline(cce: Double): Long = 800_000_000L
        override fun currentCce(): Double = 0.5
        override fun reset() {
            calls = 0
            directive = StaircaseDirective.HOLD
        }
    }

    private companion object {
        const val MAX_STEPS = 2000
    }
}
