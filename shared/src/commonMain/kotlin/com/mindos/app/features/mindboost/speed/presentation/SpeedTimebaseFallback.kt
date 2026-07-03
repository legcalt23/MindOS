@file:Suppress("FunctionNaming", "MatchingDeclarationName")

package com.mindos.app.features.mindboost.speed.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase
import kotlin.time.TimeSource

/**
 * Fallback NO-autoritativo de temporización para targets que NO son el objetivo de
 * medición de MVP-0 (desktop/iOS). Usa `TimeSource.Monotonic` (commonMain) en lugar de
 * Choreographer/MotionEvent. Se centraliza aquí para que desktop e iOS no dupliquen
 * código; sus `actual` solo delegan.
 *
 * Marca la precisión como [InputTimestampPrecision.MILLIS_DERIVED] y la fuente como
 * [TimestampSource.SYSTEM_CLOCK_MONOTONIC] para que cualquier dato producido fuera de
 * Android quede señalado como no-nativo.
 */
internal class FallbackSpeedTimebaseHandle : SpeedTimebaseHandle {
    private val origin = TimeSource.Monotonic.markNow()
    override val apiLevel: Int = 0
    override val inputTimestampPrecision: InputTimestampPrecision = InputTimestampPrecision.MILLIS_DERIVED
    override val timestampSource: TimestampSource = TimestampSource.SYSTEM_CLOCK_MONOTONIC
    override val timebase: Timebase = Timebase.ANDROID_UI_UPTIME_NANOS
    override val deviceModel: String = "non-android-fallback"
    override val osVersion: String = "fallback"

    override fun nowMonotonicNanos(): Long = origin.elapsedNow().inWholeNanoseconds

    override fun requestStimulusFrame(onFrame: (Long) -> Unit) = onFrame(nowMonotonicNanos())
}

@Composable
internal fun rememberFallbackSpeedTimebaseHandle(): SpeedTimebaseHandle = remember { FallbackSpeedTimebaseHandle() }

@Composable
internal fun FallbackTapArea(
    handle: SpeedTimebaseHandle,
    enabled: Boolean,
    onResponse: (responseUptimeNanos: Long) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val tapModifier = if (enabled) {
        Modifier.pointerInput(handle) {
            detectTapGestures(onPress = { onResponse(handle.nowMonotonicNanos()) })
        }
    } else {
        Modifier
    }
    Box(modifier.then(tapModifier)) { content() }
}
