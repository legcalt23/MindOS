@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.domain.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** MC9-D · E8 — the pure metrics mapper: honest Measured/Unavailable, balanced-accuracy inhibitory control. */
class GoNoGoMetricsMapperTest {

    private fun stim(type: GoNoGoTrialType, difficulty: Int = 0, pos: Int = 0) = GoNoGoStimulus(
        trialType = type,
        stimulusId = "id-$pos",
        stimulusFamily = if (type == GoNoGoTrialType.GO) "go" else "nogo",
        deadlineMs = 500L,
        foreperiodMs = 600L,
        difficultyLevel = difficulty,
        runPosition = pos,
        trialConfigVersion = "test",
    )

    private fun out(type: GoNoGoTrialType, rt: Long?, foreperiodTap: Boolean = false, difficulty: Int = 0) =
        GoNoGoOutcome.resolve(stim(type, difficulty), GoNoGoResponse(foreperiodTap, rt))

    private fun goOk(rt: Long = 300L, difficulty: Int = 0) = out(GoNoGoTrialType.GO, rt, difficulty = difficulty)
    private fun goOmit(difficulty: Int = 0) = out(GoNoGoTrialType.GO, null, difficulty = difficulty)
    private fun noGoOk(difficulty: Int = 0) = out(GoNoGoTrialType.NO_GO, null, difficulty = difficulty)
    private fun noGoCommit(rt: Long = 300L, difficulty: Int = 0) = out(GoNoGoTrialType.NO_GO, rt, difficulty = difficulty)

    private fun measured(signal: GoNoGoSignal): Double {
        assertIs<GoNoGoSignal.Measured>(signal)
        return signal.value
    }

    @Test
    fun a_perfect_run_measures_full_accuracy_and_control() {
        val m = GoNoGoMetricsMapper.map(List(6) { goOk() } + List(4) { noGoOk() })
        assertEquals(1.0, measured(m.goAccuracy), 1e-9)
        assertEquals(1.0, measured(m.noGoAccuracy), 1e-9)
        assertEquals(1.0, measured(m.inhibitoryControlIndex), 1e-9)
        assertEquals(0.0, measured(m.commissionRate), 1e-9)
        assertEquals(0.0, measured(m.omissionRate), 1e-9)
        assertEquals(300.0, measured(m.correctGoSpeedMs), 1e-9)
    }

    @Test
    fun tapping_all_trials_yields_zero_no_go_accuracy_and_balanced_control_of_half() {
        // 6 GO tapped (correct) + 4 NO-GO tapped (commission): velocity in GO does NOT rescue inhibition.
        val m = GoNoGoMetricsMapper.map(List(6) { goOk() } + List(4) { noGoCommit() })
        assertEquals(1.0, measured(m.goAccuracy), 1e-9)
        assertEquals(0.0, measured(m.noGoAccuracy), 1e-9)
        assertEquals(1.0, measured(m.commissionRate), 1e-9)
        assertEquals(0.5, measured(m.inhibitoryControlIndex), 1e-9, "balanced accuracy = (1 + 0) / 2")
    }

    @Test
    fun tapping_nothing_yields_zero_go_accuracy_and_balanced_control_of_half() {
        val m = GoNoGoMetricsMapper.map(List(6) { goOmit() } + List(4) { noGoOk() })
        assertEquals(0.0, measured(m.goAccuracy), 1e-9)
        assertEquals(1.0, measured(m.noGoAccuracy), 1e-9)
        assertEquals(1.0, measured(m.omissionRate), 1e-9)
        assertEquals(0.5, measured(m.inhibitoryControlIndex), 1e-9)
        assertIs<GoNoGoSignal.Unavailable>(m.correctGoSpeedMs)
    }

    @Test
    fun commission_and_speed_accuracy_tradeoff_count_fast_impulsive_taps() {
        val outcomes = List(6) { goOk() } + List(3) { noGoOk() } + listOf(noGoCommit(rt = 150L))
        val m = GoNoGoMetricsMapper.map(outcomes)
        assertEquals(1.0 / 4.0, measured(m.commissionRate), 1e-9)
        assertEquals(1.0 / 4.0, measured(m.speedAccuracyTradeoff), 1e-9, "the single commission was fast (<250ms)")
    }

    @Test
    fun insufficient_no_go_leaves_inhibition_metrics_unavailable() {
        val m = GoNoGoMetricsMapper.map(List(6) { goOk() } + List(2) { noGoOk() })
        assertIs<GoNoGoSignal.Unavailable>(m.noGoAccuracy)
        assertIs<GoNoGoSignal.Unavailable>(m.commissionRate)
        assertIs<GoNoGoSignal.Unavailable>(m.inhibitoryControlIndex)
        assertIs<GoNoGoSignal.Measured>(m.goAccuracy)
    }

    @Test
    fun difficulty_adjusted_control_is_unavailable_with_one_level_and_measured_with_two() {
        val single = GoNoGoMetricsMapper.map(List(6) { goOk() } + List(4) { noGoOk() })
        assertIs<GoNoGoSignal.Unavailable>(single.difficultyAdjustedControl)

        val twoLevels = List(3) { goOk(difficulty = 0) } + List(1) { noGoOk(difficulty = 0) } +
            List(3) { goOk(difficulty = 1) } + List(1) { noGoOk(difficulty = 1) }
        val m = GoNoGoMetricsMapper.map(twoLevels)
        assertEquals(1.0, measured(m.difficultyAdjustedControl), 1e-9)
    }

    @Test
    fun an_empty_run_yields_all_unavailable_signals_never_fabricated_zero() {
        val m = GoNoGoMetricsMapper.map(emptyList())
        assertEquals(0, m.totalTrials)
        assertIs<GoNoGoSignal.Unavailable>(m.goAccuracy)
        assertIs<GoNoGoSignal.Unavailable>(m.noGoAccuracy)
        assertIs<GoNoGoSignal.Unavailable>(m.inhibitoryControlIndex)
        assertIs<GoNoGoSignal.Unavailable>(m.commissionRate)
        assertEquals(GONOGO_METRICS_VERSION, m.version)
    }
}
