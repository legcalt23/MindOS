package com.mindos.app.features.mindboost.core.antigaming

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * INFRA-03 · D12 red-team harness. Proves the invariant "anti-gaming only counts if tested with synthetic
 * adversaries": each adversary profile is caught by its detector, an honest engaged user trips NOTHING
 * (false-positive control), and an emotional safety-exit is NEVER penalized (Regla #8).
 */
class AntiGamingRedTeamTest {

    private fun longs(vararg v: Int): List<Long> = v.map { it.toLong() }

    // --- adversary profiles (each should trigger exactly its detector) ---
    private fun rhythmicTapper() = TrialTelemetry(
        interResponseIntervalsMs = List(12) { 1000L + if (it % 2 == 0) 1L else -1L },
        rtsMs = List(12) { 300L },
        difficulties = List(12) { (it % 3 + 1).toDouble() },
    )
    private fun waitingStrategist() = TrialTelemetry(stopRespondRate = 0.10)
    private fun speedAccuracyHacker() = TrialTelemetry(medianRtMs = 150L, accuracy = 0.52, chanceAccuracy = 0.5)
    private fun buttonSpammer() = TrialTelemetry(anticipationRateInBlock = 0.50)
    private fun bankMemorizer() =
        TrialTelemetry(repeatedItemRtDropFraction = 0.60, repeatedItemAccuracy = 1.0, isomorphGain = 0.0)
    private fun leftStubborn() = TrialTelemetry(responseKeys = List(12) { 0 })
    private fun bankGrinder() = TrialTelemetry(reexposedFraction = 0.80)
    private fun keywordStuffer() = TrialTelemetry(typeTokenRatio = 0.20, keywordDensity = 0.50)
    private fun revealAllNoRead() = TrialTelemetry(revealedBeforeCommitFraction = 0.80)
    private fun buttonMasher() = TrialTelemetry(multiTouchFraction = 0.40)
    private fun quitter() = TrialTelemetry(difficultyAbandonmentCorrelation = 0.80, isEmotionalOrFrustration = false)

    // --- controls ---
    private fun safetyExit() = TrialTelemetry(difficultyAbandonmentCorrelation = 0.80, isEmotionalOrFrustration = true)
    private fun honestUser() = TrialTelemetry(
        interResponseIntervalsMs = longs(820, 1100, 640, 1500, 900, 1230, 700, 1010, 1340, 780, 1180, 950),
        rtsMs = longs(420, 520, 380, 640, 460, 610, 400, 560, 650, 440, 600, 500),
        difficulties = listOf(1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 3.0, 2.0),
        accuracy = 0.85,
        medianRtMs = 500L,
        anticipationRateInBlock = 0.05,
        stopRespondRate = 0.50,
        responseKeys = listOf(0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0),
        reexposedFraction = 0.10,
        typeTokenRatio = 0.85,
        keywordDensity = 0.10,
        repeatedItemRtDropFraction = 0.10,
        repeatedItemAccuracy = 0.70,
        isomorphGain = 0.30,
    )

    @Test
    fun everyAdversaryIsCaughtByItsDetector() {
        assertTrue(RhythmTappingDetector.evaluate(rhythmicTapper()).triggered, "D1 RhythmicTapper")
        assertTrue(WaitingStrategyDetector.evaluate(waitingStrategist()).triggered, "D2 WaitingStrategist")
        assertTrue(SpeedAccuracyHackDetector.evaluate(speedAccuracyHacker()).triggered, "D3 SpeedAccuracyHacker")
        assertTrue(TooFastToProcessDetector.evaluate(buttonSpammer()).triggered, "D4 ButtonSpammer")
        assertTrue(MemorizationDetector.evaluate(bankMemorizer()).triggered, "D5 BankMemorizer")
        assertTrue(PositionBiasDetector.evaluate(leftStubborn()).triggered, "D6 LeftStubborn")
        assertTrue(RepeatedBankExploitDetector.evaluate(bankGrinder()).triggered, "D7 BankGrinder")
        assertTrue(KeywordStuffingDetector.evaluate(keywordStuffer()).triggered, "D8 KeywordStuffer")
        assertTrue(LowEffortRevealDetector.evaluate(revealAllNoRead()).triggered, "D9 RevealAllNoRead")
        assertTrue(MotorContaminationDetector.evaluate(buttonMasher()).triggered, "D10 ButtonMasher")
        assertTrue(AbandonmentManipulationDetector.evaluate(quitter()).triggered, "D11 Quitter")
    }

    @Test
    fun honestUser_triggersNoDetector() {
        val honest = honestUser()
        val flagged = GamingDetectors.all.filter { it.evaluate(honest).triggered }
        assertTrue(flagged.isEmpty(), "honest user wrongly flagged by: ${flagged.map { it.id }}")
    }

    @Test
    fun safetyExit_inEmotionalModule_isNeverPenalized() {
        val exit = safetyExit()
        assertFalse(AbandonmentManipulationDetector.evaluate(exit).triggered, "Regla #8 violated")
        val verdict = AntiGamingFramework.aggregate(GamingDetectors.all.map { it.evaluate(exit) })
        assertTrue(verdict.isClean, "emotional safety exit must never produce a gaming penalty")
    }

    @Test
    fun aggregateVerdict_overAdversary_isNotClean_andNamesTheDetector() {
        val verdict = AntiGamingFramework.aggregate(GamingDetectors.all.map { it.evaluate(leftStubborn()) })
        assertFalse(verdict.isClean)
        assertTrue("D6_POSITION_BIAS" in verdict.firedDetectors)
    }
}
