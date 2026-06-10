package com.mindos.app.features.social.presentation.profile

import androidx.compose.ui.graphics.Color
import com.mindos.app.features.social.domain.model.ProfileModule
import com.mindos.app.features.social.domain.model.ProfileModuleId

internal fun Long.toProfileColor(): Color = Color(this)

internal fun argbFromRgb(red: Int, green: Int, blue: Int): Long =
    (0xFFL shl 24) or (red.coerceIn(0, 255).toLong() shl 16) or
        (green.coerceIn(0, 255).toLong() shl 8) or blue.coerceIn(0, 255).toLong()

internal fun ProfileModule.profileLabel(): String =
    when (id) {
        ProfileModuleId.Main -> "Foto principal"
        ProfileModuleId.SmallOne -> "Bloque 1"
        ProfileModuleId.SmallTwo -> "Bloque 2"
    }

internal val predefinedProfileColors = listOf(
    0xFF000000,
    0xFFFFFFFF,
    0xFF6B7280,
    0xFFE11D48,
    0xFF2563EB,
    0xFF10B981,
    0xFFFACC15,
    0xFF7C3AED,
    0xFFEC4899,
    0xFFF97316,
    0xFF06B6D4,
    0xFF14F195,
    0xFFFFF7ED,
    0xFF111827,
)
