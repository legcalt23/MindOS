package com.mindos.app.features.mindboost.core.scoring

/**
 * INFRA-01 · The render NEVER receives a raw Double: a verdict is translated to an honest visual state.
 * Non-VALID states map to "—"/"calibrating"/"insufficient"/"not counted", NEVER to a low number
 * (invariant `null != 0`).
 */
sealed interface ScoreUiState {
    data class Confident(val value: Double, val band: ClosedRange<Double>, val label: String) : ScoreUiState
    data class Preliminary(val value: Double, val band: ClosedRange<Double>) : ScoreUiState
    data object Calibrating : ScoreUiState
    data object InsufficientData : ScoreUiState
    data object NotUpdatedQuality : ScoreUiState
    data object Invalidated : ScoreUiState
}

/** Honest visual state for this verdict. Exhaustive over [ScoreState]; never emits a number unless VALID. */
fun ScoreResult.toUiState(): ScoreUiState = when (state) {
    ScoreState.VALID -> validUiState()
    ScoreState.CALIBRATING, ScoreState.ABANDONED_SAFE, ScoreState.SUSPECTED_GAMING -> ScoreUiState.Calibrating
    ScoreState.INSUFFICIENT_N -> ScoreUiState.InsufficientData
    ScoreState.DEVICE_UNRELIABLE, ScoreState.BANK_CONTAMINATED -> ScoreUiState.NotUpdatedQuality
    ScoreState.INVALIDATED -> ScoreUiState.Invalidated
}

private fun ScoreResult.validUiState(): ScoreUiState {
    val signal = primarySignal
    val value = signal?.value
    return if (value == null) {
        ScoreUiState.Calibrating // defensive: VALID always carries a signal, but never emit a number we lack.
    } else {
        val band = (signal.lowerBound ?: value)..(signal.upperBound ?: value)
        if (preliminary) ScoreUiState.Preliminary(value, band) else ScoreUiState.Confident(value, band, claimLevel.name)
    }
}
