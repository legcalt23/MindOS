package com.example.mindos.features.social.presentation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.features.social.domain.model.InAppNotification
import com.example.mindos.features.social.domain.model.InAppNotificationType
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onOpenActorProfile: (InAppNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NotificationBlack),
    ) {
        NotificationAtmosphere(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "NOTIFICACIONES",
                        color = NotificationWhite,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotificationMicroButton(text = "INICIO", onClick = onBack)
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            when {
                state.isLoading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = NotificationGreen,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                state.errorMessage != null -> item {
                    NotificationEmptyState(text = state.errorMessage)
                }

                state.items.isEmpty() -> item {
                    NotificationEmptyState(text = "Aun no hay senales nuevas.")
                }

                else -> items(state.items, key = { it.notificationId }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onOpenActorProfile = { onOpenActorProfile(notification) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: InAppNotification,
    onOpenActorProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, NotificationGreen.copy(alpha = 0.30f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenActorProfile)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotificationActorAvatar(
            notification = notification,
            modifier = Modifier
                .size(58.dp)
                .clickable(onClick = onOpenActorProfile),
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.messageText(),
                color = NotificationWhite,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenActorProfile),
            )
            Text(
                text = "Senal recibida",
                color = NotificationMist,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun NotificationActorAvatar(
    notification: InAppNotification,
    modifier: Modifier = Modifier,
) {
    val source = remember(
        notification.actorPhotoStoragePath,
        notification.actorPhotoThumbnailStoragePath,
    ) {
        notification.actorPhotoStoragePath?.let { storagePath ->
            ProfileMediaRenderSource(
                storagePath = storagePath,
                thumbnailStoragePath = notification.actorPhotoThumbnailStoragePath,
                pendingPreviewBytes = null,
                isVideo = false,
                contentDescription = notification.actorUsername,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(NotificationGreen.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f)),
                ),
            )
            .border(BorderStroke(1.dp, NotificationGreen.copy(alpha = 0.52f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (source != null) {
            ProfilePlatformMediaPreview(
                source = source,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                NotificationInitials(notification)
            }
        } else {
            NotificationInitials(notification)
        }
    }
}

@Composable
private fun NotificationInitials(notification: InAppNotification) {
    Text(
        text = notification.actorDisplayName
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { notification.actorUsername.take(2).uppercase() },
        color = NotificationWhite,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        maxLines = 1,
    )
}

@Composable
private fun NotificationEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(BorderStroke(1.dp, NotificationGreen.copy(alpha = 0.22f)), RoundedCornerShape(8.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = NotificationMist,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotificationMicroButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = NotificationWhite,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun NotificationAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(NotificationBlack)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NotificationGreen.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.14f),
                radius = size.minDimension * 0.70f,
            ),
        )
    }
}

private fun InAppNotification.messageText(): String =
    when (type) {
        InAppNotificationType.Observed -> "${actorUsername.removePrefix("@")} te observa"
    }

private val NotificationBlack = Color(0xFF020504)
private val NotificationWhite = Color(0xFFF4FFF9)
private val NotificationGreen = Color(0xFF13ECA4)
private val NotificationMist = Color(0xFFA7CFC1)

