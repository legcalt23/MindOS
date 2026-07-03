package com.mindos.app.features.mindboost.creativity.config

/**
 * Single source of truth for the Creativity practice MVP runtime posture.
 *
 * The Creativity / Intelligence module ships as a LOCAL, OFFLINE practice tool only.
 * Every AI / network / remote-telemetry capability is BLOCKED by design and stays
 * blocked until the hard evidence gates in `14_evidence_manifest.md` are PASSED with
 * real executed evidence (see [CreativityEvidenceGate] / [CreativityEvidenceGates]).
 *
 * This is creativity practice with local self-assessment — not a diagnosis, not a
 * graded ability test, not a clinical tool. No AI-derived field is ever computed in this MVP.
 *
 * Authoritative scope: `.antigravity-only/plans/current-plan-review-evidence-based_20260621_050307`
 * (`10_claude_handoff.md`, `12_final_signoff.md`, `15_package_integrity.md`).
 */
object CreativityRuntimePolicy {

    /** AI scoring / coaching / evaluator-cascade runtime. Hard-blocked in the local MVP. */
    const val AI_RUNTIME_ENABLED: Boolean = false

    /** Any cloud sync / backend transmission of practice data. Hard-blocked. */
    const val NETWORK_SYNC_ENABLED: Boolean = false

    /** Remote analytics / telemetry SDK emission. Hard-blocked. */
    const val REMOTE_TELEMETRY_ENABLED: Boolean = false

    /**
     * Whether the hard evidence gates (G01..G10) have been PASSED with executed evidence.
     * Stays `false`: a written gate is not a passed gate (`12_final_signoff.md`).
     */
    const val EVIDENCE_GATES_PASSED: Boolean = false

    /** True only when the module is a pure local/offline practice surface. */
    val isLocalOnlyMvp: Boolean
        get() = !AI_RUNTIME_ENABLED && !NETWORK_SYNC_ENABLED && !REMOTE_TELEMETRY_ENABLED

    /** No AI-derived feature may activate while the evidence gates are pending. */
    val isAiSurfaceUnlocked: Boolean
        get() = AI_RUNTIME_ENABLED && EVIDENCE_GATES_PASSED

    /**
     * Defense-in-depth guard. Production wiring must call this before mounting the
     * feature so an accidental flag flip fails closed instead of silently shipping an
     * unvalidated AI/network path.
     */
    fun requireLocalOnly() {
        check(isLocalOnlyMvp) { LOCAL_ONLY_VIOLATION }
        check(!isAiSurfaceUnlocked) { AI_UNLOCK_VIOLATION }
    }

    private const val LOCAL_ONLY_VIOLATION =
        "Creativity MVP must stay local/offline: AI, network and telemetry are blocked."
    private const val AI_UNLOCK_VIOLATION =
        "Creativity AI surface cannot unlock while evidence gates are pending."
}
