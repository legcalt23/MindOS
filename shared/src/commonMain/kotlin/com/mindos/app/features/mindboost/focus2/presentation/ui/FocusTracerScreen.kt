@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus2.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindos.app.features.mindboost.focus2.domain.generator.PlannedTrial
import com.mindos.app.features.mindboost.focus2.domain.runtime.InputModality
import com.mindos.app.features.mindboost.focus2.domain.runtime.TrialPhase
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.presentation.a11y.SequentialFocusCursor
import com.mindos.app.features.mindboost.focus2.presentation.a11y.platformMinimumTarget

/**
 * Focus V5 · S3 · tarea (6) — the tracer screen, accessibility-first.
 *
 * Three rules shape everything below, and each of them rules something out:
 *
 *  - **Every interactive target is at least [platformMinimumTarget]**, which is 48dp on Android and 44pt on
 *    iOS — the vendors' own numbers, never a single "universal" value (P1-16).
 *  - **No decorative animation.** Movement on a timed screen is not neutral: it competes for the very
 *    resource the exercise is asking the participant to spend, so any motion here would be a confound rather
 *    than a flourish.
 *  - **Two input modalities, both complete.** The sequential one is not a reduced fallback: it presents the
 *    same trials, in the same order, at the same pace. It is recorded as a DISTINCT modality because evidence
 *    gathered by moving a focus ring is not comparable with evidence gathered by pointing — a difference of
 *    measurement, not of worth.
 *
 * Feedback during a measured block stays neutral (C0): the screen shows where you are, never how you are
 * doing. Per-trial correctness would change the task into one that includes reacting to your own score.
 */
data class FocusTracerUiState(
    val trial: PlannedTrial?,
    val phase: TrialPhase,
    val modality: InputModality,
    val trialNumber: Int,
    val totalTrials: Int,
    val cursor: SequentialFocusCursor<Response>? = null,
    val paused: Boolean = false,
)

/** What the screen can ask the holder to do. Grouped so the composable takes one parameter, not seven. */
data class FocusTracerActions(
    val onRespond: (Response) -> Unit = {},
    val onAdvanceCursor: () -> Unit = {},
    val onActivateCursor: () -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
)

@Composable
fun FocusTracerScreen(
    state: FocusTracerUiState,
    actions: FocusTracerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ensayo ${state.trialNumber} de ${state.totalTrials}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics {
                contentDescription = "Ensayo ${state.trialNumber} de ${state.totalTrials}"
            },
        )
        Spacer(Modifier.height(32.dp))

        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            val trial = state.trial
            if (trial != null && state.phase == TrialPhase.AWAITING_RESPONSE) {
                StimulusRow(trial)
            } else {
                Text("·", style = MaterialTheme.typography.displayMedium)
            }
        }

        Spacer(Modifier.height(32.dp))

        when (state.modality) {
            InputModality.SWITCH_SEQUENTIAL -> SequentialResponses(state, actions)
            else -> TouchResponses(actions)
        }

        Spacer(Modifier.height(24.dp))
        TargetSizedButton(
            label = if (state.paused) "Continuar" else "Pausa",
            description = if (state.paused) "Continuar la sesion" else "Pausar la sesion",
            onClick = if (state.paused) actions.onResume else actions.onPause,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The stimulus: a centre element flanked on both sides.
 *
 * The whole row carries ONE description rather than one per arrow. A screen reader announcing eleven separate
 * arrows would turn a visual-selection task into a memory task, which is a different exercise — and the
 * accessible version has to remain the same exercise, or its results mean something else.
 */
@Composable
private fun StimulusRow(trial: PlannedTrial) {
    val centre = if (trial.centrePointsLeft) "◀" else "▶"
    val flanker = if (trial.congruent) centre else if (trial.centrePointsLeft) "▶" else "◀"
    val side = if (trial.congruent) "iguales" else "distintos"
    val perSide = trial.flankerCount / 2

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            val direction = if (trial.centrePointsLeft) "izquierda" else "derecha"
            contentDescription = "El elemento central apunta a la $direction. " +
                "Los elementos que lo rodean son $side."
        },
    ) {
        repeat(perSide) { Text(flanker, style = MaterialTheme.typography.displaySmall) }
        Text(centre, style = MaterialTheme.typography.displayLarge)
        repeat(trial.flankerCount - perSide) { Text(flanker, style = MaterialTheme.typography.displaySmall) }
    }
}

/** Direct touch: two large targets, one per side. */
@Composable
private fun TouchResponses(actions: FocusTracerActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TargetSizedButton(
            label = "Izquierda",
            description = "Responder izquierda",
            onClick = { actions.onRespond(Response.LEFT) },
            modifier = Modifier.weight(1f),
        )
        TargetSizedButton(
            label = "Derecha",
            description = "Responder derecha",
            onClick = { actions.onRespond(Response.RIGHT) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Sequential input for one or two switches: one control moves the focus, one activates it.
 *
 * With a single switch the focus wraps, so both options stay reachable without a "back" control. The focused
 * option is marked visually AND named in the activate button's description, because a person using switches
 * may not be looking at the highlight.
 */
@Composable
private fun SequentialResponses(state: FocusTracerUiState, actions: FocusTracerActions) {
    val cursor = state.cursor ?: return
    val focusedLabel = labelOf(cursor.focused)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            cursor.options.forEachIndexed { index, option ->
                val isFocused = index == cursor.focusedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = platformMinimumTarget)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isFocused) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .semantics {
                            contentDescription =
                                labelOf(option) + if (isFocused) ", opcion enfocada" else ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(labelOf(option), textAlign = TextAlign.Center)
                }
            }
        }
        TargetSizedButton(
            label = "Siguiente opcion",
            description = "Mover el foco a la siguiente opcion",
            onClick = actions.onAdvanceCursor,
            modifier = Modifier.fillMaxWidth(),
        )
        TargetSizedButton(
            label = "Seleccionar",
            description = "Seleccionar $focusedLabel",
            onClick = actions.onActivateCursor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Every button in this screen goes through here, so no target can be built below the platform minimum. */
@Composable
private fun TargetSizedButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .defaultMinSize(minWidth = platformMinimumTarget, minHeight = platformMinimumTarget)
            .semantics { contentDescription = description },
    ) {
        Text(label)
    }
}

private fun labelOf(response: Response): String = when (response) {
    Response.LEFT -> "Izquierda"
    Response.RIGHT -> "Derecha"
    Response.TAP -> "Marcar"
    Response.NONE -> "Sin respuesta"
}
