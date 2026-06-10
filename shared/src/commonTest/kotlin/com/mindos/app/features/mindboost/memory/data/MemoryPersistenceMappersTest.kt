package com.mindos.app.features.mindboost.memory.data

import com.mindos.app.features.mindboost.memory.data.dto.MemoryProfileStateDto
import com.mindos.app.features.mindboost.memory.data.local.MemoryJson
import com.mindos.app.features.mindboost.memory.data.mapper.toDomain
import com.mindos.app.features.mindboost.memory.data.mapper.toDto
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.CmeSensitivity
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.domain.personalization.MemoryArchetype
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryPersistenceMappersTest {

    private val profile = MemoryProfileState(
        userId = "u1",
        archetype = MemoryArchetype.FAST_FORGETTER,
        sensitivity = CmeSensitivity(alpha = 1.2, beta = 0.8),
        difficultyByExercise = mapOf(
            MemoryExerciseId.EX1_SPAN_COMPLEJO to
                MemoryDifficultyVector.EASIEST.adjust(MemoryDifficultyAxis.LOAD, AxisLevel(0.5)),
        ),
        currentCycleWeek = 3,
    )

    @Test
    fun profileState_roundTrips() {
        assertEquals(profile, profile.toDto().toDomain())
    }

    @Test
    fun sessionSummary_roundTrips() {
        val s = MemorySessionSummary("u1:100", "u1", MemoryExerciseId.EX3_LISTA_ESTRATEGIA, 100, 0.8, 0.6, 0.7)
        assertEquals(s, s.toDto().toDomain())
    }

    @Test
    fun deferredItem_roundTrips() {
        val item = DeferredRecallItem(
            id = "d1",
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            language = Language.SPANISH_ES,
            studiedWordIds = listOf(WordId("w1"), WordId("w2")),
            studiedAtEpoch = 12_345,
        )
        assertEquals(item, item.toDto().toDomain())
    }

    @Test
    fun recentRegistry_roundTrips() {
        val reg = RecentItemRegistry(
            exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
            wordUsage = mapOf(WordId("w1") to 10L),
            pairUsage = mapOf(PairId("p1") to 20L),
        )
        assertEquals(reg, reg.toDto().toDomain())
    }

    @Test
    fun profileState_jsonRoundTrips() {
        val dto = profile.toDto()
        val encoded = MemoryJson.instance.encodeToString(dto)
        val decoded = MemoryJson.instance.decodeFromString<MemoryProfileStateDto>(encoded)
        assertEquals(dto, decoded)
        assertEquals(profile, decoded.toDomain())
    }

    @Test
    fun unknownKeysAreIgnoredOnRead() {
        // Forward-compat: a payload with an extra future field still decodes.
        val raw = """{"schemaVersion":1,"userId":"u1","archetype":"BEGINNER",""" +
            """"currentCycleWeek":1,"futureField":"ignored"}"""
        val decoded = MemoryJson.instance.decodeFromString<MemoryProfileStateDto>(raw)
        assertEquals("u1", decoded.userId)
    }
}
