package com.example.mindos.features.profile.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.mindos.features.profile.domain.audio.ProfileFormatting
import com.example.mindos.features.profile.domain.model.MindOSProfile
import com.example.mindos.features.profile.domain.model.PresenceState
import com.example.mindos.features.profile.domain.model.ProfileImageRef
import com.example.mindos.features.profile.domain.model.ProfileTag
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview

internal val HeaderHeight = 408.dp
private val AvatarSize = 132.dp
private val OnlineDotSize = 11.dp
private const val HeaderCornerRadius = 36

/** Maps a [ProfileImageRef] to the shared media render source, or null when empty. */
internal fun ProfileImageRef.toRenderSource(description: String): ProfileMediaRenderSource? =
    if (isEmpty) {
        null
    } else {
        ProfileMediaRenderSource(
            storagePath = reference,
            thumbnailStoragePath = null,
            pendingPreviewBytes = localBytes,
            isVideo = false,
            contentDescription = description,
            directUrl = null,
            thumbnailUrl = null,
        )
    }

/**
 * The top ~50% header: full-bleed cover (`foto_portada`), centered circular
 * avatar (`foto_perfil`) with the presence dot above it, and — for the owner —
 * a blue-bordered EDITAR control on each editable image.
 */
@Composable
fun ProfileHeaderSection(
    profile: MindOSProfile,
    isOwner: Boolean,
    onEditCover: () -> Unit,
    onEditAvatar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(HeaderHeight)) {
        // Cover artwork or photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(
                        bottomStart = HeaderCornerRadius.dp,
                        bottomEnd = HeaderCornerRadius.dp,
                    ),
                ),
        ) {
            val coverSource = profile.fotoPortada.toRenderSource("${profile.username} portada")
            if (coverSource != null) {
                ProfilePlatformMediaPreview(
                    source = coverSource,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) { CoverArtworkFallback() }
            } else {
                CoverArtworkFallback()
            }
            // Top + bottom vignettes for legibility.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, AppColors.ProfileCanvas.copy(alpha = 0.65f)),
                        ),
                    ),
            )
        }

        // Back affordance (minimal) + cover EDITAR, on the status-bar row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackChip(onBack = onBack)
            if (isOwner) {
                EditarButton(onClick = onEditCover, contentDescription = "Editar foto de portada")
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }

        // Centered avatar cluster.
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OnlineStatusDot(presence = profile.presence)
            Spacer(modifier = Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(AppColors.ProfileCanvas)
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                ) {
                    val avatarSource = profile.fotoPerfil.toRenderSource("${profile.username} avatar")
                    if (avatarSource != null) {
                        ProfilePlatformMediaPreview(
                            source = avatarSource,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        ) { PortraitArtworkFallback() }
                    } else {
                        PortraitArtworkFallback()
                    }
                }
                if (isOwner) {
                    EditarButton(onClick = onEditAvatar, contentDescription = "Editar foto de perfil")
                }
            }
        }
    }
}

/** Small green/red presence indicator with a subtle glow. */
@Composable
fun OnlineStatusDot(presence: PresenceState, modifier: Modifier = Modifier) {
    val color = if (presence.isOnline) AppColors.ProfileOnline else AppColors.Error
    val label = if (presence.isOnline) "En línea" else "Desconectado"
    Box(
        modifier = modifier
            .size(OnlineDotSize)
            .clip(CircleShape)
            .border(3.dp, AppColors.ProfileCanvas.copy(alpha = 0.55f), CircleShape)
            .background(color, CircleShape)
            .semanticsLabel(label),
    )
}

/**
 * Futuristic, blue-bordered EDITAR control reused for every editable element.
 */
@Composable
fun EditarButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, AppColors.ComposerBlue, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semanticsLabel(contentDescription)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = AppColors.ComposerBlue,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "EDITAR",
            color = AppColors.ProfileTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun BackChip(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(role = Role.Button, onClick = onBack)
            .semanticsLabel("Volver"),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "‹", color = AppColors.ProfileTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Identity row: interactive `@`/emoji prefix + username, the eye/pencil action,
 * the observer count, and the interest tags.
 */
@Composable
fun ProfileIdentityRow(
    profile: MindOSProfile,
    isOwner: Boolean,
    onPrefixTap: () -> Unit,
    onObserveToggle: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            UsernameWithPrefix(profile = profile, isOwner = isOwner, onPrefixTap = onPrefixTap)
            Spacer(modifier = Modifier.height(10.dp))
            ProfileTagsRow(tags = profile.tags)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ObserverActionButton(
                isOwner = isOwner,
                isObserving = profile.isObservedByViewer,
                onObserveToggle = onObserveToggle,
                onEditProfile = onEditProfile,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = ProfileFormatting.observerLabel(profile.observerCount),
                color = AppColors.ProfileTextPrimary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun UsernameWithPrefix(
    profile: MindOSProfile,
    isOwner: Boolean,
    onPrefixTap: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val prefixModifier = if (isOwner) {
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onPrefixTap)
                .semanticsLabel("Cambiar prefijo del nombre de usuario")
                .padding(horizontal = 2.dp)
        } else {
            Modifier
        }
        Text(
            text = profile.usernamePrefix,
            color = AppColors.ProfileTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = prefixModifier,
        )
        Text(
            text = profile.username,
            color = AppColors.ProfileTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ObserverActionButton(
    isOwner: Boolean,
    isObserving: Boolean,
    onObserveToggle: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val icon: ImageVector
    val label: String
    val filled: Boolean
    when {
        isOwner -> {
            icon = Icons.Filled.Edit
            label = "Editar perfil"
            filled = false
        }
        isObserving -> {
            icon = Icons.Filled.Visibility
            label = "Dejar de observar"
            filled = true
        }
        else -> {
            icon = Icons.Filled.VisibilityOff
            label = "Observar"
            filled = false
        }
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (filled) AppColors.ProfileTextPrimary else Color.White.copy(alpha = 0.04f))
            .border(
                0.5.dp,
                if (filled) AppColors.ProfileTextPrimary else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(11.dp),
            )
            .clickable(role = Role.Button, onClick = if (isOwner) onEditProfile else onObserveToggle)
            .semanticsLabel(label),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) AppColors.ProfileCanvas else AppColors.ProfileTextPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun ProfileTagsRow(tags: List<ProfileTag>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach { tag -> InterestPill(tag = tag) }
    }
}

@Composable
private fun InterestPill(tag: ProfileTag) {
    val icon = when (tag.id) {
        "ciencia" -> Icons.Filled.Science
        else -> Icons.Filled.Palette
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.ProfileTextPrimary.copy(alpha = 0.62f),
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = tag.label,
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.62f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
