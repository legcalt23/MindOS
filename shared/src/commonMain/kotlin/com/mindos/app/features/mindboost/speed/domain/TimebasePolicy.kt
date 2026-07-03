package com.mindos.app.features.mindboost.speed.domain

/**
 * TimebasePolicy Android (§4.1), expresada como funciones PURAS para poder probarla en
 * `commonTest` sin dispositivo (acceptance #1, #2, #16; "add tests or test seams proving
 * this policy").
 *
 * Base temporal única: ANDROID_UI_UPTIME_NANOS (CLOCK_MONOTONIC / uptime), compartida
 * por `Choreographer.frameTimeNanos` (estímulo) y `MotionEvent` (respuesta).
 *
 * Prohibiciones (§4.1 reglas 4–6):
 * - JAMÁS calcular `rtNanos` con `SystemClock.elapsedRealtimeNanos()`.
 * - JAMÁS restar `elapsedRealtimeNanos()` contra marcas de Choreographer/MotionEvent.
 * - `elapsedRealtimeNanos()` solo para duraciones globales/timeouts, nunca para RT.
 *
 * Esta clase NO importa nada de plataforma: encapsula exclusivamente la aritmética y la
 * clasificación. El `actual` de Android se limita a leer las marcas y delegar aquí.
 */
object TimebasePolicy {

    /** Factor de conversión de millis a nanos para el fallback de API < 34. */
    const val MILLIS_TO_NANOS: Long = 1_000_000L

    /** API a partir de la cual `MotionEvent.getEventTimeNanos()` está disponible. */
    const val NANOS_NATIVE_MIN_API: Int = 34

    /** La fuente de referencia del trial es el frame del Choreographer (onset). */
    val TRIAL_TIMESTAMP_SOURCE: TimestampSource = TimestampSource.CHOREOGRAPHER_FRAME

    /** Precisión obligatoria según el nivel de API (§4.1 reglas 7–8). */
    fun precisionFor(apiLevel: Int): InputTimestampPrecision =
        if (apiLevel >= NANOS_NATIVE_MIN_API) {
            InputTimestampPrecision.NANOS_NATIVE
        } else {
            InputTimestampPrecision.MILLIS_DERIVED
        }

    /**
     * Resuelve `responseUptimeNanos` (§4.1 regla 3):
     * - API 34+: `eventTimeNanos` nativo (si está presente).
     * - API < 34 (o nanos ausente): `eventTimeMillis * 1_000_000`.
     */
    fun resolveResponseUptimeNanos(eventTimeNanos: Long?, eventTimeMillis: Long, apiLevel: Int): Long =
        if (apiLevel >= NANOS_NATIVE_MIN_API && eventTimeNanos != null) {
            eventTimeNanos
        } else {
            eventTimeMillis * MILLIS_TO_NANOS
        }

    /**
     * `rtNanos = responseUptimeNanos - stimulusUptimeNanos` (§4.1 regla 1). Ambos en la
     * misma base monotónica. NUNCA usa `elapsedRealtimeNanos()`.
     */
    fun computeRtNanos(stimulusUptimeNanos: Long, responseUptimeNanos: Long): Long =
        responseUptimeNanos - stimulusUptimeNanos

    /** Un RT es físicamente válido si la respuesta no precede al estímulo (§ TrialEngine). */
    fun isValidRt(rtNanos: Long): Boolean = rtNanos >= 0L

    /**
     * Riesgo temporal del dispositivo (§8), inducido solo por hardware/entorno:
     * - jitter crítico del Choreographer ⇒ HIGH;
     * - precisión nativa de nanos ⇒ LOW;
     * - precisión derivada de millis ⇒ MODERATE.
     */
    fun deviceTimingRisk(
        precision: InputTimestampPrecision,
        criticalFrameJitter: Boolean,
    ): DeviceTimingRisk = when {
        criticalFrameJitter -> DeviceTimingRisk.HIGH
        precision == InputTimestampPrecision.NANOS_NATIVE -> DeviceTimingRisk.LOW
        else -> DeviceTimingRisk.MODERATE
    }
}
