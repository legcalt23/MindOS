@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E3 · Cifrado — verificación de código símbolo↔valor (2 vías), adaptativo multi-señal
 * (MVP-2). Sin score, sin claims.
 */
@Composable
fun E3CifradoScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E3_CIFRADO,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
