@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation

import android.os.Build
import android.view.Choreographer
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.TimebasePolicy
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase

/**
 * Actual de Android (§4.1). Implementación física real de la TimebasePolicy:
 * - estímulo: `Choreographer.postFrameCallback { frameTimeNanos }`;
 * - respuesta: `MotionEvent.getEventTimeNanos()` (API 34+) o `eventTime*1e6` (API < 34),
 *   delegando la resolución y la clasificación de precisión a [TimebasePolicy].
 *
 * Jamás se usa `elapsedRealtimeNanos()` para RT (§4.1 reglas 4–6).
 */
private class AndroidSpeedTimebaseHandle : SpeedTimebaseHandle {
    override val apiLevel: Int = Build.VERSION.SDK_INT
    override val inputTimestampPrecision: InputTimestampPrecision = TimebasePolicy.precisionFor(apiLevel)
    override val timestampSource: TimestampSource = TimebasePolicy.TRIAL_TIMESTAMP_SOURCE
    override val timebase: Timebase = Timebase.ANDROID_UI_UPTIME_NANOS
    override val deviceModel: String = Build.MODEL ?: "android"
    override val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    // System.nanoTime() comparte la base monotónica de uptime; se usa solo para
    // duraciones/fallback, NUNCA para el RT (que sale de Choreographer + MotionEvent).
    override fun nowMonotonicNanos(): Long = System.nanoTime()

    override fun requestStimulusFrame(onFrame: (Long) -> Unit) {
        Choreographer.getInstance().postFrameCallback { frameTimeNanos -> onFrame(frameTimeNanos) }
    }
}

@Composable
actual fun rememberSpeedTimebaseHandle(): SpeedTimebaseHandle = remember { AndroidSpeedTimebaseHandle() }

@Composable
actual fun SpeedTapArea(
    handle: SpeedTimebaseHandle,
    enabled: Boolean,
    onResponse: (responseUptimeNanos: Long) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val tapModifier = if (enabled) {
        Modifier.pointerInteropFilter { event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val eventTimeNanos = if (Build.VERSION.SDK_INT >= TimebasePolicy.NANOS_NATIVE_MIN_API) {
                    event.eventTimeNanos
                } else {
                    null
                }
                onResponse(
                    TimebasePolicy.resolveResponseUptimeNanos(
                        eventTimeNanos = eventTimeNanos,
                        eventTimeMillis = event.eventTime,
                        apiLevel = Build.VERSION.SDK_INT,
                    ),
                )
                true // consume el ACTION_DOWN; la respuesta ya quedó registrada.
            } else {
                false
            }
        }
    } else {
        Modifier
    }
    Box(modifier.then(tapModifier)) { content() }
}
