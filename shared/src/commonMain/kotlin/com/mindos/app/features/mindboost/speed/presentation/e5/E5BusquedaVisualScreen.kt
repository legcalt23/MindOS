@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.speed.presentation.e5

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.presentation.ui.SpeedExerciseScreen

/**
 * E5 · Búsqueda Visual — Visual Scan Race (MC9-C). Detección de un objetivo entre distractores bajo carga
 * visual y presión temporal. Reutiliza el bucle de 2 vías, el motor de score preliminar 0–1 y la persistencia;
 * el feedback (detección visual con precisión/estabilidad) se deriva del perfil preliminar de la corrida.
 */
@Composable
fun E5BusquedaVisualScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) = SpeedExerciseScreen(
    exerciseId = SpeedExerciseId.E5_BUSQUEDA,
    graph = graph,
    userId = userId,
    onExit = onExit,
    modifier = modifier,
)
