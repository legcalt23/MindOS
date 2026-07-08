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
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.speed.di.rememberSpeedGraph
import com.mindos.app.features.mindboost.speed.di.speedSessionRecovery
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import com.mindos.app.features.mindboost.speed.presentation.e1.E1DestelloScreen
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
    LaunchedEffect(Unit) {
        // MC1c: once per Velocidad track entry, mark orphaned STARTED/CHECKPOINT sessions (crash/kill/
        // nav-away) ABANDONED when stale by updatedAtMs. Idempotent, off the frame path; never touches
        // FINALIZED, scoring, or other blocks. No exercise session is active yet at this mount point.
        speedSessionRecovery(sessionsStore, graph.clock).recover()
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
            E8InhibicionScreen(graph, userId, onExit = { screen = SpeedScreen.Menu }, modifier = modifier)
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
