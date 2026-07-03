package com.mindos.app.features.mindboost.speed.domain

/**
 * Proveedor de marcas temporales monotónicas (§4). Interfaz `commonMain` fiel al plan;
 * la implementación física (Choreographer + MotionEvent) vive en `androidMain` y los
 * fallbacks de desktop/iOS son no-autoritativos (MVP-0 es solo Android).
 */
interface TimestampProvider {
    /** Marca monotónica actual en nanos (base ANDROID_UI_UPTIME_NANOS en Android). */
    fun getCurrentMonotonicNanos(): Long

    fun getTimestampSource(): TimestampSource

    fun getInputTimestampPrecision(): InputTimestampPrecision

    /** Siempre ANDROID_UI_UPTIME_NANOS en MVP-0. */
    fun getTimebase(): Timebase = Timebase.ANDROID_UI_UPTIME_NANOS
}
