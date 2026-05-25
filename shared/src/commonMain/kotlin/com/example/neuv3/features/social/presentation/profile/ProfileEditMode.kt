package com.example.neuv3.features.social.presentation.profile

import com.example.neuv3.features.social.domain.model.ProfileModuleId

sealed interface ProfileEditTarget {
    data object FotoPortada : ProfileEditTarget
    data object Username : ProfileEditTarget
    data object UserPhrase : ProfileEditTarget
    data object ThemeColors : ProfileEditTarget
    data class Highlight(val moduleId: ProfileModuleId) : ProfileEditTarget
}
