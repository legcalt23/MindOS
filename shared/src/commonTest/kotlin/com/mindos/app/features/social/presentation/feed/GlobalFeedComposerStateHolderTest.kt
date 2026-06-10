package com.mindos.app.features.social.presentation.feed

import com.mindos.app.core.session.AuthenticatedUser
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.social.domain.feed.CreateGlobalFeedPostRequest
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxAudioClips
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxTextLength
import com.mindos.app.features.social.domain.feed.GlobalFeedPost
import com.mindos.app.features.social.domain.feed.GlobalFeedPostType
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository
import com.mindos.app.features.social.domain.feed.GlobalFeedTextBackgroundStyle
import com.mindos.app.features.social.domain.error.InvalidValue
import com.mindos.app.features.social.shared.util.DomainResult
import com.mindos.app.features.dm.presentation.media.DMAudioRecording
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Coverage for [GlobalFeedComposerStateHolder] — the composer that drives the
 * "create post" screen.
 *
 * Focused on the business rules that aren't trivially obvious from the code:
 *  - Mode swipe boundary (PHOTO is first, AUDIO is last)
 *  - Text length cap is enforced character-by-character
 *  - canPublish reflects the active mode + content + publishing state
 *  - Audio clip limit and minimum duration
 *  - onPublishClick: validates → calls repo → resets on success / surfaces error on failure
 *  - onPublishClick is a no-op while isPublishing
 */
class GlobalFeedComposerStateHolderTest {

    private val user = AuthenticatedUser(
        uid = "uid_alice",
        email = "alice@example.com",
        displayName = "Alice",
        isEmailVerified = true,
    )

    // ── Mode swipe ────────────────────────────────────────────────────────

    @Test
    fun modeSwipeNext_cyclesPhotoVideoTextAudio_andStopsAtAudio() = runTest {
        val holder = newHolder()
        assertEquals(ComposerMode.PHOTO, holder.uiState.selectedMode)

        holder.onModeSwipeNext()
        assertEquals(ComposerMode.VIDEO, holder.uiState.selectedMode)
        holder.onModeSwipeNext()
        assertEquals(ComposerMode.TEXT, holder.uiState.selectedMode)
        holder.onModeSwipeNext()
        assertEquals(ComposerMode.AUDIO, holder.uiState.selectedMode)

        // Already at the last mode → no transition, but a feedback message surfaces.
        holder.onModeSwipeNext()
        assertEquals(ComposerMode.AUDIO, holder.uiState.selectedMode)
        assertNotNull(holder.uiState.feedbackMessage)
    }

    @Test
    fun modeSwipePrevious_stopsAtPhoto() = runTest {
        val holder = newHolder()
        holder.onModeSwipePrevious()
        assertEquals(ComposerMode.PHOTO, holder.uiState.selectedMode)
        assertNotNull(holder.uiState.feedbackMessage)
    }

    // ── Text content ──────────────────────────────────────────────────────

    @Test
    fun onTextChanged_clampsToMaxLength() = runTest {
        val holder = newHolder()
        val overlong = "a".repeat(GlobalFeedMaxTextLength + 100)
        holder.onTextChanged(overlong)
        assertEquals(GlobalFeedMaxTextLength, holder.uiState.textContent.length)
    }

    // ── canPublish gating ─────────────────────────────────────────────────

    @Test
    fun canPublish_textMode_requiresNonBlankText() = runTest {
        val holder = newHolder()
        holder.onTextEditorClick()
        assertFalse(holder.uiState.canPublish, "Empty text must not be publishable")

        holder.onTextChanged("   \n  ")
        assertFalse(holder.uiState.canPublish, "Whitespace-only text must not be publishable")

        holder.onTextChanged("hello world")
        assertTrue(holder.uiState.canPublish, "Non-blank text must be publishable")
    }

    @Test
    fun canPublish_isFalse_whileIsPublishing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = GatedFakeFeedRepository(gate)
        val holder = newHolder(repository = repo)
        holder.onTextEditorClick()
        holder.onTextChanged("publish me")
        assertTrue(holder.uiState.canPublish)

        holder.onPublishClick()
        runCurrent()
        // While the suspending createPost is still gated, isPublishing=true → canPublish=false.
        assertFalse(holder.uiState.canPublish, "canPublish must be false during publish")
        // Release the gate so the launched coroutine completes before runTest tears down.
        gate.complete(Unit)
        advanceUntilIdle()
    }

    // ── Audio recording ───────────────────────────────────────────────────

    @Test
    fun onStartAudioRecording_setsIsRecording() = runTest {
        val holder = newHolder()
        holder.onStartAudioRecording()
        assertTrue(holder.uiState.isRecording)
        assertEquals(ComposerMode.AUDIO, holder.uiState.selectedMode)
    }

    @Test
    fun onAudioRecorded_rejectsTooShortClip() = runTest {
        val holder = newHolder()
        holder.onStartAudioRecording()
        holder.onAudioRecorded(makeRecording(durationMs = 500L))  // < 1s
        assertFalse(holder.uiState.isRecording)
        assertNotNull(holder.uiState.errorMessage)
        assertEquals(0, holder.uiState.audioRecordings.size)
    }

    // Previously uncovered because draftId() called Clock.System.now() directly
    // (removed from the kotlinx-datetime 0.7.1 runtime classpath). Now the holder
    // takes an injected EpochClock (see newHolder), so these run deterministically.

    @Test
    fun onAudioRecorded_enforcesMaxClipsLimit() = runTest {
        val holder = newHolder()
        // Fill up to the limit with valid (>= 1s) clips.
        repeat(GlobalFeedMaxAudioClips) { index ->
            holder.onAudioRecorded(makeRecording(durationMs = 2_000L, idHint = "ok_$index"))
        }
        assertEquals(GlobalFeedMaxAudioClips, holder.uiState.audioRecordings.size)

        // One more must be rejected, leaving the count at the cap.
        holder.onAudioRecorded(makeRecording(durationMs = 2_000L, idHint = "overflow"))
        assertEquals(GlobalFeedMaxAudioClips, holder.uiState.audioRecordings.size)
        assertNotNull(holder.uiState.errorMessage)
    }

    @Test
    fun onDeleteAudio_removesClipById() = runTest {
        val holder = newHolder()
        holder.onAudioRecorded(makeRecording(durationMs = 2_000L))
        assertEquals(1, holder.uiState.audioRecordings.size)
        val clipId = holder.uiState.audioRecordings.first().id

        holder.onDeleteAudio(clipId)
        assertEquals(0, holder.uiState.audioRecordings.size)
    }

    // ── Publish flow ──────────────────────────────────────────────────────

    @Test
    fun onPublishClick_textPost_callsRepoAndResetsOnSuccess() = runTest {
        val repo = RecordingFakeFeedRepository()
        val holder = newHolder(repository = repo)
        holder.onTextEditorClick()
        holder.onTextChanged("hola mundo")

        var published: GlobalFeedPost? = null
        holder.onPublishClick { published = it }
        advanceUntilIdle()

        // Repository was called with a TEXT-type request.
        val request = repo.lastRequest
        assertNotNull(request)
        assertEquals(GlobalFeedPostType.TEXT, request.postType)
        assertEquals("hola mundo", request.text?.trim())

        // Composer was reset.
        assertEquals("", holder.uiState.textContent)
        assertEquals(ComposerMode.PHOTO, holder.uiState.selectedMode)
        assertNull(holder.uiState.errorMessage)
        assertFalse(holder.uiState.isPublishing)
        // The published callback fired with the repo's post.
        assertNotNull(published)
    }

    @Test
    fun onPublishClick_failure_surfacesErrorMessage() = runTest {
        val repo = FailingFakeFeedRepository()
        val holder = newHolder(repository = repo)
        holder.onTextEditorClick()
        holder.onTextChanged("publish me")

        holder.onPublishClick()
        advanceUntilIdle()

        assertFalse(holder.uiState.isPublishing)
        assertNotNull(holder.uiState.errorMessage)
        // Text content is preserved on failure so the user can retry.
        assertEquals("publish me", holder.uiState.textContent)
    }

    @Test
    fun onPublishClick_isNoOp_whileAlreadyPublishing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = GatedFakeFeedRepository(gate)
        val holder = newHolder(repository = repo)
        holder.onTextEditorClick()
        holder.onTextChanged("first")

        holder.onPublishClick()
        runCurrent()
        // Second click while the first is still in flight must be a no-op.
        holder.onPublishClick()
        runCurrent()
        assertEquals(1, repo.inFlightCount, "A second publish click must not start a new upload")
        // Release the gate so runTest can complete child jobs cleanly.
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun onPublishClick_emptyText_blocksWithValidationError() = runTest {
        val repo = RecordingFakeFeedRepository()
        val holder = newHolder(repository = repo)
        holder.onTextEditorClick()
        // textContent is empty.

        holder.onPublishClick()
        advanceUntilIdle()

        assertNotNull(holder.uiState.errorMessage)
        assertNull(repo.lastRequest, "Repository must not be invoked when validation fails")
    }

    // ── Style presets ─────────────────────────────────────────────────────

    @Test
    fun onApplyTextPreset_updatesBackgroundStyle() = runTest {
        val holder = newHolder()
        val initialStyle = holder.uiState.textStyleConfig.backgroundStyle
        holder.onApplyTextPreset(GlobalFeedTextBackgroundStyle.MinimalLight)
        val newStyle = holder.uiState.textStyleConfig.backgroundStyle
        assertEquals(GlobalFeedTextBackgroundStyle.MinimalLight, newStyle)
        // Sanity: a no-op preset would be a regression.
        assertTrue(initialStyle != newStyle || initialStyle == GlobalFeedTextBackgroundStyle.MinimalLight)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun makeRecording(durationMs: Long, idHint: String = "clip"): DMAudioRecording =
        DMAudioRecording(
            bytes = ByteArray(8),
            sizeBytes = 8L,
            durationMs = durationMs,
            mimeType = "audio/wav",
            fileName = "rec_$idHint.wav",
            localPath = null,
        )

    private fun TestScope.newHolder(
        repository: GlobalFeedRepository = RecordingFakeFeedRepository(),
        clock: EpochClock = IncrementingEpochClock(),
    ): GlobalFeedComposerStateHolder =
        GlobalFeedComposerStateHolder(user = user, repository = repository, scope = this, clock = clock)

    /** Deterministic clock so draftId() never touches Clock.System and ids stay unique. */
    private class IncrementingEpochClock(start: Long = 1_700_000_000_000L) : EpochClock {
        private var next = start
        override fun nowMillis(): Long = next++
    }

    // ── Fakes ─────────────────────────────────────────────────────────────

    private class RecordingFakeFeedRepository : GlobalFeedRepository {
        var lastRequest: CreateGlobalFeedPostRequest? = null
            private set

        override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
            DomainResult.success(emptyList())

        override suspend fun loadPostsByUser(
            userId: String,
            limit: Int,
        ): DomainResult<List<GlobalFeedPost>> = DomainResult.success(emptyList())

        override suspend fun createPost(
            request: CreateGlobalFeedPostRequest,
        ): DomainResult<GlobalFeedPost> {
            lastRequest = request
            return DomainResult.success(
                GlobalFeedPost(
                    id = "post_1",
                    userId = request.userId,
                    authorUsername = request.authorUsername,
                    authorDisplayName = request.authorDisplayName,
                    postType = request.postType,
                    text = request.text,
                    description = request.description,
                    textStyle = request.textStyle,
                    attachments = emptyList(),
                    createdAtMillis = 1L,
                ),
            )
        }
    }

    private class FailingFakeFeedRepository : GlobalFeedRepository {
        override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
            DomainResult.success(emptyList())

        override suspend fun loadPostsByUser(
            userId: String,
            limit: Int,
        ): DomainResult<List<GlobalFeedPost>> = DomainResult.success(emptyList())

        override suspend fun createPost(
            request: CreateGlobalFeedPostRequest,
        ): DomainResult<GlobalFeedPost> =
            DomainResult.failure(InvalidValue("text", "remote rejection"))
    }

    /**
     * Suspends `createPost` on a [gate] [CompletableDeferred] so the test can
     * observe the in-flight state machine. Tests MUST complete the gate before
     * the runTest block returns to avoid `UncompletedCoroutinesError`.
     */
    private class GatedFakeFeedRepository(
        private val gate: CompletableDeferred<Unit>,
    ) : GlobalFeedRepository {
        var inFlightCount = 0
            private set

        override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
            DomainResult.success(emptyList())

        override suspend fun loadPostsByUser(
            userId: String,
            limit: Int,
        ): DomainResult<List<GlobalFeedPost>> = DomainResult.success(emptyList())

        override suspend fun createPost(
            request: CreateGlobalFeedPostRequest,
        ): DomainResult<GlobalFeedPost> {
            inFlightCount++
            gate.await()
            return DomainResult.success(
                GlobalFeedPost(
                    id = "gated_post",
                    userId = request.userId,
                    authorUsername = request.authorUsername,
                    authorDisplayName = request.authorDisplayName,
                    postType = request.postType,
                    text = request.text,
                    description = request.description,
                    textStyle = request.textStyle,
                    attachments = emptyList(),
                    createdAtMillis = 1L,
                ),
            )
        }
    }
}
