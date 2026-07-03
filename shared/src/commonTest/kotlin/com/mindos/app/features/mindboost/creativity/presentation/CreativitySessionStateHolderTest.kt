package com.mindos.app.features.mindboost.creativity.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.creativity.data.persistence.SecureCreativitySessionStore
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySessionCoordinator
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreativitySessionStateHolderTest {

    // Monotonic fake: each call advances 1s, so elapsed (submit - start) is positive.
    private fun incrementingMonotonic(): MonotonicMillis {
        var t = 0L
        return MonotonicMillis { t += 1_000; t }
    }

    private fun holder(
        scope: CoroutineScope,
    ): Pair<CreativitySessionStateHolder, SecureCreativitySessionStore> {
        val store = SecureCreativitySessionStore(InMemorySecureKeyValueStore())
        val coordinator = CreativitySessionCoordinator(store, EpochClock { 5_000L })
        return CreativitySessionStateHolder(scope, coordinator, "u1", incrementingMonotonic()) to store
    }

    @Test
    fun startsIdle() = runTest {
        val (h, _) = holder(this)
        assertIs<CreativityUiState.Idle>(h.state.value)
    }

    @Test
    fun startExerciseMovesToInProgressWithPrompt() = runTest {
        val (h, _) = holder(this)
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        val state = h.state.value
        assertIs<CreativityUiState.InProgress>(state)
        assertEquals(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, state.active.exerciseId)
        assertTrue(state.active.prompt.text.isNotBlank())
    }

    @Test
    fun validSubmitCompletesAndPersists() = runTest {
        val (h, store) = holder(this)
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        h.submit(listOf("doorstop", "weight", "wedge"))
        advanceUntilIdle()
        val state = h.state.value
        assertIs<CreativityUiState.Completed>(state)
        assertEquals(1, state.session.rubric?.rubricVersion)
        assertEquals("u1", store.read(state.active.sessionId)?.session?.userId)
    }

    @Test
    fun tooFewIdeasIsRejected() = runTest {
        val (h, _) = holder(this)
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        h.submit(listOf("only-one"))
        advanceUntilIdle()
        assertIs<CreativityUiState.Rejected>(h.state.value)
    }

    @Test
    fun resetReturnsToIdle() = runTest {
        val (h, _) = holder(this)
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        h.reset()
        assertIs<CreativityUiState.Idle>(h.state.value)
    }
}
