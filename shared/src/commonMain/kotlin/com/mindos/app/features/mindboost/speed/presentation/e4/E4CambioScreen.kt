@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e4

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E4 · Cambio de Regla — Rule Switch Sprint (MC9-B). Task-switching color/forma: la regla activa alterna durante
 * la corrida y el usuario debe adaptarse rápido. Reutiliza el bucle de 2 vías, el motor de score preliminar y la
 * persistencia; las métricas de cambio (coste/recuperación/precisión post-cambio) se derivan de los trials.
 */
@Composable
fun E4CambioScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E4_CAMBIO,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
