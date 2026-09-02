package com.mindos.app.features.mindboost.core.lifecycle

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * FOC-P0-01 · Contract of the platform seam. The invariant is the charter's `null != 0` applied to a
 * lifecycle: "we could not watch the foreground" and "we watched it and nothing happened" are two
 * facts, and a platform without a foreground source must return the FIRST — never an empty
 * [LifecycleObservation.Observed], which downstream would read as a clean session.
 *
 * Mutation this test exists to catch: the desktop `actual` returning `Observed(emptyList())`.
 * Discriminant half: a real trace with one interruption IS observed, so "everything NotObservable" does
 * not pass either.
 *
 * Ceiling, declared: only the DESKTOP `actual` is exercised here. The Android adapter is Compose-bound
 * (`LocalLifecycleOwner`) and is covered by the pure [ForegroundTraceProbeTest] it delegates to; iOS is
 * `BLOQUEADO_IOS` and its `actual` is asserted by reading, not by execution.
 */
@OptIn(ExperimentalTestApi::class)
class AppLifecycleProbeContractTest {

    @Test
    fun theNoOpProbeDeclaresAbsenceWithATypedReason() {
        NoOpAppLifecycleProbe.start()

        assertEquals(
            LifecycleObservation.NotObservable(NotObservableReason.NO_PLATFORM_SOURCE),
            NoOpAppLifecycleProbe.stop(),
        )
    }

    @Test
    fun theDesktopActualDeclaresAbsence_neverAnEmptyObservation() = runComposeUiTest {
        var provided: AppLifecycleProbe? = null
        setContent { provided = rememberAppLifecycleProbe() }
        val probe = assertNotNull(provided, "the desktop actual must provide a probe")

        probe.start()
        val observation = probe.stop()

        assertIs<LifecycleObservation.NotObservable>(
            observation,
            "desktop has no foreground source; reporting Observed(empty) would fabricate a clean window",
        )
    }

    @Test
    fun aRealInterruptionIsObservedWithItsInterval_discriminantHalf() {
        var now = 100L
        val probe = ForegroundTraceProbe { now }
        probe.start()
        now = 250
        probe.onForegroundChanged(inForeground = false)
        now = 400
        probe.onForegroundChanged(inForeground = true)
        now = 500

        assertEquals(
            LifecycleObservation.Observed(listOf(ForegroundLoss(lostAtNanos = 250, regainedAtNanos = 400))),
            probe.stop(),
        )
    }
}
