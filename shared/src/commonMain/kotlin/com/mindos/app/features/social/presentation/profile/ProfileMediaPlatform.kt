package com.mindos.app.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.mindos.app.features.social.domain.model.ProfileMediaUpload
import com.mindos.app.features.social.domain.model.ProfileMediaUploadKind
import com.mindos.app.features.social.domain.model.ProfileModuleId

interface ProfileMediaPicker {
    fun pick(moduleId: ProfileModuleId, kind: ProfileMediaUploadKind)
}

data class ProfileMediaRenderSource(
    val storagePath: String?,
    val thumbnailStoragePath: String?,
    val pendingPreviewBytes: ByteArray?,
    val isVideo: Boolean,
    val contentDescription: String,
    val directUrl: String? = null,
    val thumbnailUrl: String? = null,
)

@Composable
expect fun rememberProfileMediaPicker(
    onPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onError: (String) -> Unit,
): ProfileMediaPicker

@Composable
expect fun ProfilePlatformMediaPreview(
    source: ProfileMediaRenderSource,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
)
