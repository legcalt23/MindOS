@file:Suppress("MagicNumber", "TooManyFunctions", "MaxLineLength") // Seed data; one mutator per card interaction; dense IO flow.

package com.mindos.app.features.profile.card.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.CanvasItem
import com.mindos.app.features.profile.card.domain.CanvasItemKind
import com.mindos.app.features.profile.card.domain.DefaultProfileContent
import com.mindos.app.features.profile.card.domain.FlagOrTeam
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardMediaScope
import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.ProfileCanvasItem
import com.mindos.app.features.profile.card.domain.ProfileCanvasItemType
import com.mindos.app.features.profile.card.domain.ProfilePanel
import com.mindos.app.features.profile.card.domain.ReputationVote
import com.mindos.app.features.profile.card.domain.appearance
import com.mindos.app.features.profile.card.domain.repository.ProfileCardMediaRepository
import com.mindos.app.features.profile.card.domain.repository.ProfileCardRepository
import com.mindos.app.features.profile.card.domain.withAppearance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Initial-load lifecycle for the card (restore from Firestore). */
enum class ProfileCardLoadStatus { LoadingRemoteProfile, Loaded }

/**
 * Explicit background-media lifecycle (no ambiguous flags). Maps the product states:
 * PickingMedia/PreviewingLocalMedia → the local bytes are already shown while [Uploading];
 * UploadingMedia → [Uploading]; SavingProfile → [Saving]; SaveSuccess → [Saved];
 * UploadError → [UploadFailed]; SaveError → [SaveFailed]; OfflinePending → a failed status
 * with retry (offline is a recoverable failure). The local preview is NEVER dropped on failure.
 */
sealed interface ProfileCardMediaStatus {
    data object Idle : ProfileCardMediaStatus
    data object Uploading : ProfileCardMediaStatus
    data object Saving : ProfileCardMediaStatus
    data object Saved : ProfileCardMediaStatus
    data class UploadFailed(val message: String) : ProfileCardMediaStatus
    data class SaveFailed(val message: String) : ProfileCardMediaStatus
}

/** Session-only picked/restored media: local bytes (preview) and/or a remote Storage ref. */
class CardMedia(
    val bytes: ByteArray? = null,
    val storagePath: String? = null,
    val downloadUrl: String? = null,
    val isVideo: Boolean = false,
)

private data class PendingBackground(
    val ref: String,
    val bytes: ByteArray,
    val mimeType: String,
    val isVideo: Boolean,
    val previous: ProfileCardMedia?,
    val uploaded: ProfileCardMedia?,
)

private data class CanvasPending(
    val ref: String,
    val bytes: ByteArray,
    val mimeType: String,
    val isVideo: Boolean,
)

/**
 * Holds and mutates [ProfileCardState]. Appearance changes auto-save (debounced) and
 * auto-publish (private + public). Background media is uploaded to Storage, referenced
 * from Firestore (metadata only), and restored on next launch.
 */
class ProfileCardStateHolder(
    initial: ProfileCardState,
    private val userId: String? = null,
    private val repository: ProfileCardRepository? = null,
    private val mediaRepository: ProfileCardMediaRepository? = null,
    private val scope: CoroutineScope? = null,
) {

    var state by mutableStateOf(initial)
        private set

    var loadStatus by mutableStateOf(ProfileCardLoadStatus.Loaded)
        private set

    var backgroundStatus by mutableStateOf<ProfileCardMediaStatus>(ProfileCardMediaStatus.Idle)
        private set

    /** ref → media (local bytes and/or remote source). UI renders from this. */
    val media = mutableStateMapOf<String, CardMedia>()

    private var nextCanvasId: Long = (initial.canvasItems.maxOfOrNull { it.id } ?: 0L) + 1
    private var nextMediaId = 0
    private var saveJob: Job? = null
    private var backgroundJob: Job? = null

    /** The durable (uploaded + committed) background media, or null for a solid/gradient. */
    private var committedBackground: ProfileCardMedia? = null
    private var pendingBackground: PendingBackground? = null

    /** Canvas item id → committed (uploaded) media, for persistence + delete-on-remove. */
    private val canvasMedia = mutableMapOf<Long, ProfileCardMedia>()
    private val canvasPending = mutableMapOf<Long, CanvasPending>()

    /** Canvas item ids whose media upload/publish failed (retryable; UI may surface them). */
    val canvasUploadFailures = mutableStateListOf<Long>()

    fun mediaFor(ref: String?): CardMedia? = ref?.let { media[it] }

    init {
        val repo = repository
        val uid = userId?.takeIf { it.isNotBlank() }
        val sc = scope
        if (repo != null && uid != null && sc != null) {
            loadStatus = ProfileCardLoadStatus.LoadingRemoteProfile
            sc.launch {
                val doc = runCatching { repo.load(uid) }.getOrNull()
                if (doc != null) applyLoadedDoc(doc)
                loadStatus = ProfileCardLoadStatus.Loaded
            }
        }
    }

    private fun applyLoadedDoc(doc: ProfileCardDoc) {
        var restored = state.withAppearance(doc.appearance)
        doc.backgroundMedia?.let { remote ->
            committedBackground = remote
            val ref = "card:${nextMediaId++}"
            media[ref] = CardMedia(
                storagePath = remote.storagePath,
                downloadUrl = remote.downloadUrl,
                isVideo = remote.isVideo,
            )
            restored = restored.copy(cardBackground = BackgroundPaint.Image(ref))
        }
        // Canvas restore: the persisted canvas is the source of truth (replaces seed demo items).
        val restoredItems = doc.canvas.mapNotNull { persisted ->
            val remote = persisted.media ?: return@mapNotNull null
            canvasMedia[persisted.id] = remote
            nextCanvasId = maxOf(nextCanvasId, persisted.id + 1)
            val ref = "canvas:${nextMediaId++}"
            media[ref] = CardMedia(
                storagePath = remote.storagePath,
                downloadUrl = remote.downloadUrl,
                isVideo = remote.isVideo,
            )
            CanvasItem(
                id = persisted.id,
                kind = if (remote.isVideo) CanvasItemKind.VIDEO else CanvasItemKind.IMAGE,
                imageRef = ref,
                x = persisted.x,
                y = persisted.y,
                width = persisted.width,
                height = persisted.height,
                z = persisted.z,
            )
        }
        restored = restored.copy(canvasItems = restoredItems, selectedCanvasItemId = null)
        state = restored
    }

    private fun update(transform: (ProfileCardState) -> ProfileCardState) {
        state = transform(state)
    }

    /** Like [update], but also persists (debounced, best-effort) the doc to private + public. */
    private fun updateAppearance(transform: (ProfileCardState) -> ProfileCardState) {
        update(transform)
        scheduleSave()
    }

    private fun scheduleSave() {
        val repo = repository
        val uid = userId?.takeIf { it.isNotBlank() }
        val sc = scope
        if (repo == null || uid == null || sc == null) return
        saveJob?.cancel()
        saveJob = sc.launch {
            delay(SaveDebounceMs)
            // Appearance auto-save is best-effort; media saves surface errors explicitly.
            runCatching { repo.save(uid, currentDoc()) }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
            }
        }
    }

    /** The structured document to persist. The image background lives in backgroundMedia. */
    private fun currentDoc(): ProfileCardDoc {
        val appearance = state.appearance()
        val sanitized = if (appearance.cardBackground is BackgroundPaint.Image) {
            appearance.copy(cardBackground = DefaultProfileContent.cardBackground)
        } else {
            appearance
        }
        return ProfileCardDoc(appearance = sanitized, backgroundMedia = committedBackground, canvas = currentCanvasItems())
    }

    /** Only canvas items with committed (uploaded) media are persisted — a clean snapshot. */
    private fun currentCanvasItems(): List<ProfileCanvasItem> =
        state.canvasItems.mapNotNull { item ->
            val committed = canvasMedia[item.id] ?: return@mapNotNull null
            ProfileCanvasItem(
                id = item.id,
                type = ProfileCanvasItemType.MEDIA,
                media = committed,
                x = item.x,
                y = item.y,
                width = item.width,
                height = item.height,
                rotation = 0f,
                z = item.z,
                locked = false,
            )
        }

    // ---- panels / tabs ----
    fun selectPanel(panel: ProfilePanel) = update { it.copy(activePanel = panel) }

    // ---- identity toggles (persisted) ----
    fun cyclePegatina() = updateAppearance { it.copy(pegatinaIndex = (it.pegatinaIndex + 1) % 4) }
    fun toggleInsignia() = updateAppearance { it.copy(insigniaOn = !it.insigniaOn) }
    fun toggleFlagTeam() = updateAppearance {
        it.copy(flagOrTeam = if (it.flagOrTeam == FlagOrTeam.FLAG) FlagOrTeam.TEAM else FlagOrTeam.FLAG)
    }

    // ---- social actions (session) ----
    fun toggleFriend() = update { it.copy(isFriend = !it.isFriend) }
    fun toggleBlock() = update { it.copy(isBlocked = !it.isBlocked) }
    fun toggleMute() = update { it.copy(isMuted = !it.isMuted) }

    fun setLike() = update { s ->
        when (s.reputation) {
            ReputationVote.LIKE -> s.copy(reputation = ReputationVote.NONE, likeCount = s.likeCount - 1)
            ReputationVote.DISLIKE -> s.copy(
                reputation = ReputationVote.LIKE,
                likeCount = s.likeCount + 1,
                dislikeCount = s.dislikeCount - 1,
            )
            ReputationVote.NONE -> s.copy(reputation = ReputationVote.LIKE, likeCount = s.likeCount + 1)
        }
    }

    fun setDislike() = update { s ->
        when (s.reputation) {
            ReputationVote.DISLIKE -> s.copy(reputation = ReputationVote.NONE, dislikeCount = s.dislikeCount - 1)
            ReputationVote.LIKE -> s.copy(
                reputation = ReputationVote.DISLIKE,
                dislikeCount = s.dislikeCount + 1,
                likeCount = s.likeCount - 1,
            )
            ReputationVote.NONE -> s.copy(reputation = ReputationVote.DISLIKE, dislikeCount = s.dislikeCount + 1)
        }
    }

    // ---- color / background appearance (persisted) ----
    fun applyAccentAndScreen(argb: Long) = updateAppearance {
        it.copy(
            accentArgb = argb,
            screenBackground = BackgroundPaint.Solid(argb),
            recentColors = pushRecent(it.recentColors, argb),
        )
    }

    fun applyScreenGradient(stops: List<Long>, kind: GradientKind) = updateAppearance {
        it.copy(screenBackground = BackgroundPaint.Gradient(stops, kind))
    }

    fun applyCardSolid(argb: Long) = clearBackgroundMediaThen {
        updateAppearance {
            it.copy(cardBackground = BackgroundPaint.Solid(argb), recentColors = pushRecent(it.recentColors, argb))
        }
    }

    fun saveColor(argb: Long) = update {
        if (it.savedColors.any { saved -> saved == argb }) it
        else it.copy(savedColors = it.savedColors + argb)
    }

    // ---- card BACKGROUND media (durable: upload → save → publish → restore) ----

    /** Picks a new background photo/GIF/video: instant local preview, then upload + auto-publish. */
    fun setCardImage(bytes: ByteArray, mimeType: String) {
        val isVideo = ProfileCardMediaKind.fromMimeType(mimeType) == ProfileCardMediaKind.VIDEO
        val ref = "card:${nextMediaId++}"
        media[ref] = CardMedia(bytes = bytes, isVideo = isVideo) // instant local preview
        update { it.copy(cardBackground = BackgroundPaint.Image(ref)) }

        if (!backendReady()) {
            backgroundStatus = ProfileCardMediaStatus.Idle
            return
        }
        pendingBackground = PendingBackground(
            ref = ref,
            bytes = bytes,
            mimeType = mimeType,
            isVideo = isVideo,
            previous = committedBackground,
            uploaded = null,
        )
        startBackgroundFlow()
    }

    /** Retries the current background upload/publish after a failure (preview is preserved). */
    fun retryBackgroundMedia() {
        if (pendingBackground != null) startBackgroundFlow()
    }

    /** Discards a failed background change and reverts to the previously committed background. */
    fun discardBackgroundMedia() {
        backgroundJob?.cancel()
        val pending = pendingBackground
        val previous = pending?.previous
        // Media uploaded but never committed (e.g. save failed, then discarded) is an orphan.
        val discardedUpload = pending?.uploaded
        pendingBackground = null
        backgroundStatus = ProfileCardMediaStatus.Idle
        committedBackground = previous
        if (previous == null) {
            update { it.copy(cardBackground = DefaultProfileContent.cardBackground) }
        } else {
            val ref = "card:${nextMediaId++}"
            media[ref] = CardMedia(storagePath = previous.storagePath, downloadUrl = previous.downloadUrl, isVideo = previous.isVideo)
            update { it.copy(cardBackground = BackgroundPaint.Image(ref)) }
        }
        deleteMediaBestEffort(discardedUpload?.storagePath)
    }

    /** Resets the card background to the default gradient and deletes the old media best-effort. */
    fun resetCardBackground() {
        backgroundJob?.cancel()
        pendingBackground = null
        backgroundStatus = ProfileCardMediaStatus.Idle
        val previous = committedBackground
        committedBackground = null
        updateAppearance { it.copy(cardBackground = DefaultProfileContent.cardBackground) }
        deleteMediaBestEffort(previous?.storagePath)
    }

    private fun startBackgroundFlow() {
        val sc = scope ?: return
        val uid = userId?.takeIf { it.isNotBlank() }
        val mediaRepo = mediaRepository
        val cardRepo = repository
        if (uid == null || mediaRepo == null || cardRepo == null) return
        backgroundJob?.cancel()
        backgroundJob = sc.launch { runBackgroundFlow(uid, mediaRepo, cardRepo) }
    }

    @Suppress("ReturnCount") // Multi-step IO flow (upload → save → cleanup) with fail-fast per step.
    private suspend fun runBackgroundFlow(
        uid: String,
        mediaRepo: ProfileCardMediaRepository,
        cardRepo: ProfileCardRepository,
    ) {
        val pending = pendingBackground ?: return

        val uploaded = pending.uploaded ?: run {
            backgroundStatus = ProfileCardMediaStatus.Uploading
            val result = runCatching { mediaRepo.upload(uid, ProfileCardMediaScope.PUBLIC, pending.bytes, pending.mimeType) }
                .getOrElse { error ->
                    rethrowIfCancellation(error)
                    backgroundStatus = ProfileCardMediaStatus.UploadFailed(error.message ?: "No se pudo subir el archivo.")
                    return
                }
            committedBackground = result
            media[pending.ref] = CardMedia(
                bytes = pending.bytes,
                storagePath = result.storagePath,
                downloadUrl = result.downloadUrl,
                isVideo = pending.isVideo,
            )
            pendingBackground = pending.copy(uploaded = result)
            result
        }

        backgroundStatus = ProfileCardMediaStatus.Saving
        runCatching { cardRepo.save(uid, currentDoc()) }.getOrElse { error ->
            rethrowIfCancellation(error)
            backgroundStatus = ProfileCardMediaStatus.SaveFailed(error.message ?: "No se pudo guardar la tarjeta.")
            return
        }

        backgroundStatus = ProfileCardMediaStatus.Saved
        pendingBackground = null
        deleteMediaBestEffort(pending.previous?.storagePath, scopedUid = uid, repo = mediaRepo)
        // Keep the uploaded reference live.
        committedBackground = uploaded
    }

    private fun clearBackgroundMediaThen(block: () -> Unit) {
        val previous = committedBackground
        if (previous != null || pendingBackground != null) {
            backgroundJob?.cancel()
            pendingBackground = null
            committedBackground = null
            backgroundStatus = ProfileCardMediaStatus.Idle
            deleteMediaBestEffort(previous?.storagePath)
        }
        block()
    }

    private fun deleteMediaBestEffort(
        storagePath: String?,
        scopedUid: String? = userId?.takeIf { it.isNotBlank() },
        repo: ProfileCardMediaRepository? = mediaRepository,
    ) {
        val sc = scope ?: return
        val path = storagePath?.takeIf { it.isNotBlank() }
        if (path == null || scopedUid == null || repo == null) return
        sc.launch { repo.delete(scopedUid, path) }
    }

    private fun rethrowIfCancellation(error: Throwable) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
    }

    private fun backendReady(): Boolean =
        userId?.isNotBlank() == true && repository != null && mediaRepository != null && scope != null

    private fun pushRecent(current: List<Long>, argb: Long): List<Long> =
        (listOf(argb) + current.filter { it != argb }).take(8)

    // ---- canvas (session in Ola 1·B; durable persistence wired in Ola 2) ----
    fun selectCanvasItem(id: Long?) = update { it.copy(selectedCanvasItemId = id) }

    // Geometry edits persist the doc (debounced) so position/size/order survive a restart.
    fun moveCanvasItem(id: Long, dx: Float, dy: Float, canvasWidth: Float, canvasHeight: Float) = updateAppearance { s ->
        s.copy(
            canvasItems = s.canvasItems.map { item ->
                if (item.id != id) item else {
                    val x = (item.x + dx).coerceIn(0f, (canvasWidth - item.width).coerceAtLeast(0f))
                    val y = (item.y + dy).coerceIn(0f, (canvasHeight - item.height).coerceAtLeast(0f))
                    item.copy(x = x, y = y)
                }
            },
        )
    }

    fun resizeCanvasItem(id: Long, dx: Float, dy: Float, canvasWidth: Float, canvasHeight: Float) = updateAppearance { s ->
        s.copy(
            canvasItems = s.canvasItems.map { item ->
                if (item.id != id) item else {
                    val w = (item.width + dx).coerceIn(MIN_ITEM, (canvasWidth - item.x).coerceAtLeast(MIN_ITEM))
                    val h = (item.height + dy).coerceIn(MIN_ITEM, (canvasHeight - item.y).coerceAtLeast(MIN_ITEM))
                    item.copy(width = w, height = h)
                }
            },
        )
    }

    fun bringToFront(id: Long) = updateAppearance { s ->
        val maxZ = s.canvasItems.maxOfOrNull { it.z } ?: 0
        s.copy(canvasItems = s.canvasItems.map { if (it.id == id) it.copy(z = maxZ + 1) else it })
    }

    fun sendToBack(id: Long) = updateAppearance { s ->
        val minZ = s.canvasItems.minOfOrNull { it.z } ?: 0
        s.copy(canvasItems = s.canvasItems.map { if (it.id == id) it.copy(z = minZ - 1) else it })
    }

    /** Removes an item, persists the doc, and deletes its media best-effort (no orphan). */
    fun removeCanvasItem(id: Long) {
        val removed = canvasMedia.remove(id)
        canvasPending.remove(id)
        canvasUploadFailures.remove(id)
        updateAppearance { s ->
            s.copy(canvasItems = s.canvasItems.filterNot { it.id == id }, selectedCanvasItemId = null)
        }
        deleteMediaBestEffort(removed?.storagePath)
    }

    /** Adds a freshly picked photo/GIF/video to the canvas: instant preview, then upload + persist. */
    fun addCanvasMedia(bytes: ByteArray, mimeType: String) {
        val isVideo = ProfileCardMediaKind.fromMimeType(mimeType) == ProfileCardMediaKind.VIDEO
        val ref = "canvas:${nextMediaId++}"
        val itemId = nextCanvasId++
        media[ref] = CardMedia(bytes = bytes, isVideo = isVideo) // instant local preview
        update { s ->
            val maxZ = s.canvasItems.maxOfOrNull { it.z } ?: 0
            val item = CanvasItem(
                id = itemId,
                kind = if (isVideo) CanvasItemKind.VIDEO else CanvasItemKind.IMAGE,
                imageRef = ref,
                x = 30f, y = 40f, width = 150f, height = 120f, z = maxZ + 1,
            )
            s.copy(canvasItems = s.canvasItems + item, selectedCanvasItemId = item.id)
        }
        if (!backendReady()) return
        canvasPending[itemId] = CanvasPending(ref, bytes, mimeType, isVideo)
        startCanvasUpload(itemId)
    }

    /** Retries a failed canvas-media upload/publish (the local preview is preserved). */
    fun retryCanvasMedia(itemId: Long) {
        if (canvasPending.containsKey(itemId)) startCanvasUpload(itemId)
    }

    private fun startCanvasUpload(itemId: Long) {
        val sc = scope ?: return
        sc.launch { runCanvasUpload(itemId) }
    }

    @Suppress("ReturnCount") // Multi-step IO (upload → save) with per-step failure handling.
    private suspend fun runCanvasUpload(itemId: Long) {
        val pending = canvasPending[itemId] ?: return
        val uid = userId?.takeIf { it.isNotBlank() }
        val mediaRepo = mediaRepository
        val cardRepo = repository
        if (uid == null || mediaRepo == null || cardRepo == null) return
        canvasUploadFailures.remove(itemId)
        val uploaded = runCatching {
            mediaRepo.upload(uid, ProfileCardMediaScope.PUBLIC, pending.bytes, pending.mimeType)
        }.getOrElse { error ->
            rethrowIfCancellation(error)
            canvasUploadFailures.add(itemId)
            return
        }
        canvasMedia[itemId] = uploaded
        media[pending.ref] = CardMedia(pending.bytes, uploaded.storagePath, uploaded.downloadUrl, pending.isVideo)
        runCatching { cardRepo.save(uid, currentDoc()) }.getOrElse { error ->
            rethrowIfCancellation(error)
            canvasUploadFailures.add(itemId)
            return
        }
        canvasPending.remove(itemId)
    }

    /** Adds a placeholder tile (used by tests / before platform picking). */
    fun addPlaceholder() = update { s ->
        val maxZ = s.canvasItems.maxOfOrNull { it.z } ?: 0
        val palette = listOf(
            0xFF241A2EL to 0xFF1F1628L,
            0xFF16261FL to 0xFF122019L,
            0xFF1B2746L to 0xFF0F1830L,
        )
        val (start, end) = palette[(nextCanvasId.toInt()).coerceAtLeast(0) % palette.size]
        val item = CanvasItem(
            id = nextCanvasId++,
            kind = CanvasItemKind.PLACEHOLDER,
            label = "media",
            placeholderGradientStart = start,
            placeholderGradientEnd = end,
            x = 30f, y = 40f, width = 140f, height = 110f, z = maxZ + 1,
        )
        s.copy(canvasItems = s.canvasItems + item, selectedCanvasItemId = item.id)
    }

    private companion object {
        const val MIN_ITEM = 56f
        const val SaveDebounceMs = 600L
    }
}

/**
 * Remembers a [ProfileCardStateHolder]. When [userId] + [repository] + [mediaRepository]
 * are provided, the card restores from Firestore on first composition and persists
 * appearance + background media durably (private draft + public snapshot).
 */
@Composable
fun rememberProfileCardStateHolder(
    username: String,
    userId: String? = null,
    repository: ProfileCardRepository? = null,
    mediaRepository: ProfileCardMediaRepository? = null,
): ProfileCardStateHolder {
    val scope = rememberCoroutineScope()
    return remember(userId, username) {
        ProfileCardStateHolder(
            initial = ProfileCardState(username = username),
            userId = userId,
            repository = repository,
            mediaRepository = mediaRepository,
            scope = scope,
        )
    }
}
