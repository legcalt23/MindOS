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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.decisiones.presentation.DecisionResultStateHolder
import com.mindos.app.features.mindboost.decisiones.presentation.ui.components.CognitivePolygon
import com.mindos.app.features.mindboost.decisiones.presentation.ui.components.PolygonAxisLegend
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DiaryEntry
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound
import kotlin.math.roundToInt

/** Composition-scoped factory for [DecisionResultStateHolder] (assembles the session once). */
@Composable
fun rememberDecisionResultStateHolder(
    sessionId: String,
    timestampMs: Long,
    checkIn: CheckInInput,
    rounds: List<DecisionRound>,
): DecisionResultStateHolder = remember(sessionId, timestampMs, checkIn, rounds) {
    DecisionResultStateHolder(
        sessionId = sessionId,
        timestampMs = timestampMs,
        checkIn = checkIn,
        rounds = rounds,
    )
}

/**
 * Resultado + Diario screen: the cognitive polygon, an honest coach note, and a free-text diary the
 * user can seal. Sealing freezes the diary and emits the session + entry for persistence.
 */
@Composable
fun DecisionResultScreen(
    holder: DecisionResultStateHolder,
    onSealed: (DecisionSession, DiaryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = holder.uiState
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(text = "Resultado", color = AppColors.ProfileTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CognitivePolygon(axes = state.axes)
        }

        Spacer(Modifier.height(16.dp))
        PolygonAxisLegend(axes = state.axes)

        Spacer(Modifier.height(12.dp))
        StatRow(label = "Reto enfrentado", value = "${state.complexity.roundToInt()}")

        state.coachMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            CoachCard(message = message)
        }

        Spacer(Modifier.height(22.dp))
        Text(text = "TU DIARIO", color = AppColors.FocusReadout, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        DiaryField(text = holder.diaryText, enabled = !holder.sealed, onChange = holder::onDiaryTextChange)

        Spacer(Modifier.height(20.dp))
        SealButton(sealed = holder.sealed) {
            val entry = holder.seal()
            onSealed(holder.session, entry)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(text = value, color = AppColors.ProfileTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoachCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.06f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(text = "COACH", color = AppColors.FocusSignal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text = message, color = AppColors.ProfileTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun DiaryField(text: String, enabled: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = text,
        onValueChange = onChange,
        enabled = enabled,
        textStyle = TextStyle(color = AppColors.ProfileTextPrimary, fontSize = 15.sp, lineHeight = 21.sp),
        cursorBrush = SolidColor(AppColors.FocusSignal),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.05f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(16.dp))
            .padding(14.dp),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = "Escribe una reflexión (opcional)…",
                        color = AppColors.FocusReadout,
                        fontSize = 15.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun SealButton(sealed: Boolean, onSeal: () -> Unit) {
    val background = if (sealed) AppColors.ProfileOnline.copy(alpha = 0.2f) else AppColors.FocusSignal
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .then(if (sealed) Modifier else Modifier.clickable(onClick = onSeal)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (sealed) "Sesión sellada" else "Sellar sesión",
            color = if (sealed) AppColors.ProfileOnline else AppColors.OnBright,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
