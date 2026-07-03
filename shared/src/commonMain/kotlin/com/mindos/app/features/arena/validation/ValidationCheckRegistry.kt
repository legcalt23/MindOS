@file:Suppress("MaxLineLength") // Dense one-row-per-check documentation table; wrapping each row hurts readability.

package com.mindos.app.features.arena.validation

/** Phase 12: An integration-validation check. `blockingBeforeLiveShadow` checks gate go-live (NOT done here). */
data class ValidationCheck(
    val id: String,
    val description: String,
    val blockingBeforeLiveShadow: Boolean = false,
)

/**
 * Phase 12: The 25 integration-validation checks (VAL_01..VAL_25). These are OFFLINE verification
 * checks over the built code/schemas — they assert the offline system's invariants and completeness,
 * including that NOTHING is live (VAL_07 flag disabled, VAL_25 no motor live). Implementing/running
 * these checks does not perform the Codex sign-off or any go-live; those remain forbidden human gates.
 */
object ValidationCheckRegistry {

    val CHECKS: List<ValidationCheck> = listOf(
        ValidationCheck("VAL_01", "Forbidden terminology scan passes"),
        ValidationCheck("VAL_02", "No readiness / self-report / fatigue inputs in rating math"),
        ValidationCheck("VAL_03", "Premium subscription cannot affect rating (zero premium inputs)"),
        ValidationCheck("VAL_04", "Premium-vs-free equivalence (identical inputs -> identical outcome)"),
        ValidationCheck("VAL_05", "Mode ratings are isolated per mode"),
        ValidationCheck("VAL_06", "Session environment snapshot is captured"),
        ValidationCheck("VAL_07", "Mental Speed ranked feature flag is disabled"),
        ValidationCheck("VAL_08", "Evidence ledgers complete for the 3 MVP motors"),
        ValidationCheck("VAL_09", "Evidence ledger sign-offs validated"),
        ValidationCheck("VAL_10", "Task delta and match delta are separated (blending)"),
        ValidationCheck("VAL_11", "Smurf suspicion keeps sigma high (not collapsed)"),
        ValidationCheck("VAL_12", "Session-validity user copy is safe (no leakage)"),
        ValidationCheck("VAL_13", "Review copy reveals no anti-cheat internals"),
        ValidationCheck("VAL_14", "Post-review workflow (resolve + appeal) functions"),
        ValidationCheck("VAL_15", "Integrity gate decision is produced"),
        ValidationCheck("VAL_16", "Comparability decision is produced"),
        ValidationCheck("VAL_17", "Measurement confidence decision is produced"),
        ValidationCheck("VAL_18", "Session validity decision gates the rating update"),
        ValidationCheck("VAL_19", "Session results use synthetic references only"),
        ValidationCheck("VAL_20", "Simulation harness is built and runs"),
        ValidationCheck("VAL_21", "Harness processes 100k sessions deterministically"),
        ValidationCheck("VAL_22", "Acceptance metrics pass their thresholds"),
        ValidationCheck("VAL_23", "Red-team matrix complete (27 threats)"),
        ValidationCheck("VAL_24", "Trials anti-coaching controls function"),
        ValidationCheck("VAL_25", "No Phase 1 motor is live / Shadow-ranked-eligible before sign-off", blockingBeforeLiveShadow = true),
    )
}
