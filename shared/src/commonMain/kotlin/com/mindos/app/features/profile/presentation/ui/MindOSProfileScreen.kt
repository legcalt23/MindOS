package com.mindos.app.features.profile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.features.profile.presentation.MindOSProfileStateHolder
import com.mindos.app.features.profile.presentation.ProfileForegroundEffect
import com.mindos.app.features.profile.presentation.audio.rememberMicrophonePermission
import com.mindos.app.features.profile.presentation.audio.rememberProfileAudioPlayer
import com.mindos.app.features.profile.presentation.audio.rememberProfileAudioRecorder
import com.mindos.app.features.profile.presentation.state.AudioUiState
import com.mindos.app.features.profile.presentation.state.MindOSProfileUiState
import com.mindos.app.features.social.domain.model.ProfileMediaUploadKind
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.presentation.profile.rememberProfileMediaPicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val AmplitudePollMillis = 60L
private const val PlaybackTickMillis = 50L
private val LinksColumnWidth = 46.dp

private enum class ImagePickTarget { Cover, Avatar, LinkIcon }

/**
 * The MindOS premium profile screen (Perfil.html). Owns the platform side
 * effects — image picking, audio recording/playback, microphone permission,
 * foreground presence, and URL launching — and delegates all state to
 * [MindOSProfileStateHolder]. Nothing here fabricates user state.
 */
@Composable
fun MindOSProfileScreen(
    holder: MindOSProfileStateHolder,
    viewerId: String,
    ownerId: String,
    username: String,
    isOwnProfile: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(holder, viewerId, ownerId, username, isOwnProfile) {
        holder.load(viewerId = viewerId, ownerId = ownerId, username = username, isOwnProfile = isOwnProfile)
    }

    // Local user presence follows the app foreground lifecycle.
    ProfileForegroundEffect { isForeground -> holder.setForeground(isForeground) }

    // ── Image picking (cover / avatar / link icon) reuses the shared picker ──
    var pickTarget by remember { mutableStateOf<ImagePickTarget?>(null) }
    val picker = rememberProfileMediaPicker(
        onPicked = { _, upload ->
            when (pickTarget) {
                ImagePickTarget.Cover ->
                    holder.onImagePicked(com.mindos.app.features.profile.domain.model.ProfileImageTarget.CoverPhoto, upload)
                ImagePickTarget.Avatar ->
                    holder.onImagePicked(com.mindos.app.features.profile.domain.model.ProfileImageTarget.ProfilePhoto, upload)
                ImagePickTarget.LinkIcon -> holder.onLinkIconPicked(upload)
                null -> Unit
            }
            pickTarget = null
        },
        onError = { message ->
            if (pickTarget == ImagePickTarget.LinkIcon) holder.onLinkIconPickError(message) else holder.onImagePickError(message)
            pickTarget = null
        },
    )
    fun pickImage(target: ImagePickTarget) {
        pickTarget = target
        picker.pick(ProfileModuleId.Main, ProfileMediaUploadKind.Photo)
    }

    // ── Audio recording ──
    val recorder = rememberProfileAudioRecorder(onError = { holder.onRecordingError(it) })
    val micPermission = rememberMicrophonePermission()
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var stopEarly by remember { mutableStateOf(false) }

    fun startRecording() {
        if (!recorder.isSupported) {
            holder.onRecordingError("La grabación no está disponible en esta plataforma.")
            return
        }
        stopEarly = false
        recordingJob = scope.launch {
            holder.onRecordingStarted()
            val started = runCatching { recorder.start() }.isSuccess
            if (!started) return@launch
            var elapsed = 0L
            while (elapsed < MindOSProfileStateHolder.MaxAudioMillis && !stopEarly && isActive) {
                delay(AmplitudePollMillis)
                elapsed += AmplitudePollMillis
                holder.onAmplitudeSample(recorder.currentAmplitude(), elapsed)
            }
            holder.onRecordingFinished(recorder.stop())
        }
    }

    fun onMicTap() {
        holder.setMicPermissionMessage(null)
        if (micPermission.state.isGranted) {
            startRecording()
        } else {
            micPermission.request { granted ->
                if (granted) {
                    startRecording()
                } else {
                    holder.setMicPermissionMessage(
                        if (micPermission.state.shouldDirectToSettings) {
                            "Activa el micrófono en los ajustes del sistema."
                        } else {
                            "Necesitamos permiso de micrófono para grabar tu audio."
                        },
                    )
                }
            }
        }
    }

    // ── Audio playback ──
    val player = rememberProfileAudioPlayer(onError = { holder.onImagePickError(it) })
    var playbackProgress by remember { mutableStateOf(0f) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    fun stopPlayback() {
        player.stop()
        playbackJob?.cancel()
        playbackJob = null
        playbackProgress = 0f
        holder.setAudioPlaying(false)
    }

    fun togglePlayback(isPlaying: Boolean) {
        if (isPlaying) {
            stopPlayback()
            return
        }
        val clip = holder.currentPlayableClip() ?: return
        holder.setAudioPlaying(true)
        playbackProgress = 0f
        playbackJob = scope.launch {
            val duration = clip.durationMillis.coerceAtLeast(1L)
            val ticker = launch {
                var elapsed = 0L
                while (elapsed < duration && isActive) {
                    delay(PlaybackTickMillis)
                    elapsed += PlaybackTickMillis
                    playbackProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                }
            }
            player.play(clip)
            ticker.cancel()
            playbackProgress = 0f
            holder.setAudioPlaying(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recordingJob?.cancel()
            recorder.cancel()
            playbackJob?.cancel()
            player.stop()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AppColors.ProfileCanvas)) {
        when (val state = holder.uiState) {
            is MindOSProfileUiState.Loading -> ProfileCentered { CircularProgressIndicator(color = AppColors.ProfileTextPrimary) }
            is MindOSProfileUiState.Error -> ProfileCentered {
                Text(state.message, color = AppColors.ProfileTextPrimary, fontWeight = FontWeight.Medium)
            }
            is MindOSProfileUiState.Loaded -> LoadedProfile(
                state = state,
                playbackProgress = playbackProgress,
                onBack = onBack,
                onEditCover = { pickImage(ImagePickTarget.Cover) },
                onEditAvatar = { pickImage(ImagePickTarget.Avatar) },
                onPrefixTap = holder::openEmojiPicker,
                onObserveToggle = holder::toggleObserve,
                onEditProfile = holder::openEmojiPicker,
                onMicTap = ::onMicTap,
                onStopTap = { stopEarly = true },
                onPlayPauseTap = { togglePlayback(isAudioPlaying(state.audio)) },
                onSaveAudio = holder::saveRecordedAudio,
                onDiscardAudio = holder::discardRecording,
                onReRecord = { stopPlayback(); onMicTap() },
                onAddLink = holder::openLinkEditor,
                onEditLink = holder::openLinkEditor,
                onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
                onDismissBanner = holder::dismissTransientMessage,
            )
        }

        (holder.uiState as? MindOSProfileUiState.Loaded)?.let { loaded ->
            loaded.emojiPicker?.let { picker ->
                EmojiPrefixPickerDialog(
                    state = picker,
                    onSelect = holder::selectUsernameEmoji,
                    onReset = { holder.selectUsernameEmoji(null) },
                    onDismiss = holder::dismissEmojiPicker,
                )
            }
            loaded.linkEditor?.let { editor ->
                CustomLinkEditDialog(
                    state = editor,
                    onUrlChange = holder::updateLinkEditorUrl,
                    onPickIcon = { pickImage(ImagePickTarget.LinkIcon) },
                    onSave = holder::saveLink,
                    onClear = { holder.clearLink(editor.slotIndex) },
                    onDismiss = holder::dismissLinkEditor,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedProfile(
    state: MindOSProfileUiState.Loaded,
    playbackProgress: Float,
    onBack: () -> Unit,
    onEditCover: () -> Unit,
    onEditAvatar: () -> Unit,
    onPrefixTap: () -> Unit,
    onObserveToggle: () -> Unit,
    onEditProfile: () -> Unit,
    onMicTap: () -> Unit,
    onStopTap: () -> Unit,
    onPlayPauseTap: () -> Unit,
    onSaveAudio: () -> Unit,
    onDiscardAudio: () -> Unit,
    onReRecord: () -> Unit,
    onAddLink: (Int) -> Unit,
    onEditLink: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismissBanner: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeaderSection(
            profile = state.profile,
            isOwner = state.isOwnProfile,
            onEditCover = onEditCover,
            onEditAvatar = onEditAvatar,
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(18.dp))
        ProfileIdentityRow(
            profile = state.profile,
            isOwner = state.isOwnProfile,
            onPrefixTap = onPrefixTap,
            onObserveToggle = onObserveToggle,
            onEditProfile = onEditProfile,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileAudioCard(
                    audio = state.audio,
                    isOwner = state.isOwnProfile,
                    playbackProgress = playbackProgress,
                    onMicTap = onMicTap,
                    onStopTap = onStopTap,
                    onPlayPauseTap = onPlayPauseTap,
                    onSave = onSaveAudio,
                    onDiscard = onDiscardAudio,
                    onReRecord = onReRecord,
                    micPermissionMessage = state.micPermissionMessage,
                )
                ProfileDonationBar(donation = state.profile.donation)
            }
            ProfileCustomLinksColumn(
                profile = state.profile,
                isOwner = state.isOwnProfile,
                onAdd = onAddLink,
                onEdit = onEditLink,
                onOpenUrl = onOpenUrl,
                modifier = Modifier.width(LinksColumnWidth).height(122.dp),
            )
        }
        state.transientMessage?.let { message ->
            Spacer(modifier = Modifier.height(14.dp))
            ProfileTransientBanner(
                message = message,
                onDismiss = onDismissBanner,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        ProfilePublicationsGrid(modifier = Modifier.padding(horizontal = 22.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ProfileCentered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun isAudioPlaying(audio: AudioUiState): Boolean =
    (audio as? AudioUiState.RecordedPreview)?.isPlaying == true ||
        (audio as? AudioUiState.Saved)?.isPlaying == true
