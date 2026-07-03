@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e7

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E7 · Compuerta — regla condicional con inversión por puerta (2 vías), adaptativo
 * multi-señal (MVP-2). Sin score, sin claims.
 */
@Composable
fun E7CompuertaScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E7_COMPUERTA,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
