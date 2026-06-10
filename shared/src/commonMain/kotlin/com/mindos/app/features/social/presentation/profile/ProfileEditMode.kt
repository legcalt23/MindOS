package com.mindos.app.features.social.presentation.profile

import com.mindos.app.features.social.domain.model.ProfileModuleId

sealed interface ProfileEditTarget {
    data object FotoPortada : ProfileEditTarget
    data object Username : ProfileEditTarget
    data object UserPhrase : ProfileEditTarget
    data object ThemeColors : ProfileEditTarget
    data class Highlight(val moduleId: ProfileModuleId) : ProfileEditTarget
}
