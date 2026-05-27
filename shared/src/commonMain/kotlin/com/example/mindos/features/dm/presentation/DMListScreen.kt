package com.example.mindos.features.dm.presentation

import com.example.mindos.core.logging.safeDebugLog
import com.example.mindos.core.security.SafeInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.core.session.AuthenticatedUser
import com.example.mindos.features.dm.domain.repository.DMRepository
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview
import com.example.mindos.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

@Composable
fun rememberDMListViewModel(
    repository: DMRepository,
    waveRequestService: WaveRequestService,
    currentUserId: String,
    scope: CoroutineScope = rememberCoroutineScope(),
): DMListViewModel {
    return remember(repository, waveRequestService, currentUserId, scope) {
        DMListViewModel(repository, waveRequestService, currentUserId, scope)
    }
}

@Composable
fun DMListScreen(
    user: AuthenticatedUser,
    dmRepository: DMRepository,
    waveRequestService: WaveRequestService,
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenCajonera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberDMListViewModel(dmRepository, waveRequestService, user.uid)
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(user.uid) {
        dmUiLog("event=screen_enter screen=DMList")
    }

    LaunchedEffect(uiState.navigationTarget) {
        val target = uiState.navigationTarget
        if (target is DMListNavigationTarget.Chat) {
            onOpenChat(target.chatId, target.otherUserId)
            viewModel.navigationHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DMListBlack)
            .statusBarsPadding()
    ) {
        // 1. CABECERA Y NAVEGACIÓN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "WAVES",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = onOpenCajonera,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                DMCajoneraEntryIcon(
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 2. BARRA DE BÚSQUEDA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Buscar usuarios y chats",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = SafeInput.normalizeSearchQuery(it) },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(DMListAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = { searchQuery = "" },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val status = uiState.status) {
                is DMListStatus.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = DMListAccent,
                    )
                }

                is DMListStatus.Empty -> {
                    Text(
                        text = "No hay conversaciones ni usuarios disponibles.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }

                is DMListStatus.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = status.message,
                            color = Color.Red.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadUsers() },
                            colors = ButtonDefaults.buttonColors(containerColor = DMListAccent),
                        ) {
                            Text("REINTENTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is DMListStatus.Ready -> {
                    val filteredItems = if (searchQuery.isBlank()) {
                        uiState.items
                    } else {
                        uiState.items.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                it.displayName?.contains(searchQuery, ignoreCase = true) == true ||
                                it.username?.contains(searchQuery, ignoreCase = true) == true
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 50.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filteredItems, key = { it.key }) { item ->
                            DMConversationListItem(
                                item = item,
                                onClick = { viewModel.openChat(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dmUiLog(message: String) {
    safeDebugLog("DMListUi", message)
}

@Composable
private fun DMConversationListItem(
    item: DMListEntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(28.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Perfil
        DMConversationAvatar(
            item = item,
            modifier = Modifier.size(60.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Contenido (Cuerpo Central)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = cardShape,
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.lastMessageIsIncomingAndUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3)) // Punto azul brillante
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.previewText,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Indicadores (Columna Derecha)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(60.dp)
        ) {
            val (topTime, bottomTime) = remember(item.lastTimestamp) {
                formatDMTimestamp(item.lastTimestamp)
            }

            if (topTime != null) {
                Text(
                    text = topTime,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (item.isTyping) {
                    TypingWavesAnimation()
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(
                            if (item.isOnline) {
                                Modifier.border(2.dp, DMListAccent, CircleShape)
                            } else Modifier
                        )
                        .clip(CircleShape)
                        .background(if (item.unreadCount != null && item.unreadCount > 0) DMListAccent else Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.unreadCount != null && item.unreadCount > 0) {
                        Text(
                            text = item.unreadCount.toString(),
                            color = DMListBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Text(
                text = bottomTime,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TypingWavesAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val radiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = DMListAccent.copy(alpha = alpha),
            radius = (size.minDimension / 2) * radiusScale,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun formatDMTimestamp(timestamp: Long): Pair<String?, String> {
    if (timestamp == 0L) return null to ""
    val now = Clock.System.now()
    val msgTime = Instant.fromEpochMilliseconds(timestamp)
    val period = now - msgTime
    
    val localDateTime = msgTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeStr = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"

    return when {
        period < 24.hours -> null to timeStr
        period < 48.hours -> "Ayer" to timeStr
        period < 7.days -> {
            val dayName = when (localDateTime.dayOfWeek) {
                DayOfWeek.MONDAY -> "Lunes"
                DayOfWeek.TUESDAY -> "Martes"
                DayOfWeek.WEDNESDAY -> "Miércoles"
                DayOfWeek.THURSDAY -> "Jueves"
                DayOfWeek.FRIDAY -> "Viernes"
                DayOfWeek.SATURDAY -> "Sábado"
                DayOfWeek.SUNDAY -> "Domingo"
                else -> ""
            }
            dayName to timeStr
        }
        else -> {
            val days = period.toInt(DurationUnit.DAYS)
            "${days}d" to timeStr
        }
    }
}

@Composable
private fun DMConversationAvatar(
    item: DMListEntryUiModel,
    modifier: Modifier = Modifier,
) {
    val avatarSource = remember(item.avatarUrl, item.title) {
        item.avatarUrl?.let { avatar ->
            ProfileMediaRenderSource(
                storagePath = null,
                thumbnailStoragePath = null,
                pendingPreviewBytes = null,
                isVideo = false,
                contentDescription = item.title,
                directUrl = avatar,
                thumbnailUrl = avatar,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(DMListAccent.copy(alpha = 0.28f), Color.White.copy(alpha = 0.055f)),
                ),
            )
            .border(BorderStroke(1.dp, DMListAccent.copy(alpha = 0.40f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarSource != null) {
            ProfilePlatformMediaPreview(
                source = avatarSource,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                DMAvatarFallback(item)
            }
        } else {
            DMAvatarFallback(item)
        }
    }
}

@Composable
private fun DMAvatarFallback(item: DMListEntryUiModel) {
    val initials = remember(item.title, item.username) {
        item.title
            .split(" ")
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank {
                item.username?.take(2)?.uppercase()
                    ?: item.title.take(2).uppercase()
            }
    }

    Text(
        text = initials,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        maxLines = 1,
    )
}

private val DMListBlack = Color(0xFF020504)
private val DMListAccent = Color(0xFF13ECA4)

@Composable
expect fun DMCajoneraEntryIcon(
    modifier: Modifier = Modifier,
)
