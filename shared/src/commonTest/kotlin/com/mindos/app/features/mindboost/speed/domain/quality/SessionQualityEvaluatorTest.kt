// El helper signals() espeja QualitySignals (9 campos) para legibilidad de los tests.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.quality

import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.QualityReason
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionQualityEvaluatorTest {

    private val evaluator = SessionQualityEvaluator()

    private fun signals(
        totalTrials: Int = 24,
        cv: Double? = 0.12,
        anticipationRate: Double = 0.02,
        omissionRate: Double = 0.0,
        precision: InputTimestampPrecision = InputTimestampPrecision.NANOS_NATIVE,
        // Default MEDIDO: los tests preexistentes describen corridas cuyos frames SÍ se observaron, que
        // es lo que `frameDrop = 0.0` afirma. `sampledFrames = 0` es un hecho distinto y se pasa explícito.
        sampledFrames: Int = 600,
        frameDrop: Double = 0.0,
        jitter: Boolean = false,
        source: TimestampSource = TimestampSource.CHOREOGRAPHER_FRAME,
    ) = QualitySignals(
        totalTrials, cv, anticipationRate, omissionRate, precision, sampledFrames, frameDrop, jitter, source,
    )

    @Test
    fun cleanRun_onNanosNativeDevice_isHigh() {
        val decision = evaluator.evaluate(signals())
        assertEquals(SessionQuality.HIGH, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.LOW, decision.deviceTimingRisk)
    }

    @Test
    fun frameDropOverThreshold_isContaminatedWithReason() {
        val decision = evaluator.evaluate(signals(frameDrop = 0.10))
        assertEquals(SessionQuality.CONTAMINATED, decision.sessionQuality)
        assertTrue(QualityReason.CONTAMINATED_SYSTEM_NOISE in decision.reasons)
    }

    @Test
    fun criticalJitter_isContaminatedAndHighDeviceRisk() {
        val decision = evaluator.evaluate(signals(jitter = true))
        assertEquals(SessionQuality.CONTAMINATED, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.HIGH, decision.deviceTimingRisk)
        assertTrue(QualityReason.FRAME_JITTER_DETECTED in decision.reasons)
    }

    @Test
    fun highAnticipationRate_isLowWithReason() {
        val decision = evaluator.evaluate(signals(anticipationRate = 0.40))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.HIGH_ANTICIPATION_RATE in decision.reasons)
    }

    @Test
    fun highCv_isLowWithReason() {
        val decision = evaluator.evaluate(signals(cv = 0.50))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.HIGH_CV in decision.reasons)
    }

    @Test
    fun lowTrialCount_isLowWithReason() {
        val decision = evaluator.evaluate(signals(totalTrials = 3))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.LOW_TRIAL_COUNT in decision.reasons)
    }

    @Test
    fun millisDerived_isAcceptableNotHigh_withInformationalReason() {
        val decision = evaluator.evaluate(signals(precision = InputTimestampPrecision.MILLIS_DERIVED))
        assertEquals(SessionQuality.ACCEPTABLE, decision.sessionQuality)
        assertEquals(DeviceTimingRisk.MODERATE, decision.deviceTimingRisk)
        assertTrue(QualityReason.INPUT_PRECISION_MILLIS_DERIVED in decision.reasons)
    }

    @Test
    fun nonFrameSource_isAcceptableNotHigh_evenOnNanosNativeDevice() {
        // A non-Choreographer onset (desktop/iOS monotonic fallback) is non-authoritative for RT: even
        // native-nanos precision cannot reach HIGH — the source, not just the precision, gates HIGH.
        val decision = evaluator.evaluate(signals(source = TimestampSource.SYSTEM_CLOCK_MONOTONIC))
        assertEquals(SessionQuality.ACCEPTABLE, decision.sessionQuality)
        assertTrue(QualityReason.TIMESTAMP_SOURCE_NON_FRAME in decision.reasons)
    }

    // ── MB-FOCUS-0071 · sample gate: la ausencia de medición no fabrica un dispositivo impecable ──

    /**
     * El par que fija el defecto y su mitad discriminante A LA VEZ: dos corridas idénticas en TODOS los
     * campos salvo `sampledFrames`. La medida (600 deltas, 0 drops, sin jitter) es un dispositivo
     * observado y limpio ⇒ HIGH. La no medida lleva EXACTAMENTE la carga de `FrameStats.UNAVAILABLE`
     * (`0.0` / `false`) y no puede sostener esa misma afirmación ⇒ ACCEPTABLE.
     *
     * Sin la mitad medida, «clampar todo a ACCEPTABLE» pasaría el test y rompería la métrica para todos.
     */
    @Test
    fun unmeasuredFrames_cannotClaimTheFlawlessDeviceThatMeasuredFramesCan() {
        val measured = evaluator.evaluate(signals(sampledFrames = 600))
        val unmeasured = evaluator.evaluate(signals(sampledFrames = 0))

        assertEquals(SessionQuality.HIGH, measured.sessionQuality)
        assertTrue(QualityReason.FRAME_TIMING_UNMEASURED !in measured.reasons)

        assertEquals(SessionQuality.ACCEPTABLE, unmeasured.sessionQuality)
        assertTrue(QualityReason.FRAME_TIMING_UNMEASURED in unmeasured.reasons)
    }

    /**
     * Dirección fail-closed: retener la afirmación superior, JAMÁS acusar. El gate corre ANTES de los
     * umbrales, así que ni siquiera una carga de frames imposible (drops y jitter sin un solo frame
     * observado) puede convertir la ausencia en una acusación sobre la corrida del usuario.
     */
    @Test
    fun unmeasuredFrames_neverAccuse_evenWithAnImpossibleFramePayload() {
        val decision = evaluator.evaluate(signals(sampledFrames = 0, frameDrop = 0.90, jitter = true))

        assertNotEquals(SessionQuality.CONTAMINATED, decision.sessionQuality)
        assertNotEquals(SessionQuality.LOW, decision.sessionQuality)
        assertEquals(SessionQuality.ACCEPTABLE, decision.sessionQuality)
        assertTrue(QualityReason.CONTAMINATED_SYSTEM_NOISE !in decision.reasons)
        assertTrue(QualityReason.FRAME_JITTER_DETECTED !in decision.reasons)
    }

    /**
     * La ausencia de medición de frames NO es un escudo: las guardas CONDUCTUALES siguen aplicando, que
     * es lo que impide que «no medir» se convierta en una ruta para esquivar LOW.
     */
    @Test
    fun unmeasuredFrames_doNotShieldFromBehaviouralGates() {
        val decision = evaluator.evaluate(signals(sampledFrames = 0, anticipationRate = 0.40))
        assertEquals(SessionQuality.LOW, decision.sessionQuality)
        assertTrue(QualityReason.HIGH_ANTICIPATION_RATE in decision.reasons)
    }

    /**
     * `MB-FOCUS-0071`, SEGUNDO canal de la misma ausencia. El sample gate protegía `sessionQuality`,
     * pero `deviceTimingRisk` se derivaba ANTES de él, así que la ausencia —que viaja dentro de
     * `FrameStats.UNAVAILABLE` con `criticalFrameJitter = false`— seguía publicando el riesgo MÍNIMO.
     * Y este canal es el que MÁS se ve: `SpeedExerciseScreen` lo pinta bajo «Precisión de marca
     * temporal», mientras que la copia de `DeviceProfile` (`MB-FOCUS-0088`) no tiene lector.
     *
     * Lleva su mitad discriminante: la MISMA señal con frames observados SÍ afirma LOW, así que el
     * guard no puede pasar clampándolo todo a MODERATE.
     */
    @Test
    fun unmeasuredFrames_cannotAffirmTheMinimumTimingRiskThatMeasuredFramesCan() {
        val measured = evaluator.evaluate(signals(sampledFrames = 600))
        assertEquals(DeviceTimingRisk.LOW, measured.deviceTimingRisk)

        val unmeasured = evaluator.evaluate(signals(sampledFrames = 0))
        assertNotEquals(
            DeviceTimingRisk.LOW,
            unmeasured.deviceTimingRisk,
            "sin un solo frame observado no se puede AFIRMAR el riesgo de timing mínimo",
        )
        // Fail-closed estricto y simétrico con el resto de la fase: no afirmar, pero tampoco acusar.
        assertNotEquals(
            DeviceTimingRisk.HIGH,
            unmeasured.deviceTimingRisk,
            "la ausencia jamás acusa: HIGH exigiría un jitter que nadie observó",
        )
        // El hecho se modela aparte y es lo que desambigua el MODERATE resultante.
        assertTrue(QualityReason.FRAME_TIMING_UNMEASURED in unmeasured.reasons)
    }
}
