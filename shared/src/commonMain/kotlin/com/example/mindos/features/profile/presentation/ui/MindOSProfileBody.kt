package com.example.mindos.features.profile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.design.AppColors
import com.example.mindos.features.profile.domain.audio.AudioWaveform
import com.example.mindos.features.profile.domain.audio.ProfileFormatting
import com.example.mindos.features.profile.domain.model.DonationProgress
import com.example.mindos.features.profile.domain.model.MindOSProfile
import com.example.mindos.features.profile.domain.model.ProfileCustomLink
import com.example.mindos.features.profile.presentation.MindOSProfileStateHolder
import com.example.mindos.features.profile.presentation.state.AudioUiState
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview

private val CardHeight = 56.dp
private val CardCorner = RoundedCornerShape(16.dp)
private const val MaxAudioSeconds = 10

/**
 * The audio profile card. Shows a microphone (no audio yet) or play/pause
 * (recorded/saved), a real amplitude waveform (flat line when empty), and the
 * remaining/total seconds. Owner-only review actions (Guardar/Descartar) and
 * permission messaging render beneath the card.
 */
@Composable
fun ProfileAudioCard(
    audio: AudioUiState,
    isOwner: Boolean,
    playbackProgress: Float,
    onMicTap: () -> Unit,
    onStopTap: () -> Unit,
    onPlayPauseTap: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onReRecord: () -> Unit,
    micPermissionMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .clip(CardCorner)
                .background(Color.White.copy(alpha = 0.04f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), CardCorner)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioPrimaryButton(
                audio = audio,
                isOwner = isOwner,
                onMicTap = onMicTap,
                onStopTap = onStopTap,
                onPlayPauseTap = onPlayPauseTap,
            )
            ProfileWaveformBars(
                bars = waveformFor(audio),
                progress = progressFor(audio, playbackProgress),
                activeColor = AppColors.ProfileTextPrimary,
                inactiveColor = AppColors.ProfileTextPrimary.copy(alpha = 0.22f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${secondsLabelFor(audio)}s",
                color = AppColors.ProfileTextPrimary.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        when (audio) {
            is AudioUiState.RecordedPreview -> AudioReviewActions(onSave = onSave, onDiscard = onDiscard)
            is AudioUiState.Error -> AudioHelperText(audio.message, AppColors.ErrorWarmSoft)
            is AudioUiState.Saved -> if (isOwner) ReRecordButton(onReRecord)
            else -> Unit
        }

        micPermissionMessage?.let { AudioHelperText(it, AppColors.Warning) }
    }
}

@Composable
private fun AudioPrimaryButton(
    audio: AudioUiState,
    isOwner: Boolean,
    onMicTap: () -> Unit,
    onStopTap: () -> Unit,
    onPlayPauseTap: () -> Unit,
) {
    val isPlaying = (audio as? AudioUiState.RecordedPreview)?.isPlaying == true ||
        (audio as? AudioUiState.Saved)?.isPlaying == true

    val (icon, action, background, enabled, label) = when (audio) {
        is AudioUiState.Empty -> AudioButtonSpec(
            Icons.Filled.Mic, onMicTap, AppColors.ProfileTextPrimary, isOwner, "Grabar audio",
        )
        is AudioUiState.Recording -> AudioButtonSpec(
            Icons.Filled.Stop, onStopTap, AppColors.RecordingRed, true, "Detener grabación",
        )
        is AudioUiState.RecordedPreview, is AudioUiState.Saved -> AudioButtonSpec(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            onPlayPauseTap, AppColors.ProfileTextPrimary, true,
            if (isPlaying) "Pausar audio" else "Reproducir audio",
        )
        is AudioUiState.Saving -> AudioButtonSpec(
            Icons.Filled.PlayArrow, {}, AppColors.ProfileTextPrimary, false, "Guardando audio",
        )
        is AudioUiState.Error -> AudioButtonSpec(
            Icons.Filled.Mic, onMicTap, AppColors.ProfileTextPrimary, isOwner, "Reintentar grabación",
        )
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) background else background.copy(alpha = 0.3f))
            .clickable(enabled = enabled, role = Role.Button, onClick = action)
            .semanticsLabel(label),
        contentAlignment = Alignment.Center,
    ) {
        if (audio is AudioUiState.Saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AppColors.ProfileCanvas,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.ProfileCanvas,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class AudioButtonSpec(
    val icon: ImageVector,
    val action: () -> Unit,
    val background: Color,
    val enabled: Boolean,
    val label: String,
)

@Composable
private fun AudioReviewActions(onSave: () -> Unit, onDiscard: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AudioActionChip(
            icon = Icons.Filled.Check,
            label = "Guardar",
            primary = true,
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
        AudioActionChip(
            icon = Icons.Filled.Close,
            label = "Descartar",
            primary = false,
            onClick = onDiscard,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AudioActionChip(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (primary) AppColors.ProfileTextPrimary else Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) AppColors.ProfileCanvas else AppColors.ProfileTextPrimary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (primary) AppColors.ProfileCanvas else AppColors.ProfileTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReRecordButton(onReRecord: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onReRecord)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = AppColors.ProfileTextPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "Regrabar",
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AudioHelperText(message: String, color: Color) {
    Text(text = message, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

/** Donation/impact bar — "0/10K€" with a thin progress track and a bear glyph. */
@Composable
fun ProfileDonationBar(donation: DonationProgress, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardCorner)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), CardCorner)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ProfileFormatting.donationLabel(donation),
                    color = AppColors.ProfileTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "DONADO",
                    color = AppColors.ProfileTextPrimary.copy(alpha = 0.22f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.ProfileTextPrimary.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(donation.fraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AppColors.ProfileTextPrimary.copy(alpha = 0.55f),
                                    AppColors.ProfileTextPrimary.copy(alpha = 0.95f),
                                ),
                            ),
                        ),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BearGlyph(modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Right-hand stack of three custom-link slots.
 * - Owner: every slot shows either a "+" (empty) or the saved icon (with an edit
 *   badge); tapping an empty slot adds, a saved slot opens its URL.
 * - Other users: only saved slots render; if none, the column is absent.
 */
@Composable
fun ProfileCustomLinksColumn(
    profile: MindOSProfile,
    isOwner: Boolean,
    onAdd: (slotIndex: Int) -> Unit,
    onEdit: (slotIndex: Int) -> Unit,
    onOpenUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = if (isOwner) profile.customLinks else profile.savedCustomLinks
    if (slots.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        slots.forEach { slot ->
            CustomLinkTile(
                slot = slot,
                isOwner = isOwner,
                onAdd = { onAdd(slot.slotIndex) },
                onEdit = { onEdit(slot.slotIndex) },
                onOpenUrl = { slot.url?.let(onOpenUrl) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun CustomLinkTile(
    slot: ProfileCustomLink,
    isOwner: Boolean,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onOpenUrl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(role = Role.Button, onClick = if (slot.isEmpty) onAdd else onOpenUrl)
            .semanticsLabel(if (slot.isEmpty) "Añadir enlace" else "Abrir enlace ${slot.url}"),
        contentAlignment = Alignment.Center,
    ) {
        if (slot.isEmpty) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = AppColors.ProfileTextPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        } else {
            val iconSource = customLinkIconSource(slot)
            if (iconSource != null) {
                ProfilePlatformMediaPreview(
                    source = iconSource,
                    modifier = Modifier.fillMaxSize().clip(shape),
                    contentScale = ContentScale.Crop,
                ) { LinkInitialFallback(slot.url) }
            } else {
                LinkInitialFallback(slot.url)
            }
            if (isOwner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(role = Role.Button, onClick = onEdit)
                        .semanticsLabel("Editar enlace"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = AppColors.ComposerBlue,
                        modifier = Modifier.size(9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkInitialFallback(url: String?) {
    val initial = url?.substringAfter("://")?.firstOrNull()?.uppercase() ?: "?"
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            color = AppColors.ProfileTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun customLinkIconSource(slot: ProfileCustomLink): ProfileMediaRenderSource? {
    if (slot.iconReference.isNullOrBlank() && (slot.iconLocalBytes == null || slot.iconLocalBytes.isEmpty())) {
        return null
    }
    return ProfileMediaRenderSource(
        storagePath = slot.iconReference,
        thumbnailStoragePath = null,
        pendingPreviewBytes = slot.iconLocalBytes,
        isVideo = false,
        contentDescription = "Icono de enlace",
        directUrl = slot.iconReference,
        thumbnailUrl = null,
    )
}

/**
 * Publications grid — purely visual placeholders (no posts system). Four columns
 * with the bottom rows reading as "continues below", matching the design.
 */
@Composable
fun ProfilePublicationsGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(PublicationRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(PublicationColumns) { col ->
                    PublicationCell(tone = col, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PublicationCell(tone: Int, modifier: Modifier = Modifier) {
    val tones = listOf(
        listOf(Color(0xFF1A1A1C), Color(0xFF0E0E10)),
        listOf(Color(0xFF161618), Color(0xFF0C0C0E)),
        listOf(Color(0xFF1C1A18), Color(0xFF0D0C0B)),
        listOf(Color(0xFF181A1C), Color(0xFF0B0C0D)),
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(tones[tone % tones.size]))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
    )
}

/** Transient error/info banner shown above the body content. */
@Composable
fun ProfileTransientBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.ErrorWarmSoft.copy(alpha = 0.1f))
            .border(0.5.dp, AppColors.ErrorWarmSoft.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = AppColors.ErrorWarmSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Audio display helpers (pure) ──────────────────────────────────────────────

private fun waveformFor(audio: AudioUiState): List<Float> = when (audio) {
    is AudioUiState.Recording -> audio.liveWaveform
    is AudioUiState.RecordedPreview -> audio.waveform
    is AudioUiState.Saved -> audio.waveform
    else -> emptyList()
}

private fun progressFor(audio: AudioUiState, playbackProgress: Float): Float = when (audio) {
    is AudioUiState.Recording -> 1f
    is AudioUiState.RecordedPreview -> if (audio.isPlaying) playbackProgress else 0f
    is AudioUiState.Saved -> if (audio.isPlaying) playbackProgress else 0f
    else -> 0f
}

private fun secondsLabelFor(audio: AudioUiState): Int = when (audio) {
    is AudioUiState.Empty -> MaxAudioSeconds
    is AudioUiState.Recording ->
        AudioWaveform.remainingSeconds(MindOSProfileStateHolder.MaxAudioMillis, audio.elapsedMillis)
    is AudioUiState.RecordedPreview -> AudioWaveform.durationSecondsLabel(audio.durationMillis, MaxAudioSeconds)
    is AudioUiState.Saved -> AudioWaveform.durationSecondsLabel(audio.durationMillis, MaxAudioSeconds)
    is AudioUiState.Saving -> MaxAudioSeconds
    is AudioUiState.Error -> MaxAudioSeconds
}

private const val PublicationRows = 2
private const val PublicationColumns = 4
