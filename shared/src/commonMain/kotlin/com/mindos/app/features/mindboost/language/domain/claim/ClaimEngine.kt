package com.mindos.app.features.mindboost.language.domain.claim

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlinx.serialization.Serializable

/**
 * Claim governance (v6 §5.7 ClaimEngine, §11.6 forbidden copy). This is the gate that
 * blocks any user-facing or marketing copy the evidence does not support. It is the
 * difference between an honest trainer and a motivational toy.
 *
 * Each [ClaimType] declares the *minimum* evidence state that permits it. The engine never
 * upgrades a claim above the evidence, always offers the strongest **safe alternative** the
 * evidence does support, and always surfaces uncertainty while calibrating.
 */
fun interface ClaimEngine {
    fun evaluate(request: ClaimRequest): ClaimDecision
}

/**
 * The tiered claims the product may make. There is deliberately **no** "increases your
 * intelligence" / "real comprehension improved" tier: such claims are unrepresentable, so
 * they can never be requested nor allowed (§11.6).
 */
@Serializable
enum class ClaimType(val minimumState: EvidenceState, val safeCopy: String) {
    TRAINED_IMPROVEMENT(EvidenceState.E1_TRAINED_IMPROVEMENT, "Mejoras en este tipo de ejercicio."),
    NEAR_TRANSFER(EvidenceState.E2_NEAR_TRANSFER, "Tu rendimiento mejora en material nuevo similar."),
    FUNCTIONAL_TRANSFER(EvidenceState.E3_FUNCTIONAL_TRANSFER, "Has demostrado esta habilidad en una tarea aplicada."),
    MAINTAINED(EvidenceState.E4_MAINTAINED, "La mejora se mantiene tras una pausa."),
    ROBUST_GENERALIZATION(EvidenceState.E5_ROBUST_GENERALIZATION, "Hay evidencia fuerte de transferencia amplia."),
    ;

    companion object {
        /** The strongest claim permitted by [state], or `null` if still calibrating (E0). */
        fun strongestFor(state: EvidenceState): ClaimType? =
            entries.filter { state.isAtLeast(it.minimumState) }.maxByOrNull { it.minimumState.rank }
    }
}

/**
 * A request to show a claim for a given [trait] at the user's current [evidenceState].
 * [evidenceState] is the *measured* state for that trait — supplied by the profile, never
 * assumed by the caller.
 */
@Serializable
data class ClaimRequest(
    val requestedClaim: ClaimType,
    val evidenceState: EvidenceState,
    val trait: TraitCode? = null,
)

/** Why a claim was blocked (v6 §5.7 blockedReasons). */
@Serializable
enum class ClaimBlockReason {
    STILL_CALIBRATING,
    INSUFFICIENT_EVIDENCE,
    NO_FUNCTIONAL_TRANSFER,
    NO_DELAYED_RETEST,
}

/** Outcome of a claim evaluation. Sealed so callers branch exhaustively. */
@Serializable
sealed interface ClaimDecision {
    val evidenceState: EvidenceState

    /** The claim is permitted; show [safeCopy] together with any [caveats]. */
    @Serializable
    data class Allowed(
        val claim: ClaimType,
        val safeCopy: String,
        val caveats: List<String>,
        override val evidenceState: EvidenceState,
    ) : ClaimDecision

    /** The claim is blocked; show [safeAlternativeCopy] instead (the strongest honest copy). */
    @Serializable
    data class Blocked(
        val requested: ClaimType,
        val reason: ClaimBlockReason,
        val safeAlternativeCopy: String,
        override val evidenceState: EvidenceState,
    ) : ClaimDecision
}
