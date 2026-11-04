@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.presentation.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.creativity.presentation.CreativityStrings
import com.mindos.app.features.mindboost.creativity.presentation.ui.CreativityRootScreen
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.payload.PayloadKind
import com.mindos.app.features.mindboost.core.session.payload.PayloadReplayScanner
import com.mindos.app.features.mindboost.core.session.payload.SecurePayloadJournalStore
import com.mindos.app.features.mindboost.decisiones.data.LocalReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.data.SecureDecisionSessionRepository
import com.mindos.app.features.mindboost.decisiones.data.SecureDiaryRepository
import com.mindos.app.features.mindboost.decisiones.presentation.DecisionsSessionRecorder
import com.mindos.app.features.mindboost.decisiones.presentation.abandonDecisionsSession
import com.mindos.app.features.mindboost.decisiones.presentation.decisionsSessionRecovery
import com.mindos.app.features.mindboost.decisiones.presentation.encodeReversibilityTrials
import com.mindos.app.features.mindboost.decisiones.presentation.reversibilityReplayHandler
import com.mindos.app.features.mindboost.decisiones.presentation.sealDecisionsSession
import com.mindos.app.features.mindboost.decisiones.presentation.ui.DecisionsRootScreen
import com.mindos.app.features.mindboost.decisiones.presentation.ui.rememberDecisionsFlowStateHolder
import com.mindos.app.features.mindboost.emotions.presentation.ui.EmotionRootScreen
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.ui.FocusRootScreen
import com.mindos.app.features.mindboost.language.presentation.ui.LanguageRootScreen
import com.mindos.app.features.mindboost.logic.presentation.ui.LogicRootScreen
import com.mindos.app.features.mindboost.memory.presentation.ui.MemoryRootScreen
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.presentation.rememberTodayPlanStateHolder
import com.mindos.app.features.mindboost.profile.presentation.state.toProductMotorLabel
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedRootScreen
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import kotlinx.coroutines.launch

/**
 * MindBoost hub: lets the user choose a training track (Enfoque / Memoria) and
 * mounts the corresponding self-contained root. This is the single point the app's
 * HomeScreen mounts for MindBoost; each track owns its own internal navigation.
 */
@Composable
fun MindBoostHubScreen(
    userId: String,
    displayName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var track by remember { mutableStateOf<MindBoostTrack>(MindBoostTrack.Hub) }
    // Hub-level scope so a launched persist (e.g. Decisiones reversibility on session finish) survives
    // intra-hub navigation (Decisiones → Result → Hub); it is cancelled only when MindBoost itself leaves
    // composition, far past the sub-second local write — closing the prior screen-scope cancellation window.
    val mindBoostScope = rememberCoroutineScope()
    val planHolder = rememberTodayPlanStateHolder(userId)
    LaunchedEffect(userId, track) {
        if (track is MindBoostTrack.Hub) planHolder.load()
    }

    when (val current = track) {
        is MindBoostTrack.Hub -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            HubContent(
                onEnfoque = { track = MindBoostTrack.Enfoque },
                onMemoria = { track = MindBoostTrack.Memoria },
                onVelocidad = { track = MindBoostTrack.Velocidad },
                onLogica = { track = MindBoostTrack.Logica },
                onLenguaje = { track = MindBoostTrack.Lenguaje },
                onCreatividad = { track = MindBoostTrack.Creatividad },
                onDecisiones = {
                    val now = SystemEpochClock.nowMillis()
                    track = MindBoostTrack.Decisiones(sessionId = "$userId:$now", timestampMs = now)
                },
                onEmociones = { track = MindBoostTrack.Emociones },
                plan = planHolder.plan,
                onBack = onExit,
                modifier = modifier,
            )
        }

        is MindBoostTrack.Enfoque -> FocusRootScreen(
            userId = userId,
            displayName = displayName,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Memoria -> MemoryRootScreen(
            userId = userId,
            displayName = displayName,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Velocidad -> SpeedRootScreen(
            userId = userId,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Logica -> LogicRootScreen(
            userId = userId,
            displayName = displayName,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Lenguaje -> LanguageRootScreen(
            userId = userId,
            displayName = displayName,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Creatividad -> CreativityRootScreen(
            userId = userId,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )

        is MindBoostTrack.Decisiones -> {
            val decisionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Decisiones)
            val reversibilityStore = remember(decisionsStore) { LocalReversibilityResultStore(decisionsStore) }
            // S3 write-ahead journal for the reversibility log: the finished-session trials are journaled durably
            // BEFORE they are merged, and an idempotent replay (recordOnce dedups by session id) re-applies any
            // entry a process death left un-persisted. Subject-scoped over the account-bound payload-journal namespace.
            val payloadJournalStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.PayloadJournal)
            val reversibilityJournal = remember(payloadJournalStore, userId) {
                SecurePayloadJournalStore(payloadJournalStore, subject = userId, clock = SystemEpochClock)
            }
            val reversibilityReplay = remember(reversibilityJournal, reversibilityStore, userId) {
                PayloadReplayScanner(
                    reversibilityJournal,
                    mapOf(PayloadKind.REVERSIBILITY_LOG to reversibilityReplayHandler(reversibilityStore, userId)),
                )
            }
            // The Decisiones flow id — the journal entry id that makes replay idempotent per session. Captured into a
            // local so the reversibility callback closes over a stable value, not the smart-cast `current`.
            val decisionsSessionId = current.sessionId
            val sessionRepository = remember(decisionsStore) { SecureDecisionSessionRepository(decisionsStore) }
            val diaryRepository = remember(decisionsStore) { SecureDiaryRepository(decisionsStore) }
            // MC6: write-ahead SessionRecord over mindboost_sessions — opaque, minimized lifecycle only (never
            // diary/scenario/choice/round content). The coordinator keeps DecisionsFlowStateHolder pure.
            val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
            // SYSTEM-1 slice 6-R3 (P2-01): shared revocation-tombstone namespace (never blanket-cleared).
            val revocationStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Revocation)
            val decisionsRecorder = remember(current.sessionId, userId, revocationStore) {
                // SYSTEM-1 slice 6-R3 (§14 fold-in): subject = userId scopes every physical key in the shared
                // `mindboost_sessions` namespace to THIS user, closing the cross-account isolation gap.
                val store = SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = userId)
                DecisionsSessionRecorder(SecureSessionRecorder(store, SystemEpochClock))
            }
            // handleSealed calls BOTH onSessionSealed and onExit on seal; this flag (set synchronously in
            // onSessionSealed) makes the seal's onExit a no-op so we never abandon a session being finalized.
            val sealed = remember(current.sessionId) { mutableStateOf(false) }
            // MC6c: recover write-ahead sessions abandoned in a prior process, once per Decisiones entry
            // (block-agnostic over mindboost_sessions; never touches FINALIZED, scoring, or Decisiones stores).
            // S3: also re-apply any reversibility trials journaled but not persisted before a prior process death.
            LaunchedEffect(Unit) {
                decisionsSessionRecovery(sessionsStore, revocationStore, SystemEpochClock, userId).recover()
                reversibilityReplay.replay()
            }
            LaunchedEffect(current.sessionId) { decisionsRecorder.start(mindBoostScope) }
            DecisionsRootScreen(
                flow = rememberDecisionsFlowStateHolder(
                    sessionId = current.sessionId,
                    timestampMs = current.timestampMs,
                ),
                onExit = {
                    // Mid-flow back: abandon (coordinator no-ops if no real session / already finalized) before
                    // navigating. On the seal path `sealed` is already true → this is a no-op; the seal owns nav.
                    if (!sealed.value) {
                        mindBoostScope.launch {
                            abandonDecisionsSession(
                                abandon = { decisionsRecorder.abandon() },
                                navigate = { track = MindBoostTrack.Hub },
                            )
                        }
                    }
                },
                modifier = modifier,
                onReversibilityTrials = { trials ->
                    // S3 (was fire-and-forget `record`, which lost the session on a process death between launch
                    // and write): journal the trials durably FIRST, THEN drain (apply idempotently + confirm).
                    // mindBoostScope outlives Decisiones->Result->Hub, so neither write is cancelled by navigation;
                    // a crash after the enqueue is recovered by replay (recordOnce dedups by session id).
                    mindBoostScope.launch {
                        reversibilityJournal.enqueue(
                            entryId = decisionsSessionId,
                            kind = PayloadKind.REVERSIBILITY_LOG,
                            payload = encodeReversibilityTrials(trials),
                        )
                        reversibilityReplay.replay()
                    }
                },
                onSessionSealed = { session, entry ->
                    // B1 + MC6: persist own data (session + diary), THEN finalize the write-ahead record, and only
                    // THEN navigate. mindBoostScope outlives Decisiones->Hub, so neither write is cancelled.
                    sealed.value = true
                    mindBoostScope.launch {
                        sealDecisionsSession(
                            persistOwnData = {
                                sessionRepository.save(session)
                                diaryRepository.save(entry)
                            },
                            finalize = { decisionsRecorder.finalize() },
                            navigate = { track = MindBoostTrack.Hub },
                        )
                    }
                },
            )
        }

        is MindBoostTrack.Emociones -> EmotionRootScreen(
            userId = userId,
            onExit = { track = MindBoostTrack.Hub },
            modifier = modifier,
        )
    }
}

@Composable
@Suppress("LongParameterList") // The hub owns one navigation callback per self-contained track.
private fun HubContent(
    onEnfoque: () -> Unit,
    onMemoria: () -> Unit,
    onVelocidad: () -> Unit,
    onLogica: () -> Unit,
    onLenguaje: () -> Unit,
    onCreatividad: () -> Unit,
    onDecisiones: () -> Unit,
    onEmociones: () -> Unit,
    plan: List<TrainingRecommendation>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("MindBoost", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Elige tu entrenamiento", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            OnboardingNote()
            Spacer(Modifier.height(8.dp))
            TodayPlanCard(
                plan = plan,
                onTrain = { module ->
                    when (module) {
                        CognitiveModuleId.ENFOQUE -> onEnfoque()
                        CognitiveModuleId.MEMORIA -> onMemoria()
                        CognitiveModuleId.LOGICA -> onLogica()
                        CognitiveModuleId.DECISIONES -> onDecisiones()
                        CognitiveModuleId.EMOCIONES -> onEmociones()
                        else -> Unit
                    }
                },
            )
            TrackCard(
                title = "Enfoque",
                subtitle = "Atención, inhibición, control e integración.",
                onClick = onEnfoque,
            )
            TrackCard(
                title = "Memoria",
                subtitle = "Trabajo, episódica, asociativa y recuerdo diferido.",
                onClick = onMemoria,
            )
            TrackCard(
                title = "Velocidad Mental",
                subtitle = SpeedStrings.HUB_SUBTITLE,
                tag = "BETA",
                onClick = onVelocidad,
            )
            TrackCard(
                title = "Lógica",
                subtitle = "Inferencias, condicionales, contradicciones, contraejemplos y confianza.",
                onClick = onLogica,
            )
            TrackCard(
                title = "Decisiones",
                subtitle = "Calibración bayesiana, triaje de reversibilidad y registro reflexivo.",
                onClick = onDecisiones,
            )
            TrackCard(
                title = "Emociones",
                subtitle = "Pausa, recuperación tras error y claridad bajo carga. Entrenamiento funcional.",
                onClick = onEmociones,
            )
            TrackCard(
                title = "Lenguaje",
                subtitle = "Elige ejercicio: evidencia, precisión léxica, síntesis, argumentos y más.",
                onClick = onLenguaje,
            )
            TrackCard(
                title = "Creatividad",
                subtitle = CreativityStrings.HUB_SUBTITLE,
                tag = "PRÁCTICA",
                onClick = onCreatividad,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Volver",
                color = AppColors.FocusReadout,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }.padding(8.dp),
            )
        }
    }
}

/** Lightweight onboarding note on the hub: how MindBoost works, honestly, and where the profile lives. */
@Composable
private fun OnboardingNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.FocusSignal.copy(alpha = 0.08f))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Cómo funciona", color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            // R19 · 2026-09-07: the previous wording named a forbidden lexeme (denying it does not exempt
            // it) and said the exercises "mide", the claim Enfoque's own catalogue was corrected not to
            // make. Bounded positively instead; `presentation` is now inside the copy-safety gate.
            "Entrena un poco cada día. Cada ejercicio registra tu rendimiento frente a tu propia línea " +
                "base — es orientativo, solo describe tu práctica en esta app y no promete subir tu nota. " +
                "Tu perfil cognitivo se construye con el tiempo: deslízalo a la izquierda desde tu perfil " +
                "para verlo.",
            color = AppColors.FocusReadout,
            fontSize = 12.sp,
        )
    }
}

/** Cross-module "plan de hoy": the 2–3 tracks the profile suggests today. Hidden while the plan is empty. */
@Composable
private fun TodayPlanCard(plan: List<TrainingRecommendation>, onTrain: (CognitiveModuleId) -> Unit) {
    if (plan.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.FocusSignal.copy(alpha = 0.10f))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Tu plan de hoy", color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        plan.forEach { rec ->
            rec.moduleId?.let { module ->
                PlanRow(module.toProductMotorLabel(), planReasonText(rec.reason)) { onTrain(module) }
            }
        }
    }
}

@Composable
private fun PlanRow(title: String, reason: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (reason.isNotEmpty()) Text(reason, color = AppColors.FocusReadout, fontSize = 12.sp)
        }
        Text("Entrenar ›", color = AppColors.FocusSignal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun planReasonText(reason: RecommendationReason): String = when (reason) {
    RecommendationReason.GATHER_DATA -> "para reunir datos de tu perfil"
    RecommendationReason.DEVELOP_AREA -> "para desarrollar esta área"
    RecommendationReason.NONE_AVAILABLE -> ""
}

@Composable
private fun TrackCard(title: String, subtitle: String, onClick: () -> Unit, tag: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = AppColors.FocusSignal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            if (tag != null) TrackTag(tag)
        }
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

/** Small honesty badge (e.g. BETA / PRÁCTICA) marking a track that is not yet an authoritative measure. */
@Composable
private fun TrackTag(text: String) {
    Text(
        text = text,
        color = AppColors.OnBright,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.Warning)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private sealed interface MindBoostTrack {
    data object Hub : MindBoostTrack
    data object Enfoque : MindBoostTrack
    data object Memoria : MindBoostTrack
    data object Velocidad : MindBoostTrack
    data object Logica : MindBoostTrack
    data object Lenguaje : MindBoostTrack
    data object Creatividad : MindBoostTrack
    data object Emociones : MindBoostTrack
    data class Decisiones(val sessionId: String, val timestampMs: Long) : MindBoostTrack
}
