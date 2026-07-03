@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e1

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E1 · Destello — tiempo de reacción simple, dificultad fija (MVP-0). Aparece un destello
 * y se mide el TR del primer toque. Sin score ni claims.
 */
@Composable
fun E1DestelloScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E1_DESTELLO,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
