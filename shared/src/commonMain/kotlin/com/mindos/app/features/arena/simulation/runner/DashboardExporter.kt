package com.mindos.app.features.arena.simulation.runner

import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SanitizedArenaOutput

/**
 * Phase 07: Exports a [SimulationReport] as a sanitized aggregate dashboard (dashboard_exporter.md).
 *
 * The report already contains only aggregate metrics (no identifiers), and the export is routed
 * through the [DataSanitizer] as a mandatory final boundary — so the result is a [SanitizedArenaOutput]
 * that downstream code can trust contains no UIDs or PII.
 */
class DashboardExporter(
    private val sanitizer: DataSanitizer,
) {

    fun exportDashboard(report: SimulationReport): SanitizedArenaOutput = sanitizer.sanitize(toJson(report))

    private fun toJson(report: SimulationReport): String = buildString {
        append("{")
        append("\"populationSize\":${report.populationSize},")
        append("\"sessionsProcessed\":${report.sessionsProcessed},")
        append("\"updatesApplied\":${report.updatesApplied},")
        append("\"updatesRejected\":${report.updatesRejected},")
        append("\"cleanAppliedRate\":${report.cleanAppliedRate},")
        append("\"abusersDetectedRate\":${report.abusersDetectedRate},")
        append("\"resultDigest\":\"${report.resultDigest}\"")
        append("}")
    }
}
