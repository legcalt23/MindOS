package com.example.mindos.features.dm.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.mindos.design.AppColors
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.security.SafeInput
import com.example.mindos.core.media.FileKind
import com.example.mindos.core.media.rememberPlatformFilePicker
import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.DMMessageType
import com.example.mindos.features.dm.domain.model.MessageStatus
import com.example.mindos.features.dm.presentation.media.DMAudioRecorderState
import com.example.mindos.features.dm.presentation.media.DMAudioRecordingStartResult
import com.example.mindos.features.dm.presentation.media.DMAudioRecordingStopResult
import com.example.mindos.features.dm.presentation.media.rememberDMAudioRecorder
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.presentation.media.AppMediaThumbnail
import com.example.mindos.features.social.shared.util.MediaContentType
import com.example.mindos.features.social.presentation.feed.FeedAudioPlayer
import com.example.mindos.features.social.presentation.feed.FeedAudioSource
import com.example.mindos.features.social.presentation.feed.rememberFeedAudioPlayer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class AudioCaptureUiState {
    Stopped,
    Recording,
    Sent,
    Error,
}

@Composable
fun DMChatScreen(
    user: AuthenticatedUser,
    chatId: String?,
    otherUserId: String?,
    stateHolder: DMStateHolder,
    onBack: () -> Unit,
    onOpenUserInfo: () -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by stateHolder.currentMessages.collectAsState()
    val resolvedChatId by stateHolder.resolvedChatId.collectAsState()
    val availableUsers by stateHolder.availableUsers.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val audioRecorder = rememberDMAudioRecorder()
    val recorderState by audioRecorder.state.collectAsState()
    val audioPlayer = rememberFeedAudioPlayer()
    val isRecording = recorderState == DMAudioRecorderState.Recording
    var audioCaptureState by remember { mutableStateOf(AudioCaptureUiState.Stopped) }
    var audioCaptureMessage by remember { mutableStateOf<String?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId, otherUserId) {
        stateHolder.resolveAndLoadChat(user.uid, otherUserId, chatId)
    }

    LaunchedEffect(audioCaptureState) {
        if (audioCaptureState == AudioCaptureUiState.Sent || audioCaptureState == AudioCaptureUiState.Error) {
            delay(1600)
            audioCaptureState = AudioCaptureUiState.Stopped
            audioCaptureMessage = null
        }
    }

    LaunchedEffect(attachmentError) {
        if (attachmentError != null) {
            delay(2200)
            attachmentError = null
        }
    }
    
    val mediaPicker = rememberPlatformFilePicker(
        onPicked = { file ->
            val chat = resolvedChatId ?: return@rememberPlatformFilePicker
            attachmentError = null
            stateHolder.sendMessage(
                chatId = chat,
                senderId = user.uid,
                file = file,
                type = when (file.mimeType.substringBefore("/")) {
                    "image" -> DMMessageType.Image
                    "video" -> DMMessageType.Video
                    "audio" -> DMMessageType.Audio
                    else -> DMMessageType.File
                }
            )
        },
        onError = { attachmentError = it }
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.DeepBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val w = maxWidth
        val h = maxHeight
        
        val unitW = maxWidth.value / 900f
        val unitH = maxHeight.value / 1760f
        val unitSp = maxWidth.value / 900f
        
        fun Float.ux() = (this * unitW).dp
        fun Float.uy() = (this * unitH).dp
        fun Float.usp() = (this * unitSp).sp
        fun Int.ux() = (this.toFloat() * unitW).dp
        fun Int.uy() = (this.toFloat() * unitH).dp
        fun Int.usp() = (this.toFloat() * unitSp).sp
        fun Double.ux() = (this.toFloat() * unitW).dp
        fun Double.uy() = (this.toFloat() * unitH).dp
        fun Double.usp() = (this.toFloat() * unitSp).sp

        val partner = availableUsers.find { it.userId == otherUserId }
        val partnerTitle = partner?.username?.takeIf { it.isNotBlank() } ?: "Usuario"

        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER ---
            Box(modifier = Modifier.fillMaxWidth().height(168.5.uy())) {
                // Línea inferior
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                // Zona atrás: línea vertical derecha
                Box(
                    modifier = Modifier
                        .offset(x = 158.5.ux())
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                // Icono flecha
                Canvas(
                    modifier = Modifier
                        .offset(x = 45.ux(), y = 50.uy())
                        .size(70.ux(), 70.uy())
                        .clickable { onBack() }
                ) {
                    val path = Path().apply {
                        moveTo(size.width * 0.8f, 0f)
                        lineTo(size.width * 0.2f, size.height * 0.5f)
                        lineTo(size.width * 0.8f, size.height)
                    }
                    drawPath(path, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                
                // Avatar
                Box(
                    modifier = Modifier
                        .offset(x = 185.ux(), y = 30.uy())
                        .size(110.ux(), 110.uy())
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable { onOpenUserInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(partnerTitle.take(1).uppercase(), color = AppColors.NeuGreen, fontWeight = FontWeight.Bold, fontSize = 24.usp())
                }
                
                // Nombre
                Text(
                    text = partnerTitle,
                    modifier = Modifier
                        .offset(x = 335.ux(), y = 55.uy())
                        .size(500.ux(), 80.uy())
                        .clickable { onOpenUserInfo() },
                    fontSize = 42.usp(),
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    maxLines = 1
                )
            }

            // --- ÁREA DE CONVERSACIÓN ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(16.ux())
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    val isMe = msg.senderId == user.uid
                    ChatBubble(msg, isMe, audioPlayer, onOpenMedia, unitW, unitH)
                }
            }

            // ATTACHMENT MENU
            if (showAttachmentMenu) {
                AttachmentMenu(
                    onOptionSelected = { kind ->
                        showAttachmentMenu = false
                        mediaPicker.pick(kind)
                    }
                )
            }

            // --- BARRA INFERIOR ---
            Box(modifier = Modifier.fillMaxWidth().height((1760f - 1558.5f).uy())) {
                // Línea divisoria
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.ux()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cámara
                    Box(
                        modifier = Modifier
                            .size(110.ux(), 110.uy())
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { showAttachmentMenu = !showAttachmentMenu },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(48.ux(), 48.uy())) {
                            drawRoundRect(Color.White, topLeft = Offset(size.width * 0.1f, size.height * 0.25f), size = Size(size.width * 0.8f, size.height * 0.5f), cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                            drawCircle(Color.White, radius = size.width * 0.15f, center = Offset(size.width / 2, size.height * 0.5f), style = Stroke(1.5.dp.toPx()))
                            drawRect(Color.White, topLeft = Offset(size.width * 0.3f, size.height * 0.15f), size = Size(size.width * 0.4f, size.height * 0.1f), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(25.ux()))

                    // Input
                    val currentChatId = resolvedChatId
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = SafeInput.normalizeMultilineHumanText(it, 4_000) },
                        modifier = Modifier.weight(1f).height(90.uy()),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = { Text("Escribe algo", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Light)
                    )

                    Spacer(modifier = Modifier.width(25.ux()))

                    // Micrófono / Enviar
                    Box(
                        modifier = Modifier
                            .size(110.ux(), 110.uy())
                            .border(1.5.dp, if (isRecording) Color.Red else Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    if (currentChatId != null) {
                                        stateHolder.sendMessage(currentChatId, user.uid, DMMessageType.Text, text = inputText)
                                        inputText = ""
                                    }
                                } else {
                                    // Logic for mic... (simplified for brevity, keeping existing recorder logic)
                                    if (currentChatId != null) {
                                        if (isRecording) {
                                            audioRecorder.stopRecording { result ->
                                                audioCaptureState = AudioCaptureUiState.Stopped
                                                if (result is DMAudioRecordingStopResult.Success) {
                                                    stateHolder.sendMessage(currentChatId, user.uid, type = DMMessageType.Audio, mediaBytes = result.recording.bytes, mimeType = result.recording.mimeType, fileName = result.recording.fileName, fileSizeBytes = result.recording.sizeBytes, durationMs = result.recording.durationMs)
                                                }
                                            }
                                        } else {
                                            audioRecorder.startRecording { result -> if (result is DMAudioRecordingStartResult.Started) audioCaptureState = AudioCaptureUiState.Recording }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (inputText.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = AppColors.NeuGreen, modifier = Modifier.size(32.ux()))
                        } else {
                            Canvas(modifier = Modifier.size(52.ux(), 52.uy())) {
                                drawRoundRect(Color.White, topLeft = Offset(size.width * 0.35f, size.height * 0.1f), size = Size(size.width * 0.3f, size.height * 0.4f), cornerRadius = CornerRadius(size.width * 0.15f), style = Stroke(1.5.dp.toPx()))
                                val path = Path().apply {
                                    moveTo(size.width * 0.2f, size.height * 0.4f)
                                    arcTo(Rect(size.width * 0.2f, size.height * 0.2f, size.width * 0.8f, size.height * 0.7f), 0f, 180f, false)
                                }
                                drawPath(path, Color.White, style = Stroke(1.5.dp.toPx()))
                                drawLine(Color.White, start = Offset(size.width / 2, size.height * 0.7f), end = Offset(size.width / 2, size.height * 0.9f), strokeWidth = 1.5.dp.toPx())
                                drawLine(Color.White, start = Offset(size.width * 0.3f, size.height * 0.9f), end = Offset(size.width * 0.7f, size.height * 0.9f), strokeWidth = 1.5.dp.toPx())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: DMMessage,
    isMe: Boolean,
    audioPlayer: FeedAudioPlayer,
    onOpenMedia: (AppMediaContent) -> Unit,
    unitW: Float,
    unitH: Float
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isMe) {
                // Indicador circular de la referencia
                Box(modifier = Modifier.size((24 * unitW).dp).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape))
                Spacer(modifier = Modifier.width((16 * unitW).dp))
            }
            
            Column(
                modifier = Modifier
                    .widthIn(max = (560 * unitW).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isMe) AppColors.NeuGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    .padding(if (msg.type == DMMessageType.Text) 14.dp else 4.dp)
            ) {
                when (msg.type) {
                    DMMessageType.Text -> {
                        Text(msg.text ?: "", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Light)
                    }
                    DMMessageType.Image, DMMessageType.Video -> {
                        MediaBubble(msg, onOpenMedia)
                    }
                    DMMessageType.Audio -> {
                        AudioBubble(msg, audioPlayer, isMe)
                    }
                    DMMessageType.File -> {
                        FileBubble(msg, onOpenMedia)
                    }
                }
                Text(
                    formatMessageTime(msg.createdAt),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MediaBubble(msg: DMMessage, onOpenMedia: (AppMediaContent) -> Unit) {
    val mediaContent = remember(msg) { msg.toAppMediaContent() }
    val canOpen = mediaContent != null && msg.status != MessageStatus.Failed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (msg.width != null && msg.height != null) msg.width.toFloat() / msg.height.toFloat() else 1.33f)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(enabled = canOpen) {
                mediaContent?.let(onOpenMedia)
            },
        contentAlignment = Alignment.Center
    ) {
        if (mediaContent != null) {
            AppMediaThumbnail(
                content = mediaContent,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = {
                    MediaBubbleFallbackIcon(msg.type)
                },
            )
        } else {
            MediaBubbleFallbackIcon(msg.type)
        }

        if (msg.status == MessageStatus.Sending) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("Enviando", color = Color.White, fontSize = 10.sp)
            }
        }

        if (msg.type == DMMessageType.Video) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            
            msg.durationMs?.let { duration ->
                Text(
                    text = formatDuration(duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaBubbleFallbackIcon(type: DMMessageType) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            if (type == DMMessageType.Video) Icons.Default.PlayArrow else Icons.Default.Image,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp),
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

private fun formatMessageTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val localTime = Instant
        .fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localTime.hour.twoDigits()}:${localTime.minute.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun audioCaptureStatusText(state: AudioCaptureUiState, message: String?): String? =
    when (state) {
        AudioCaptureUiState.Stopped -> null
        AudioCaptureUiState.Recording -> "Grabando"
        AudioCaptureUiState.Sent -> "Enviado"
        AudioCaptureUiState.Error -> message ?: "No se pudo grabar"
    }

private fun audioCaptureStatusColor(state: AudioCaptureUiState): Color =
    when (state) {
        AudioCaptureUiState.Recording -> AppColors.RecordingRed
        AudioCaptureUiState.Sent -> AppColors.NeuGreen
        AudioCaptureUiState.Error -> AppColors.ErrorWarmSoft
        AudioCaptureUiState.Stopped -> Color.White.copy(alpha = 0.42f)
    }

private fun audioBubbleLabel(msg: DMMessage): String =
    when (msg.status) {
        MessageStatus.Sending -> "Enviando"
        MessageStatus.Failed -> "Error"
        else -> msg.durationMs?.let(::formatDuration) ?: "Audio"
    }

@Composable
fun AudioBubble(
    msg: DMMessage,
    player: FeedAudioPlayer,
    isMe: Boolean,
) {
    val playerState by player.state.collectAsState()
    val playbackState = playerState.forAudio(msg.id)
    val isPlaying = playbackState.isPlaying
    val progress = playbackState.progress
    val positionMs = playbackState.positionMs
    val durationMs = if (playbackState.durationMs > 0) playbackState.durationMs else (msg.durationMs ?: 0L)

    val canPlay = msg.attachmentUrl?.isNotBlank() == true && msg.status != MessageStatus.Failed
    val accentColor = if (isMe) Color.White else AppColors.NeuGreen

    // Resolución de URL de Firebase Storage a URL de descarga
    var resolvedUrl by remember(msg.attachmentUrl) { mutableStateOf<String?>(null) }
    LaunchedEffect(msg.attachmentUrl) {
        val url = msg.attachmentUrl
        if (url != null) {
            if (url.startsWith("http") || url.startsWith("file") || url.startsWith("content")) {
                resolvedUrl = url
            } else {
                try {
                    resolvedUrl = Firebase.storage.reference.child(url).getDownloadUrl()
                } catch (e: Exception) {
                    // Si falla la resolución, no hacemos nada (el player no podrá reproducir)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Play/Pause
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .clickable(enabled = canPlay && resolvedUrl != null) {
                    player.playOrPause(
                        FeedAudioSource(
                            audioId = msg.id,
                            remoteUrl = resolvedUrl,
                            mimeType = msg.attachmentMimeType ?: "audio/mpeg",
                            durationMs = msg.durationMs
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Área de progreso y tiempo
        Column(modifier = Modifier.weight(1f)) {
            // Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tiempos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(if (isPlaying || progress > 0f) positionMs else 0L),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDuration(durationMs),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun FileBubble(msg: DMMessage, onOpenMedia: (AppMediaContent) -> Unit) {
    val mediaContent = remember(msg) { msg.toAppMediaContent() }
    val contentType = remember(msg.attachmentMimeType, msg.attachmentUrl, msg.thumbnailUrl, msg.attachmentName) {
        msg.resolvedMediaContentType()
    }
    val canOpen = mediaContent != null && msg.status != MessageStatus.Failed

    Row(
        modifier = Modifier
            .clickable(enabled = canOpen) {
                mediaContent?.let(onOpenMedia)
            }
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilePreviewThumb(
            content = mediaContent,
            contentType = contentType,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(msg.attachmentName ?: "Archivo", color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(fileSubtitle(msg, contentType), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FilePreviewThumb(
    content: AppMediaContent?,
    contentType: MediaContentType,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.NeuGreen.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (
            content != null &&
            (content.thumbnailUrl != null || contentType == MediaContentType.Image || contentType == MediaContentType.Video)
        ) {
            AppMediaThumbnail(
                content = content,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = { FileIcon(contentType) },
            )
        } else {
            FileIcon(contentType)
        }
    }
}

@Composable
private fun FileIcon(contentType: MediaContentType) {
    Icon(
        imageVector = when (contentType) {
            MediaContentType.Image -> Icons.Default.Image
            MediaContentType.Video -> Icons.Default.PlayArrow
            MediaContentType.Audio -> Icons.Default.Mic
            else -> Icons.Default.Description
        },
        contentDescription = "File",
        tint = AppColors.NeuGreen,
        modifier = Modifier.size(24.dp),
    )
}

private fun fileSubtitle(msg: DMMessage, contentType: MediaContentType): String {
    val typeLabel = when (contentType) {
        MediaContentType.Image -> "Imagen"
        MediaContentType.Video -> "Video"
        MediaContentType.Audio -> "Audio"
        MediaContentType.Pdf -> "PDF"
        MediaContentType.Text -> "Texto"
        MediaContentType.Unsupported,
        MediaContentType.Unknown -> msg.attachmentMimeType
            ?.substringAfterLast("/")
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: "Archivo"
    }
    val sizeLabel = msg.attachmentSizeBytes?.humanFileSize()
    return listOfNotNull(typeLabel, sizeLabel).joinToString(" - ")
}

private fun Long.humanFileSize(): String =
    when {
        this >= 1024L * 1024L -> "${this / (1024L * 1024L)} MB"
        this >= 1024L -> "${this / 1024L} KB"
        else -> "$this B"
    }

@Composable
fun AttachmentMenu(onOptionSelected: (FileKind) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.AttachmentMenuBackground, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttachmentOption(Icons.Default.Photo, "Fotos", AppColors.NeuGreen) { onOptionSelected(FileKind.Photo) }
            AttachmentOption(Icons.Default.Videocam, "Video", AppColors.NeuGreen) { onOptionSelected(FileKind.Video) }
            AttachmentOption(Icons.Default.Mic, "Audio", AppColors.NeuGreen) { onOptionSelected(FileKind.Audio) }
            AttachmentOption(Icons.Default.Description, "Archivo", AppColors.NeuGreen) { onOptionSelected(FileKind.File) }
        }
    }
}

@Composable
fun AttachmentOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(50.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
