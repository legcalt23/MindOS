@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.presentation.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.ui.FocusRootScreen
import com.mindos.app.features.mindboost.memory.presentation.ui.MemoryRootScreen

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

    when (track) {
        is MindBoostTrack.Hub -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            HubContent(
                onEnfoque = { track = MindBoostTrack.Enfoque },
                onMemoria = { track = MindBoostTrack.Memoria },
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
    }
}

@Composable
private fun HubContent(
    onEnfoque: () -> Unit,
    onMemoria: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("MindBoost", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Elige tu entrenamiento", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
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

@Composable
private fun TrackCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = AppColors.FocusSignal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

private sealed interface MindBoostTrack {
    data object Hub : MindBoostTrack
    data object Enfoque : MindBoostTrack
    data object Memoria : MindBoostTrack
}
