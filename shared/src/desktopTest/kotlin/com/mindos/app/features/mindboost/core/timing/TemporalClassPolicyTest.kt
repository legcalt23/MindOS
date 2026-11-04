package com.mindos.app.features.mindboost.core.timing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §17 [R2-HIST] — pins the two load-bearing guards of [TemporalClassPolicy]:
 * sample-gate-first (absence never reads CLEAN, criterio 7) and the {T0, T1} codomain (T2/T3 have
 * no producer, §4.1 [R2-HIST]), plus the marker/no-marker branch semantics of the truncation carve-out.
 */
class TemporalClassPolicyTest {

    private val thresholds = TemporalQualityThresholds.DEFAULT

    private fun obs(
        sampled: Int,
        dropped: Int,
        critical: Boolean = false,
        markerNanos: Long? = null,
        overflow: Boolean = false,
        // A normally observed window: the platform stated a cadence and that cadence is what arrived.
        expectedHz: Double? = 60.0,
        detectedHz: Double? = 60.0,
    ) = TemporalWindowObservation(
        sampledFrames = sampled,
        droppedFrames = dropped,
        criticalFrameJitter = critical,
        presentToFirstFrameCommitNanos = markerNanos,
        bufferOverflow = overflow,
        expectedCadenceHz = expectedHz,
        detectedCadenceHz = detectedHz,
    )

    // --- P0-3: the fabricated-CLEAN trap (gate on sample BEFORE thresholds) ---

    @Test
    fun unavailableShapedObservation_isNoObservation_neverClean() {
        // Same shape as FrameStats.UNAVAILABLE: zero frames, zero drops, no jitter — looks "perfect".
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 0, dropped = 0), thresholds)
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun nullObservation_isNoObservation() {
        assertEquals(
            TemporalWindowVerdict.NoObservation,
            TemporalClassPolicy.windowIntegrity(null, thresholds),
        )
    }

    @Test
    fun windowBelowMinFrames_isNoObservation() {
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 2, dropped = 0), thresholds)
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    // --- Paso 5 / B1: an INCOMPLETE sample is not a clean one, and is not the user's fault either ---

    @Test
    fun bufferOverflow_isNoObservation_evenWhenTheRetainedSampleLooksPerfect() {
        // The retained tail of an overflowed ring can look flawless precisely BECAUSE the janky part
        // was overwritten. Classifying it CLEAN would let a lost sample fabricate perfection.
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 0, overflow = true),
            thresholds = thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun bufferOverflow_neverWithdrawsAccuracy() {
        // Fail-closed does NOT mean Excluded here: our buffer being too small is a fact about the
        // instrument, never evidence that the user's trial was truncated. Only Excluded withdraws
        // accuracy, so overflow must not reach it — not even with drops far above the exclude floor.
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 90, critical = true, overflow = true),
            thresholds = thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun theSameSampleWithoutOverflowIsStillClassified() {
        // DISCRIMINATING HALF: the overflow flag is what changes the verdict, not the sample size.
        assertEquals(
            TemporalWindowVerdict.Clean,
            TemporalClassPolicy.windowIntegrity(obs(sampled = 100, dropped = 0), thresholds),
        )
    }

    // --- Session class: capability gates before stats; codomain is {T0, T1} ---

    @Test
    fun sessionClass_noOpCapability_isT0_evenWithManyFrames() {
        val clazz = TemporalClassPolicy.sessionClass(
            capability = ProbeCapability.NONE_MONOTONIC_FALLBACK,
            clockDomain = ClockDomain.NONE_MONOTONIC_FALLBACK,
            sampledFramesSession = 10_000,
            thresholds = thresholds,
        )
        assertEquals(TemporalClass.T0_NON_AUTHORITATIVE, clazz)
    }

    @Test
    fun sessionClass_domainMismatch_isT0() {
        val clazz = TemporalClassPolicy.sessionClass(
            capability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.IOS_CADISPLAYLINK_MACH_TIME,
            sampledFramesSession = 10_000,
            thresholds = thresholds,
        )
        assertEquals(TemporalClass.T0_NON_AUTHORITATIVE, clazz)
    }

    @Test
    fun sessionClass_authoritative_belowSessionFloor_isT0() {
        val clazz = TemporalClassPolicy.sessionClass(
            capability = ProbeCapability.ANDROID_CHOREOGRAPHER,
            clockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            sampledFramesSession = thresholds.minFramesSession - 1,
            thresholds = thresholds,
        )
        assertEquals(TemporalClass.T0_NON_AUTHORITATIVE, clazz)
    }

    @Test
    fun sessionClass_authoritative_atFloor_isT1() {
        val clazz = TemporalClassPolicy.sessionClass(
            capability = ProbeCapability.IOS_CADISPLAYLINK,
            clockDomain = ClockDomain.IOS_CADISPLAYLINK_MACH_TIME,
            sampledFramesSession = thresholds.minFramesSession,
            thresholds = thresholds,
        )
        assertEquals(TemporalClass.T1_FRAME_OBSERVED, clazz)
    }

    @Test
    fun policyCodomain_isOnlyT0OrT1_forEveryInputCombination() {
        val reachable = setOf(TemporalClass.T0_NON_AUTHORITATIVE, TemporalClass.T1_FRAME_OBSERVED)
        for (capability in ProbeCapability.entries) {
            for (domain in ClockDomain.entries) {
                for (frames in listOf(0, thresholds.minFramesSession - 1, thresholds.minFramesSession, 10_000)) {
                    val clazz = TemporalClassPolicy.sessionClass(capability, domain, frames, thresholds)
                    val msg = "T2/T3 must be unreachable, got $clazz for ($capability, $domain, $frames)"
                    assertTrue(clazz in reachable, msg)
                }
            }
        }
    }

    // --- The single conservative branch (Paso 5: the marker branch is gone) ---

    @Test
    fun criticalJitter_excludes() {
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 30, dropped = 1, critical = true), thresholds)
        assertEquals(TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK), verdict)
    }

    @Test
    fun dropsAboveExcludeFloor_excludes() {
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 100, dropped = 16), thresholds)
        assertEquals(TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK), verdict)
    }

    @Test
    fun dropsBetweenCleanAndExclude_degrades() {
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 100, dropped = 10), thresholds)
        assertEquals(TemporalWindowVerdict.DegradedUsable, verdict)
    }

    @Test
    fun lightDrops_stayClean_discriminantHalf() {
        // The honest window must SURVIVE, or the "fix" is clamping everything to excluded.
        val verdict = TemporalClassPolicy.windowIntegrity(obs(sampled = 100, dropped = 2), thresholds)
        assertEquals(TemporalWindowVerdict.Clean, verdict)
    }

    // --- Paso 5 / B4: expected cadence and delivered cadence are two facts, and both are required ---

    @Test
    fun withoutAnExpectedCadence_thereIsNothingToDropFrom() {
        // The iOS-in-S5 case: CADisplayLink cannot state what THIS app's loop was targeting. Zero
        // drops against an unstated expectation is not a clean window, it is no window.
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 0, expectedHz = null, detectedHz = 60.0),
            thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun withoutADeliveredCadence_theWindowIsUnresolved() {
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 0, expectedHz = 60.0, detectedHz = null),
            thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun sixtyFramesOnA120HzPanel_isUnresolved_notATotallyDroppedWindow() {
        // Pacing at 60 on a 120 Hz panel makes EVERY delta twice the nominal period, so the drop
        // fraction reads 1.0 and the old single-refreshHz model would have excluded the trial —
        // withdrawing an honest user's accuracy for a pacing mode. Unresolved is the honest verdict.
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 100, expectedHz = 120.0, detectedHz = 60.0),
            thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun halfRatePacing_isUnresolved_ratherThanFlawless() {
        // The mirror error, and the worse one: classify the stream against ITSELF (what the iOS
        // adapter used to do) and a half-rate window looks perfectly clean.
        val verdict = TemporalClassPolicy.windowIntegrity(
            obs(sampled = 100, dropped = 0, expectedHz = 60.0, detectedHz = 30.0),
            thresholds,
        )
        assertEquals(TemporalWindowVerdict.NoObservation, verdict)
    }

    @Test
    fun aCadenceWithinToleranceStillClassifies_discriminantHalf() {
        // Real panels never report exactly their nominal rate; if the gate were exact equality,
        // nothing would ever be classified and "unresolved" would be meaningless.
        assertEquals(
            TemporalWindowVerdict.Clean,
            TemporalClassPolicy.windowIntegrity(
                obs(sampled = 100, dropped = 0, expectedHz = 60.0, detectedHz = 59.94),
                thresholds,
            ),
        )
    }

    @Test
    fun aNonFiniteCadenceIsUnresolved() {
        assertEquals(
            TemporalWindowVerdict.NoObservation,
            TemporalClassPolicy.windowIntegrity(
                obs(sampled = 100, dropped = 0, expectedHz = Double.NaN, detectedHz = Double.NaN),
                thresholds,
            ),
        )
    }

    // --- AC#3 (Paso 5, B6/L1/D-R3-03): the render marker is a PROXY and must not classify ---

    @Test
    fun theRenderMarkerNeverChangesAVerdict() {
        // The property, not an example: for every window shape, the verdict is IDENTICAL whatever the
        // marker says — including markers far past any plausible response window. Re-introducing a
        // marker gate (the R2 behaviour: delay > slack ⇒ Excluded) turns at least one of these red.
        val shapes = listOf(
            obs(sampled = 100, dropped = 0),
            obs(sampled = 100, dropped = 10),
            obs(sampled = 100, dropped = 90),
            obs(sampled = 30, dropped = 1, critical = true),
            obs(sampled = 1, dropped = 0),
        )
        val markers = listOf(null, 0L, 1_000L, 200_000_000L, 600_000_000L, Long.MAX_VALUE)

        for (shape in shapes) {
            val baseline = TemporalClassPolicy.windowIntegrity(shape, thresholds)
            for (marker in markers) {
                val withMarker = shape.copy(presentToFirstFrameCommitNanos = marker)
                assertEquals(
                    baseline,
                    TemporalClassPolicy.windowIntegrity(withMarker, thresholds),
                    "marker $marker must not classify (shape: $shape)",
                )
            }
        }
    }

    @Test
    fun aCleanWindowWithAHugeMarkerDelayIsStillClean() {
        // The exact case the R2 excluded. Naming it separately so the regression is legible: the
        // frame pipeline being late is not evidence that the user's response window was truncated.
        assertEquals(
            TemporalWindowVerdict.Clean,
            TemporalClassPolicy.windowIntegrity(obs(sampled = 30, dropped = 0, markerNanos = 600_000_000L), thresholds),
        )
    }

    // --- AC#4: every exclusion reason has a live producer (no phantom taxonomy) ---

    @Test
    fun everyExclusionReasonIsProducedSomewhereInProductionCode() {
        // Source scanner, not a hardcoded list: the R2 shipped ONSET_INTEGRITY and PRE_ONSET_RESPONSE
        // with no producer anywhere, which reads as coverage that does not exist. Adding an entry
        // without a producer now fails here, and deleting one that IS produced fails to compile.
        val sources = commonMainSources()
        assertTrue(sources.size >= MIN_SCANNED_SOURCES, "anti-vacuity: only ${sources.size} commonMain sources scanned")
        val declaring = "TemporalWindowVerdict.kt"
        // A CONSTRUCTION SITE, not a mention. Two mention shapes that are not producers and used to
        // pass: a consumer that names the entry with a SUFFIX (`REASON.name`) - hence the negative
        // lookahead - and a comment that spells it out - hence stripping comments before matching.
        //
        // CEILING, declared rather than papered over: a bare `== REASON`, a `when` branch or an
        // `import` of the entry still satisfy this and would still count as a producer. No such site
        // exists in this tree today - the only matches outside the declaring file are the three real
        // constructions - and telling those apart from a construction is not something a regex does
        // robustly. Recorded as the residual of `MB-FOCUS-0078`, not claimed as closed.
        val orphans = WindowExclusionReason.entries.filter { reason ->
            val site = Regex("""WindowExclusionReason\.${reason.name}\b(?!\s*\.)""")
            sources.none { it.name != declaring && site.containsMatchIn(codeWithoutComments(it.readText())) }
        }
        assertTrue(orphans.isEmpty(), "exclusion reasons with no producer in commonMain: ${orphans.map { it.name }}")
    }

    private fun codeWithoutComments(text: String): String = text
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\r\n]*"""), " ")

    private fun commonMainSources(): List<File> {
        val relative = "src/commonMain/kotlin/com/mindos/app/features/mindboost"
        var base: File? = File(".").absoluteFile
        repeat(MAX_LOOKUP_DEPTH) {
            val current = base ?: return emptyList()
            for (candidate in listOf(File(current, relative), File(current, "shared/$relative"))) {
                if (candidate.isDirectory) {
                    return candidate.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
                }
            }
            base = current.parentFile
        }
        error("expected to locate commonMain sources from ${File(".").absolutePath}")
    }

    private companion object {
        const val MAX_LOOKUP_DEPTH = 8
        const val MIN_SCANNED_SOURCES = 100
    }
}
