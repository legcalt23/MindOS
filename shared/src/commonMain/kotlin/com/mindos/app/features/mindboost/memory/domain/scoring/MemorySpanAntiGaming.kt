package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.antigaming.AntiGamingFramework
import com.mindos.app.features.mindboost.core.antigaming.GamingVerdict
import com.mindos.app.features.mindboost.core.antigaming.PositionBiasDetector
import com.mindos.app.features.mindboost.core.antigaming.TrialTelemetry

/**
 * P1 EX1 (V1_01 §5) · Memoria anti-gaming for the complex-span PROCESSING task. Reuses the INFRA-03
 * framework: the processing judgements (the yes/no answers, as 0/1) are run through the response-entropy
 * detector so a `ButtonSpammer` ("always yes") is caught. Because this detector now EXISTS and is tested
 * against synthetic adversaries, EX1's `DetectorCoverage` may legitimately be `ADEQUATE` (it is absent
 * otherwise, capping the span at `CALIBRATING`). The processing-validity floor (`accuracy ≥ 0.85`) is the
 * scorer's separate `INVALIDATED` gate; together they catch both "answer randomly" and "answer all-same".
 */
object MemorySpanAntiGaming {
    /** Clean iff the processing responses are varied (not button-spammed). */
    fun evaluate(processingResponses: List<Int>): GamingVerdict {
        val telemetry = TrialTelemetry(responseKeys = processingResponses)
        return AntiGamingFramework.aggregate(listOf(PositionBiasDetector.evaluate(telemetry)))
    }
}
