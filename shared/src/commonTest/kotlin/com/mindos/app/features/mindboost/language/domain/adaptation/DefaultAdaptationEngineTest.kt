package com.mindos.app.features.mindboost.language.domain.adaptation

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the adaptation utility (§6.4): repeated and fatiguing items are avoided, repair targets
 * and goal traits are boosted, and the ranking is deterministic and explainable.
 */
class DefaultAdaptationEngineTest {

    private val engine = DefaultAdaptationEngine()
    private val profile = UserLanguageProfile(UserIdHash("u1"), emptyMap(), updatedAtEpochMs = 0)

    private fun candidate(
        id: String,
        trait: TraitCode = TraitCode.INF,
        fatigueCost: Double = 0.2,
        repairTargets: Set<ErrorType> = emptySet(),
    ) = ItemCandidate(
        taskId = TaskId(id),
        primaryTrait = trait,
        bankType = BankType.EVALUATION,
        transferDistance = 0.3,
        fatigueCost = fatigueCost,
        repairTargets = repairTargets,
    )

    @Test
    fun emptyCandidatesReturnsNoSelection() {
        val decision = engine.selectNext(emptyList(), AdaptationContext(profile))
        assertNull(decision.selected)
        assertTrue(decision.ranked.isEmpty())
    }

    @Test
    fun rankingIsSortedDescendingByUtility() {
        val decision = engine.selectNext(
            listOf(candidate("a"), candidate("b"), candidate("c")),
            AdaptationContext(profile),
        )
        val utilities = decision.ranked.map { it.utility }
        assertEquals(utilities.sortedDescending(), utilities)
        assertEquals(decision.ranked.first().candidate, decision.selected)
    }

    @Test
    fun avoidsRepeatedItem() {
        val exposed = candidate("seen")
        val fresh = candidate("fresh")
        val context = AdaptationContext(
            profile,
            exposure = ExposureSnapshot(mapOf(exposed.taskId to 5)),
        )
        val decision = engine.selectNext(listOf(exposed, fresh), context)
        assertEquals(fresh, decision.selected)
    }

    @Test
    fun avoidsFatiguingItemUnderHighFatigue() {
        val heavy = candidate("heavy", fatigueCost = 0.9)
        val light = candidate("light", fatigueCost = 0.1)
        val context = AdaptationContext(profile, fatigue = FatigueState(Proportion(0.9)))
        val decision = engine.selectNext(listOf(heavy, light), context)
        assertEquals(light, decision.selected)
    }

    @Test
    fun repairTargetBoostsCandidate() {
        val repairing = candidate("rep", repairTargets = setOf(ErrorType.INF_OVER))
        val plain = candidate("plain")
        val context = AdaptationContext(profile, activeRepairTargets = setOf(ErrorType.INF_OVER))
        val decision = engine.selectNext(listOf(plain, repairing), context)
        assertEquals(repairing, decision.selected)
    }

    @Test
    fun goalTraitBoostsCandidate() {
        val onGoal = candidate("goal", trait = TraitCode.INF)
        val offGoal = candidate("off", trait = TraitCode.ARG)
        val context = AdaptationContext(profile, goalTraits = setOf(TraitCode.INF))
        val decision = engine.selectNext(listOf(offGoal, onGoal), context)
        assertEquals(onGoal, decision.selected)
    }

    @Test
    fun exposureSnapshotRecordIncrements() {
        val id = TaskId("t1")
        val snapshot = ExposureSnapshot().record(id).record(id)
        assertEquals(2, snapshot.timesSeen(id))
        assertEquals(0, snapshot.timesSeen(TaskId("other")))
    }
}
