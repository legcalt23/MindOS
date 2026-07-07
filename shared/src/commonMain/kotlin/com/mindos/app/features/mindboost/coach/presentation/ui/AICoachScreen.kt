@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "MaxLineLength", "TooManyFunctions") // Compose screen: PascalCase composables + layout constants + full copy read best whole.

package com.mindos.app.features.mindboost.coach.presentation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.coach.presentation.AICoachStrings
import com.mindos.app.features.mindboost.coach.presentation.AICoachUiState
import com.mindos.app.features.mindboost.coach.presentation.CoachAccessState
import com.mindos.app.features.mindboost.coach.presentation.CoachConfirm
import com.mindos.app.features.mindboost.coach.presentation.CoachMemoryUi
import com.mindos.app.features.mindboost.coach.presentation.CoachMessageUi
import com.mindos.app.features.mindboost.coach.presentation.CoachRecommendationUi

/**
 * MC8u-G — the minimal, premium, non-manipulative AI Coach screen. Renders every access phase and, within READY, the
 * conversation, quota, memory, and recommendation panels. Each state is a labelled Text (never color-only). The screen
 * holds no logic beyond a local draft field.
 */
@Composable
fun AICoachScreen(state: AICoachUiState, actions: AICoachActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(AICoachStrings.TITLE, color = AppColors.FocusSignal, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        when (state.access) {
            CoachAccessState.LOADING -> Text("Cargando…", color = AppColors.FocusReadout, fontSize = 14.sp)
            CoachAccessState.FREE_LOCKED -> FreeLockedView()
            CoachAccessState.CONSENT_REQUIRED -> ConsentRequiredView(actions)
            CoachAccessState.CONSENT_EXPLANATION -> ConsentExplanationView(actions)
            CoachAccessState.READY -> ReadyView(state, actions)
            CoachAccessState.QUOTA_REACHED -> QuotaReachedView(state, actions)
        }
        Disclaimers()
    }
}

@Composable
private fun FreeLockedView() {
    Text(AICoachStrings.FREE_ENTRY, color = AppColors.FocusReadout, fontSize = 14.sp)
}

@Composable
private fun ConsentRequiredView(actions: AICoachActions) {
    Text(AICoachStrings.ANALYZES, color = AppColors.FocusReadout, fontSize = 14.sp)
    Text(AICoachStrings.STRUCTURED_SUMMARIES, color = AppColors.FocusReadout, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onGrantConsent) { Text(AICoachStrings.CONSENT_ACCEPT) }
        TextButton(onClick = actions.onShowConsentExplanation) { Text("Ver detalles") }
    }
}

@Composable
private fun ConsentExplanationView(actions: AICoachActions) {
    Text(AICoachStrings.CONSENT_TITLE, color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    Text(AICoachStrings.CONSENT_BODY, color = AppColors.FocusReadout, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onGrantConsent) { Text(AICoachStrings.CONSENT_ACCEPT) }
        TextButton(onClick = actions.onDismissConsentExplanation) { Text(AICoachStrings.CONSENT_DECLINE) }
    }
}

@Composable
private fun ReadyView(state: AICoachUiState, actions: AICoachActions) {
    Text(AICoachStrings.messagesLeftToday(state.remainingQuota), color = AppColors.FocusReadout, fontSize = 12.sp)
    Transcript(state.messages)
    MessageComposer(isSending = state.isSending, onSend = actions.onSend)
    MemorySection(state.memory, state.confirming, actions)
    RecommendationSection(state.recommendation, actions)
    TextButton(onClick = actions.onRevokeConsent) { Text(AICoachStrings.CONSENT_REVOKE) }
}

@Composable
private fun QuotaReachedView(state: AICoachUiState, actions: AICoachActions) {
    Text(AICoachStrings.DAILY_LIMIT, color = AppColors.FocusReadout, fontSize = 14.sp)
    Text(AICoachStrings.TRAINING_AVAILABLE_AT_LIMIT, color = AppColors.FocusReadout, fontSize = 13.sp)
    MemorySection(state.memory, state.confirming, actions)
    TextButton(onClick = actions.onRevokeConsent) { Text(AICoachStrings.CONSENT_REVOKE) }
}

@Composable
private fun Transcript(messages: List<CoachMessageUi>) {
    if (messages.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        messages.forEach { message ->
            val who = if (message.fromUser) "Tú" else "Coach"
            Text("$who: ${message.text}", color = AppColors.FocusReadout, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MessageComposer(isSending: Boolean, onSend: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(AICoachStrings.INPUT_HINT) },
        enabled = !isSending,
        singleLine = false,
    )
    Button(
        onClick = { onSend(draft); draft = "" },
        enabled = !isSending && draft.isNotBlank(),
    ) { Text(AICoachStrings.SEND) }
}

@Composable
private fun MemorySection(memory: CoachMemoryUi, confirming: CoachConfirm?, actions: AICoachActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(AICoachStrings.MEMORY_TITLE, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        when (memory) {
            CoachMemoryUi.Empty -> {
                Text(AICoachStrings.MEMORY_EMPTY, color = AppColors.FocusReadout, fontSize = 13.sp)
                TextButton(onClick = actions.onViewMemory) { Text("Actualizar") }
            }
            is CoachMemoryUi.Loaded -> {
                Text(memory.summary, color = AppColors.FocusReadout, fontSize = 13.sp)
                Text(memory.recommendations, color = AppColors.FocusReadout, fontSize = 12.sp)
                if (!memory.cloudActive) Text(AICoachStrings.MEMORY_LOCAL_ONLY, color = AppColors.FocusReadout, fontSize = 11.sp)
                if (confirming == CoachConfirm.DELETE_MEMORY) {
                    Text(AICoachStrings.DELETE_MEMORY_CONFIRM, color = AppColors.FocusReadout, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = actions.onConfirmDeleteMemory) { Text("Borrar") }
                        TextButton(onClick = actions.onCancelConfirmation) { Text(AICoachStrings.CONSENT_DECLINE) }
                    }
                } else {
                    OutlinedButton(onClick = actions.onRequestDeleteMemory) { Text(AICoachStrings.DELETE_MEMORY) }
                }
            }
            CoachMemoryUi.Deleted -> Text(AICoachStrings.MEMORY_DELETED, color = AppColors.FocusReadout, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RecommendationSection(recommendation: CoachRecommendationUi, actions: AICoachActions) {
    when (recommendation) {
        CoachRecommendationUi.None -> TextButton(onClick = actions.onShowRecommendation) { Text("Ver sugerencia de práctica") }
        is CoachRecommendationUi.Shown -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(AICoachStrings.RECOMMENDATION_TITLE, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(recommendation.suggestion.explanation, color = AppColors.FocusReadout, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = actions.onConfirmApplyRecommendation) { Text(AICoachStrings.RECOMMENDATION_CONFIRM) }
                    TextButton(onClick = actions.onRejectRecommendation) { Text(AICoachStrings.RECOMMENDATION_REJECT) }
                }
            }
        }
        CoachRecommendationUi.Applied -> Text(AICoachStrings.RECOMMENDATION_APPLIED, color = AppColors.FocusReadout, fontSize = 13.sp)
        CoachRecommendationUi.PendingNotWired -> Text(AICoachStrings.RECOMMENDATION_PENDING, color = AppColors.FocusReadout, fontSize = 13.sp)
        CoachRecommendationUi.Rejected -> Text(AICoachStrings.RECOMMENDATION_REJECTED, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

@Composable
private fun Disclaimers() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        AICoachStrings.DISCLAIMERS.forEach { Text(it, color = AppColors.FocusReadout, fontSize = 11.sp) }
    }
}
