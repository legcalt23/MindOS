// Composables expect en PascalCase (SpeedTapArea) + interfaz como top-level junto a las
// factories: patrón idiomático de Compose. Se suprimen las reglas de naming/archivo.
@file:Suppress("FunctionNaming", "MatchingDeclarationName")

package com.mindos.app.features.mindboost.speed.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.TimestampProvider
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase
import com.mindos.app.features.mindboost.speed.domain.metrics.MeasurementContext

/**
 * ÚNICA superficie de temporización por plataforma del feature (espejo de
 * `SecureKeyValueStore`). Android provee la captura real Choreographer (estímulo) +
 * MotionEvent (respuesta); desktop/iOS usan un fallback monotónico documentado y
 * NO-autoritativo (MVP-0 es solo Android — §2).
 *
 * El resto de la presentación (pantallas E1/E6, state holder) es `commonMain` y consume
 * esta superficie a través de [SpeedTimebaseHandle] + [SpeedTapArea].
 */
interface SpeedTimebaseHandle {
    val timestampSource: TimestampSource
    val inputTimestampPrecision: InputTimestampPrecision
    val timebase: Timebase
    val apiLevel: Int

    /** Modelo del dispositivo (para [com...DeviceProfile]). No es un identificador de hardware. */
    val deviceModel: String

    /** Versión del SO (para [com...DeviceProfile]). */
    val osVersion: String

    /** Marca monotónica actual (nanos). Para duraciones/fallback, no para RT en Android. */
    fun nowMonotonicNanos(): Long

    /**
     * Solicita el timestamp del frame de ONSET del estímulo. En Android publica un
     * `Choreographer.FrameCallback` y entrega su `frameTimeNanos`; en fallback entrega
     * [nowMonotonicNanos] de inmediato.
     */
    fun requestStimulusFrame(onFrame: (stimulusUptimeNanos: Long) -> Unit)

    /** Metadata temporal para sellar trials/métricas (acceptance #3). */
    fun measurementContext(): MeasurementContext =
        MeasurementContext(timestampSource, inputTimestampPrecision, timebase)

    /** Adaptador a la interfaz §4 [TimestampProvider]. */
    fun asTimestampProvider(): TimestampProvider = object : TimestampProvider {
        override fun getCurrentMonotonicNanos(): Long = nowMonotonicNanos()
        override fun getTimestampSource(): TimestampSource = timestampSource
        override fun getInputTimestampPrecision(): InputTimestampPrecision = inputTimestampPrecision
        override fun getTimebase(): Timebase = timebase
    }
}

/** Provee el handle de temporización de la plataforma. */
@Composable
expect fun rememberSpeedTimebaseHandle(): SpeedTimebaseHandle

/**
 * Área táctil que captura el timestamp de respuesta. En Android lee el `MotionEvent`
 * (nanos nativos en API 34+, `eventTime*1e6` por debajo); en fallback usa el reloj
 * monotónico. Invoca [onResponse] con `responseUptimeNanos` en el ACTION_DOWN.
 */
@Composable
expect fun SpeedTapArea(
    handle: SpeedTimebaseHandle,
    enabled: Boolean,
    onResponse: (responseUptimeNanos: Long) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
)
