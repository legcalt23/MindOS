package com.example.mindos.features.social.presentation.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.mindos.design.AppColors
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.media.FileKind
import com.example.mindos.core.media.rememberPlatformFilePicker
import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.features.dm.presentation.media.DMAudioRecordingStartResult
import com.example.mindos.features.dm.presentation.media.DMAudioRecordingStopResult
import com.example.mindos.features.dm.presentation.media.rememberDMAudioRecorder
import com.example.mindos.features.social.data.feed.rememberGlobalFeedRepository
import com.example.mindos.features.social.domain.feed.GlobalFeedPost
import com.example.mindos.features.social.domain.feed.GlobalFeedMaxDescriptionLength
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository
import com.example.mindos.features.social.domain.feed.GlobalFeedTextAlign
import com.example.mindos.features.social.domain.feed.GlobalFeedTextBackgroundStyle
import com.example.mindos.features.social.domain.feed.GlobalFeedTextStyle
import com.example.mindos.features.social.domain.feed.globalFeedCodePointLength
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

@Composable
fun GlobalFeedComposerScreen(
    user: AuthenticatedUser,
    onBack: () -> Unit,
    onPublished: (GlobalFeedPost) -> Unit,
    repository: GlobalFeedRepository = rememberGlobalFeedRepository(),
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val holder = rememberGlobalFeedComposerStateHolder(user, repository, scope)
    val state = holder.uiState
    var showDescriptionEditor by rememberSaveable { mutableStateOf(false) }
    val picker = rememberPlatformFilePicker(
        onPicked = { file ->
            when (state.selectedMode) {
                ComposerMode.PHOTO -> holder.onSelectPhoto(file)
                ComposerMode.VIDEO -> holder.onSelectVideo(file)
                else -> Unit
            }
        },
        onError = holder::onPickerError,
    )
    val recorder = rememberDMAudioRecorder()
    val player = rememberFeedAudioPlayer()
    val playerState by player.state.collectAsStateCompat()

    LaunchedEffect(state.isRecording) {
        if (!state.isRecording) return@LaunchedEffect
        val startedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        while (state.isRecording) {
            holder.onRecordingDurationChanged(kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startedAt)
            delay(80L)
        }
    }
    LaunchedEffect(playerState) {
        playerState.activeAudioId?.let { holder.onPlayPauseAudio(it, playerState.forAudio(it)) }
    }

    Box(
        modifier = modifier.fillMaxSize().background(ComposerBlack).pointerInput(state.selectedMode) {
            detectHorizontalDragGestures { _, drag ->
                if (abs(drag) < 16f) return@detectHorizontalDragGestures
                if (drag < 0f) holder.onModeSwipeNext() else holder.onModeSwipePrevious()
            }
        },
    ) {
        ComposerAtmosphere()
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SmallAction("HOME", onBack)
                Text("MindOS CREATOR", color = ComposerWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                SmallAction("RESET", holder::onResetComposer)
            }
            Spacer(modifier = Modifier.height(18.dp))
            ModeStrip(state.selectedMode)
            Spacer(modifier = Modifier.height(18.dp))
            AnimatedContent(targetState = state.selectedMode, label = "mode") { mode ->
                when (mode) {
                    ComposerMode.PHOTO -> MediaModePanel("PHOTO", AppColors.ComposerTeal, { picker.pick(FileKind.Photo) }) {
                        state.selectedPhoto?.let { MediaPreview(it.previewBytes, false, it.attachment.fileName, "PHOTO · ${formatBytes(it.attachment.sizeBytes)}") }
                    }
                    ComposerMode.VIDEO -> MediaModePanel("VIDEO", AppColors.ComposerBlue, { picker.pick(FileKind.Video) }) {
                        state.selectedVideo?.let { MediaPreview(it.previewBytes, true, it.attachment.fileName, "VIDEO · ${formatDuration(it.durationMs)}") }
                    }
                    ComposerMode.TEXT -> TextPanel(state, holder)
                    ComposerMode.AUDIO -> AudioPanel(state, holder, recorder, player, playerState)
                }
            }
            AnimatedVisibility(visible = state.errorMessage != null || state.feedbackMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = state.errorMessage ?: state.feedbackMessage.orEmpty(),
                    color = if (state.errorMessage != null) ComposerError else ComposerMist,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            PublishBar(enabled = state.canPublish, isBusy = state.isPublishing) { holder.onPublishClick(onPublished) }
        }
        DescriptionFloatingButton(
            onClick = { showDescriptionEditor = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp),
        )
        if (showDescriptionEditor) {
            PostDescriptionEditorSheet(
                description = state.description,
                maxChars = GlobalFeedMaxDescriptionLength,
                onDescriptionChange = holder::onDescriptionChanged,
                onDismiss = { showDescriptionEditor = false },
                onSave = { showDescriptionEditor = false },
            )
        }
    }
}

@Composable private fun MediaModePanel(title: String, accent: Color, onPick: () -> Unit, content: @Composable () -> Unit) {
    Panel {
        Text(title, color = ComposerWhite, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
        Spacer(Modifier.height(14.dp))
        OrbButton(label = "+", accent = accent, onTap = onPick)
        Spacer(Modifier.height(16.dp))
        ActionChip("ABRIR", accent, onPick)
        content()
    }
}

@Composable private fun TextPanel(state: GlobalFeedComposerUiState, holder: GlobalFeedComposerStateHolder) {
    Panel {
        Text("TEXT", color = ComposerWhite, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
        Spacer(Modifier.height(12.dp))
        TextPreview(state.textContent, state.textStyleConfig)
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(16.dp)) {
            BasicTextField(
                value = state.textContent,
                onValueChange = holder::onTextChanged,
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ComposerWhite, lineHeight = 24.sp),
                cursorBrush = Brush.verticalGradient(listOf(AppColors.ComposerTeal, AppColors.ComposerBlue)),
                decorationBox = { inner -> if (state.textContent.isBlank()) Text("Escribe tu inspiración.", color = ComposerMist); inner() },
            )
        }
        Spacer(Modifier.height(12.dp))
        ControlRow("PRESETS", GlobalFeedTextBackgroundStyle.entries.map { it.name }, state.textStyleConfig.backgroundStyle.name) {
            holder.onApplyTextPreset(GlobalFeedTextBackgroundStyle.valueOf(it))
        }
        Spacer(Modifier.height(10.dp))
        ControlRow("ALINEAR", listOf("IZQ", "CENTRO", "DER"), when (state.textStyleConfig.textAlign) { GlobalFeedTextAlign.Start -> "LEFT"; GlobalFeedTextAlign.Center -> "CENTER"; GlobalFeedTextAlign.End -> "RIGHT" }) { alignment ->
            holder.onTextStyleChanged { current ->
                current.copy(textAlign = when (alignment) { "LEFT" -> GlobalFeedTextAlign.Start; "CENTER" -> GlobalFeedTextAlign.Center; else -> GlobalFeedTextAlign.End })
            }
        }
        NeonSlider("TAMAÑO", state.textStyleConfig.fontSizeSp, 18f..42f) { value -> holder.onTextStyleChanged { it.copy(fontSizeSp = value) } }
        NeonSlider("BRILLO", state.textStyleConfig.glowIntensity, 0f..1.2f) { value -> holder.onTextStyleChanged { it.copy(glowIntensity = value) } }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            ActionChip("NEGRITA", AppColors.ComposerTeal, { holder.onTextStyleChanged { it.copy(fontWeight = if (it.fontWeight >= 700) 500 else 800) } }, compact = true)
            ActionChip("CURSIVA", AppColors.ComposerTeal, { holder.onTextStyleChanged { it.copy(italic = !it.italic) } }, compact = true)
            ActionChip("RESETEAR", Color.White, holder::onResetTextStyle, compact = true)
        }
    }
}

@Composable private fun AudioPanel(state: GlobalFeedComposerUiState, holder: GlobalFeedComposerStateHolder, recorder: com.example.mindos.features.dm.presentation.media.DMAudioRecorder, player: FeedAudioPlayer, playerState: FeedAudioPlayerState) {
    Panel {
        Text("AUDIO", color = ComposerWhite, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
        Spacer(Modifier.height(12.dp))
        if (state.isRecording) {
            RecordingSurface(state.recordingDurationMs)
            Spacer(Modifier.height(12.dp))
            ActionChip("STOP", ComposerError, {
                holder.onStopAudioRecording()
                recorder.stopRecording { result -> if (result is DMAudioRecordingStopResult.Success) holder.onAudioRecorded(result.recording) else if (result is DMAudioRecordingStopResult.Failure) holder.onPickerError(result.message) }
            })
        } else {
            OrbButton(label = "\uD83C\uDFA4", accent = AppColors.ComposerAudioCyan, onTap = {
                holder.onStartAudioRecording()
                recorder.startRecording { result -> if (result is DMAudioRecordingStartResult.PermissionDenied) holder.onPickerError("Permiso de micrófono denegado.") else if (result is DMAudioRecordingStartResult.Failure) holder.onPickerError(result.message) }
            }, onLongTap = {
                holder.onAudioLongPressStart()
                recorder.startRecording { result -> if (result is DMAudioRecordingStartResult.PermissionDenied) holder.onPickerError("Permiso de micrófono denegado.") else if (result is DMAudioRecordingStartResult.Failure) holder.onPickerError(result.message) }
            })
        }
        state.audioRecordings.forEach { clip ->
            Spacer(Modifier.height(10.dp))
            AudioCapsule(
                clip = clip,
                playback = state.playbackStateByAudioId[clip.id] ?: FeedAudioPlaybackUiState(durationMs = clip.durationMs),
                onPlayPause = { player.playOrPause(FeedAudioSource(clip.id, clip.localPath, null, clip.attachment.mimeType, clip.durationMs)) },
                onDelete = { if (playerState.activeAudioId == clip.id) player.stop(); holder.onDeleteAudio(clip.id) },
            )
        }
    }
}

@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.05f)).border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(30.dp)).padding(20.dp), content = content)
}

@Composable private fun ModeStrip(selected: ComposerMode) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(ComposerMode.PHOTO, ComposerMode.VIDEO, ComposerMode.TEXT, ComposerMode.AUDIO).forEachIndexed { index, mode ->
            val active = mode == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(if (active) modeAccent(mode) else Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = if (active) ComposerBlack else ComposerMist, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                }
                Spacer(Modifier.height(8.dp))
                if (active) Text(modeGlyph(mode), color = ComposerWhite, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)) else Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable private fun MediaPreview(bytes: ByteArray?, isVideo: Boolean, title: String, subtitle: String) {
    if (bytes == null) return
    Spacer(Modifier.height(16.dp))
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(14.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp))) {
            ProfilePlatformMediaPreview(ProfileMediaRenderSource(null, null, bytes, isVideo, title), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) { }
        }
        Text(title, color = ComposerWhite, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 10.dp))
        Text(subtitle, color = ComposerMist, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable private fun TextPreview(text: String, style: GlobalFeedTextStyle) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(style.cornerRadiusDp.dp)).background(textBrush(style)).border(BorderStroke(style.borderWidthDp.dp, Color(style.borderColorArgb)), RoundedCornerShape(style.cornerRadiusDp.dp)).padding(style.paddingDp.dp)) {
        Text(
            text = text.ifBlank { "Preview compacto MindOS." },
            color = Color(style.textColorArgb),
            textAlign = when (style.textAlign) { GlobalFeedTextAlign.Start -> TextAlign.Start; GlobalFeedTextAlign.Center -> TextAlign.Center; GlobalFeedTextAlign.End -> TextAlign.End },
            style = TextStyle(fontSize = style.fontSizeSp.sp, fontWeight = FontWeight(style.fontWeight), fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal, lineHeight = (style.fontSizeSp * style.lineHeightEm).sp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable private fun RecordingSurface(durationMs: Long) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(16.dp)) {
        Text("Grabando audio", color = ComposerWhite, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black))
        Text(formatDuration(durationMs), color = ComposerMist, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(10.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) { drawWaveform(fakeWaveform("recording", durationMs), 0.9f) }
    }
}

@Composable private fun AudioCapsule(clip: ComposerAudioClip, playback: FeedAudioPlaybackUiState, onPlayPause: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onPlayPause), contentAlignment = Alignment.Center) {
            Icon(if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = ComposerWhite, modifier = Modifier.size(18.dp))
        }
        Canvas(modifier = Modifier.weight(1f).height(42.dp)) { drawWaveform(clip.waveform, playback.progress.coerceIn(0f, 1f)) }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDuration(if (playback.durationMs > 0L) playback.durationMs else clip.durationMs), color = ComposerWhite, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
            Text("DEL", color = ComposerMist, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onDelete))
        }
    }
}

@Composable private fun PublishBar(enabled: Boolean, isBusy: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.Center) {
        Row(modifier = Modifier.shadow(18.dp, RoundedCornerShape(999.dp)).clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(listOf(AppColors.ComposerTeal.copy(alpha = if (enabled) 0.24f else 0.08f), AppColors.ComposerBlue.copy(alpha = if (enabled) 0.24f else 0.08f)))).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.ArrowUpward, null, tint = if (enabled) ComposerWhite else ComposerMist)
            Text(if (isBusy) "PUBLICANDO" else "PUBLICAR", color = if (enabled) ComposerWhite else ComposerMist, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun DescriptionFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(18.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = "Descripción",
            tint = ComposerWhite,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PostDescriptionEditorSheet(
    description: String,
    maxChars: Int,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val currentCount = description.globalFeedCodePointLength()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF081110).copy(alpha = 0.96f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(30.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Descripción",
                    color = ComposerWhite,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                )
                Text(
                    text = "$currentCount/$maxChars",
                    color = if (currentCount >= maxChars) Color(0xFFFFB7B7) else ComposerMist,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 168.dp, max = 320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.26f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
                    .padding(16.dp),
            ) {
                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 136.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ComposerWhite, lineHeight = 24.sp),
                    cursorBrush = Brush.verticalGradient(listOf(AppColors.ComposerTeal, AppColors.ComposerBlue)),
                    decorationBox = { inner ->
                        if (description.isBlank()) {
                            Text("Añade una descripción opcional.", color = ComposerMist)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                ActionChip("CERRAR", Color.White, onDismiss, compact = true)
                ActionChip("GUARDAR", AppColors.ComposerTeal, onSave, compact = true)
            }
        }
    }
}

@Composable private fun ControlRow(title: String, items: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column {
        Text(title, color = ComposerMist, style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item -> ActionChip(item, if (item == selected) AppColors.ComposerTeal else Color.White, { onSelected(item) }, compact = true) }
        }
    }
}

@Composable private fun NeonSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = ComposerMist, style = MaterialTheme.typography.labelSmall)
            Text(value.toInt().toString(), color = ComposerWhite, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = AppColors.ComposerTeal, activeTrackColor = AppColors.ComposerTeal, inactiveTrackColor = Color.White.copy(alpha = 0.08f)))
    }
}

@Composable private fun ActionChip(text: String, accent: Color, onClick: () -> Unit, compact: Boolean = false) {
    Text(text = text, color = ComposerWhite, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(accent.copy(alpha = if (compact) 0.10f else 0.16f)).border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)), RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp))
}

@Composable private fun SmallAction(text: String, onClick: () -> Unit) {
    Text(text = text, color = ComposerWhite, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable private fun OrbButton(label: String, accent: Color, onTap: () -> Unit, onLongTap: (() -> Unit)? = null) {
    Box(modifier = Modifier.size(188.dp).shadow(28.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), Color.Black.copy(alpha = 0.14f)))).border(BorderStroke(1.dp, accent.copy(alpha = 0.36f)), CircleShape).combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap, onLongClick = { onLongTap?.invoke() }), contentAlignment = Alignment.Center) {
        Text(label, color = ComposerWhite, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, fontSize = 62.sp))
    }
}

@Composable
private fun StateFlow<FeedAudioPlayerState>.collectAsStateCompat(): State<FeedAudioPlayerState> = collectAsState(initial = value)

private fun DrawScope.drawWaveform(values: List<Float>, progress: Float) {
    if (values.isEmpty()) return
    val step = size.width / (values.size - 1).coerceAtLeast(1)
    val centerY = size.height / 2f
    val left = Path()
    val right = Path()
    values.forEachIndexed { index, value ->
        val x = step * index
        val y = centerY - (value * size.height * 0.42f)
        val path = if (x <= size.width * progress) left else right
        if (index == 0 || path.isEmpty) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (!right.isEmpty) drawPath(right, Color.White.copy(alpha = 0.16f), style = Stroke(width = 4f, cap = StrokeCap.Round))
    if (!left.isEmpty) drawPath(left, AppColors.ComposerAudioCyan, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
}

private fun fakeWaveform(seed: String, durationMs: Long): List<Float> = List(56) { index ->
    val phase = index / 56f
    val base = kotlin.math.sin((phase * 10f + seed.hashCode() * 0.0001f + durationMs * 0.0002f).toDouble()).toFloat()
    (0.25f + kotlin.math.abs(base) * 0.55f).coerceIn(0.12f, 0.9f)
}

private fun textBrush(style: GlobalFeedTextStyle): Brush = Brush.linearGradient(listOf(Color(style.backgroundPrimaryArgb), Color(style.backgroundSecondaryArgb)))
private fun modeGlyph(mode: ComposerMode): String = when (mode) { ComposerMode.PHOTO -> "◉"; ComposerMode.VIDEO -> "▶"; ComposerMode.TEXT -> "Aa"; ComposerMode.AUDIO -> "🎙" }
private fun modeAccent(mode: ComposerMode): Color = when (mode) { ComposerMode.PHOTO -> AppColors.ComposerTeal; ComposerMode.VIDEO -> AppColors.ComposerBlue; ComposerMode.TEXT -> AppColors.ComposerTextGold; ComposerMode.AUDIO -> AppColors.ComposerAudioCyan }
private fun formatDuration(durationMs: Long?): String { val total = ((durationMs ?: 0L) / 1000L).toInt(); return "%02d:%02d".format(total / 60, total % 60) }
private fun formatBytes(bytes: Long): String = when { bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"; bytes >= 1024L -> "${bytes / 1024L} KB"; else -> "$bytes B" }

@Composable private fun ComposerAtmosphere() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(ComposerBlack)
        drawCircle(brush = Brush.radialGradient(listOf(AppColors.ComposerTeal.copy(alpha = 0.15f), Color.Transparent), center = Offset(size.width * 0.18f, size.height * 0.16f), radius = size.minDimension * 0.72f))
        drawCircle(brush = Brush.radialGradient(listOf(AppColors.ComposerBlue.copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * 0.82f, size.height * 0.76f), radius = size.minDimension * 0.66f))
    }
}

private val ComposerBlack = AppColors.DeepBlack
private val ComposerWhite = AppColors.OnBackground
private val ComposerMist = AppColors.Mist
private val ComposerError = AppColors.Error
