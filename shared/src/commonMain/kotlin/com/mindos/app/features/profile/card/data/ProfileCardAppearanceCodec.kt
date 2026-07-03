@file:Suppress("TooManyFunctions") // Serialization mapper file: many small domain↔DTO mappers.

package com.mindos.app.features.profile.card.data

import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.FlagOrTeam
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.domain.ProfileCanvasItem
import com.mindos.app.features.profile.card.domain.ProfileCanvasItemType
import com.mindos.app.features.profile.card.domain.ProfileCardAppearance
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileRank
import com.mindos.app.features.profile.card.domain.RankDivision
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializes [ProfileCardAppearance] to/from a JSON string. The string is the only
 * payload stored in the `profile_cards/{uid}` document, which keeps the Firestore
 * security rule trivial (owner-only + size cap) instead of validating nested maps.
 */
object ProfileCardAppearanceCodec {

    const val SCHEMA_VERSION: Int = 1

    /** Hard cap mirrored by the Firestore rule; oversized payloads are rejected. */
    const val MAX_PAYLOAD_CHARS: Int = 8_000

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(appearance: ProfileCardAppearance): String =
        json.encodeToString(ProfileCardAppearanceDto.serializer(), appearance.toDto())

    /** Decodes a stored payload; returns null on any malformed/incompatible content. */
    fun decode(payload: String?): ProfileCardAppearance? {
        if (payload.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(ProfileCardAppearanceDto.serializer(), payload) }
            .getOrNull()
            ?.toDomain()
    }
}

@Serializable
data class ProfileCardAppearanceDto(
    val schemaVersion: Int = ProfileCardAppearanceCodec.SCHEMA_VERSION,
    val accentArgb: Long,
    val screen: PaintDto,
    val card: PaintDto,
    val pegatinaIndex: Int,
    val insigniaOn: Boolean,
    val flagOrTeam: String,
    val level: Int,
    val rank: String,
    val division: String,
)

@Serializable
data class PaintDto(
    val type: String,
    val argb: Long? = null,
    val stops: List<Long> = emptyList(),
    val gradientKind: String? = null,
    val imageRef: String? = null,
)

private fun ProfileCardAppearance.toDto(): ProfileCardAppearanceDto = ProfileCardAppearanceDto(
    accentArgb = accentArgb,
    screen = screenBackground.toDto(),
    card = cardBackground.toDto(),
    pegatinaIndex = pegatinaIndex,
    insigniaOn = insigniaOn,
    flagOrTeam = flagOrTeam.name,
    level = level,
    rank = rank.name,
    division = division.name,
)

private fun ProfileCardAppearanceDto.toDomain(): ProfileCardAppearance = ProfileCardAppearance(
    accentArgb = accentArgb,
    screenBackground = screen.toDomain(),
    cardBackground = card.toDomain(),
    pegatinaIndex = pegatinaIndex.coerceAtLeast(0),
    insigniaOn = insigniaOn,
    flagOrTeam = enumOrNull<FlagOrTeam>(flagOrTeam) ?: FlagOrTeam.FLAG,
    level = level,
    rank = enumOrNull<ProfileRank>(rank) ?: ProfileRank.DIAMANTE,
    division = enumOrNull<RankDivision>(division) ?: RankDivision.III,
)

private fun BackgroundPaint.toDto(): PaintDto = when (this) {
    is BackgroundPaint.Solid -> PaintDto(type = "SOLID", argb = argb)
    is BackgroundPaint.Gradient -> PaintDto(type = "GRADIENT", stops = stops, gradientKind = kind.name)
    is BackgroundPaint.Image -> PaintDto(type = "IMAGE", imageRef = ref)
}

private fun PaintDto.toDomain(): BackgroundPaint = when (type) {
    "SOLID" -> BackgroundPaint.Solid(argb ?: DEFAULT_SOLID)
    "GRADIENT" -> if (stops.isEmpty()) {
        BackgroundPaint.Solid(DEFAULT_SOLID)
    } else {
        BackgroundPaint.Gradient(stops, enumOrNull<GradientKind>(gradientKind) ?: GradientKind.LINEAR_DIAGONAL)
    }
    "IMAGE" -> imageRef?.takeIf { it.isNotBlank() }?.let { BackgroundPaint.Image(it) }
        ?: BackgroundPaint.Solid(DEFAULT_SOLID)
    else -> BackgroundPaint.Solid(DEFAULT_SOLID)
}

private const val DEFAULT_SOLID: Long = 0xFF0E1426

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

// ============================================================================
// Structured profile-card document (schemaVersion 2): appearance + background
// media + canvas. Serialized to the Firestore `payload` string (metadata only,
// NO bytes). schemaVersion 1 is the legacy appearance-only blob (migrated on load).
// ============================================================================

object ProfileCardDocCodec {
    const val SCHEMA_VERSION: Int = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(doc: ProfileCardDoc): String =
        json.encodeToString(ProfileCardDocDto.serializer(), doc.toDto())

    /** Decodes by envelope [schemaVersion]: <=1 = legacy appearance blob; 2+ = structured doc. */
    fun decode(schemaVersion: Int, payload: String?): ProfileCardDoc? {
        if (payload.isNullOrBlank()) return null
        return if (schemaVersion <= 1) {
            ProfileCardAppearanceCodec.decode(payload)?.let { ProfileCardDoc(appearance = it) }
        } else {
            runCatching { json.decodeFromString(ProfileCardDocDto.serializer(), payload) }
                .getOrNull()
                ?.toDomain()
        }
    }
}

@Serializable
data class ProfileCardDocDto(
    val appearance: ProfileCardAppearanceDto,
    val backgroundMedia: ProfileCardMediaDto? = null,
    val canvas: List<ProfileCanvasItemDto> = emptyList(),
)

@Serializable
data class ProfileCardMediaDto(
    val id: String,
    val kind: String,
    val storagePath: String,
    val downloadUrl: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
    val createdAtMillis: Long = 0L,
)

@Serializable
data class ProfileCanvasItemDto(
    val id: Long,
    val type: String,
    val media: ProfileCardMediaDto? = null,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val z: Int,
    val locked: Boolean = false,
)

private fun ProfileCardDoc.toDto(): ProfileCardDocDto = ProfileCardDocDto(
    appearance = appearance.toDto(),
    backgroundMedia = backgroundMedia?.toMediaDto(),
    canvas = canvas.map { it.toDto() },
)

private fun ProfileCardDocDto.toDomain(): ProfileCardDoc = ProfileCardDoc(
    appearance = appearance.toDomain(),
    backgroundMedia = backgroundMedia?.toDomain(),
    canvas = canvas.map { it.toDomain() },
)

private fun ProfileCardMedia.toMediaDto(): ProfileCardMediaDto = ProfileCardMediaDto(
    id = id,
    kind = kind.name,
    storagePath = storagePath,
    downloadUrl = downloadUrl,
    mimeType = mimeType,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    createdAtMillis = createdAtMillis,
)

private fun ProfileCardMediaDto.toDomain(): ProfileCardMedia = ProfileCardMedia(
    id = id,
    kind = enumOrNull<ProfileCardMediaKind>(kind) ?: ProfileCardMediaKind.IMAGE,
    storagePath = storagePath,
    downloadUrl = downloadUrl,
    mimeType = mimeType,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    createdAtMillis = createdAtMillis,
)

private fun ProfileCanvasItem.toDto(): ProfileCanvasItemDto = ProfileCanvasItemDto(
    id = id,
    type = type.name,
    media = media?.toMediaDto(),
    x = x,
    y = y,
    width = width,
    height = height,
    rotation = rotation,
    z = z,
    locked = locked,
)

private fun ProfileCanvasItemDto.toDomain(): ProfileCanvasItem = ProfileCanvasItem(
    id = id,
    type = enumOrNull<ProfileCanvasItemType>(type) ?: ProfileCanvasItemType.MEDIA,
    media = media?.toDomain(),
    x = x,
    y = y,
    width = width,
    height = height,
    rotation = rotation,
    z = z,
    locked = locked,
)
