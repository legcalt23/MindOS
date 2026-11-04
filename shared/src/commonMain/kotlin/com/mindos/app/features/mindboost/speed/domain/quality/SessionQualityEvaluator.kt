// QualitySignals y DeviceProfileFactory.create agrupan muchas señales por diseño del §8.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.quality

import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.QualityReason
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.TimebasePolicy
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile

/** Decisión de calidad con sus motivos auditables (alimenta [com...QualityDecisionLog]). */
data class QualityDecision(
    val deviceTimingRisk: DeviceTimingRisk,
    val sessionQuality: SessionQuality,
    val reasons: List<String>,
)

/** Señales conductuales/técnicas de entrada para evaluar la calidad de una corrida. */
data class QualitySignals(
    val totalTrials: Int,
    val cv: Double?,
    val anticipationRate: Double,
    val omissionRate: Double,
    val inputPrecision: InputTimestampPrecision,
    /**
     * Deltas inter-frame REALMENTE observados en la corrida. `0` significa «no se midió», y es un hecho
     * DISTINTO de «se midió y salió limpio»: sin este campo, [frameDropFraction] `= 0.0` y
     * [criticalFrameJitter] `= false` —los valores que lleva `FrameStats.UNAVAILABLE`— son
     * indistinguibles de un dispositivo medido e impecable (`MB-FOCUS-0071`).
     *
     * OBLIGATORIO a propósito, sin default: un default reinstauraría en silencio la fabricación que este
     * campo existe para impedir.
     */
    val sampledFrames: Int,
    val frameDropFraction: Double,
    val criticalFrameJitter: Boolean,
    val source: TimestampSource,
)

/**
 * Clasifica una corrida en [SessionQuality] separando el riesgo de hardware
 * ([DeviceTimingRisk]) de la estabilidad conductual (§8). Registra SIEMPRE los motivos
 * de una clasificación `LOW`/`CONTAMINATED` (acceptance #17, #22).
 *
 * Precedencia: CONTAMINATED > LOW > (HIGH | ACCEPTABLE). Una corrida `CONTAMINATED` se
 * persiste para depuración pero queda marcada para exclusión de cualquier lógica
 * adaptativa o histórico de perfil (§8; en MVP-0 no hay adaptación activa todavía).
 */
class SessionQualityEvaluator(
    private val thresholds: MetricThresholds = MetricThresholds.DEFAULT,
    private val minTrialsForQuality: Int = DEFAULT_MIN_TRIALS,
) {

    fun evaluate(signals: QualitySignals): QualityDecision {
        val reasons = mutableListOf<String>()

        // SAMPLE GATE PRIMERO (`MB-FOCUS-0071`, espejo de la guarda portante nº1 de
        // `core.timing.TemporalClassPolicy`): `FrameStats.UNAVAILABLE` lleva `frameDropFraction = 0.0` y
        // `criticalFrameJitter = false`, así que cualquier umbral evaluado antes de esta guarda fabrica
        // perfección a partir de la ausencia. Sin frames medidos no se puede afirmar contaminación —la
        // ausencia NO acusa— ni conceder HIGH, que afirma un dispositivo medido y limpio.
        val framesMeasured = signals.sampledFrames > 0
        if (!framesMeasured) {
            reasons += QualityReason.FRAME_TIMING_UNMEASURED
        }

        // SEGUNDO canal de la MISMA ausencia (`MB-FOCUS-0071`). Esta derivación vivía ARRIBA, antes
        // del sample gate, así que una sesión sin un solo frame observado publicaba el riesgo MÍNIMO:
        // `criticalFrameJitter = false` viaja dentro de `FrameStats.UNAVAILABLE`, la rama HIGH no se
        // toma, y con precisión nativa el `when` cae directo en LOW. Es la misma fabricación desde la
        // ausencia que el gate de abajo ya impedía para `sessionQuality` — y ésta es la que SÍ ve el
        // usuario (`SpeedExerciseScreen` la pinta como «Precisión de marca temporal»).
        //
        // Dirección fail-closed idéntica al resto: no AFIRMAR (nada de LOW sin medida) y jamás ACUSAR
        // (nada de HIGH con un jitter que nadie observó). MODERATE es el bucket «no afirmado limpio».
        // TECHO DECLARADO: MODERATE queda sobrecargado —«millis-derived» y «sin medir» comparten valor—
        // y lo que los desambigua es `FRAME_TIMING_UNMEASURED` en `reasons`, que se persiste con la
        // decisión. Modelarlo en el propio tipo exige `UNKNOWN`/nullable en un enum SERIALIZADO
        // (`QualityDecisionLog`), decisión de esquema durable que no pertenece a esta fase.
        val deviceTimingRisk = if (framesMeasured) {
            TimebasePolicy.deviceTimingRisk(
                precision = signals.inputPrecision,
                criticalFrameJitter = signals.criticalFrameJitter,
            )
        } else {
            DeviceTimingRisk.MODERATE
        }

        val contaminated = framesMeasured && evaluateContamination(signals, reasons)
        val low = evaluateBehavioralInstability(signals, reasons)

        if (signals.inputPrecision == InputTimestampPrecision.MILLIS_DERIVED) {
            // Informativo: no degrada por sí solo a LOW, pero impide HIGH (no es nanos nativos).
            reasons += QualityReason.INPUT_PRECISION_MILLIS_DERIVED
        }

        // El onset autoritativo es el frame del Choreographer. Una fuente monotónica (fallback
        // desktop/iOS) NO es autoritativa para el RT: informativa, no degrada por sí sola a LOW,
        // pero impide HIGH aunque la precisión sea nativa (espeja TimingPolicyClassifier).
        val authoritativeSource = signals.source == TimestampSource.CHOREOGRAPHER_FRAME
        if (!authoritativeSource) {
            reasons += QualityReason.TIMESTAMP_SOURCE_NON_FRAME
        }

        val quality = when {
            contaminated -> SessionQuality.CONTAMINATED
            low -> SessionQuality.LOW
            framesMeasured && deviceTimingRisk == DeviceTimingRisk.LOW && authoritativeSource ->
                SessionQuality.HIGH
            else -> SessionQuality.ACCEPTABLE
        }

        return QualityDecision(deviceTimingRisk, quality, reasons.distinct())
    }

    private fun evaluateContamination(signals: QualitySignals, reasons: MutableList<String>): Boolean {
        var contaminated = false
        if (signals.criticalFrameJitter) {
            reasons += QualityReason.FRAME_JITTER_DETECTED
            contaminated = true
        }
        if (signals.frameDropFraction > thresholds.contaminatedFrameDropThreshold) {
            reasons += QualityReason.CONTAMINATED_SYSTEM_NOISE
            contaminated = true
        }
        return contaminated
    }

    private fun evaluateBehavioralInstability(signals: QualitySignals, reasons: MutableList<String>): Boolean {
        var low = false
        if (signals.totalTrials < minTrialsForQuality) {
            reasons += QualityReason.LOW_TRIAL_COUNT
            low = true
        }
        if (signals.cv != null && signals.cv > thresholds.highCvThreshold) {
            reasons += QualityReason.HIGH_CV
            low = true
        }
        if (signals.anticipationRate > thresholds.highAnticipationRateThreshold) {
            reasons += QualityReason.HIGH_ANTICIPATION_RATE
            low = true
        }
        if (signals.omissionRate > thresholds.highOmissionRateThreshold) {
            reasons += QualityReason.HIGH_OMISSION_RATE
            low = true
        }
        return low
    }

    private companion object {
        const val DEFAULT_MIN_TRIALS = 8
    }
}

/**
 * Construye un [DeviceProfile] a partir de datos crudos de dispositivo, calculando su
 * [DeviceTimingRisk] vía [TimebasePolicy]. El `id` debe ser estable por dispositivo para
 * que el perfil pueda compartirse entre usuarios (§16).
 */
object DeviceProfileFactory {
    fun create(
        id: String,
        deviceModel: String,
        osVersion: String,
        apiLevel: Int,
        simpleReactionMedianMs: Double,
        criticalFrameJitter: Boolean = false,
    ): DeviceProfile {
        val precision = TimebasePolicy.precisionFor(apiLevel)
        return DeviceProfile(
            id = id,
            deviceModel = deviceModel,
            osVersion = osVersion,
            apiLevel = apiLevel,
            simpleReactionMedianMs = simpleReactionMedianMs,
            deviceTimingRisk = TimebasePolicy.deviceTimingRisk(precision, criticalFrameJitter),
        )
    }
}
