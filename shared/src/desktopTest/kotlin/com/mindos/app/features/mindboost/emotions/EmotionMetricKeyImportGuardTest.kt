package com.mindos.app.features.mindboost.emotions

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Boundary guard: the emotions module must OWN its `EmotionMetricKey` and never import focus's closed
 * `MetricKey` enum (which has no emotion keys). Scans emotions main sources for the offending import.
 */
class EmotionMetricKeyImportGuardTest {

    @Test
    fun emotions_does_not_import_focus_metric_key() {
        val main = EmotionSourceLocator.mainDir()
        val offenders = EmotionSourceLocator.kotlinFiles(main).filter { file ->
            EmotionSourceLocator.importLines(file).any { it.contains("focus.domain.metrics.MetricKey") }
        }
        assertTrue(
            offenders.isEmpty(),
            "Emotions must own EmotionMetricKey; focus MetricKey imported in: ${offenders.map { it.name }}",
        )
    }
}
