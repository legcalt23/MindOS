package com.mindos.app.features.mindboost.speed.domain

import kotlinx.serialization.Serializable

/**
 * MindBoost · Velocidad Mental — enumeraciones núcleo de medición (MVP-0).
 *
 * Identificadores en inglés camelCase/UPPER_SNAKE (acceptance #15); la prosa y la
 * documentación de producto en español. Toda esta capa es `commonMain` y pura, de
 * modo que las pruebas (incluido el simulador del §12) corren en la JVM de escritorio
 * sin dispositivo.
 */

/** Fuente del timestamp temporal usado para una marca de medición (§4.1). */
@Serializable
enum class TimestampSource {
    /** `Choreographer.FrameCallback#doFrame(frameTimeNanos)` — onset de estímulo. */
    CHOREOGRAPHER_FRAME,

    /** Reloj monotónico del sistema (uptime). Usado por la respuesta y por fallbacks. */
    SYSTEM_CLOCK_MONOTONIC,
}

/** Precisión real de la marca temporal de la respuesta táctil (§4.1, reglas 7–8). */
@Serializable
enum class InputTimestampPrecision {
    /** API 34+: `MotionEvent.getEventTimeNanos()` — nanos nativos. */
    NANOS_NATIVE,

    /** API < 34: `MotionEvent.eventTime * 1_000_000L` — derivado de millis. */
    MILLIS_DERIVED,
}

/**
 * Base temporal única de toda la medición. v2.2.3 fija el nombre
 * `ANDROID_UI_UPTIME_NANOS` (Choreographer + MotionEvent, ambos CLOCK_MONOTONIC).
 */
@Serializable
enum class Timebase {
    ANDROID_UI_UPTIME_NANOS,
}

/**
 * Riesgo de precisión temporal inducido EXCLUSIVAMENTE por el hardware/entorno (§8).
 * Ortogonal a la conducta del usuario.
 */
@Serializable
enum class DeviceTimingRisk {
    /** Gama alta, API 34+ con precisión nativa de nanos. */
    LOW,

    /** API < 34 con marcas derivadas de millis. */
    MODERATE,

    /** Inestabilidad crítica del Choreographer o latencia excesiva. */
    HIGH,
}

/**
 * Clasificación psicométrica combinada (hardware + conducta) de una corrida de
 * medición (§8). `CONTAMINATED` se persiste para depuración pero queda marcada para
 * ser excluida de cualquier lógica adaptativa o histórico de perfil.
 */
@Serializable
enum class SessionQuality {
    HIGH,
    ACCEPTABLE,
    LOW,
    CONTAMINATED,
}

/**
 * Ejercicios de la batería. MVP-0/1: E1 (Destello, RT simple) y E6 (Multivía, 2 vías).
 * MVP-2 añade E3 (Cifrado, verificación de código) y E7 (Compuerta, regla condicional).
 * MC9-A añade E2 (Comparación — Number Pulse Compare, 2 vías): comparación rápida de
 * magnitudes con precisión. El resto del catálogo llega en hitos posteriores.
 */
@Serializable
enum class SpeedExerciseId(val code: String) {
    /** Destello — tiempo de reacción simple. */
    E1_DESTELLO("E1"),

    /** Comparación — comparar dos magnitudes y tocar el lado del mayor (2 vías), MC9-A. */
    E2_COMPARA("E2"),

    /** Cambio de Regla — task-switching color/forma con cambio de regla bajo presión (2 vías), MC9-B. */
    E4_CAMBIO("E4"),

    /** Búsqueda Visual — detección de un objetivo entre distractores bajo carga (2 vías), MC9-C. */
    E5_BUSQUEDA("E5"),

    /** Cifrado — verificación de código símbolo↔valor (2 vías), MVP-2. */
    E3_CIFRADO("E3"),

    /** Multivía — elección libre de 2 alternativas. */
    E6_MULTIVIA("E6"),

    /** Compuerta — regla condicional con inversión por puerta (2 vías), MVP-2. */
    E7_COMPUERTA("E7"),
}

/**
 * Alternativa de respuesta en ejercicios de 2 vías (E6/E3/E7). E1 (RT simple) no la usa.
 * Vive en dominio porque los generadores de trials producen la respuesta correcta.
 */
@Serializable
enum class SpeedChoice { LEFT, RIGHT }
