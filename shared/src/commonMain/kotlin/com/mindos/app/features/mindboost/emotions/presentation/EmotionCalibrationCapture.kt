package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.core.calibration.BlockPrediction
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionOutcome
import com.mindos.app.features.mindboost.core.calibration.CalibrationJudgment
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.emotions.domain.calibration.EmotionCalibrationHost
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ola 1 · N1 — the transversal confidence layer's own state, kept out of the session holder.
 *
 * It lives in its own class because it is genuinely a separate concern: the host exercise decides what to show and
 * how to score it, while this only ever asks two questions and writes down the answers. It owns no session state,
 * reads none, and cannot influence the block — the strongest available guarantee that adding calibration did not
 * quietly change what the exercises measure.
 *
 * [show] hands a prompt state back to the holder rather than the holder polling for one, so the layer never needs
 * a reference to the holder and cannot touch anything else on it.
 */
internal class EmotionCalibrationCapture(private val show: (EmotionSessionUiState) -> Unit) {

    private var pendingConfidence: CompletableDeferred<ConfidenceLevel>? = null
    private var pendingForecast: CompletableDeferred<Int>? = null
    private val judgments = mutableListOf<CalibrationJudgment>()
    private val forecasts = mutableListOf<BlockPredictionOutcome>()

    val sessionJudgments: List<CalibrationJudgment> get() = judgments.toList()
    val sessionForecasts: List<BlockPredictionOutcome> get() = forecasts.toList()

    fun submitConfidence(level: ConfidenceLevel) {
        pendingConfidence?.takeIf { !it.isCompleted }?.complete(level)
    }

    fun submitForecast(predictedCorrect: Int) {
        pendingForecast?.takeIf { !it.isCompleted }?.complete(predictedCorrect)
    }

    /** Releases anything waiting so an exit or a safety stop is never held up by an unanswered prompt. */
    fun cancelPending() {
        pendingConfidence?.cancel()
        pendingForecast?.cancel()
    }

    fun clearPending() {
        pendingConfidence = null
        pendingForecast = null
    }

    /**
     * Offers the confidence strip for [CONFIDENCE_WINDOW_MS] and records the judgment if one arrives.
     *
     * Everything about this is deliberately non-committal. The host paradigm decides whether the strip is offered
     * at all ([EmotionCalibrationHost]); an unanswered strip records nothing rather than a low-confidence guess;
     * and the window is short enough that a user who ignores it loses well under a second. The one thing it must
     * never do is show the user whether they were right — the correctness bit computed here stays inside the
     * calibration record, because a strip that leaked feedback would turn the host task into a guided one and
     * would make the block forecast assemblable from crumbs.
     */
    suspend fun capture(
        payload: EmotionStimulusPayload,
        responseKey: EmotionResponseKey,
        category: EmotionCategory?,
    ) {
        val chance = EmotionCalibrationHost.chanceLevelOf(payload)
        val correct = EmotionCalibrationHost.wasCorrect(payload, responseKey, category)
        if (chance != null && correct != null && EmotionCalibrationHost.acceptsJudgment(payload, responseKey)) {
            show(EmotionSessionUiState.ConfidencePrompt(CONFIDENCE_WINDOW_MS))
            pendingConfidence = CompletableDeferred()
            val level = withTimeoutOrNull(CONFIDENCE_WINDOW_MS) { pendingConfidence?.await() }
            pendingConfidence = null
            if (level != null) judgments += CalibrationJudgment(level, wasCorrect = correct, chanceLevel = chance)
        }
    }

    /**
     * Asks for the block forecast before the block runs, and returns it so the block can be scored against it.
     * Null when the host cannot carry calibration or the user let the prompt lapse: a forecast the user did not
     * make is not a forecast of zero.
     */
    suspend fun askForecast(serves: Boolean, blockIndex: Int, blockSize: Int): BlockPrediction? {
        var prediction: BlockPrediction? = null
        if (serves) {
            show(EmotionSessionUiState.BlockForecastPrompt(blockSize = blockSize, blockIndex = blockIndex))
            pendingForecast = CompletableDeferred()
            val predicted = withTimeoutOrNull(FORECAST_WINDOW_MS) { pendingForecast?.await() }
            pendingForecast = null
            prediction = predicted?.takeIf { it in 0..blockSize }?.let { BlockPrediction(it, blockSize) }
        }
        return prediction
    }

    /** Pairs a forecast with what the block actually delivered. A block with no forecast records nothing. */
    fun score(forecast: BlockPrediction?, correctCount: Int) {
        if (forecast != null) {
            forecasts += BlockPredictionOutcome(forecast, correctCount.coerceIn(0, forecast.blockSize))
        }
    }

    companion object {
        /** Long enough to press one of three buttons, short enough that ignoring it costs almost nothing. */
        const val CONFIDENCE_WINDOW_MS = 600L

        /** A forecast is a considered judgment, not a speeded one — this window is generous on purpose. */
        const val FORECAST_WINDOW_MS = 20_000L
    }
}
