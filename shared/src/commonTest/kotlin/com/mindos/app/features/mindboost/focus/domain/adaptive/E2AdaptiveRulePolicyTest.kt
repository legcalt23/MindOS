package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `ContainImpulsivity` must not run on a flanker.
 *
 * The rule is outcome-keyed — it reads `TrialOutcome.FALSE_ALARM` and raw trial RTs directly — so it slips
 * underneath any policy that governs only metric keys. Its construct is a go/no-go **commission error**:
 * failing to withhold a response to a stimulus that demands none. E2 has no such stimulus; every trial demands
 * a response. A fast wrong-direction press is a **choice error**, and the rule's effect is `stepUp`, so it
 * would *raise* difficulty on a user who is merely erring fast.
 *
 * Both the efficacy and psychometric reviews converged on this independently.
 */
class E2AdaptiveRulePolicyTest {

    private val engine = DefaultAdaptiveEngine()
    private val e2 = ExerciseCatalog.E2_FILTRO

    /** The exact adversary the rule looks for: two fast wrong-direction errors + a false-alarm count of 2. */
    private fun impulsiveInput(exercise: ExerciseDefinition) = AdaptiveInput(
        exercise = exercise,
        current = DifficultyVector.EASIEST.stepUp(exercise.governingAxis, 0.4),
        recentBlock = mapOf(
            MetricKey.FALSE_ALARMS to MetricValue(
                key = MetricKey.FALSE_ALARMS,
                value = 2.0,
                n = 40,
                confident = true,
                unit = MetricUnit.COUNT,
            ),
            MetricKey.ACCURACY to MetricValue(
                key = MetricKey.ACCURACY,
                value = 0.75, // mid-band, so any step can only come from the impulsivity rule
                n = 40,
                confident = true,
                unit = MetricUnit.RATIO,
            ),
        ),
        recentTrials = listOf(
            FocusTestFixtures.trial(0, TrialOutcome.FALSE_ALARM, rtMs = 120, isTarget = false),
            FocusTestFixtures.trial(1, TrialOutcome.FALSE_ALARM, rtMs = 130, isTarget = false),
        ),
        perAxisMastery = emptyMap(),
        fatigue = FatigueSignal(level = FatigueLevel.LOW, sustained = false, emerging = false),
        blocksSinceChange = 99,
        config = AdaptiveConfig(),
    )

    @Test
    fun the_flanker_never_raises_difficulty_for_impulsivity() {
        val input = impulsiveInput(e2)
        val result = engine.decide(input)

        assertFalse(
            result.decision is AdaptiveDecision.ContainImpulsivity,
            "E2 has no withhold-response demand: a fast choice error is not a commission error, and must " +
                "never be answered by raising difficulty. Was ${result.decision}",
        )
        assertFalse(
            result.decision is AdaptiveDecision.StepUp,
            "a user erring fast on a flanker must not be read as mastery. Was ${result.decision}",
        )
        assertEquals(
            AdaptiveDecision.Hold(HoldReason.MAINTENANCE_BAND),
            result.decision,
            "with a mid-band accuracy and the impulsivity rule inapplicable, E2 holds",
        )
        assertEquals(input.current, result.nextDifficulty, "difficulty must not move")
    }

    /**
     * The discriminating arm. Without it, "disable the rule everywhere" would pass the test above while
     * silently removing a control that go/no-go exercises legitimately need.
     */
    @Test
    fun a_go_no_go_exercise_still_contains_impulsivity() {
        val goNoGo = ExerciseCatalog.E3_GATILLO
        val result = engine.decide(impulsiveInput(goNoGo))

        assertTrue(
            result.decision is AdaptiveDecision.ContainImpulsivity,
            "E3 is go/no-go: withholding IS the demand, so a fast commission error must still be contained. " +
                "Was ${result.decision}",
        )
    }

    @Test
    fun the_policy_decides_by_paradigm_shape_not_by_an_exercise_allowlist() {
        // E2's own paradigm must be rejected on its shape, so a future flanker inherits the right answer.
        assertFalse(
            FocusAdaptiveRulePolicy.allowsImpulsivityContainment(e2.id, e2.paradigm),
            "the flanker paradigm must not admit impulsivity containment",
        )
        val e3 = ExerciseCatalog.E3_GATILLO
        assertTrue(
            FocusAdaptiveRulePolicy.allowsImpulsivityContainment(e3.id, e3.paradigm),
            "a go/no-go paradigm must admit it",
        )
    }
}
