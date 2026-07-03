package com.mindos.app.features.arena.metrics

import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SanitizedArenaOutput

/**
 * Phase 09: Renders the 24 acceptance metrics as a sanitized dashboard payload. The metrics carry only
 * metric ids, numeric values, and statuses (no identifiers), and the payload is routed through the
 * [DataSanitizer] as the mandatory final boundary.
 */
class MetricsDashboardExporter(
    private val sanitizer: DataSanitizer,
) {

    fun export(metrics: List<AcceptanceMetric>): SanitizedArenaOutput = sanitizer.sanitize(toJson(metrics))

    private fun toJson(metrics: List<AcceptanceMetric>): String =
        metrics.joinToString(prefix = "{\"metrics\":[", postfix = "]}", separator = ",") { metric ->
            "{\"id\":\"${metric.id.name}\",\"value\":${metric.value ?: "null"},\"status\":\"${metric.status.name}\"}"
        }
}
