@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.FocusSessionStateHolder
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusPhase
import com.mindos.app.features.mindboost.focus.presentation.state.FocusRunningModel
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAnchorField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAbortButton
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusChoiceField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusDualTaskField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusRuleSwitchField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusSearchField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusStimulusField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusStreamField
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusTrackingField

/**
 * Live E1 (Vigía) session surface. During the run the whole field is tappable;
 * a calm HUD shows progress and live counters without distracting from the task.
 */
@Composable
fun FocusSessionScreen(
    holder: FocusSessionStateHolder,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (val state = holder.uiState) {
            is FocusSessionUiState.Intro -> IntroContent(
                state = state,
                onStart = { holder.start(state.template) },
                onBack = onExit,
            )

            is FocusSessionUiState.Running -> RunningContent(
                model = state.model,
                onResponse = { holder.onResponse(it) },
                // Completion-aware abandonment is wired from the root (awaits ABANDONED before navigating).
                onAbort = onAbort,
            )

            is FocusSessionUiState.Finished -> FocusSummaryScreen(
                report = state.report,
                template = state.template,
                onDone = onExit,
                onRepeat = onRestart,
            )

            is FocusSessionUiState.Aborted -> AbortedContent(reason = state.reason, onBack = onExit)
        }
    }
}

@Composable
private fun IntroContent(
    state: FocusSessionUiState.Intro,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.7f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.exerciseName,
            color = AppColors.ProfileTextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = state.exerciseDescription,
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.82f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        InstructionCard(state.instructions)
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan)))
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Estoy listo · ${state.template.totalMinutes} min",
                color = AppColors.OnBright,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Salir",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun InstructionCard(instructions: List<String>) {
    if (instructions.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        instructions.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(AppColors.FocusSignal))
                Spacer(Modifier.width(10.dp))
                Text(line, color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RunningContent(
    model: FocusRunningModel,
    onResponse: (ResponseKey) -> Unit,
    onAbort: () -> Unit,
) {
    // Dim AND freeze the ambient during the run: RT is measured here, so the
    // decorative animation must not invalidate the Canvas per frame (C5 honesty).
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.4f, animated = false)

    Box(modifier = Modifier.fillMaxSize()) {
        when (val field = model.field) {
            // Two-choice (E2/E5/E13): LEFT/RIGHT halves + the arrow/deadline visual.
            is FocusFieldState.Choice -> ChoiceResponseLayer(onResponse) {
                FocusChoiceField(field = field, modifier = Modifier.fillMaxSize())
            }
            // Task switching (E6): same LEFT/RIGHT halves + the coloured-shape + rule cue.
            is FocusFieldState.RuleSwitch -> ChoiceResponseLayer(onResponse) {
                FocusRuleSwitchField(field = field, modifier = Modifier.fillMaxSize())
            }
            // Digital distraction (E11): LEFT/RIGHT main task + an intruder banner to ignore.
            is FocusFieldState.Anchor -> ChoiceResponseLayer(onResponse) {
                FocusAnchorField(field = field, modifier = Modifier.fillMaxSize())
            }
            // Visual search (E7): single-tap the whole field if the target is present.
            is FocusFieldState.Search -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusSearchField(field = field, modifier = Modifier.fillMaxSize())
            }
            // MOT (E9): tap if the probed dot was a tracked target.
            is FocusFieldState.Tracking -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusTrackingField(field = field, modifier = Modifier.fillMaxSize())
            }
            // RSVP (E10): tap when a target letter flashes.
            is FocusFieldState.Stream -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusStreamField(field = field, modifier = Modifier.fillMaxSize())
            }
            // Dual task (E8): tap on the green go target while monitoring the dual-trial counter.
            is FocusFieldState.Dual -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusDualTaskField(field = field, modifier = Modifier.fillMaxSize())
            }
            // Single-tap paradigms (E1/E3/E4/E12): the whole field is one response zone.
            else -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusStimulusField(field = field, modifier = Modifier.fillMaxSize())
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            SessionHud(model = model, onAbort = onAbort)
        }

        val hint = hintFor(model)
        val hintAlpha by animateFloatAsState(
            targetValue = if (hint.isEmpty()) 0f else 1f,
            animationSpec = tween(280),
            label = "hint",
        )
        Text(
            text = hint,
            color = AppColors.FocusReadout.copy(alpha = hintAlpha),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
        )
    }
}

@Composable
private fun ChoiceTapZone(modifier: Modifier, onTap: () -> Unit) {
    Box(modifier = modifier.fillMaxHeight().pointerInput(Unit) { detectTapGestures { onTap() } })
}

@Composable
private fun SingleTapField(onTap: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onTap() } }) {
        content()
    }
}

/** Two-choice response surface: LEFT/RIGHT screen halves under a paradigm-specific [content]. */
@Composable
private fun ChoiceResponseLayer(onResponse: (ResponseKey) -> Unit, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        ChoiceTapZone(modifier = Modifier.weight(1f)) { onResponse(ResponseKey.LEFT) }
        ChoiceTapZone(modifier = Modifier.weight(1f)) { onResponse(ResponseKey.RIGHT) }
    }
    content()
}

private fun hintFor(model: FocusRunningModel): String = when {
    model.phase == FocusPhase.WARMUP -> "Prepárate…"
    model.field is FocusFieldState.Stop -> "¡Frena! No toques"
    model.field is FocusFieldState.Choice -> "Izquierda o derecha según la flecha"
    model.field is FocusFieldState.RuleSwitch -> "Aplica la regla activa (color o forma)"
    model.field is FocusFieldState.Anchor -> "Sigue la flecha; ignora la notificación"
    model.field is FocusFieldState.Search -> "Toca solo si ves la L"
    model.field is FocusFieldState.Tracking -> "Sigue los puntos verdes"
    model.field is FocusFieldState.Stream -> "Toca al ver una letra de color"
    model.field is FocusFieldState.Dual -> "Toca en verde; vigila el contador"
    model.field is FocusFieldState.Idle -> "Mantén la mirada en el centro"
    model.field is FocusFieldState.Lure -> "No lo toques"
    else -> ""
}

@Composable
private fun SessionHud(model: FocusRunningModel, onAbort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FocusAbortButton(onAbort = onAbort)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudStat(value = "${model.hits}", label = "aciertos", color = AppColors.FocusHit)
            HudStat(value = "${model.lapses}", label = "lapsos", color = AppColors.FocusMiss)
            HudStat(value = "${model.falseAlarms}", label = "fallos", color = AppColors.FocusError)
        }
    }
    Spacer(Modifier.height(14.dp))
    ProgressBar(progress = model.progress)
}

@Composable
private fun HudStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = AppColors.FocusReadout.copy(alpha = 0.7f), fontSize = 9.sp)
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan))),
        )
    }
}

@Composable
private fun AbortedContent(reason: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = reason, color = AppColors.ProfileTextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusSignal,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onBack).padding(10.dp),
        )
    }
}
