package com.mindos.app.features.mindboost.memory.domain.session

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemoryRecoveryPolicyTest {

    private val policy = DefaultMemoryRecoveryPolicy()

    @Test
    fun tooSoonAfterSameModule_restRecommended() {
        val gate = policy.gate(
            MemoryRecoveryInput(
                hoursSinceLastSameModuleSession = 2.0,
                consecutiveTrainingDays = 1,
                recentSessionPerformance = emptyList(),
            ),
        )
        assertIs<MemoryRecoveryGate.RestRecommended>(gate)
    }

    @Test
    fun tooManyConsecutiveDays_restRecommended() {
        val gate = policy.gate(
            MemoryRecoveryInput(
                hoursSinceLastSameModuleSession = 24.0,
                consecutiveTrainingDays = 6,
                recentSessionPerformance = emptyList(),
            ),
        )
        assertIs<MemoryRecoveryGate.RestRecommended>(gate)
    }

    @Test
    fun decliningPerformance_reducesVolume() {
        val gate = policy.gate(
            MemoryRecoveryInput(
                hoursSinceLastSameModuleSession = 24.0,
                consecutiveTrainingDays = 2,
                recentSessionPerformance = listOf(0.50, 0.62, 0.72), // newest 0.50 << oldest 0.72
            ),
        )
        assertIs<MemoryRecoveryGate.Reduced>(gate)
    }

    @Test
    fun freshAndStable_fullGo() {
        val gate = policy.gate(
            MemoryRecoveryInput(
                hoursSinceLastSameModuleSession = 24.0,
                consecutiveTrainingDays = 2,
                recentSessionPerformance = listOf(0.70, 0.69, 0.71),
            ),
        )
        assertIs<MemoryRecoveryGate.FullGo>(gate)
    }

    @Test
    fun firstSession_fullGo() {
        val gate = policy.gate(
            MemoryRecoveryInput(
                hoursSinceLastSameModuleSession = null,
                consecutiveTrainingDays = 1,
                recentSessionPerformance = emptyList(),
            ),
        )
        assertIs<MemoryRecoveryGate.FullGo>(gate)
    }

    @Test
    fun intraSession_restWhenDropExceeds25Percent() {
        assertTrue(policy.shouldRestIntraSession(startPerformance = 0.80, currentPerformance = 0.55))
        assertFalse(policy.shouldRestIntraSession(startPerformance = 0.80, currentPerformance = 0.65))
    }
}
