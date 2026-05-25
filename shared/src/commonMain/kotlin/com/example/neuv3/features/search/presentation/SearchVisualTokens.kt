package com.example.neuv3.features.search.presentation

import androidx.compose.ui.graphics.Color
import com.example.neuv3.features.search.domain.UsernameSearchMinQueryLength
import kotlin.math.PI

internal val NeuBlack = Color(0xFF020504)
internal val NeuWhite = Color(0xFFF4FFF9)
internal val NeuGreen = Color(0xFF13ECA4)
internal val NeuMist = Color(0xFFA7CFC1)
internal val TargetGold = Color(0xFFE9FF9B)

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
