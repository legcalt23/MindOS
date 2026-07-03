package com.mindos.app.features.mindboost.creativity.config

/**
 * Lifecycle states of a hard evidence gate, mirroring `14_evidence_manifest.md`.
 *
 * Only [PASSED] permits the gated capability to activate, and only with real executed
 * evidence attached. The local MVP keeps every gate in [DESIGNED] / pending.
 */
enum class CreativityGateState {
    DESIGNED,
    IMPLEMENTED,
    TESTED,
    PASSED,
    FAILED,
    BLOCKED,
    WAIVED_BY_HUMAN,
}

/**
 * A single hard evidence gate.
 *
 * Core release rule (`12_final_signoff.md`): a written gate is not a passed gate. A gate
 * may only be [CreativityGateState.PASSED] when [evidenceRef] points at executed, validated
 * evidence — the [init] block makes self-certification impossible by construction.
 */
data class CreativityEvidenceGate(
    val id: String,
    val title: String,
    val state: CreativityGateState,
    /** Human-readable description of what this gate blocks until PASSED. */
    val blocks: String,
    /** Reference (log/hash) to executed evidence. Required to reach PASSED. */
    val evidenceRef: String? = null,
) {
    init {
        require(state != CreativityGateState.PASSED || !evidenceRef.isNullOrBlank()) {
            "Gate $id cannot be PASSED without an executed-evidence reference."
        }
    }

    val isPassed: Boolean get() = state == CreativityGateState.PASSED
}

/**
 * Registry of the hard gates from `14_evidence_manifest.md`, all `DESIGNED` / pending.
 *
 * [G10] (Pre-Gate AI Inactivity) gates the Local MVP Offline Release and stays pending
 * until the verification harness is executed. None of these may be flipped to PASSED
 * here — that requires real evidence outside this source.
 */
object CreativityEvidenceGates {

    val all: List<CreativityEvidenceGate> = listOf(
        gate("G01", "Adversarial Metric Suite", "AI Layers Activation & Competitive Live Scoring"),
        gate("G02", "Codex Security Audit (Timer/Payload)", "Offline MVP Deployment and Sync Release"),
        gate("G03", "Frontier LLM Cost Simulation", "AI Cascade Production Release"),
        gate("G04", "Metric Integrity Layer Attacks", "Competitive Rating Production Release"),
        gate("G05", "Privacy Gateway DLP Canaries", "Client Data Transmission to LLM Backend"),
        gate("G06", "Privacy Gateway Verification", "Client Data Transmission to LLM Backend"),
        gate("G07", "Coaching Safety Assessment", "AI Coach / Feedback Layer Activation"),
        gate("G08", "Telemetry Friction Budget", "Full Beta Release and Scaling"),
        gate("G09", "Hard Execution Gates Verification", "Production Deployment of any AI features"),
        gate("G10", "Pre-Gate AI Inactivity", "Local MVP Offline Release"),
    )

    /** AI runtime stays locked unless every hard gate is PASSED with evidence. */
    val aiRuntimeUnlocked: Boolean get() = all.isNotEmpty() && all.all { it.isPassed }

    fun byId(id: String): CreativityEvidenceGate? = all.firstOrNull { it.id == id }

    private fun gate(id: String, title: String, blocks: String) =
        CreativityEvidenceGate(id = id, title = title, state = CreativityGateState.DESIGNED, blocks = blocks)
}
