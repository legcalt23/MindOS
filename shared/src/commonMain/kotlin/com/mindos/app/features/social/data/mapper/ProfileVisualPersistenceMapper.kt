package com.mindos.app.features.social.data.mapper

import com.mindos.app.features.social.domain.model.DefaultProfileCategoryIcons
import com.mindos.app.features.social.domain.model.IdentityPosition
import com.mindos.app.features.social.domain.model.ProfileCategoryIcon
import com.mindos.app.features.social.domain.model.ProfileLayoutPreset
import com.mindos.app.features.social.domain.model.ProfileLayoutSlot
import com.mindos.app.features.social.domain.model.ProfileMediaMeta
import com.mindos.app.features.social.domain.model.ProfileModule
import com.mindos.app.features.social.domain.model.ProfileModuleContentType
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.domain.model.ProfileSmallTextMaxLength
import com.mindos.app.features.social.domain.model.ProfileUserphraseMaxLength
import com.mindos.app.features.social.domain.model.ProfileThemeColors
import com.mindos.app.features.social.domain.model.ProfileVisualConfig
import com.mindos.app.features.social.domain.model.ProfileVisualLayoutVersion
import com.mindos.app.features.social.domain.model.ProfileVisualSchemaVersion
import com.mindos.app.features.social.domain.model.defaultSize
import com.mindos.app.features.social.domain.model.safeProfileDisplayName
import com.mindos.app.features.social.domain.model.safeProfileUsername
import com.mindos.app.features.social.domain.model.largeSlot
import com.mindos.app.features.social.domain.model.smallSlots
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId
import kotlinx.serialization.Serializable

@Serializable
data class ProfileVisualConfigDto(
    val schemaVersion: Int = ProfileVisualSchemaVersion,
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val userphrase: String? = null,
    val mainProfilePhotoUrl: String? = null,
    val searchable: Boolean = true,
    val directoryVisible: Boolean = true,
    val usernameSearchTokens: List<String> = emptyList(),
    val observersCount: Int = 0,
    val identityPosition: String = IdentityPosition.Top.name,
    val primaryColor: Long = ProfileThemeColors.Default.primaryArgb,
    val secondaryColor: Long = ProfileThemeColors.Default.secondaryArgb,
    val categoryIconsConfig: List<String> = emptyList(),
    val layoutVersion: Int = ProfileVisualLayoutVersion,
    val layoutPreset: String = ProfileLayoutPreset.LargeLeft.name,
    val modules: List<ProfileModuleDto> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class ProfileModuleDto(
    val id: String = "",
    val size: String = "",
    val slot: String = "",
    val contentType: String = ProfileModuleContentType.Empty.name,
    val text: String? = null,
    val media: ProfileMediaMetaDto? = null,
)

@Serializable
data class ProfileMediaMetaDto(
    val url: String = "",
    val storagePath: String = "",
    val thumbnailUrl: String? = null,
    val thumbnailStoragePath: String? = null,
    val mimeType: String = "application/octet-stream",
    val durationMillis: Long? = null,
    val uploadedAt: Long = 0L,
)

fun ProfileVisualConfigDto.toDomain(fallbackUserId: UserId? = null): ProfileVisualConfig? {
    val resolvedUserId =
        userId.takeIf { it.isNotBlank() }?.let(::UserId) ?: fallbackUserId ?: return null

    val created = Timestamp(createdAt.coerceAtLeast(0L))
    val preset =
        enumValueOrNull<ProfileLayoutPreset>(layoutPreset) ?: ProfileLayoutPreset.LargeLeft

    val default = ProfileVisualConfig.createDefault(
        userId = resolvedUserId,
        displayName = displayName,
        username = username,
        observersCount = observersCount,
        now = created,
        userphrase = userphrase.orEmpty(),
    )

    val loadedModules = modules.mapNotNull { moduleDto -> moduleDto.toDomain(preset) }
    val resolvedMainPhoto = mainProfilePhotoUrl?.takeIf { it.isNotBlank() }
    val modulesById = (default.modules.associateBy { it.id } + loadedModules.associateBy { it.id })
        .withFallbackMainPhoto(resolvedMainPhoto, created)

    val resolvedUsername = username.takeIf { it.isNotBlank() }
        ?.let { safeProfileUsername(resolvedUserId, it, displayName) }
        ?: default.username
    return default.copy(
        displayName = displayName.takeIf { it.isNotBlank() }
            ?.let { safeProfileDisplayName(resolvedUserId, it, resolvedUsername) }
            ?: default.displayName,
        username = resolvedUsername,
        userphrase = userphrase.orEmpty().trim().take(ProfileUserphraseMaxLength),
        mainProfilePhotoUrl = resolvedMainPhoto,
        searchable = searchable,
        observersCount = observersCount.coerceAtLeast(0),
        identityPosition =
            enumValueOrNull<IdentityPosition>(identityPosition) ?: default.identityPosition,
        colors = ProfileThemeColors(
            primaryArgb = primaryColor,
            secondaryArgb = secondaryColor,
        ),
        categoryIcons = categoryIconsConfig
            .mapNotNull { iconName -> enumValueOrNull<ProfileCategoryIcon>(iconName) }
            .ifEmpty { DefaultProfileCategoryIcons },
        layoutVersion = layoutVersion.coerceAtLeast(ProfileVisualLayoutVersion),
        layoutPreset = preset,
        modules = listOf(
            ProfileModuleId.Main,
            ProfileModuleId.SmallOne,
            ProfileModuleId.SmallTwo,
        ).map { moduleId ->
            modulesById.getValue(moduleId)
        },
        schemaVersion = schemaVersion.coerceAtLeast(ProfileVisualSchemaVersion),
        createdAt = created,
        updatedAt = Timestamp(updatedAt.coerceAtLeast(created.epochMillis)),
    ).normalized()
}

private fun ProfileModuleDto.toDomain(preset: ProfileLayoutPreset): ProfileModule? {
    val moduleId = ProfileModuleId.fromPersistedName(id) ?: return null

    val resolvedSlot = ProfileLayoutSlot.fromPersistedName(slot) ?: when (moduleId) {
        ProfileModuleId.Main -> preset.largeSlot()
        ProfileModuleId.SmallOne -> preset.smallSlots().first()
        ProfileModuleId.SmallTwo -> preset.smallSlots().last()
    }

    val resolvedContentType =
        enumValueOrNull<ProfileModuleContentType>(contentType) ?: ProfileModuleContentType.Empty

    return ProfileModule(
        id = moduleId,
        size = moduleId.defaultSize(),
        slot = resolvedSlot,
        contentType = if (
            moduleId == ProfileModuleId.Main &&
            resolvedContentType == ProfileModuleContentType.Text
        ) {
            ProfileModuleContentType.Empty
        } else {
            resolvedContentType
        },
        text = text?.take(ProfileSmallTextMaxLength),
        media = media?.toDomain(),
    )
}

private fun ProfileMediaMetaDto.toDomain(): ProfileMediaMeta? {
    if (storagePath.isBlank()) return null

    return ProfileMediaMeta(
        url = url.ifBlank { storagePath },
        storagePath = storagePath,
        thumbnailUrl = thumbnailUrl ?: thumbnailStoragePath,
        thumbnailStoragePath = thumbnailStoragePath,
        mimeType = mimeType.ifBlank { "application/octet-stream" },
        durationMillis = durationMillis,
        uploadedAt = Timestamp(uploadedAt.coerceAtLeast(0L)),
    )
}

fun ProfileVisualConfig.toClientCreateFirestoreMap(): Map<String, Any?> =
    normalized().toFullFirestoreMap(observersCountOverride = 0)

fun ProfileVisualConfig.toClientMutableFirestoreUpdateMap(): Map<String, Any?> =
    normalized().let { config ->
        mapOf(
            "displayName" to config.displayName,
            "username" to config.username.toFirestoreUsername(config.userId),
            "userphrase" to config.userphrase.takeIf { it.isNotBlank() },
            "mainProfilePhotoUrl" to config.mainProfilePhotoUrl,
            "identityPosition" to config.identityPosition.name,
            "primaryColor" to config.colors.primaryArgb,
            "secondaryColor" to config.colors.secondaryArgb,
            "categoryIconsConfig" to config.categoryIcons.map { it.name },
            "layoutPreset" to config.layoutPreset.name,
            "modules" to config.modules.map { module -> module.toFirestoreMap() },
            "searchable" to config.searchable,
            "usernameSearchTokens" to emptyList<String>(),
            "updatedAt" to config.updatedAt.epochMillis,
        )
    }

fun ProfileVisualConfig.toClientWritableFirestoreMap(): Map<String, Any?> =
    toClientMutableFirestoreUpdateMap()

private fun ProfileVisualConfig.toFullFirestoreMap(observersCountOverride: Int = observersCount): Map<String, Any?> =
    mapOf(
        "schemaVersion" to schemaVersion,
        "userId" to userId.value,
        "displayName" to displayName,
        "username" to username.toFirestoreUsername(userId),
        "userphrase" to userphrase.takeIf { it.isNotBlank() },
        "mainProfilePhotoUrl" to mainProfilePhotoUrl,
        "searchable" to searchable,
        "usernameSearchTokens" to emptyList<String>(),
        "observersCount" to observersCountOverride.coerceAtLeast(0),
        "identityPosition" to identityPosition.name,
        "primaryColor" to colors.primaryArgb,
        "secondaryColor" to colors.secondaryArgb,
        "categoryIconsConfig" to categoryIcons.map { it.name },
        "layoutVersion" to layoutVersion,
        "layoutPreset" to layoutPreset.name,
        "modules" to modules.map { module -> module.toFirestoreMap() },
        "createdAt" to createdAt.epochMillis,
        "updatedAt" to updatedAt.epochMillis,
    )

fun firestoreMapToProfileVisualConfig(
    raw: Map<String, Any?>,
    fallbackUserId: UserId? = null,
): ProfileVisualConfig? {
    val resolvedUserId =
        raw.string("userId")?.takeIf { it.isNotBlank() }?.let(::UserId) ?: fallbackUserId ?: return null

    val createdAt = Timestamp(raw.long("createdAt") ?: 0L)
    val updatedAt = Timestamp(raw.long("updatedAt") ?: createdAt.epochMillis)
    val preset = raw.enumValue<ProfileLayoutPreset>("layoutPreset") ?: ProfileLayoutPreset.LargeLeft

    val default = ProfileVisualConfig.createDefault(
        userId = resolvedUserId,
        displayName = raw.string("displayName").orEmpty(),
        username = raw.string("username").orEmpty(),
        observersCount = raw.int("observersCount") ?: 0,
        now = createdAt,
        userphrase = raw.string("userphrase").orEmpty(),
    )

    val loadedModules = raw.mapList("modules").mapNotNull { moduleMap -> moduleMap.toProfileModule(preset) }
    val resolvedMainPhoto = raw.string("mainProfilePhotoUrl")?.takeIf { it.isNotBlank() }
    val modulesById = (default.modules.associateBy { it.id } + loadedModules.associateBy { it.id })
        .withFallbackMainPhoto(resolvedMainPhoto, createdAt)

    val resolvedUsername = raw.string("username")
        ?.takeIf { it.isNotBlank() }
        ?.let { safeProfileUsername(resolvedUserId, it, raw.string("displayName")) }
        ?: default.username
    return default.copy(
        displayName = raw.string("displayName")
            ?.takeIf { it.isNotBlank() }
            ?.let { safeProfileDisplayName(resolvedUserId, it, resolvedUsername) }
            ?: default.displayName,
        username = resolvedUsername,
        userphrase = raw.string("userphrase").orEmpty().trim().take(ProfileUserphraseMaxLength),
        mainProfilePhotoUrl = resolvedMainPhoto,
        searchable = raw.bool("searchable") ?: default.searchable,
        observersCount = raw.int("observersCount")?.coerceAtLeast(0) ?: default.observersCount,
        identityPosition = raw.enumValue<IdentityPosition>("identityPosition") ?: default.identityPosition,
        colors = ProfileThemeColors(
            primaryArgb = raw.long("primaryColor") ?: default.colors.primaryArgb,
            secondaryArgb = raw.long("secondaryColor") ?: default.colors.secondaryArgb,
        ),
        categoryIcons = raw.stringList("categoryIconsConfig")
            .mapNotNull { iconName -> enumValueOrNull<ProfileCategoryIcon>(iconName) }
            .ifEmpty { DefaultProfileCategoryIcons },
        layoutVersion = raw.int("layoutVersion")?.coerceAtLeast(ProfileVisualLayoutVersion)
            ?: ProfileVisualLayoutVersion,
        layoutPreset = preset,
        modules = listOf(
            ProfileModuleId.Main,
            ProfileModuleId.SmallOne,
            ProfileModuleId.SmallTwo,
        ).map { moduleId ->
            modulesById.getValue(moduleId)
        },
        schemaVersion = raw.int("schemaVersion")?.coerceAtLeast(ProfileVisualSchemaVersion)
            ?: ProfileVisualSchemaVersion,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ).normalized()
}

private fun ProfileModule.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "id" to id.persistedName,
        "size" to size.name,
        "slot" to slot.name,
        "contentType" to contentType.name,
        "text" to text,
        "media" to media?.toFirestoreMap(),
    )

private fun Map<String, Any?>.toProfileModule(preset: ProfileLayoutPreset): ProfileModule? {
    val resolvedId = ProfileModuleId.fromPersistedName(string("id")) ?: return null

    val resolvedSlot = ProfileLayoutSlot.fromPersistedName(string("slot")) ?: when (resolvedId) {
        ProfileModuleId.Main -> preset.largeSlot()
        ProfileModuleId.SmallOne -> preset.smallSlots().first()
        ProfileModuleId.SmallTwo -> preset.smallSlots().last()
    }

    val requestedType =
        enumValueOrNull<ProfileModuleContentType>(string("contentType"))
            ?: ProfileModuleContentType.Empty

    val resolvedContentType = if (
        resolvedId == ProfileModuleId.Main &&
        requestedType == ProfileModuleContentType.Text
    ) {
        ProfileModuleContentType.Empty
    } else {
        requestedType
    }

    return ProfileModule(
        id = resolvedId,
        size = resolvedId.defaultSize(),
        slot = resolvedSlot,
        contentType = resolvedContentType,
        text = string("text")?.take(ProfileSmallTextMaxLength),
        media = map("media")?.toProfileMediaMeta(),
    )
}

private fun ProfileMediaMeta.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "url" to url.ifBlank { storagePath },
        "storagePath" to storagePath,
        "thumbnailUrl" to (thumbnailUrl ?: thumbnailStoragePath),
        "thumbnailStoragePath" to thumbnailStoragePath,
        "mimeType" to mimeType,
        "durationMillis" to durationMillis,
        "uploadedAt" to uploadedAt.epochMillis,
    )

private fun String.toFirestoreUsername(userId: UserId): String {
    val normalized = trim().removePrefix("@").lowercase()
    return normalized.takeIf { it.isValidFirestoreUsername() } ?: safeProfileUsername(userId, normalized)
}

private val ProfileVisualUsernamePattern = Regex("^[a-z0-9._]{3,30}$")

private fun String.isValidFirestoreUsername(): Boolean =
    ProfileVisualUsernamePattern.matches(this) &&
        !startsWith(".") &&
        !endsWith(".") &&
        !contains("..")

private fun Map<String, Any?>.toProfileMediaMeta(): ProfileMediaMeta? {
    val storagePath = string("storagePath")?.takeIf { it.isNotBlank() } ?: return null
    val mimeType = string("mimeType")?.takeIf { it.isNotBlank() } ?: "application/octet-stream"

    return ProfileMediaMeta(
        url = string("url")?.takeIf { it.isNotBlank() } ?: storagePath,
        storagePath = storagePath,
        thumbnailUrl = string("thumbnailUrl") ?: string("thumbnailStoragePath"),
        thumbnailStoragePath = string("thumbnailStoragePath"),
        mimeType = mimeType,
        durationMillis = long("durationMillis"),
        uploadedAt = Timestamp(long("uploadedAt") ?: 0L),
    )
}

private fun Map<ProfileModuleId, ProfileModule>.withFallbackMainPhoto(
    mainProfilePhotoUrl: String?,
    createdAt: Timestamp,
): Map<ProfileModuleId, ProfileModule> {
    if (mainProfilePhotoUrl.isNullOrBlank()) return this
    val main = this[ProfileModuleId.Main] ?: return this
    if (main.media != null) return this
    return this + (
        ProfileModuleId.Main to main.copy(
            contentType = ProfileModuleContentType.Photo,
            media = ProfileMediaMeta(
                url = mainProfilePhotoUrl,
                storagePath = mainProfilePhotoUrl,
                mimeType = "image/jpeg",
                uploadedAt = createdAt,
            ),
        )
    )
}

private fun Map<String, Any?>.string(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.int(key: String): Int? =
    long(key)?.toInt()

private fun Map<String, Any?>.long(key: String): Long? =
    when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

private fun Map<String, Any?>.bool(key: String): Boolean? =
    this[key] as? Boolean

private fun Map<String, Any?>.map(key: String): Map<String, Any?>? =
    (this[key] as? Map<*, *>)?.toStringAnyMap()

private fun Map<String, Any?>.mapList(key: String): List<Map<String, Any?>> =
    (this[key] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.toStringAnyMap()
    }.orEmpty()

private fun Map<String, Any?>.stringList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { item -> item as? String }.orEmpty()

private inline fun <reified T : Enum<T>> Map<String, Any?>.enumValue(key: String): T? =
    enumValueOrNull(string(key))

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
    value?.let { raw ->
        enumValues<T>().firstOrNull { enumConstant ->
            enumConstant.name.equals(raw, ignoreCase = true)
        }
    }

private fun Map<*, *>.toStringAnyMap(): Map<String, Any?> =
    entries.mapNotNull { (key, value) ->
        (key as? String)?.let { safeKey -> safeKey to value }
    }.toMap()
