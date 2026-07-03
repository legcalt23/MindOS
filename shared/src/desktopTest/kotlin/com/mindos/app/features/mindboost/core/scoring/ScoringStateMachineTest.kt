package com.mindos.app.features.mindboost.core.scoring

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * INFRA-01 acceptance suite. Named `ScoringStateMachineTest` to match the runbook gate
 * `--tests "*ScoringStateMachineTest"` (14_RUNBOOK §1.8/§2.1/§6.2). Lives in desktopTest (not scanned by
 * Detekt; holds the heavy property test).
 */
class ScoringStateMachineTest {

    private fun provenance() = ScoreProvenance(
        exerciseId = "EX1",
        moduleId = ScoreModuleId.MEMORY,
        sessionId = "s1",
        pseudoUserId = "hash",
        timestampMs = 0L,
        scoringVersion = "v1",
    )

    private fun signal(value: Double? = 0.8) = Signal(
        key = SignalKey("accuracy"),
        value = value,
        lowerBound = value?.let { it - 0.05 },
        upperBound = value?.let { it + 0.05 },
        unit = SignalUnit.RATIO,
        uncertaintyLabel = UncertaintyLabel.LOW,
    )

    /** Inputs that pass every gate → VALID (robust). Override one field per test. */
    private fun validInputs(
        criticality: TimingCriticality = TimingCriticality.RT_IRRELEVANT,
        isEmotionalOrFrustration: Boolean = false,
        completed: Boolean = true,
        deviceTimingQuality: TimebaseQuality = TimebaseQuality.AUTHORITATIVE,
        criticalFrameJitter: Boolean = false,
        frameDropFraction: Double = 0.0,
        itemBankIntegrity: BankIntegrity = BankIntegrity.OK,
        metricStability: MetricStability = MetricStability.STABLE,
        invalidatedBy: InvalidationReason? = null,
        antiGamingClean: Boolean = true,
        detectorCoverage: DetectorCoverage = DetectorCoverage.ADEQUATE,
        nEffective: Int = 30,
        minNRequired: Int = 20,
        confidence: Double = 0.8,
        crossSessionAggregated: Boolean = true,
        primaryComputable: Boolean = true,
        secondaryPasses: Boolean = true,
    ) = ScoreInputs(
        criticality = criticality,
        isEmotionalOrFrustration = isEmotionalOrFrustration,
        completed = completed,
        quality = DataQuality(
            deviceTimingQuality, criticalFrameJitter, frameDropFraction,
            itemBankIntegrity, metricStability, invalidatedBy,
        ),
        antiGaming = AntiGaming(antiGamingClean, detectorCoverage),
        sampling = Sampling(nEffective, minNRequired, confidence, crossSessionAggregated, 0.0..1.0),
        primaryComputable = primaryComputable,
        secondaryAccuracyGate = SecondaryGate(secondaryPasses),
    )

    private fun result(inputs: ScoreInputs, withSignal: Signal? = signal()) =
        ScoreResultFactory.from(AdapterOutput(inputs, provenance(), withSignal))

    // --- happy paths ---

    @Test
    fun cleanInputs_produceValid_andUpdateProfile() {
        val r = result(validInputs())
        assertEquals(ScoreState.VALID, r.state)
        assertTrue(r.updatesProfile)
        assertTrue(r.canUpdateProfile())
        assertEquals(false, r.preliminary, "0.8 confidence + cross-session is robust")
        assertEquals(ClaimLevel.WITHIN_TASK_TREND, r.claimLevel)
        assertEquals(0.8, r.primarySignal?.value)
    }

    @Test
    fun rtCritical_withAuthoritativeTimebase_isValid() {
        assertEquals(ScoreState.VALID, result(validInputs(criticality = TimingCriticality.RT_CRITICAL)).state)
    }

    @Test
    fun valid_isPreliminary_whenNotRobust() {
        assertEquals(true, result(validInputs(confidence = 0.5)).preliminary)
        assertEquals(true, result(validInputs(crossSessionAggregated = false)).preliminary)
    }

    // --- safety gates (cascade) ---

    @Test
    fun emotionalAbandonment_isAbandonedSafe_notBlamed() {
        val r = result(validInputs(isEmotionalOrFrustration = true, completed = false))
        assertEquals(ScoreState.ABANDONED_SAFE, r.state)
        assertEquals(false, r.updatesProfile)
        assertNull(r.primarySignal)
    }

    @Test
    fun rtCritical_withoutAuthoritativeTimebase_isDeviceUnreliable() {
        val r = result(
            validInputs(
                criticality = TimingCriticality.RT_CRITICAL,
                deviceTimingQuality = TimebaseQuality.MONOTONIC_NONFRAME,
            ),
        )
        assertEquals(ScoreState.DEVICE_UNRELIABLE, r.state)
        assertEquals(false, r.updatesProfile)
    }

    @Test
    fun jankOverThreshold_isDeviceUnreliable_forTimeDependentMetric() {
        val r = result(validInputs(criticality = TimingCriticality.RT_SECONDARY, frameDropFraction = 0.5))
        assertEquals(ScoreState.DEVICE_UNRELIABLE, r.state)
    }

    @Test
    fun jank_isIgnored_forRtIrrelevantMetric() {
        // RT_IRRELEVANT skips the jank gate entirely → still VALID despite high frame drop.
        assertEquals(ScoreState.VALID, result(validInputs(frameDropFraction = 0.9)).state)
    }

    @Test
    fun contaminatedBank_isBankContaminated() {
        assertEquals(
            ScoreState.BANK_CONTAMINATED,
            result(validInputs(itemBankIntegrity = BankIntegrity.CONTAMINATED)).state,
        )
        assertEquals(
            ScoreState.BANK_CONTAMINATED,
            result(validInputs(itemBankIntegrity = BankIntegrity.HOLDOUT_EXHAUSTED)).state,
        )
        // A small/unvalidated bank also cannot back a profile signal (charter invariant enforced here).
        assertEquals(
            ScoreState.BANK_CONTAMINATED,
            result(validInputs(itemBankIntegrity = BankIntegrity.SMALL_UNVALIDATED)).state,
        )
    }

    @Test
    fun antiGamingFlag_isSuspectedGaming() {
        assertEquals(ScoreState.SUSPECTED_GAMING, result(validInputs(antiGamingClean = false)).state)
    }

    @Test
    fun invalidatedSession_isInvalidated() {
        assertEquals(
            ScoreState.INVALIDATED,
            result(validInputs(invalidatedBy = InvalidationReason.QUALITY_LOW)).state,
        )
    }

    @Test
    fun safetyGatesWinOverEverything() {
        // emotional-abandon + bad timebase + contaminated bank + gaming → ABANDONED_SAFE (first gate).
        val r = result(
            validInputs(
                isEmotionalOrFrustration = true,
                completed = false,
                criticality = TimingCriticality.RT_CRITICAL,
                deviceTimingQuality = TimebaseQuality.UNTRUSTED,
                itemBankIntegrity = BankIntegrity.CONTAMINATED,
                antiGamingClean = false,
            ),
        )
        assertEquals(ScoreState.ABANDONED_SAFE, r.state)
    }

    // --- evidence gates ---

    @Test
    fun nonComputablePrimary_isCalibrating() {
        assertEquals(ScoreState.CALIBRATING, result(validInputs(primaryComputable = false)).state)
    }

    @Test
    fun belowMinN_isInsufficientData_withNullSignal() {
        val r = result(validInputs(nEffective = 19, minNRequired = 20))
        assertEquals(ScoreState.INSUFFICIENT_N, r.state)
        assertNull(r.primarySignal, "n<nMin must never carry a number (null != 0)")
    }

    @Test
    fun absentDetectorCoverage_onRtCritical_isCalibrating_notValid() {
        assertEquals(
            ScoreState.CALIBRATING,
            result(validInputs(criticality = TimingCriticality.RT_CRITICAL, detectorCoverage = DetectorCoverage.PARTIAL)).state,
        )
    }

    @Test
    fun partialDetectorCoverage_isAcceptable_forNonCriticalMetric() {
        assertEquals(ScoreState.VALID, result(validInputs(detectorCoverage = DetectorCoverage.PARTIAL)).state)
    }

    @Test
    fun secondaryAccuracyGateFails_onRtCritical_isCalibrating() {
        assertEquals(
            ScoreState.CALIBRATING,
            result(validInputs(criticality = TimingCriticality.RT_CRITICAL, secondaryPasses = false)).state,
        )
    }

    @Test
    fun unstableMetric_isCalibrating() {
        assertEquals(ScoreState.CALIBRATING, result(validInputs(metricStability = MetricStability.UNSTABLE)).state)
    }

    // --- factory invariants ---

    @Test
    fun factory_nullsSignal_forNonValidEvenWhenProvided() {
        val r = result(validInputs(antiGamingClean = false), withSignal = signal(0.9))
        assertEquals(ScoreState.SUSPECTED_GAMING, r.state)
        assertNull(r.primarySignal)
    }

    @Test
    fun canUpdateProfile_iff_valid() {
        assertTrue(result(validInputs()).canUpdateProfile())
        assertTrue(!result(validInputs(primaryComputable = false)).canUpdateProfile())
    }

    // --- serialization canary (null != 0) ---

    @Test
    fun dto_nonValidState_serializesNullValue() {
        val insufficient = result(validInputs(nEffective = 1, minNRequired = 20)).toDto()
        assertEquals(ScoreState.INSUFFICIENT_N.name, insufficient.state)
        assertNull(insufficient.primaryValue, "non-VALID DTO must never carry a number")

        val valid = result(validInputs()).toDto()
        assertEquals(ScoreState.VALID.name, valid.state)
        assertEquals(0.8, valid.primaryValue)
    }

    // --- UI state mapping ---

    @Test
    fun toUiState_mapsHonestly() {
        assertTrue(result(validInputs()).toUiState() is ScoreUiState.Confident)
        assertTrue(result(validInputs(confidence = 0.5)).toUiState() is ScoreUiState.Preliminary)
        assertTrue(result(validInputs(primaryComputable = false)).toUiState() is ScoreUiState.Calibrating)
        assertTrue(result(validInputs(nEffective = 1, minNRequired = 20)).toUiState() is ScoreUiState.InsufficientData)
        assertTrue(
            result(validInputs(criticality = TimingCriticality.RT_CRITICAL, deviceTimingQuality = TimebaseQuality.UNTRUSTED))
                .toUiState() is ScoreUiState.NotUpdatedQuality,
        )
        assertTrue(result(validInputs(itemBankIntegrity = BankIntegrity.CONTAMINATED)).toUiState() is ScoreUiState.NotUpdatedQuality)
        assertTrue(result(validInputs(invalidatedBy = InvalidationReason.AGRS_HIGH)).toUiState() is ScoreUiState.Invalidated)
        assertTrue(result(validInputs(isEmotionalOrFrustration = true, completed = false)).toUiState() is ScoreUiState.Calibrating)
    }

    // --- property tests (the two named in the runbook/checklist) ---

    @Test
    fun validEsElUnicoEstado_conUpdatesProfileTrue() {
        val rng = Random(seed = 20260628)
        repeat(PROPERTY_ITERATIONS) {
            val r = result(randomInputs(rng))
            assertEquals(r.state == ScoreState.VALID, r.updatesProfile)
            assertEquals(r.state == ScoreState.VALID, r.canUpdateProfile())
        }
    }

    @Test
    fun ningunEstadoNoValid_emitePrimarySignalNoNull() {
        val rng = Random(seed = 99999)
        repeat(PROPERTY_ITERATIONS) {
            val r = result(randomInputs(rng))
            if (r.state != ScoreState.VALID) {
                assertNull(r.primarySignal, "non-VALID carried a signal: ${r.state}")
                assertNull(r.toDto().primaryValue, "non-VALID DTO carried a number: ${r.state}")
            }
        }
    }

    private fun randomInputs(r: Random) = ScoreInputs(
        criticality = TimingCriticality.entries.random(r),
        isEmotionalOrFrustration = r.nextBoolean(),
        completed = r.nextBoolean(),
        quality = DataQuality(
            deviceTimingQuality = TimebaseQuality.entries.random(r),
            criticalFrameJitter = r.nextBoolean(),
            frameDropFraction = r.nextDouble(),
            itemBankIntegrity = BankIntegrity.entries.random(r),
            metricStability = MetricStability.entries.random(r),
            invalidatedBy = if (r.nextBoolean()) null else InvalidationReason.entries.random(r),
        ),
        antiGaming = AntiGaming(r.nextBoolean(), DetectorCoverage.entries.random(r)),
        sampling = Sampling(
            nEffective = r.nextInt(0, 60),
            minNRequired = r.nextInt(1, 40),
            confidence = r.nextDouble(),
            crossSessionAggregated = r.nextBoolean(),
            confidenceInterval = 0.0..1.0,
        ),
        primaryComputable = r.nextBoolean(),
        secondaryAccuracyGate = SecondaryGate(r.nextBoolean()),
    )

    private companion object {
        const val PROPERTY_ITERATIONS = 5000
    }
}
