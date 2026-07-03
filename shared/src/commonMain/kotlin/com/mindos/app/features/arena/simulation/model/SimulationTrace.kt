package com.mindos.app.features.arena.simulation.model

import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Simulation Trace
 * Captures an entire path of a simulated event for debugging and validation.
 */
@Serializable
data class SimulationTrace(
    val traceId: String,
    val syntheticSessionId: String,
    val seedUsed: Long,
    val executionTimestampMs: Long,
    val eventType: String,
    val payloadJsonSnapshot: String
)
