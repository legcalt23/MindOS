package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryHomeUiState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MemoryHomeStateHolderTest {

    private val hour = 3_600_000L

    private fun holder(
        profiles: InMemoryMemoryProfileRepository = InMemoryMemoryProfileRepository(),
        deferred: InMemoryDeferredRecallRepository = InMemoryDeferredRecallRepository(),
        now: Long = 0L,
    ) = MemoryHomeStateHolder(
        profileRepository = profiles,
        deferredRecallRepository = deferred,
        deferredScheduler = DeferredRecallScheduler(),
        content = defaultMemoryContent(),
        epochClock = EpochClock { now },
    )

    @Test
    fun load_listsImplementedExercises() = runTest {
        val h = holder()
        h.load("u1", "Curro")
        val ready = assertIs<MemoryHomeUiState.Ready>(h.uiState)
        assertEquals(
            setOf(
                MemoryExerciseId.EX1_SPAN_COMPLEJO,
                MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                MemoryExerciseId.EX4_PARES_ASOCIADOS,
            ),
            ready.exercises.map { it.exerciseId }.toSet(),
        )
        assertEquals("Curro", ready.displayName)
    }

    @Test
    fun load_newUserDefaultsToBeginnerWeekOne() = runTest {
        val h = holder()
        h.load("u1", "Curro")
        val ready = assertIs<MemoryHomeUiState.Ready>(h.uiState)
        assertEquals("BEGINNER", ready.archetypeName)
        assertEquals(1, ready.cycleWeek)
    }

    @Test
    fun load_countsDueDeferredItems() = runTest {
        val deferred = InMemoryDeferredRecallRepository()
        val now = 200 * hour
        fun item(id: String, studiedAt: Long) = DeferredRecallItem(
            id = id,
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            language = Language.SPANISH_ES,
            studiedWordIds = listOf(WordId(id)),
            studiedAtEpoch = studiedAt,
        )
        deferred.save(
            "u1",
            listOf(
                item("d1", now - 24 * hour), // 24h ago → DUE
                item("d2", now - 5 * hour), // 5h ago → PENDING
            ),
        )
        val h = holder(deferred = deferred, now = now)
        h.load("u1", "Curro")
        assertEquals(1, assertIs<MemoryHomeUiState.Ready>(h.uiState).dueDeferredCount)
    }
}
