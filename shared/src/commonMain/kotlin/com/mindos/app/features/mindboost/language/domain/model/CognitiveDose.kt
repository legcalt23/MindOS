// The LOW default's axis values ARE the data of this config object (not incidental
// constants), so MagicNumber is suppressed for the file — same convention as the other
// MindBoost config/token objects.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Cognitive dose of a task/session (v6 §4.3). The module dosifies *load*, not just
 * difficulty: a session is described by ten normalized axes (0.0 easiest/none ..
 * 1.0 hardest/max). The CognitiveDoseEngine (later slice) moves these; generators decode
 * them into concrete item parameters.
 *
 * Every axis is range-checked on construction so downstream math can assume validity.
 */
@Serializable
data class CognitiveDose(
    val semanticComplexity: Double,
    val syntacticComplexity: Double,
    val workingMemoryLoad: Double,
    val novelty: Double,
    val timePressure: Double,
    val productionDemand: Double,
    val ambiguity: Double,
    val transferDistance: Double,
    val emotionalRisk: Double,
    val fatigueCost: Double,
) {
    init {
        axes().forEach { (name, v) ->
            require(v in 0.0..1.0) { "Dose axis '$name' must be in 0.0..1.0 (was $v)." }
        }
    }

    /** Named axes, for validation and explainable dose prescriptions. */
    fun axes(): List<Pair<String, Double>> = listOf(
        "semanticComplexity" to semanticComplexity,
        "syntacticComplexity" to syntacticComplexity,
        "workingMemoryLoad" to workingMemoryLoad,
        "novelty" to novelty,
        "timePressure" to timePressure,
        "productionDemand" to productionDemand,
        "ambiguity" to ambiguity,
        "transferDistance" to transferDistance,
        "emotionalRisk" to emotionalRisk,
        "fatigueCost" to fatigueCost,
    )

    companion object {
        /** A neutral low-load dose, used as a safe default (e.g. recovery sessions). */
        val LOW = CognitiveDose(
            semanticComplexity = 0.2,
            syntacticComplexity = 0.2,
            workingMemoryLoad = 0.2,
            novelty = 0.2,
            timePressure = 0.0,
            productionDemand = 0.2,
            ambiguity = 0.2,
            transferDistance = 0.1,
            emotionalRisk = 0.0,
            fatigueCost = 0.1,
        )
    }
}
