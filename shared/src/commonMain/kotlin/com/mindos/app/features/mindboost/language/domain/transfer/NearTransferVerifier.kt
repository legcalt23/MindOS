package com.mindos.app.features.mindboost.language.domain.transfer

import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.TraitCode

/**
 * Near-transfer verification (v6 §5.2 E2, §10.2 T2). Decides whether a trait shows transfer to
 * *unseen material of the same construct* — the bridge between "improves on practiced items" (E1)
 * and any higher claim. Pure and offline.
 */
fun interface NearTransferVerifier {
    fun verify(trait: TraitCode, trials: List<TransferTrial>, config: NearTransferConfig): TransferVerdict
}

/** Thresholds for declaring near transfer. Tunable; defaults are conservative starting points. */
data class NearTransferConfig(
    val minTrials: Int = DEFAULT_MIN_TRIALS,
    val minAccuracy: Double = DEFAULT_MIN_ACCURACY,
) {
    companion object {
        const val DEFAULT_MIN_TRIALS = 3
        const val DEFAULT_MIN_ACCURACY = 0.7
    }
}

/**
 * Default [NearTransferVerifier]. Only trials that are (a) at least near-transfer level
 * ([TransferLevel.T2_NEAR]) and (b) allowed a strong update (not confounded) count as evidence.
 * Transfer is demonstrated when enough such trials clear the accuracy bar.
 */
class DefaultNearTransferVerifier : NearTransferVerifier {

    override fun verify(
        trait: TraitCode,
        trials: List<TransferTrial>,
        config: NearTransferConfig,
    ): TransferVerdict {
        val eligible = trials.filter {
            it.level.isAtLeast(TransferLevel.T2_NEAR) && it.isStrongUpdateAllowed
        }
        val reasons = mutableListOf<String>()
        val discarded = trials.size - eligible.size
        if (discarded > 0) {
            reasons += "Se descartaron $discarded intentos no válidos " +
                "(repetidos, confundidos o en material entrenado)."
        }
        if (eligible.isEmpty()) {
            return TransferVerdict(
                trait = trait,
                demonstrated = false,
                level = TransferLevel.T1_SURFACE_VARIATION,
                accuracy = null,
                trialsConsidered = 0,
                reasons = reasons + "Sin material nuevo válido.",
            )
        }
        val accuracy = eligible.sumOf { it.correctness.value } / eligible.size
        val level = eligible.maxOf { it.level }
        val enough = eligible.size >= config.minTrials
        val accurate = accuracy >= config.minAccuracy
        if (!enough) reasons += "Faltan intentos: ${eligible.size}/${config.minTrials}."
        if (!accurate) reasons += "Precisión insuficiente en material nuevo."
        val demonstrated = enough && accurate
        if (demonstrated) reasons += "Transferencia demostrada en material nuevo del mismo constructo."
        return TransferVerdict(
            trait = trait,
            demonstrated = demonstrated,
            level = level,
            accuracy = Proportion.clamped(accuracy),
            trialsConsidered = eligible.size,
            reasons = reasons,
        )
    }
}
