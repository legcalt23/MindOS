package com.mindos.app.features.mindboost.memory.domain.content.sampling

import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecentItemRegistryTest {

    private val empty = RecentItemRegistry(MemoryExerciseId.EX3_LISTA_ESTRATEGIA)

    @Test
    fun withWordsUsed_isCopyOnWrite() {
        val updated = empty.withWordsUsed(listOf(WordId("w1")), usedAtEpoch = 1_000L)
        assertTrue(empty.wordUsage.isEmpty(), "Original registry must stay unchanged.")
        assertEquals(setOf(WordId("w1")), updated.wordUsage.keys)
    }

    @Test
    fun recentWordIds_includesItemWithinTtl() {
        val reg = empty.withWordsUsed(listOf(WordId("w1")), usedAtEpoch = 0L)
        val now = RecentItemRegistry.DEFAULT_TTL_MILLIS - 1
        assertTrue(WordId("w1") in reg.recentWordIds(now))
    }

    @Test
    fun recentWordIds_excludesExpiredItem() {
        val reg = empty.withWordsUsed(listOf(WordId("w1")), usedAtEpoch = 0L)
        val now = RecentItemRegistry.DEFAULT_TTL_MILLIS + 1
        assertFalse(WordId("w1") in reg.recentWordIds(now))
    }

    @Test
    fun pruned_dropsExpiredKeepsCurrent() {
        val reg = empty
            .withWordsUsed(listOf(WordId("old")), usedAtEpoch = 0L)
            .withWordsUsed(listOf(WordId("new")), usedAtEpoch = RecentItemRegistry.DEFAULT_TTL_MILLIS)
        val pruned = reg.pruned(nowEpoch = RecentItemRegistry.DEFAULT_TTL_MILLIS + 1)
        assertFalse(WordId("old") in pruned.wordUsage.keys)
        assertTrue(WordId("new") in pruned.wordUsage.keys)
    }

    @Test
    fun defaultTtl_isFourteenDays() {
        val fourteenDaysMillis = 14L * 24 * 60 * 60 * 1000
        assertEquals(fourteenDaysMillis, RecentItemRegistry.DEFAULT_TTL_MILLIS)
    }
}
