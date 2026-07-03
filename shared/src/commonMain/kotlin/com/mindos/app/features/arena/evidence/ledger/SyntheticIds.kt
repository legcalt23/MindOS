package com.mindos.app.features.arena.evidence.ledger

import com.mindos.app.features.arena.config.UuidV4
import kotlin.random.Random

/**
 * Phase 06: Synthetic profile identifier. Must be a UUIDv4 — constructing one from a raw, UID-like
 * string (e.g. a Firebase 28-char token) fails fast, so real identifiers cannot enter the ledger.
 */
@JvmInline
value class SyntheticProfileId(val value: String) {
    init {
        require(UuidV4.isValid(value)) { "SyntheticProfileId must be a UUIDv4 synthetic id" }
    }

    companion object {
        fun random(random: Random): SyntheticProfileId = SyntheticProfileId(UuidV4.random(random))
    }
}

/**
 * Phase 06/07: Synthetic identifier for one simulation run. Must be a UUIDv4.
 */
@JvmInline
value class SimulationRunId(val value: String) {
    init {
        require(UuidV4.isValid(value)) { "SimulationRunId must be a UUIDv4 synthetic id" }
    }

    companion object {
        fun random(random: Random): SimulationRunId = SimulationRunId(UuidV4.random(random))
    }
}
