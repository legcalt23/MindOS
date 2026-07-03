package com.mindos.app.features.mindboost.logic.domain.adaptive

import com.mindos.app.features.mindboost.logic.LogicTestFixtures
import com.mindos.app.features.mindboost.logic.domain.model.FatigueSignal
import com.mindos.app.features.mindboost.logic.domain.model.LogicDifficulty
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicDifficultyAdapterTest {
    private val adapter = DefaultLogicDifficultyAdapter()
    private val isomorph = LogicTestFixtures.items.first { it.isIsomorphicProbe }

    @Test
    fun sustainedHighIcrWithSuccessfulIsomorphIncreasesDifficulty() {
        val attempts = List(3) { LogicTestFixtures.attempt(totalIcr = 88.0) } +
            LogicTestFixtures.attempt(item = isomorph, totalIcr = 86.0, isomorphic = true)

        val decision = adapter.decide(LogicDifficulty.DEVELOPING, attempts)

        assertEquals(AdaptationAction.INCREASE, decision.action)
        assertEquals(LogicDifficulty.PRACTICED, decision.nextDifficulty)
    }

    @Test
    fun highTrainedPerformanceDoesNotIncreaseWhenIsomorphFails() {
        val attempts = List(3) { LogicTestFixtures.attempt(totalIcr = 90.0) } +
            LogicTestFixtures.attempt(item = isomorph, isCorrect = false, totalIcr = 40.0, isomorphic = true)

        val decision = adapter.decide(LogicDifficulty.DEVELOPING, attempts)

        assertEquals(AdaptationAction.HOLD, decision.action)
        assertEquals(LogicDifficulty.DEVELOPING, decision.nextDifficulty)
    }

    @Test
    fun repeatedImpulsivityUpdatesSkillFatigueSignal() {
        val attempts = List(2) {
            LogicTestFixtures.attempt(
                isCorrect = false,
                totalIcr = 30.0,
                errors = listOf(LogicErrorTag.IMPULSIVE_RESPONSE),
            )
        }

        val state = adapter.updateSkillStates(emptyList(), attempts).first()

        assertEquals(FatigueSignal.POSSIBLE_IMPULSIVITY, state.recentFatigueSignal)
    }
}
