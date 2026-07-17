package com.mindos.app.features.mindboost.speed.presentation.e8

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 4B · F8 anti-regression guard.
 *
 * The E8 productive path must NEVER write telemetry: slice 4B removed the double-truth (the productive
 * `GoNoGoTelemetrySummaryV1` persistence) and moved its write helper to test-only. This source-scan fails the
 * build the moment any E8 production source re-introduces a telemetry WRITE (`saveGoNoGoSummary`), references the
 * telemetry summary TYPE, or takes an `ExerciseTelemetryStore` dependency. The sole official cognitive history is
 * `CognitiveSessionRun`. (Desktop test — file IO is fine; not Detekt-scanned.)
 */
class E8ProductiveTelemetryAbsenceTest {

    // Tokens that would signal a telemetry write creeping back into the E8 production surface.
    private val forbidden = listOf(
        "saveGoNoGoSummary",
        "GoNoGoTelemetrySummaryV1",
        "GoNoGoTelemetryStoreExt",
        "ExerciseTelemetryStore",
    )

    @Test
    fun e8_productive_path_never_writes_telemetry() {
        val files = e8ProductionSources()
        assertTrue(files.isNotEmpty(), "expected to locate E8 production sources from ${File(".").absolutePath}")
        val offenders = files.mapNotNull { file ->
            val hit = forbidden.firstOrNull { file.readText().contains(it) }
            if (hit != null) "${file.name} -> $hit" else null
        }
        assertTrue(
            offenders.isEmpty(),
            "E8 productive path must not write telemetry (double-truth removed in 4B); found: $offenders",
        )
    }

    private fun e8ProductionSources(): List<File> {
        val e8Dir = find("src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8")
        val speedRoot =
            find("src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/ui/SpeedRootScreen.kt")
        val fromDir = e8Dir?.walkTopDown()?.filter { it.isFile && it.extension == "kt" }?.toList() ?: emptyList()
        return fromDir + listOfNotNull(speedRoot)
    }

    private fun find(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            File(current, relative).takeIf { it.exists() }?.let { return it }
            File(current, "shared/$relative").takeIf { it.exists() }?.let { return it }
            base = current.parentFile
        }
        return null
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
