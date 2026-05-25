package com.example.neuv3.features.search.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.core.security.SafeInput
import com.example.neuv3.features.search.domain.UsernameSearchUser
import com.example.neuv3.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.neuv3.features.social.presentation.profile.ProfilePlatformMediaPreview

@Composable
internal fun RadarCommandInput(
    query: String,
    onQueryChanged: (String) -> Unit,
    helper: String,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
) {
    Column(modifier = modifier) {
        BasicTextField(
            value = query,
            onValueChange = { onQueryChanged(SafeInput.normalizeSearchQuery(it, maxLength = 30)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = NeuWhite,
                fontSize = if (quiet) 24.sp else 26.sp,
                fontWeight = FontWeight.Black,
            ),
            cursorBrush = SolidColor(NeuGreen),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (quiet) 0.045f else 0.065f),
                                    NeuGreen.copy(alpha = if (quiet) 0.025f else 0.055f),
                                ),
                            ),
                        )
                        .border(
                            BorderStroke(1.dp, NeuGreen.copy(alpha = if (quiet) 0.22f else 0.42f)),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "@",
                        color = NeuGreen.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isBlank()) {
                            Text(
                                text = "username",
                                color = NeuMist.copy(alpha = 0.46f),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
        Text(
            text = helper,
            color = NeuMist,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun DetectedAvatar(
    user: UsernameSearchUser,
    modifier: Modifier = Modifier,
) {
    val source = remember(user.photoStoragePath, user.photoThumbnailStoragePath, user.isVideo) {
        user.photoStoragePath?.let {
            ProfileMediaRenderSource(
                storagePath = user.photoStoragePath,
                thumbnailStoragePath = user.photoThumbnailStoragePath,
                pendingPreviewBytes = null,
                isVideo = user.isVideo,
                contentDescription = user.username,
            )
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(NeuGreen.copy(alpha = 0.28f), Color.White.copy(alpha = 0.055f)),
                ),
            )
            .border(BorderStroke(1.dp, NeuGreen.copy(alpha = 0.38f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (source != null) {
            ProfilePlatformMediaPreview(
                source = source,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            ) {
                AvatarInitials(user)
            }
        } else {
            AvatarInitials(user)
        }
    }
}

@Composable
private fun AvatarInitials(user: UsernameSearchUser) {
    Text(
        text = user.displayName
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { user.username.take(2).uppercase() },
        color = NeuWhite,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        maxLines = 1,
    )
}

@Composable
internal fun NeuMicroButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = NeuWhite,
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
internal fun ModePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = if (selected) NeuBlack else NeuWhite,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) NeuGreen else Color.White.copy(alpha = 0.05f))
            .border(
                BorderStroke(1.dp, if (selected) NeuGreen else Color.White.copy(alpha = 0.10f)),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}
