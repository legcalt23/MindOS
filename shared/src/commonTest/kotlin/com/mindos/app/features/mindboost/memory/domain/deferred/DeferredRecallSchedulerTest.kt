package com.mindos.app.features.mindboost.memory.domain.deferred

import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeferredRecallSchedulerTest {

    private val scheduler = DeferredRecallScheduler()
    private val hour = 3_600_000L

    private fun item(studiedAtEpoch: Long, id: String = "d1") = scheduler.register(
        id = id,
        exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
        language = Language.SPANISH_ES,
        studiedWordIds = listOf(WordId("w1"), WordId("w2")),
        studiedAtEpoch = studiedAtEpoch,
    )

    @Test
    fun beforeWindow_isPending() {
        val it = item(studiedAtEpoch = 0)
        assertEquals(DeferredRecallStatus.PENDING, it.status(nowEpoch = 10 * hour)) // 10h < 20h
    }

    @Test
    fun insideWindow_isDue() {
        val it = item(studiedAtEpoch = 0)
        assertEquals(DeferredRecallStatus.DUE, it.status(nowEpoch = 24 * hour)) // 24h within [20,48]
    }

    @Test
    fun pastWindow_isExpired() {
        val it = item(studiedAtEpoch = 0)
        assertEquals(DeferredRecallStatus.EXPIRED, it.status(nowEpoch = 50 * hour)) // 50h > 48h
    }

    @Test
    fun due_filtersOnlyItemsInsideWindow() {
        // Large base so every studiedAtEpoch stays non-negative.
        val now = 200 * hour
        val mix = listOf(
            item(studiedAtEpoch = now - 5 * hour, id = "pending"), // 5h → PENDING
            item(studiedAtEpoch = now - 24 * hour, id = "due"), // 24h → DUE
            item(studiedAtEpoch = now - 60 * hour, id = "expired"), // 60h → EXPIRED
        )
        assertEquals(listOf("due"), scheduler.due(mix, now).map { it.id })
        assertEquals(listOf("pending"), scheduler.pending(mix, now).map { it.id })
        assertEquals(listOf("expired"), scheduler.expired(mix, now).map { it.id })
    }

    @Test
    fun classify_groupsByStatus() {
        val now = 200 * hour
        val mix = listOf(
            item(studiedAtEpoch = now - 1 * hour, id = "p"),
            item(studiedAtEpoch = now - 25 * hour, id = "d"),
            item(studiedAtEpoch = now - 70 * hour, id = "e"),
        )
        val grouped = scheduler.classify(mix, now)
        assertEquals(setOf("p"), grouped[DeferredRecallStatus.PENDING]?.map { it.id }?.toSet())
        assertEquals(setOf("d"), grouped[DeferredRecallStatus.DUE]?.map { it.id }?.toSet())
        assertEquals(setOf("e"), grouped[DeferredRecallStatus.EXPIRED]?.map { it.id }?.toSet())
    }

    @Test
    fun elapsedNeverNegative() {
        val it = item(studiedAtEpoch = 100 * hour)
        assertEquals(0, it.elapsedMillis(nowEpoch = 50 * hour))
    }

    @Test
    fun rejectsBlankIdOrEmptyMaterial() {
        assertFailsWith<IllegalArgumentException> {
            scheduler.register("", MemoryExerciseId.EX3_LISTA_ESTRATEGIA, Language.SPANISH_ES, listOf(WordId("w1")), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            scheduler.register("d", MemoryExerciseId.EX3_LISTA_ESTRATEGIA, Language.SPANISH_ES, emptyList(), 0)
        }
    }

    @Test
    fun rejectsInvalidConfig() {
        assertFailsWith<IllegalArgumentException> {
            DeferredRecallConfig(minDelayMillis = 50 * hour, idealDelayMillis = 24 * hour, maxWindowMillis = 48 * hour)
        }
    }
}
