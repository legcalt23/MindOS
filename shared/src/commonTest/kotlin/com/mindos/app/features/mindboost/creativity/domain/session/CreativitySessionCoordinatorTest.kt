package com.mindos.app.features.mindboost.creativity.domain.session

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.creativity.data.persistence.SecureCreativitySessionStore
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityValidationError
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreativitySessionCoordinatorTest {

    private val fixedClock = EpochClock { 5_000L }

    private fun coordinator(): Pair<CreativitySessionCoordinator, SecureCreativitySessionStore> {
        val store = SecureCreativitySessionStore(InMemorySecureKeyValueStore())
        return CreativitySessionCoordinator(store, fixedClock) to store
    }

    @Test
    fun startPresentsALocalPromptForTheExercise() {
        val (coordinator, _) = coordinator()
        val active = coordinator.start("u1", CreativityExerciseId.EX02_CATEGORY_ROTATION, monotonicStartMs = 100)
        assertEquals(CreativityExerciseId.EX02_CATEGORY_ROTATION, active.prompt.exerciseId)
        assertEquals(5_000L, active.createdAtMs)
        assertTrue(active.prompt.text.isNotBlank())
    }

    @Test
    fun startIsDeterministicForTheSameMonotonicStart() {
        val (coordinator, _) = coordinator()
        val a = coordinator.start("u1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, monotonicStartMs = 100)
        val b = coordinator.start("u1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, monotonicStartMs = 100)
        assertEquals(a.sessionId, b.sessionId)
        assertEquals(a.prompt, b.prompt)
    }

    @Test
    fun validSubmissionPersistsTheSession() = runTest {
        val (coordinator, store) = coordinator()
        val active = coordinator.start("u1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, monotonicStartMs = 100)
        val result = coordinator.submit(
            active,
            ideas = listOf("doorstop", "weight", "wedge"),
            elapsedMonotonicMs = 4_000,
        )
        assertIs<CreativitySubmitResult.Completed>(result)
        val stored = store.read(active.sessionId)
        assertEquals("u1", stored?.session?.userId)
        assertEquals(1, stored?.responses?.size)
        assertEquals(1, stored?.rubric?.rubricVersion)
        assertTrue(stored?.rubric?.signals?.containsKey("fluency") == true)
    }

    @Test
    fun tooFewIdeasIsRejectedAndNotPersisted() = runTest {
        val (coordinator, store) = coordinator()
        val active = coordinator.start("u1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, monotonicStartMs = 100)
        val result = coordinator.submit(active, ideas = listOf("only-one"), elapsedMonotonicMs = 4_000)
        assertEquals(CreativitySubmitResult.Rejected(CreativityValidationError.BELOW_MIN_IDEAS), result)
        assertNull(store.read(active.sessionId))
    }

    @Test
    fun nonMonotonicTimingIsRejected() = runTest {
        val (coordinator, _) = coordinator()
        val active = coordinator.start("u1", CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, monotonicStartMs = 100)
        val result = coordinator.submit(active, ideas = listOf("a", "b", "c"), elapsedMonotonicMs = -1)
        assertEquals(CreativitySubmitResult.Rejected(CreativityValidationError.TIMING_NOT_MONOTONIC), result)
    }
}
