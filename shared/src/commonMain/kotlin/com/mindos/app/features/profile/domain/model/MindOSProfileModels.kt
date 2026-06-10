package com.mindos.app.features.profile.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain models for the MindOS premium profile screen (Perfil.html design).
 *
 * These are intentionally decoupled from the older `social` profile system
 * ([com.mindos.app.features.social.domain.model.ProfileVisualConfig]) so the
 * new screen can evolve independently. They model exactly the data the Perfil
 * design needs: a cover photo, a circular avatar, presence, an emoji username
 * prefix, an observer ("observador") count, an optional 10s audio clip with a
 * real amplitude waveform, a donation progress value, and three custom-link
 * slots.
 *
 * Image / audio binary payloads are referenced two ways:
 *  - [reference]: an opaque backend pointer (Storage path / URL) — null until a
 *    real upload backend is wired (see InMemory* repositories).
 *  - [localBytes]: bytes from the most recent in-session pick/recording so the
 *    UI can render the new media immediately, before any upload completes.
 */

/** Which editable image slot an image pick targets. */
enum class ProfileImageTarget {
    /** The large header/background image (`foto_portada`). */
    CoverPhoto,

    /** The circular centered avatar (`foto_perfil`). */
    ProfilePhoto,
}

/**
 * A reference to a stored image plus the freshly-picked bytes (if any).
 *
 * [localBytes] equality is by reference on purpose — callers bump
 * [MindOSProfile.revision] whenever they replace media so the UI recomposes
 * deterministically without relying on `ByteArray` content comparison.
 */
@Immutable
class ProfileImageRef(
    val reference: String? = null,
    val localBytes: ByteArray? = null,
) {
    val isEmpty: Boolean get() = reference.isNullOrBlank() && (localBytes == null || localBytes.isEmpty())

    companion object {
        val Empty = ProfileImageRef()
    }
}

/** Presence is derived from whether the user is actively in the app foreground. */
enum class PresenceState {
    Online,
    Offline,
    ;

    val isOnline: Boolean get() = this == Online

    companion object {
        fun fromForeground(isForeground: Boolean): PresenceState =
            if (isForeground) Online else Offline
    }
}

/** A small interest tag shown under the username (e.g. Arte, Ciencia). */
@Immutable
data class ProfileTag(
    val id: String,
    /** Localized Spanish label rendered in the UI. */
    val label: String,
)

/**
 * A saved profile audio clip with a waveform derived from real amplitude
 * samples captured during recording (never random — see [AudioWaveform]).
 *
 * @param waveform normalized amplitudes in `0f..1f`, one value per rendered bar.
 */
@Immutable
class ProfileAudioClip(
    val reference: String? = null,
    val localBytes: ByteArray? = null,
    val mimeType: String = "audio/mp4",
    val durationMillis: Long = 0L,
    val waveform: List<Float> = emptyList(),
) {
    val hasPlayableSource: Boolean
        get() = !reference.isNullOrBlank() || (localBytes != null && localBytes.isNotEmpty())
}

/** Donation progress toward a charitable target. Money is not actually moved. */
@Immutable
data class DonationProgress(
    val currentAmount: Long = DEFAULT_CURRENT,
    val targetAmount: Long = DEFAULT_TARGET,
    val currency: String = DEFAULT_CURRENCY,
) {
    /** Fraction filled, clamped to `0f..1f`. */
    val fraction: Float
        get() = if (targetAmount <= 0L) 0f else (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)

    companion object {
        const val DEFAULT_CURRENT = 0L
        const val DEFAULT_TARGET = 10_000L
        const val DEFAULT_CURRENCY = "EUR"
    }
}

/**
 * One of exactly three custom external-link slots shown to the right of the
 * audio/donation column. An empty slot renders a "+" only on the owner's
 * profile; on other users' profiles empty slots are hidden entirely.
 */
@Immutable
class ProfileCustomLink(
    val slotIndex: Int,
    val url: String? = null,
    val iconReference: String? = null,
    val iconLocalBytes: ByteArray? = null,
) {
    val isEmpty: Boolean
        get() = url.isNullOrBlank()

    companion object {
        const val SlotCount = 3

        fun emptySlots(): List<ProfileCustomLink> =
            List(SlotCount) { ProfileCustomLink(slotIndex = it) }
    }
}

/**
 * The complete profile aggregate the screen renders.
 *
 * @param revision bumped on every mutation so Compose sees a new value even
 * when only opaque `ByteArray` media changed (whose content equality we avoid).
 */
@Immutable
data class MindOSProfile(
    val userId: String,
    val username: String,
    val usernamePrefixEmoji: String? = null,
    val fotoPerfil: ProfileImageRef = ProfileImageRef.Empty,
    val fotoPortada: ProfileImageRef = ProfileImageRef.Empty,
    val presence: PresenceState = PresenceState.Offline,
    val observerCount: Int = 0,
    val isObservedByViewer: Boolean = false,
    val tags: List<ProfileTag> = DefaultTags,
    val audio: ProfileAudioClip? = null,
    val donation: DonationProgress = DonationProgress(),
    val customLinks: List<ProfileCustomLink> = ProfileCustomLink.emptySlots(),
    val revision: Int = 0,
) {
    /** The prefix shown before the username: chosen emoji, or "@" by default. */
    val usernamePrefix: String
        get() = usernamePrefixEmoji?.takeIf { it.isNotBlank() } ?: DEFAULT_PREFIX

    /** Custom links that actually have a URL (used when rendering other profiles). */
    val savedCustomLinks: List<ProfileCustomLink>
        get() = customLinks.filterNot { it.isEmpty }

    fun bumped(): MindOSProfile = copy(revision = revision + 1)

    companion object {
        const val DEFAULT_PREFIX = "@"

        val DefaultTags: List<ProfileTag> = listOf(
            ProfileTag(id = "arte", label = "Arte"),
            ProfileTag(id = "ciencia", label = "Ciencia"),
        )
    }
}
