@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e6

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E6 · Multivía — elección libre de 2 alternativas, dificultad fija (MVP-0). Una señal
 * indica el lado correcto; se mide el TR y la corrección de la elección. Sin adaptación,
 * sin score, sin claims.
 */
@Composable
fun E6MultiviaScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E6_MULTIVIA,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
