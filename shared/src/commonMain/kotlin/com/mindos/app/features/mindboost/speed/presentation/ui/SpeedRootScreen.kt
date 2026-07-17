@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.speed.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.payload.PayloadKind
import com.mindos.app.features.mindboost.core.session.payload.PayloadReplayScanner
import com.mindos.app.features.mindboost.core.session.payload.SecurePayloadJournalStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.speed.di.rememberSpeedGraph
import com.mindos.app.features.mindboost.speed.di.speedSessionRecovery
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import com.mindos.app.features.mindboost.speed.presentation.e1.E1DestelloScreen
import com.mindos.app.features.mindboost.speed.presentation.e8.e8RunReplayHandler
import com.mindos.app.features.mindboost.speed.presentation.e2.E2ComparaScreen
import com.mindos.app.features.mindboost.speed.presentation.e4.E4CambioScreen
import com.mindos.app.features.mindboost.speed.presentation.e5.E5BusquedaVisualScreen
import com.mindos.app.features.mindboost.speed.presentation.e3.E3CifradoScreen
import com.mindos.app.features.mindboost.speed.presentation.e6.E6MultiviaScreen
import com.mindos.app.features.mindboost.speed.presentation.e7.E7CompuertaScreen
import com.mindos.app.features.mindboost.speed.presentation.e8.E8InhibicionScreen

/**
 * Raíz autocontenida de Velocidad Mental (MVP-0). Menú interno → E1 / E6. Mantiene su
 * propia navegación, igual que `FocusRootScreen`/`MemoryRootScreen`. Es el único punto que
 * el hub de MindBoost monta para este track.
 */
@Composable
fun SpeedRootScreen(
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = rememberSpeedGraph()
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    // SYSTEM-1 slice 6-R3 (P2-01): dedicated revocation-tombstone namespace shared by BOTH the SessionRecord and
    // CognitiveSessionRun stores below (one tombstone per subject; the same instance the account-deletion eraser
    // consults). Never blanket-cleared — see MindBoostStorageNamespaces.Revocation.
    val revocationStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Revocation)
    // SYSTEM-1 slice 4B: the E8 write-ahead lifecycle store (SAME `mindboost_sessions` the recovery below scans)
    // and the OFFICIAL cognitive-history store (`mindboost_session_runs`). Both account-bound and eraser-covered.
    // SYSTEM-1 slice 6-R3 (§14 fold-in): `subject = userId` scopes every physical key to THIS user (same reasoning
    // as `cognitiveRunStore` below) — the shared `mindboost_sessions` namespace can no longer commingle accounts.
    val sessionRecordStore = remember(sessionsStore, revocationStore, userId) {
        SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = userId)
    }
    val runsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.SessionRuns)
    // SYSTEM-1 slice 6-R3 (P0 fix, 6R3-P0C-01): `runsStore` is ONE physical namespace shared by every
    // locally-authenticated account on the device. Binding `subject = userId` at construction is what makes
    // save/get/listRecent/listByBlock/deleteAll on THIS instance touch only the CURRENT user's own rows — a
    // sign-out (which tears down this whole composable subtree) followed by a different account signing in
    // reconstructs a store bound to the NEW uid and can never read the previous account's rows.
    val cognitiveRunStore = remember(runsStore, revocationStore, userId) {
        SecureCognitiveSessionRunStore(runsStore, revocationStore = revocationStore, subject = userId)
    }
    // S3 write-ahead journal for the E8 run: the sealed CognitiveSessionRun is journaled before its save, and this
    // subject-scoped replay re-saves (idempotently — the run store dedups by runId) any entry a process death left
    // un-persisted. Over the account-bound `mindboost_payload_journal` namespace.
    val payloadJournalStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.PayloadJournal)
    val e8Journal = remember(payloadJournalStore, userId) {
        SecurePayloadJournalStore(payloadJournalStore, subject = userId, clock = graph.clock)
    }
    val e8Replay = remember(e8Journal, cognitiveRunStore) {
        PayloadReplayScanner(e8Journal, mapOf(PayloadKind.E8_COGNITIVE_RUN to e8RunReplayHandler(cognitiveRunStore)))
    }
    LaunchedEffect(Unit) {
        // MC1c: once per Velocidad track entry, mark orphaned STARTED/CHECKPOINT sessions (crash/kill/
        // nav-away) ABANDONED when stale by updatedAtMs. Idempotent, off the frame path; never touches
        // FINALIZED, scoring, or other blocks. No exercise session is active yet at this mount point.
        speedSessionRecovery(sessionsStore, revocationStore, graph.clock, userId).recover()
        // S3: re-apply any E8 run journaled but not persisted before a prior process death (idempotent by runId).
        e8Replay.replay()
    }
    var screen by remember { mutableStateOf<SpeedScreen>(SpeedScreen.Menu) }

    when (screen) {
        SpeedScreen.Menu -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            SpeedMenu(
                onSelect = { screen = it },
                onBack = onExit,
                modifier = modifier,
            )
        }
        SpeedScreen.E1 -> E1DestelloScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E2 -> E2ComparaScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E4 -> E4CambioScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E5 ->
            E5BusquedaVisualScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E3 -> E3CifradoScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E6 -> E6MultiviaScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E7 -> E7CompuertaScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
        SpeedScreen.E8 ->
            E8InhibicionScreen(
                graph = graph,
                sessionRecordStore = sessionRecordStore,
                cognitiveRunStore = cognitiveRunStore,
                payloadJournal = e8Journal,
                onExit = { screen = SpeedScreen.Menu },
                modifier = modifier,
            )
    }
}

@Composable
private fun SpeedMenu(onSelect: (SpeedScreen) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                SpeedStrings.FEATURE_TITLE,
                color = AppColors.FocusSignal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(SpeedStrings.MVP_NOTE, color = AppColors.FocusReadout, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            ExerciseCard(SpeedStrings.E1_TITLE, SpeedStrings.E1_INSTRUCTIONS) { onSelect(SpeedScreen.E1) }
            ExerciseCard(SpeedStrings.E2_TITLE, SpeedStrings.E2_INSTRUCTIONS) { onSelect(SpeedScreen.E2) }
            ExerciseCard(SpeedStrings.E4_TITLE, SpeedStrings.E4_INSTRUCTIONS) { onSelect(SpeedScreen.E4) }
            ExerciseCard(SpeedStrings.E5_TITLE, SpeedStrings.E5_INSTRUCTIONS) { onSelect(SpeedScreen.E5) }
            ExerciseCard(SpeedStrings.E6_TITLE, SpeedStrings.E6_INSTRUCTIONS) { onSelect(SpeedScreen.E6) }
            ExerciseCard(SpeedStrings.E3_TITLE, SpeedStrings.E3_INSTRUCTIONS) { onSelect(SpeedScreen.E3) }
            ExerciseCard(SpeedStrings.E7_TITLE, SpeedStrings.E7_INSTRUCTIONS) { onSelect(SpeedScreen.E7) }
            ExerciseCard(SpeedStrings.E8_TITLE, SpeedStrings.E8_INSTRUCTIONS) { onSelect(SpeedScreen.E8) }
            Spacer(Modifier.height(8.dp))
            Text(
                SpeedStrings.BACK,
                color = AppColors.FocusReadout,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }.padding(8.dp),
            )
        }
    }
}

@Composable
private fun ExerciseCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = AppColors.FocusSignal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

private sealed interface SpeedScreen {
    data object Menu : SpeedScreen
    data object E1 : SpeedScreen
    data object E2 : SpeedScreen
    data object E4 : SpeedScreen
    data object E5 : SpeedScreen
    data object E3 : SpeedScreen
    data object E6 : SpeedScreen
    data object E7 : SpeedScreen
    data object E8 : SpeedScreen
}
