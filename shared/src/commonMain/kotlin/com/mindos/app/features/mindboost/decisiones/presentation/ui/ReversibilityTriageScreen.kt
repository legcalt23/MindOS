@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions")

package com.mindos.app.features.mindboost.decisiones.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.decisiones.presentation.ReversibilityTriageStateHolder
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityFeedback
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.session.DecisionRound
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** Composition-scoped factory for [ReversibilityTriageStateHolder] with a default card set. */
@Composable
fun rememberReversibilityTriageStateHolder(
    seed: Long = ReversibilityTriageStateHolder.DEFAULT_SEED,
): ReversibilityTriageStateHolder = remember(seed) {
    ReversibilityTriageStateHolder(scenarios = ReversibilityTriageStateHolder.generateScenarios(seed = seed))
}

/**
 * Reversibility Triaje (EX-19) screen. The user classifies each card as reversible or irreversible.
 * Choosing "irreversible" opens a confirmation step that is locked for a deliberation window — the
 * signature mechanic that forces analytical reading before an irreversible commit.
 */
@Composable
fun ReversibilityTriageScreen(
    holder: ReversibilityTriageStateHolder,
    onFinished: (List<DecisionRound>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        when (val state = holder.uiState) {
            is ReversibilityTriageUiState.Idle -> IntroContent(
                onStart = holder::start,
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )

            is ReversibilityTriageUiState.Running -> RunningContent(
                state = state,
                lockDurationMs = holder.currentLockDurationMs(),
                onCommit = holder::commit,
                modifier = Modifier.align(Alignment.Center),
            )

            is ReversibilityTriageUiState.Finished -> FinishedContent(
                state = state,
                onContinue = { onFinished(state.rounds) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun IntroContent(onStart: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Triaje", color = AppColors.ProfileTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Clasifica cada decisión: ¿se puede deshacer o no? Las irreversibles piden una pausa " +
                "antes de confirmar; las reversibles, rapidez.",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(28.dp))
        CtaButton(text = "Empezar", onClick = onStart)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun RunningContent(
    state: ReversibilityTriageUiState.Running,
    lockDurationMs: Long,
    onCommit: (Boolean, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mark = remember(state.cardIndex) { TimeSource.Monotonic.markNow() }
    var confirmingIrreversible by remember(state.cardIndex) { mutableStateOf(false) }
    var unlocked by remember(state.cardIndex) { mutableStateOf(false) }

    if (confirmingIrreversible && lockDurationMs > 0) {
        LaunchedEffect(state.cardIndex) {
            delay(lockDurationMs)
            unlocked = true
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Carta ${state.cardIndex + 1} / ${state.cardCount}",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        PromptCard(prompt = state.prompt)
        Spacer(Modifier.height(24.dp))

        if (confirmingIrreversible) {
            IrreversibleConfirm(
                unlocked = unlocked,
                onConfirm = { onCommit(false, mark.elapsedNow().inWholeMilliseconds) },
            )
        } else {
            ChoiceButton(text = "Reversible", accent = AppColors.FocusSignal) {
                onCommit(true, mark.elapsedNow().inWholeMilliseconds)
            }
            Spacer(Modifier.height(12.dp))
            ChoiceButton(text = "Irreversible", accent = AppColors.Error) {
                if (lockDurationMs > 0) {
                    confirmingIrreversible = true
                } else {
                    onCommit(false, mark.elapsedNow().inWholeMilliseconds)
                }
            }
        }
    }
}

@Composable
private fun PromptCard(prompt: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.05f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = prompt,
            color = AppColors.ProfileTextPrimary,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IrreversibleConfirm(unlocked: Boolean, onConfirm: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Decisión irreversible. Tómate un momento antes de confirmar.",
            color = AppColors.Error,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (unlocked) AppColors.Error else Color.White.copy(alpha = 0.06f))
                .then(if (unlocked) Modifier.clickable(onClick = onConfirm) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (unlocked) "Confirmar" else "Espera…",
                color = if (unlocked) AppColors.OnBright else AppColors.FocusReadout,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChoiceButton(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FinishedContent(
    state: ReversibilityTriageUiState.Finished,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${(state.summary.accuracy * 100).toInt()}%",
            color = AppColors.FocusSignal,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "clasificación correcta", color = AppColors.FocusReadout, fontSize = 13.sp)
        if (state.summary.lockViolations > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${state.summary.lockViolations} irreversible(s) confirmada(s) sin pausa",
                color = AppColors.Error,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        state.perCard.forEachIndexed { index, item ->
            CardFeedbackRow(index = index, feedback = item)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(18.dp))
        CtaButton(text = "Continuar", onClick = onContinue)
    }
}

@Composable
private fun CardFeedbackRow(index: Int, feedback: ReversibilityFeedback) {
    val accent = if (feedback.correct) AppColors.FocusSignal else AppColors.Error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.04f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Carta ${index + 1}", color = AppColors.FocusReadout, fontSize = 12.sp)
            Text(
                text = if (feedback.actuallyReversible) "Reversible" else "Irreversible",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = if (feedback.correct) "Bien clasificada" else "Mal clasificada",
            color = AppColors.ProfileTextPrimary,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.FocusSignal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.OnBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
