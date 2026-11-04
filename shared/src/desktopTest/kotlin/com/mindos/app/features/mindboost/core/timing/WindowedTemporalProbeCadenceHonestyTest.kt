package com.mindos.app.features.mindboost.core.timing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * S5 · Paso 7 (exit gate) — **no adapter may substitute a nominal cadence for an absent one.**
 *
 * `TemporalClassPolicy` refuses to classify a window whose expected cadence is missing, and Paso 5
 * made iOS declare that absence honestly (`MB-FOCUS-0065`). The Android adapter did not: it fell back
 * to `DEFAULT_REFRESH_HZ` whenever `View.getDisplay()` returned null or a nonsense rate, so on a 60 Hz
 * panel the fabricated expectation AGREED with the detected cadence, the window resolved, and a
 * verdict was derived from a constant nobody measured. The policy guard could not see it — by the time
 * the observation arrived, absence had already been dressed as a statement.
 *
 * These adapters cannot run on the JVM, so the guard is on their SOURCE, in the same idiom as
 * `Focus2PlatformPermissionTest`. The third test pins the RULE the source guard exists to protect, so
 * weakening the policy turns this red too rather than leaving a guard that protects nothing.
 *
 * **Scope widened (`MB-FOCUS-0071`).** The guard used to name only the two `WindowedTemporalProbe`
 * files, and that filename bound is precisely why it could not see the VELOCIDAD probe
 * (`FrameTimingProbe.android.kt`), which kept the very fallback this rule forbids — the reason that
 * finding was re-severized P3→P1. It now covers both probe families plus the shared calculator, so the
 * rule is bounded by the INVARIANT rather than by the two filenames that happened to hold it first.
 * (The class keeps its name: the ledger and the S5 docs cite it, and renaming would break those
 * citations for no gain. Its subject is «no probe may substitute a nominal cadence for an absent one».)
 *
 * The Velocidad-side rule that these source guards protect — an unmeasured session may not be rated
 * `HIGH` — is pinned by `SessionQualityEvaluatorTest`, not duplicated here: one invariant, one owner.
 */
class WindowedTemporalProbeCadenceHonestyTest {

    @Test
    fun theAndroidAdapterDeclaresTheStatedCadenceAsNullable() {
        val source = locate(ANDROID_ADAPTER).readText()
        assertTrue(
            source.contains("refreshHzProvider: () -> Double?"),
            "the Android provider must be able to say 'the platform stated nothing' ($ANDROID_ADAPTER)",
        )
    }

    /**
     * Whole-file, deliberately: a first draft of this guard scoped itself to the provider lambda with
     * `substringBefore("}")`, which stops at the closing brace of the `takeIf { … }` INSIDE the lambda —
     * so a re-added `?: DEFAULT_REFRESH_HZ` sat outside the slice and the guard stayed green under
     * mutation. The constant has no other legitimate use in this adapter, so the honest bound is the
     * file. Re-introducing it has to be argued for, which is the point.
     */
    @Test
    fun theAndroidAdapterNeverNamesTheNominalFallback() {
        val source = locate(ANDROID_ADAPTER).readText()

        assertFalse(
            source.contains(FALLBACK_CONSTANT),
            "the Android adapter must not fill an absent stated cadence with $FALLBACK_CONSTANT",
        )
    }

    /**
     * The iOS side of the same rule, pinned separately because its shape differs: iOS DOES use the
     * nominal constant to compute descriptive stats, but must keep declaring the STATED cadence absent
     * (`MB-FOCUS-0065`). Asserting "no constant anywhere" there would be wrong; asserting the null
     * declaration is exactly the invariant.
     */
    @Test
    fun theIosAdapterKeepsDeclaringTheStatedCadenceAbsent() {
        val source = locate(IOS_ADAPTER).readText()

        assertTrue(
            source.contains("expectedCadenceHz = null"),
            "iOS must declare it cannot state a cadence, not infer one ($IOS_ADAPTER)",
        )
    }

    @Test
    fun anAbsentStatedCadenceIsUnclassifiable_theRuleTheSourceGuardProtects() {
        val observed = TemporalWindowObservation(
            sampledFrames = 60,
            droppedFrames = 0,
            criticalFrameJitter = false,
            expectedCadenceHz = null,
            detectedCadenceHz = 60.0,
        )

        assertEquals(
            TemporalWindowVerdict.NoObservation,
            TemporalClassPolicy.windowIntegrity(observed),
            "a window with no stated cadence has no definition of a dropped frame",
        )
    }

    // ── MB-FOCUS-0071 · la sonda de la lane de Velocidad, bajo la MISMA regla ──

    @Test
    fun theVelocidadProbeDeclaresTheStatedCadenceAsNullable() {
        val source = locate(VELOCIDAD_PROBE).readText()
        assertTrue(
            source.contains("refreshHzProvider: () -> Double?"),
            "the Velocidad provider must be able to say 'the platform stated nothing' ($VELOCIDAD_PROBE)",
        )
    }

    @Test
    fun theVelocidadProbeNeverNamesTheNominalFallback() {
        val source = locate(VELOCIDAD_PROBE).readText()
        assertFalse(
            source.contains(FALLBACK_CONSTANT),
            "the Velocidad probe must not fill an absent stated cadence with $FALLBACK_CONSTANT",
        )
    }

    /**
     * The re-fabrication vector a per-file textual guard structurally CANNOT see: a default argument
     * lets a future caller re-introduce the 60 Hz nominal without ever naming the constant in its own
     * file. Every production call-site already passes the cadence explicitly, so the default bought
     * nothing and cost the invariant.
     */
    @Test
    fun theSharedCalculatorNeverDefaultsTheReferenceCadence() {
        val source = locate(CALCULATOR).readText()
        assertFalse(
            source.contains("refreshHz: Double = "),
            "refreshHz must stay mandatory: a default re-fabricates a nominal without naming it ($CALCULATOR)",
        )
    }

    private fun locate(relative: String): File {
        var base: File? = File(".").absoluteFile
        repeat(MAX_LOOKUP_DEPTH) {
            val current = base ?: error("no parent")
            File(current, relative).takeIf { it.exists() }?.let { return it }
            base = current.parentFile
        }
        error("expected to locate $relative from ${File(".").absolutePath}")
    }

    private companion object {
        const val ANDROID_ADAPTER =
            "shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/core/timing/" +
                "WindowedTemporalProbe.android.kt"
        const val IOS_ADAPTER =
            "shared/src/iosMain/kotlin/com/mindos/app/features/mindboost/core/timing/" +
                "WindowedTemporalProbe.ios.kt"
        const val VELOCIDAD_PROBE =
            "shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/core/timing/" +
                "FrameTimingProbe.android.kt"
        const val CALCULATOR =
            "shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/" +
                "FrameStatsCalculator.kt"
        const val FALLBACK_CONSTANT = "DEFAULT_REFRESH_HZ"
        const val MAX_LOOKUP_DEPTH = 8
    }
}
