package com.mindos.app.features.mindboost.language.presentation.state

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState

/**
 * Single source of claim-safe, state-derived UI copy (v6 §11.3/§11.5/§11.6). Centralized so the
 * Genome, Cockpit and Coach Card all describe an evidence state the same honest way.
 */
object EvidenceCopy {

    /** What the user needs to do next to climb the evidence ladder. */
    fun nextEvidenceNeeded(state: EvidenceState): String = when (state) {
        EvidenceState.E0_CALIBRATING -> "Completar la calibración inicial."
        EvidenceState.E1_TRAINED_IMPROVEMENT -> "Demostrarlo en material nuevo similar."
        EvidenceState.E2_NEAR_TRANSFER -> "Aplicarlo en una tarea funcional."
        EvidenceState.E3_FUNCTIONAL_TRANSFER -> "Mantenerlo tras una pausa (retest 7/30 días)."
        EvidenceState.E4_MAINTAINED -> "Generalizar a más dominios y formatos."
        EvidenceState.E5_ROBUST_GENERALIZATION -> "Mantenerlo con repasos espaciados."
    }

    /** The current honest limitation of what has been shown. */
    fun limitation(state: EvidenceState): String = when (state) {
        EvidenceState.E0_CALIBRATING -> "Aún calibrando; trata cualquier dato como provisional."
        EvidenceState.E1_TRAINED_IMPROVEMENT -> "Aún no demostrado en material nuevo ni en tarea aplicada."
        EvidenceState.E2_NEAR_TRANSFER -> "Aún no demostrado en una tarea funcional ni a 30 días."
        EvidenceState.E3_FUNCTIONAL_TRANSFER -> "Aún no validado tras una pausa."
        EvidenceState.E4_MAINTAINED -> "Validado a corto plazo; falta evidencia multidominio."
        EvidenceState.E5_ROBUST_GENERALIZATION -> "Evidencia fuerte; conviene revisar periódicamente."
    }

    /** A one-line evidence summary for a coach card; flags when transfer is not yet shown. */
    fun evidenceLine(state: EvidenceState): String {
        val suffix = if (state.isAtLeast(EvidenceState.E2_NEAR_TRANSFER)) "." else "; aún no hay transferencia."
        return "Evidencia: ${state.stateLabel}$suffix"
    }
}
