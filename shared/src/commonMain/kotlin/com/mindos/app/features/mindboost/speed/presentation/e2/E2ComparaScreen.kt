@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e2

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E2 · Comparación — Number Pulse Compare (MC9-A). Comparar dos magnitudes y tocar el lado del mayor bajo
 * presión temporal, adaptativo por deadline (staircase). Reutiliza el bucle de 2 vías, el motor de score
 * preliminar y la persistencia; sin infraestructura nueva.
 */
@Composable
fun E2ComparaScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E2_COMPARA,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
