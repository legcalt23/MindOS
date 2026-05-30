package com.example.mindos.features.search.presentation

import com.example.mindos.design.AppColors
import com.example.mindos.features.search.domain.UsernameSearchMinQueryLength
import kotlin.math.PI

internal val NeuBlack = AppColors.DeepBlack
internal val NeuWhite = AppColors.OnBackground
internal val NeuGreen = AppColors.NeuGreen
internal val NeuMist = AppColors.Mist
internal val TargetGold = AppColors.TargetGold

internal fun radarStatusText(state: UsernameSearchUiState): String =
    when (state.status) {
        UsernameSearchStatus.Latent -> "Escaneo latente"
        UsernameSearchStatus.Scanning -> "Barrido activo"
        UsernameSearchStatus.Ready -> "${state.results.size} contactos detectados"
        UsernameSearchStatus.Empty -> "Sin contactos"
        is UsernameSearchStatus.Failed -> "Canal inestable"
    }

internal fun inputHelper(state: UsernameSearchUiState): String =
    if (state.normalizedQuery.length < UsernameSearchMinQueryLength) {
        "Introduce al menos $UsernameSearchMinQueryLength caracteres. El radar no revela ruido."
    } else {
        "Busqueda por username activa con debounce y lectura controlada."
    }

internal fun Float.toRadians(): Float =
    (this / 180f * PI).toFloat()
