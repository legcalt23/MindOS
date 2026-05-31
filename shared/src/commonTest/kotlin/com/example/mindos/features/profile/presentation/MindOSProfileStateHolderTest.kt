package com.example.mindos.features.profile.presentation

import com.example.mindos.features.profile.data.InMemoryMindOSProfileRepository
import com.example.mindos.features.profile.data.InMemoryProfileObserverRepository
import com.example.mindos.features.profile.data.InMemoryProfilePresenceRepository
import com.example.mindos.features.profile.domain.audio.AudioWaveform
import com.example.mindos.features.profile.presentation.audio.ProfileAudioCapture
import com.example.mindos.features.profile.presentation.state.AudioUiState
import com.example.mindos.features.profile.presentation.state.MindOSProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MindOSProfileStateHolderTest {

    private fun CoroutineScope.newHolder(): MindOSProfileStateHolder =
        MindOSProfileStateHolder(
            profileRepository = InMemoryMindOSProfileRepository(),
            observerRepository = InMemoryProfileObserverRepository(),
            presenceRepository = InMemoryProfilePresenceRepository(),
            scope = this,
        )

    private fun MindOSProfileStateHolder.loaded(): MindOSProfileUiState.Loaded =
        assertIs<MindOSProfileUiState.Loaded>(uiState)

    @Test
    fun ownProfileLoadsWithDefaults() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()

        val loaded = holder.loaded()
        assertTrue(loaded.isOwnProfile)
        assertEquals("@", loaded.profile.usernamePrefix)
        assertEquals("currolf", loaded.profile.username)
        assertEquals(1_700, loaded.profile.observerCount)
        assertEquals(0L, loaded.profile.donation.currentAmount)
        assertEquals(10_000L, loaded.profile.donation.targetAmount)
        assertIs<AudioUiState.Empty>(loaded.audio)
        assertEquals(3, loaded.profile.customLinks.size)
        coroutineContext.cancelChildren()
    }

    @Test
    fun observeTogglesAndUpdatesCount() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "viewer", ownerId = "target", username = "otro", isOwnProfile = false)
        advanceUntilIdle()
        assertFalse(holder.loaded().profile.isObservedByViewer)
        assertEquals(1_700, holder.loaded().profile.observerCount)

        holder.toggleObserve()
        advanceUntilIdle()
        assertTrue(holder.loaded().profile.isObservedByViewer)
        assertEquals(1_701, holder.loaded().profile.observerCount)

        holder.toggleObserve()
        advanceUntilIdle()
        assertFalse(holder.loaded().profile.isObservedByViewer)
        assertEquals(1_700, holder.loaded().profile.observerCount)
        coroutineContext.cancelChildren()
    }

    @Test
    fun emojiPrefixSavesAndResets() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()

        holder.selectUsernameEmoji("🧠")
        advanceUntilIdle()
        assertEquals("🧠", holder.loaded().profile.usernamePrefix)

        holder.selectUsernameEmoji(null)
        advanceUntilIdle()
        assertEquals("@", holder.loaded().profile.usernamePrefix)
        coroutineContext.cancelChildren()
    }

    @Test
    fun audioRecordingStateMachineReachesSaved() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()

        holder.onRecordingStarted()
        assertIs<AudioUiState.Recording>(holder.loaded().audio)

        holder.onAmplitudeSample(0.6f, elapsedMillis = 100)
        val recording = assertIs<AudioUiState.Recording>(holder.loaded().audio)
        assertTrue(AudioWaveform.hasSignal(recording.liveWaveform))

        holder.onRecordingFinished(ProfileAudioCapture(byteArrayOf(1, 2, 3), "audio/mp4", durationMillis = 5_000))
        val preview = assertIs<AudioUiState.RecordedPreview>(holder.loaded().audio)
        assertEquals(5_000, preview.durationMillis)
        assertTrue(AudioWaveform.hasSignal(preview.waveform))

        holder.saveRecordedAudio()
        advanceUntilIdle()
        val saved = assertIs<AudioUiState.Saved>(holder.loaded().audio)
        assertEquals(5_000, saved.durationMillis)
        assertNotNull(holder.loaded().profile.audio)
        coroutineContext.cancelChildren()
    }

    @Test
    fun discardRecordingReturnsToEmpty() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()

        holder.onRecordingStarted()
        holder.onAmplitudeSample(0.4f, elapsedMillis = 80)
        holder.onRecordingFinished(ProfileAudioCapture(byteArrayOf(9), "audio/mp4", durationMillis = 3_000))
        assertIs<AudioUiState.RecordedPreview>(holder.loaded().audio)

        holder.discardRecording()
        assertIs<AudioUiState.Empty>(holder.loaded().audio)
        coroutineContext.cancelChildren()
    }

    @Test
    fun customLinkRejectsInvalidThenSavesValid() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()

        holder.openLinkEditor(0)
        holder.updateLinkEditorUrl("mind os .app")
        holder.saveLink()
        advanceUntilIdle()
        assertNotNull(holder.loaded().linkEditor?.validationError)

        holder.updateLinkEditorUrl("mindos.app")
        holder.saveLink()
        advanceUntilIdle()
        assertEquals(null, holder.loaded().linkEditor)
        val slot = holder.loaded().profile.customLinks.first { it.slotIndex == 0 }
        assertFalse(slot.isEmpty)
        assertEquals("https://mindos.app", slot.url)
        coroutineContext.cancelChildren()
    }

    @Test
    fun otherProfileCannotEditEmojiOrPhotos() = runTest {
        val holder = newHolder()
        holder.load(viewerId = "viewer", ownerId = "target", username = "otro", isOwnProfile = false)
        advanceUntilIdle()

        holder.openEmojiPicker()
        assertEquals(null, holder.loaded().emojiPicker)
        holder.selectUsernameEmoji("🧠")
        advanceUntilIdle()
        assertEquals("@", holder.loaded().profile.usernamePrefix)
        coroutineContext.cancelChildren()
    }
}
