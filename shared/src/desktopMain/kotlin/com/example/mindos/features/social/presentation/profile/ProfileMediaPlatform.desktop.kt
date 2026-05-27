package com.example.mindos.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModuleId

@Composable
actual fun rememberProfileMediaPicker(
    onPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onError: (String) -> Unit,
): ProfileMediaPicker =
    remember(onError) {
        object : ProfileMediaPicker {
            override fun pick(moduleId: ProfileModuleId, kind: ProfileMediaUploadKind) {
                onError("La seleccion multimedia esta disponible en Android.")
            }
        }
    }

@Composable
actual fun ProfilePlatformMediaPreview(
    source: ProfileMediaRenderSource,
    modifier: Modifier,
    contentScale: ContentScale,
    fallback: @Composable () -> Unit,
) {
    fallback()
}
