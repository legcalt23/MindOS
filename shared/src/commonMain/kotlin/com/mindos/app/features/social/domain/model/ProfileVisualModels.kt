package com.mindos.app.features.social.domain.model

import com.mindos.app.core.security.SafeInput
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId

const val ProfileVisualSchemaVersion: Int = 1
const val ProfileVisualLayoutVersion: Int = 1
const val ProfileVideoMaxDurationMillis: Long = 15_000L
const val ProfileCoverMediaMaxBytes: Long = 15L * 1024L * 1024L
const val ProfileImageMaxBytes: Long = 10L * 1024L * 1024L
const val ProfileVideoMaxBytes: Long = 75L * 1024L * 1024L
const val ProfileThumbnailMaxBytes: Long = 1L * 1024L * 1024L
const val ProfileMediaPreviewMaxBytes: Long = 16L * 1024L * 1024L
const val ProfileAudioMaxBytes: Long = 25L * 1024L * 1024L
const val ProfileFileMaxBytes: Long = 25L * 1024L * 1024L
const val ProfileUserphraseMaxLength: Int = 100


enum class IdentityPosition {
    Top,
    Bottom,
}

enum class ProfileCategoryIcon {
    Art,
    Atom,
    Cinema,
}

enum class ProfileColorTarget {
    Primary,
    Secondary,
}

data class ProfileThemeColors(
    val primaryArgb: Long,
    val secondaryArgb: Long,
) {
    companion object {
        val Default = ProfileThemeColors(
            primaryArgb = 0xFF050505,
            secondaryArgb = 0xFF13ECA4,
        )
    }
}

enum class ProfileModuleId(
    val persistedName: String,
    val storageFolder: String,
) {
    Main("main", "main"),
    SmallOne("small_1", "small_1"),
    SmallTwo("small_2", "small_2");

    companion object {
        fun fromPersistedName(value: String?): ProfileModuleId? =
            entries.firstOrNull { it.persistedName == value }
    }
}

enum class ProfileModuleSize {
    Large,
    Small,
}

enum class ProfileModuleContentType {
    Empty,
    Photo,
    Video,
    Text,
}

enum class ProfileLayoutSide {
    Left,
    Right,
}

enum class ProfileLayoutPreset {
    LargeLeft,
    LargeRight,
}

enum class ProfileLayoutSlot(
    val side: ProfileLayoutSide,
    val acceptsLarge: Boolean,
) {
    LargeLeft(ProfileLayoutSide.Left, acceptsLarge = true),
    LargeRight(ProfileLayoutSide.Right, acceptsLarge = true),
    SmallTopLeft(ProfileLayoutSide.Left, acceptsLarge = false),
    SmallBottomLeft(ProfileLayoutSide.Left, acceptsLarge = false),
    SmallTopRight(ProfileLayoutSide.Right, acceptsLarge = false),
    SmallBottomRight(ProfileLayoutSide.Right, acceptsLarge = false);

    val acceptsSmall: Boolean
        get() = !acceptsLarge

    companion object {
        fun fromPersistedName(value: String?): ProfileLayoutSlot? =
            entries.firstOrNull { it.name == value }
    }
}

fun ProfileLayoutPreset.largeSlot(): ProfileLayoutSlot =
    when (this) {
        ProfileLayoutPreset.LargeLeft -> ProfileLayoutSlot.LargeLeft
        ProfileLayoutPreset.LargeRight -> ProfileLayoutSlot.LargeRight
    }

fun ProfileLayoutPreset.smallSlots(): List<ProfileLayoutSlot> =
    when (this) {
        ProfileLayoutPreset.LargeLeft -> listOf(ProfileLayoutSlot.SmallTopRight, ProfileLayoutSlot.SmallBottomRight)
        ProfileLayoutPreset.LargeRight -> listOf(ProfileLayoutSlot.SmallTopLeft, ProfileLayoutSlot.SmallBottomLeft)
    }

fun ProfileLayoutPreset.smallSide(): ProfileLayoutSide =
    when (this) {
        ProfileLayoutPreset.LargeLeft -> ProfileLayoutSide.Right
        ProfileLayoutPreset.LargeRight -> ProfileLayoutSide.Left
    }

fun ProfileLayoutSide.toPresetWithSmallSide(): ProfileLayoutPreset =
    when (this) {
        ProfileLayoutSide.Left -> ProfileLayoutPreset.LargeRight
        ProfileLayoutSide.Right -> ProfileLayoutPreset.LargeLeft
    }

fun ProfileLayoutSlot.toPresetForLargeDrop(): ProfileLayoutPreset =
    when (this.side) {
        ProfileLayoutSide.Left -> ProfileLayoutPreset.LargeLeft
        ProfileLayoutSide.Right -> ProfileLayoutPreset.LargeRight
    }

fun ProfileModuleId.defaultSize(): ProfileModuleSize =
    if (this == ProfileModuleId.Main) ProfileModuleSize.Large else ProfileModuleSize.Small

data class ProfileMediaMeta(
    val url: String,
    val storagePath: String,
    val thumbnailUrl: String? = null,
    val thumbnailStoragePath: String? = null,
    val mimeType: String,
    val durationMillis: Long? = null,
    val uploadedAt: Timestamp,
)

data class ProfileModule(
    val id: ProfileModuleId,
    val size: ProfileModuleSize,
    val slot: ProfileLayoutSlot,
    val contentType: ProfileModuleContentType = ProfileModuleContentType.Empty,
    val text: String? = null,
    val media: ProfileMediaMeta? = null,
) {
    init {
        require((size == ProfileModuleSize.Large) == (id == ProfileModuleId.Main)) {
            "The profile visual system owns exactly one large module: main."
        }
    }

    val canHoldText: Boolean
        get() = size == ProfileModuleSize.Small
}

data class ProfileVisualConfig(
    val userId: UserId,
    val displayName: String,
    val username: String,
    val userphrase: String = "",
    val mainProfilePhotoUrl: String? = null,
    val searchable: Boolean,
    val observersCount: Int,
    val identityPosition: IdentityPosition,
    val colors: ProfileThemeColors,
    val categoryIcons: List<ProfileCategoryIcon>,
    val layoutVersion: Int,
    val layoutPreset: ProfileLayoutPreset,
    val modules: List<ProfileModule>,
    val schemaVersion: Int,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
) {
    fun module(id: ProfileModuleId): ProfileModule =
        modules.first { it.id == id }

    fun withModule(module: ProfileModule): ProfileVisualConfig =
        copy(modules = modules.map { if (it.id == module.id) module else it }.sortedByModuleId())

    fun normalized(): ProfileVisualConfig {
        val currentById = modules.associateBy { it.id }
        val smallSlots = layoutPreset.smallSlots()
        val main = currentById[ProfileModuleId.Main]?.copy(
            size = ProfileModuleSize.Large,
            slot = layoutPreset.largeSlot(),
        ) ?: defaultModule(ProfileModuleId.Main, layoutPreset)
        val orderedSmallModules = listOf(ProfileModuleId.SmallOne, ProfileModuleId.SmallTwo)
            .map { currentById[it] ?: defaultModule(it, layoutPreset) }
            .sortedWith(compareBy<ProfileModule> { it.slot.isBottomSmallSlot() }.thenBy { it.id.ordinal })
            .mapIndexed { index, module -> module.copy(size = ProfileModuleSize.Small, slot = smallSlots[index]) }
        return copy(
            displayName = SafeInput.normalizeSingleLineHumanText(displayName, ProfileDisplayNameMaxLength),
            username = normalizeProfileUsername(username),
            userphrase = SafeInput.normalizeSingleLineHumanText(userphrase, ProfileUserphraseMaxLength),
            mainProfilePhotoUrl = main.media?.storagePath?.takeIf { it.isNotBlank() },
            categoryIcons = normalizedCategoryIcons(categoryIcons),
            modules = (listOf(main) + orderedSmallModules).sortedByModuleId(),
            schemaVersion = schemaVersion.coerceAtLeast(ProfileVisualSchemaVersion),
            layoutVersion = layoutVersion.coerceAtLeast(ProfileVisualLayoutVersion),
        )
    }

    fun moveModuleToSlot(moduleId: ProfileModuleId, desiredSlot: ProfileLayoutSlot): ProfileVisualConfig {
        val current = normalized()
        return if (moduleId == ProfileModuleId.Main) {
            val nextPreset = desiredSlot.toPresetForLargeDrop()
            current.copy(
                layoutPreset = nextPreset,
                modules = current.reassignForPreset(nextPreset),
            ).normalized()
        } else {
            val nextPreset = desiredSlot.side.toPresetWithSmallSide()
            val validSmallSlots = nextPreset.smallSlots()
            val snappedSlot = if (desiredSlot in validSmallSlots) desiredSlot else validSmallSlots.first()
            val otherSmallId = if (moduleId == ProfileModuleId.SmallOne) ProfileModuleId.SmallTwo else ProfileModuleId.SmallOne
            val otherSlot = validSmallSlots.first { it != snappedSlot }
            current.copy(
                layoutPreset = nextPreset,
                modules = current.modules.map { module ->
                    when (module.id) {
                        ProfileModuleId.Main -> module.copy(slot = nextPreset.largeSlot())
                        moduleId -> module.copy(slot = snappedSlot)
                        otherSmallId -> module.copy(slot = otherSlot)
                        else -> module
                    }
                }.sortedByModuleId(),
            ).normalizedPreservingSmallOrder()
        }
    }

    fun clearModule(moduleId: ProfileModuleId): ProfileVisualConfig {
        val module = module(moduleId)
        return withModule(
            module.copy(
                contentType = ProfileModuleContentType.Empty,
                text = null,
                media = null,
            ),
        )
    }

    fun withTextModule(moduleId: ProfileModuleId, text: String): ProfileVisualConfig {
        val module = module(moduleId)
        require(module.canHoldText) { "Only small profile modules can hold text." }
        return withModule(
            module.copy(
                contentType = if (text.isBlank()) ProfileModuleContentType.Empty else ProfileModuleContentType.Text,
                text = SafeInput.normalizeMultilineHumanText(text, ProfileSmallTextMaxLength),
                media = null,
            ),
        )
    }

    fun withPendingMedia(moduleId: ProfileModuleId, kind: ProfileMediaUploadKind): ProfileVisualConfig {
        val module = module(moduleId)
        return withModule(
            module.copy(
                contentType = when (kind) {
                    ProfileMediaUploadKind.Photo -> ProfileModuleContentType.Photo
                    ProfileMediaUploadKind.Video -> ProfileModuleContentType.Video
                    ProfileMediaUploadKind.Audio,
                    ProfileMediaUploadKind.File -> module.contentType
                },
                text = null,
            ),
        )
    }

    fun withSavedMedia(moduleId: ProfileModuleId, media: ProfileMediaMeta): ProfileVisualConfig {
        val module = module(moduleId)
        val contentType = if (media.durationMillis != null || media.mimeType.startsWith("video/")) {
            ProfileModuleContentType.Video
        } else {
            ProfileModuleContentType.Photo
        }
        return withModule(
            module.copy(
                contentType = contentType,
                text = null,
                media = media,
            ),
        )
    }

    private fun reassignForPreset(nextPreset: ProfileLayoutPreset): List<ProfileModule> {
        val smallModules = modules
            .filter { it.size == ProfileModuleSize.Small }
            .sortedWith(compareBy<ProfileModule> { it.slot.isBottomSmallSlot() }.thenBy { it.id.ordinal })
        val smallSlots = nextPreset.smallSlots()
        return modules.map { module ->
            when (module.id) {
                ProfileModuleId.Main -> module.copy(slot = nextPreset.largeSlot())
                smallModules.getOrNull(0)?.id -> module.copy(slot = smallSlots.first())
                smallModules.getOrNull(1)?.id -> module.copy(slot = smallSlots.last())
                else -> module
            }
        }.sortedByModuleId()
    }

    private fun normalizedPreservingSmallOrder(): ProfileVisualConfig {
        val main = module(ProfileModuleId.Main).copy(
            size = ProfileModuleSize.Large,
            slot = layoutPreset.largeSlot(),
        )
        val smallSlots = layoutPreset.smallSlots()
        val smallModules = modules
            .filter { it.id != ProfileModuleId.Main }
            .sortedWith(compareBy<ProfileModule> { it.slot.isBottomSmallSlot() }.thenBy { it.id.ordinal })
            .mapIndexed { index, module -> module.copy(size = ProfileModuleSize.Small, slot = smallSlots[index]) }
        return copy(
            categoryIcons = normalizedCategoryIcons(categoryIcons),
            modules = (listOf(main) + smallModules).sortedByModuleId(),
        )
    }

    companion object {
        fun createDefault(
            userId: UserId,
            displayName: String,
            username: String,
            observersCount: Int,
            now: Timestamp,
            userphrase: String = "",
        ): ProfileVisualConfig {
            val preset = ProfileLayoutPreset.LargeLeft
            val resolvedUsername = safeProfileUsername(userId, username, displayName)
            val resolvedDisplayName = safeProfileDisplayName(userId, displayName, resolvedUsername)
            return ProfileVisualConfig(
                userId = userId,
                displayName = SafeInput.normalizeSingleLineHumanText(resolvedDisplayName, ProfileDisplayNameMaxLength),
                username = resolvedUsername,
                userphrase = SafeInput.normalizeSingleLineHumanText(userphrase, ProfileUserphraseMaxLength),
                searchable = true,
                observersCount = observersCount.coerceAtLeast(0),
                identityPosition = IdentityPosition.Top,
                colors = ProfileThemeColors.Default,
                categoryIcons = DefaultProfileCategoryIcons,
                layoutVersion = ProfileVisualLayoutVersion,
                layoutPreset = preset,
                modules = listOf(
                    defaultModule(ProfileModuleId.Main, preset),
                    defaultModule(ProfileModuleId.SmallOne, preset),
                    defaultModule(ProfileModuleId.SmallTwo, preset),
                ),
                schemaVersion = ProfileVisualSchemaVersion,
                createdAt = now,
                updatedAt = now,
            ).normalized()
        }
    }
}

fun normalizeProfileUsername(raw: String): String =
    SafeInput.normalizeUsername(raw)

fun isValidProfileUsername(raw: String): Boolean {
    val normalized = normalizeProfileUsername(raw)
    return SafeInput.isSafeUsername(normalized)
}

fun safeProfileUsername(
    userId: UserId,
    vararg preferred: String?,
): String {
    preferred.forEach { candidate ->
        candidate
            ?.toProfileUsernameCandidate()
            ?.takeIf(::isValidProfileUsername)
            ?.let { return it }
    }

    userId.value.toProfileUsernameCandidate()
        .takeIf(::isValidProfileUsername)
        ?.let { return it }

    val uidAlias = userId.value
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .take(24)
    val fallback = "user_${uidAlias.ifBlank { "neu" }}".take(30)
    return fallback.takeIf(::isValidProfileUsername) ?: "user_neu"
}

fun safeProfileDisplayName(
    userId: UserId,
    displayName: String,
    username: String,
): String =
    SafeInput.normalizeSingleLineHumanText(displayName, ProfileDisplayNameMaxLength).takeIf { it.isNotBlank() }
        ?: SafeInput.normalizeSingleLineHumanText(username.removePrefix("@"), ProfileDisplayNameMaxLength).takeIf { it.isNotBlank() }
        ?: SafeInput.normalizeSingleLineHumanText(userId.value, ProfileDisplayNameMaxLength)

private fun String.toProfileUsernameCandidate(): String {
    val normalized = normalizeProfileUsername(this)
        .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
        .joinToString("")
        .trim('.', '_')
        .replace(Regex("\\.{2,}"), ".")
        .replace(Regex("_{2,}"), "_")
        .take(30)
    return if (normalized.length in 1..2) {
        "user_$normalized".take(30)
    } else {
        normalized
    }
}

const val ProfileDisplayNameMaxLength: Int = 80

enum class ProfileMediaUploadKind {
    Photo,
    Video,
    Audio,
    File,
}


data class ProfileMediaUpload(
    val kind: ProfileMediaUploadKind,
    val bytes: ByteArray,
    val thumbnailBytes: ByteArray? = null,
    val mimeType: String,
    val originalName: String? = null,
    val durationMillis: Long? = null,
) {
    fun validationError(moduleId: ProfileModuleId? = null): String? {
        val normalizedMimeType = mimeType.normalizedProfileMimeType()
        return when {
            bytes.isEmpty() -> "El archivo seleccionado esta vacio."
            !normalizedMimeType.isAllowedFor(kind) -> "El tipo de archivo no esta permitido."
            bytes.size.toLong() > kind.maxAllowedBytes(moduleId) ->
                kind.maxAllowedBytesMessage(moduleId)
            thumbnailBytes != null && thumbnailBytes.size.toLong() > ProfileThumbnailMaxBytes ->
                "La miniatura del video no puede superar 1 MB."
            kind == ProfileMediaUploadKind.Video && durationMillis == null ->
                "No se pudo validar la duracion del video."
            kind == ProfileMediaUploadKind.Video && durationMillis != null && durationMillis <= 0L ->
                "No se pudo validar la duracion del video."
            kind == ProfileMediaUploadKind.Video && durationMillis != null && durationMillis >= ProfileVideoMaxDurationMillis ->
                "El video debe durar menos de 15 segundos."
            else -> null
        }
    }
}

fun String.normalizedProfileMimeType(): String =
    lowercase().substringBefore(";").trim()

fun String.isAllowedFor(kind: ProfileMediaUploadKind): Boolean {
    val normalized = normalizedProfileMimeType()
    return when (kind) {
        ProfileMediaUploadKind.Photo -> normalized in AllowedProfileImageMimeTypes
        ProfileMediaUploadKind.Video -> normalized in AllowedProfileVideoMimeTypes
        ProfileMediaUploadKind.Audio -> true
        ProfileMediaUploadKind.File -> true
    }
}

fun ProfileMediaUploadKind.maxAllowedBytes(moduleId: ProfileModuleId? = null): Long =
    when {
        moduleId == ProfileModuleId.Main && (this == ProfileMediaUploadKind.Photo || this == ProfileMediaUploadKind.Video) ->
            ProfileCoverMediaMaxBytes
        this == ProfileMediaUploadKind.Photo -> ProfileImageMaxBytes
        this == ProfileMediaUploadKind.Video -> ProfileVideoMaxBytes
        this == ProfileMediaUploadKind.Audio -> ProfileAudioMaxBytes
        else -> ProfileFileMaxBytes
    }

fun ProfileMediaUploadKind.maxAllowedBytesMessage(moduleId: ProfileModuleId? = null): String =
    when {
        moduleId == ProfileModuleId.Main && (this == ProfileMediaUploadKind.Photo || this == ProfileMediaUploadKind.Video) ->
            "La portada no puede superar 15 MB."
        this == ProfileMediaUploadKind.Photo -> "La imagen no puede superar 10 MB."
        this == ProfileMediaUploadKind.Video -> "El video no puede superar 75 MB."
        this == ProfileMediaUploadKind.Audio -> "El audio no puede superar 25 MB."
        else -> "El archivo no puede superar 25 MB."
    }


val AllowedProfileImageMimeTypes: Set<String> =
    setOf("image/jpeg", "image/png", "image/webp")

val AllowedProfileVideoMimeTypes: Set<String> =
    setOf("video/mp4", "video/quicktime")

const val ProfileSmallTextMaxLength: Int = 120

val DefaultProfileCategoryIcons: List<ProfileCategoryIcon> =
    listOf(ProfileCategoryIcon.Art, ProfileCategoryIcon.Atom, ProfileCategoryIcon.Cinema)

private fun defaultModule(
    id: ProfileModuleId,
    preset: ProfileLayoutPreset,
): ProfileModule {
    val slot = when (id) {
        ProfileModuleId.Main -> preset.largeSlot()
        ProfileModuleId.SmallOne -> preset.smallSlots().first()
        ProfileModuleId.SmallTwo -> preset.smallSlots().last()
    }
    return ProfileModule(
        id = id,
        size = id.defaultSize(),
        slot = slot,
    )
}

private fun List<ProfileModule>.sortedByModuleId(): List<ProfileModule> =
    sortedBy { it.id.ordinal }

private fun ProfileLayoutSlot.isBottomSmallSlot(): Boolean =
    this == ProfileLayoutSlot.SmallBottomLeft || this == ProfileLayoutSlot.SmallBottomRight

private fun normalizedCategoryIcons(icons: List<ProfileCategoryIcon>): List<ProfileCategoryIcon> {
    val canonical = icons.distinct().filter { it in DefaultProfileCategoryIcons }
    return (canonical + DefaultProfileCategoryIcons).distinct().take(3)
}
