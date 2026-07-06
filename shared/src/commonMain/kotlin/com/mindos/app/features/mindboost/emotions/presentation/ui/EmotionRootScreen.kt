@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui

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
import com.mindos.app.features.mindboost.emotions.di.emotionSessionRecovery
import com.mindos.app.features.mindboost.emotions.di.emotionTerminalReconciler
import com.mindos.app.features.mindboost.emotions.di.rememberEmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import com.mindos.app.features.mindboost.emotions.presentation.rememberEmotionHomeStateHolder
import com.mindos.app.features.mindboost.emotions.presentation.rememberEmotionSessionStateHolder
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionExerciseCardUi
import kotlin.coroutines.cancellation.CancellationException

/**
 * Self-contained root of MindBoost · Emociones: a menu of the playable exercises (mirrors
 * `EmotionExerciseCatalog.implemented`) → a session. Mounted by the hub; owns its own internal nav.
 */
@Composable
fun EmotionRootScreen(userId: String, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val graph = rememberEmotionGraph()
    val home = rememberEmotionHomeStateHolder()
    // MC8i P1: key local navigation by userId so an account change within this composition resets the selection —
    // no exercise/session of the previous user can stay visible. (rememberEmotionSessionStateHolder already keys
    // the holder + seed by userId, so the session holder itself is per-user too.)
    var selected by remember(userId) { mutableStateOf<EmotionExerciseId?>(null) }

    // MC8g: FIRST reconcile durable emotion terminals (retry pending summary saves + repair SessionRecords to
    // match the canonical summary), THEN run the generic orphan scanner. Order matters: a completed/safety-stop
    // session must be finalized/retried before the block-agnostic scanner could abandon its STARTED record.
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    // MC8h P1: key on userId (+ the concrete collaborators) so if the mounted account changes within this
    // composition the reconciler re-runs for the CURRENT user — never only the first user who mounted the screen.
    LaunchedEffect(userId, graph.repository, sessionsStore) {
        // reconcile() is already fault-isolated (total). MC8j P1: also isolate the generic (core-owned) scanner so a
        // recoverable storage failure there cannot crash module entry — cooperative cancellation still propagates.
        emotionTerminalReconciler(graph.repository, sessionsStore, graph.epochClock, userId).reconcile()
        runCatching { emotionSessionRecovery(sessionsStore, graph.epochClock).recover() }
            .exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    when (val current = selected) {
        null -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            EmotionMenu(home.exercises, onSelect = { selected = it }, onBack = onExit, modifier = modifier)
        }
        else -> {
            val seed = remember(userId, current) { graph.epochClock.nowMillis() }
            val holder = rememberEmotionSessionStateHolder(graph, current, userId, seed)
            EmotionSessionScreen(holder, onFinished = { selected = null }, modifier = modifier)
        }
    }
}

@Composable
private fun EmotionMenu(
    exercises: List<EmotionExerciseCardUi>,
    onSelect: (EmotionExerciseId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(EmotionStrings.MODULE_TITLE, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(EmotionStrings.MODULE_SUBTITLE, color = AppColors.FocusReadout, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            exercises.forEach { card ->
                ExerciseCard(card.name, card.description) { onSelect(card.id) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                EmotionStrings.DISCLAIMERS[0],
                color = AppColors.FocusReadout,
                fontSize = 11.sp,
            )
            Text(
                "Volver",
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}
