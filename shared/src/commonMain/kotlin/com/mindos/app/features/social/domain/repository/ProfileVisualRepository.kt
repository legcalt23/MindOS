package com.mindos.app.features.social.domain.repository

import com.mindos.app.features.social.domain.model.ProfileMediaMeta
import com.mindos.app.features.social.domain.model.ProfileMediaUpload
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.domain.model.ProfileVisualConfig
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId

interface ProfileVisualRepository {
    fun currentAuthenticatedUserId(): UserId?
    suspend fun getConfig(userId: UserId): ProfileVisualConfig?
    suspend fun saveConfig(config: ProfileVisualConfig, attemptId: String? = null)
    suspend fun uploadModuleMedia(
        userId: UserId,
        moduleId: ProfileModuleId,
        upload: ProfileMediaUpload,
        uploadedAt: Timestamp,
        attemptId: String? = null,
    ): ProfileMediaMeta
    suspend fun deleteMedia(userId: UserId, storagePath: String, attemptId: String? = null)
}
