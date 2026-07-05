@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "LongParameterList")

package com.mindos.app.features.mindboost.creativity.presentation.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.creativity.di.creativitySessionRecorder
import com.mindos.app.features.mindboost.creativity.di.creativitySessionRecovery
import com.mindos.app.features.mindboost.creativity.di.rememberCreativityGraph
import com.mindos.app.features.mindboost.creativity.domain.exercise.CreativityExerciseCatalog
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseDefinition
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.scoring.CreativityRubricScorer
import com.mindos.app.features.mindboost.creativity.presentation.CreativitySessionStateHolder
import com.mindos.app.features.mindboost.creativity.presentation.CreativityStrings
import com.mindos.app.features.mindboost.creativity.presentation.CreativityUiState
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import kotlin.math.roundToInt

/**
 * Self-contained root for the offline Creativity practice feature: internal menu → round →
 * feedback. Mirrors `SpeedRootScreen`/`FocusRootScreen`. The single entry the MindBoost hub mounts.
 * The safety disclaimer (practice / not a diagnosis / not a graded ability test) is always visible.
 */
@Composable
fun CreativityRootScreen(
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = rememberCreativityGraph()
    val scope = rememberCoroutineScope()
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val recorder = remember(sessionsStore) { creativitySessionRecorder(sessionsStore, SystemEpochClock) }
    // Recover write-ahead sessions abandoned in a prior process, once per track entry (before any round starts):
    // stale STARTED/CHECKPOINT → ABANDONED, never touching FINALIZED, scoring, or creative content.
    LaunchedEffect(Unit) { creativitySessionRecovery(sessionsStore, SystemEpochClock).recover() }
    var holder by remember { mutableStateOf<CreativitySessionStateHolder?>(null) }

    val active = holder
    if (active == null) {
        PlatformBackHandler(enabled = true, onBack = onExit)
        CreativityMenu(
            onSelect = { exerciseId ->
                val created = CreativitySessionStateHolder(scope, graph.coordinator, userId, recorder = recorder)
                created.startExercise(exerciseId)
                holder = created
            },
            onBack = onExit,
            modifier = modifier,
        )
    } else {
        PlatformBackHandler(enabled = true, onBack = { holder = null })
        CreativitySessionScreen(holder = active, onExit = { holder = null }, modifier = modifier)
    }
}

@Composable
private fun CreativityMenu(
    onSelect: (CreativityExerciseId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                CreativityStrings.FEATURE_TITLE,
                color = AppColors.FocusSignal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(CreativityStrings.MENU_SUBTITLE, color = Color.White, fontSize = 14.sp)
            Disclaimer()
            Spacer(Modifier.height(8.dp))
            CreativityExerciseCatalog.implemented.forEach { id ->
                val def = CreativityExerciseCatalog.definitionFor(id)
                ExerciseCard(def.title, def.instructions) { onSelect(id) }
            }
            Spacer(Modifier.height(8.dp))
            BackText(onBack)
        }
    }
}

@Composable
private fun CreativitySessionScreen(
    holder: CreativitySessionStateHolder,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by holder.state.collectAsState()
    val ideas = remember(holder) { mutableStateListOf<String>() }
    var draft by remember(holder) { mutableStateOf("") }

    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = state) {
                is CreativityUiState.InProgress -> RoundContent(
                    definition = CreativityExerciseCatalog.definitionFor(s.active.exerciseId),
                    promptText = s.active.prompt.text,
                    ideas = ideas,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onAdd = { addIdea(ideas) { draft }.also { draft = "" } },
                    onSubmit = { holder.submit(ideas.toList()) },
                    errorText = null,
                    onExit = onExit,
                )

                is CreativityUiState.Rejected -> RoundContent(
                    definition = CreativityExerciseCatalog.definitionFor(s.active.exerciseId),
                    promptText = s.active.prompt.text,
                    ideas = ideas,
                    draft = draft,
                    onDraftChange = { draft = it },
                    onAdd = { addIdea(ideas) { draft }.also { draft = "" } },
                    onSubmit = { holder.submit(ideas.toList()) },
                    errorText = CreativityStrings.rejectionText(s.reason),
                    onExit = onExit,
                )

                is CreativityUiState.Submitting ->
                    Text(CreativityStrings.SUBMITTING, color = AppColors.FocusReadout, fontSize = 15.sp)

                is CreativityUiState.Completed -> CompletedContent(
                    session = s,
                    onAnother = {
                        ideas.clear()
                        draft = ""
                        holder.startExercise(s.active.exerciseId)
                    },
                    onExit = onExit,
                )

                CreativityUiState.Idle -> BackText(onExit)
            }
        }
    }
}

@Composable
private fun RoundContent(
    definition: CreativityExerciseDefinition,
    promptText: String,
    ideas: List<String>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onSubmit: () -> Unit,
    errorText: String?,
    onExit: () -> Unit,
) {
    Text(definition.title, color = AppColors.FocusSignal, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text(definition.instructions, color = Color.White, fontSize = 14.sp)
    Text("Tema: $promptText", color = AppColors.FocusReadout, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    if (errorText != null) {
        Text(errorText, color = AppColors.FocusSignal, fontSize = 13.sp)
    }
    OutlinedTextField(
        value = draft,
        onValueChange = onDraftChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(CreativityStrings.IDEA_PLACEHOLDER) },
        singleLine = true,
    )
    PrimaryText(CreativityStrings.ADD_IDEA, onAdd)
    Text("${CreativityStrings.IDEAS_PREFIX}${ideas.size}", color = AppColors.FocusReadout, fontSize = 13.sp)
    ideas.forEach { idea -> Text("• $idea", color = Color.White, fontSize = 14.sp) }
    Spacer(Modifier.height(8.dp))
    PrimaryText(CreativityStrings.SUBMIT, onSubmit)
    BackText(onExit)
}

@Composable
private fun CompletedContent(
    session: CreativityUiState.Completed,
    onAnother: () -> Unit,
    onExit: () -> Unit,
) {
    val rubric = session.session.rubric
    val fluency = rubric?.signals?.get(CreativityRubricScorer.SIGNAL_FLUENCY) ?: 0.0
    val elaboration = rubric?.signals?.get(CreativityRubricScorer.SIGNAL_ELABORATION) ?: 0.0
    Text(CreativityStrings.RESULT_TITLE, color = AppColors.FocusSignal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    rubric?.let { Text(CreativityStrings.labelText(it.summary), color = Color.White, fontSize = 16.sp) }
    Text(
        "${CreativityStrings.FLUENCY_LABEL}: ${fluency.roundToInt()}",
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
    )
    Text(
        "${CreativityStrings.ELABORATION_LABEL}: ${elaboration.roundToInt()}",
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
    )
    Disclaimer()
    Spacer(Modifier.height(8.dp))
    PrimaryText(CreativityStrings.ANOTHER_ROUND, onAnother)
    BackText(onExit)
}

@Composable
private fun Disclaimer() {
    Text(CreativityStrings.DISCLAIMER, color = AppColors.FocusReadout, fontSize = 11.sp)
}

@Composable
private fun BackText(onBack: () -> Unit) {
    Text(
        CreativityStrings.BACK,
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
        modifier = Modifier.clickable { onBack() }.padding(8.dp),
    )
}

@Composable
private fun PrimaryText(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = AppColors.FocusSignal,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { onClick() }.padding(vertical = 6.dp),
    )
}

@Composable
private fun ExerciseCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

private fun addIdea(ideas: MutableList<String>, draft: () -> String) {
    val value = draft().trim()
    if (value.isNotEmpty()) ideas.add(value)
}
