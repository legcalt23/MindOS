package com.example.mindos.features.social.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.domain.model.ProfileCategoryIcon
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModule
import com.example.mindos.features.social.domain.model.ProfileModuleContentType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileUserphraseMaxLength
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.presentation.state.ProfileUiState

internal val CoverBlack = Color(0xFF09090B)
internal val CoverSurface = Color(0xFF18181B)
internal val CoverBorder = Color(0xFF27272A)
internal val CoverTextPrimary = Color(0xFFFAFAFA)
internal val CoverTextSecondary = Color(0xFFA1A1AA)

@Composable
fun ProfileCoverScreen(
    state: ProfileUiState,
    heroModifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    onEnterEdit: (ProfileEditTarget?) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveInlineEdit: () -> Unit,
    onUserphraseChange: (String) -> Unit,
    onMediaPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onMediaPickerError: (String) -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = state.editor.draft ?: state.visualConfig
    val details = state.details
    val isOwner = details?.relationshipState?.isSelf == true
    val isEditing = isOwner && state.editor.isEditing
    val picker = rememberProfileMediaPicker(
        onPicked = { moduleId, upload ->
            onMediaPicked(moduleId, upload)
            onSaveInlineEdit()
        },
        onError = onMediaPickerError,
    )
    var showPhraseDialog by rememberSaveable { mutableStateOf(false) }
    var phraseDraft by rememberSaveable { mutableStateOf("") }
    var phraseInputMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showCoverMediaDialog by rememberSaveable { mutableStateOf(false) }

    if (config == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CoverBlack),
            contentAlignment = Alignment.Center,
        ) {
            Text("Portada no disponible", color = CoverTextPrimary)
        }
        return
    }

    val username = details?.profile?.handle?.value?.takeIf { it.isNotBlank() } ?: config.username.removePrefix("@")
    val displayName = details?.profile?.displayName?.value?.takeIf { it.isNotBlank() } ?: config.displayName
    val phrase = config.userphrase
    val mainSource = config.coverRenderSource(username)
    val mainContent = config.coverMediaContent(username)
    val messages = listOfNotNull(
        state.editor.errorMessage,
        state.editor.feedbackMessage,
        state.errorMessage,
    )

    fun openPhraseEditor() {
        if (!isEditing || state.editor.isSaving) return
        phraseDraft = config.userphrase
        phraseInputMessage = null
        showPhraseDialog = true
    }

    fun openCoverMediaEditor() {
        if (!isEditing || state.editor.isSaving) return
        showCoverMediaDialog = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CoverBlack)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp + contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (isOwner) {
            ProfileEditToggleButton(
                idleLabel = "Editar portada",
                isEditing = isEditing,
                isSaving = state.editor.isSaving,
                onEnterEdit = { onEnterEdit(null) },
                onExitEdit = onCancelEdit,
            )
        }

        if (messages.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                messages.forEach { CoverMessageCard(it) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = heroModifier
                        .fillMaxWidth()
                        .aspectRatio(0.62f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(CoverSurface)
                        .border(1.dp, CoverBorder, RoundedCornerShape(26.dp))
                        .let {
                            if (mainContent != null && !state.editor.isSaving) {
                                it.clickable { onOpenMedia(mainContent) }
                            } else {
                                it
                            }
                        },
                ) {
                    if (mainSource != null) {
                        ProfilePlatformMediaPreview(
                            source = mainSource,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        ) {
                            MainCoverFallback(displayName = displayName)
                        }
                    } else {
                        MainCoverFallback(displayName = displayName)
                    }

                    if (isEditing) {
                        EditableOverlayButton(
                            onClick = ::openCoverMediaEditor,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = displayName,
                        color = CoverTextPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${username.removePrefix("@")}",
                        color = CoverTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CoverCubeCarousel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f),
                    modules = listOf(
                        config.module(ProfileModuleId.SmallOne),
                        config.module(ProfileModuleId.SmallTwo),
                    ),
                    onOpenMedia = onOpenMedia,
                )

                Box {
                    CoverTextTile(
                        title = "FRASE",
                        text = if (phrase.isBlank()) "Sin frase publica aun." else phrase,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let {
                                if (isEditing && !state.editor.isSaving) {
                                    it.clickable { openPhraseEditor() }
                                } else {
                                    it
                                }
                            },
                    )
                    if (isEditing) {
                        EditableOverlayButton(
                            onClick = ::openPhraseEditor,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }

        CoverIconRow(title = "CLUBS", icons = config.categoryIcons)
        CoverPulseRow(config = config)
    }

    if (showPhraseDialog) {
        EditPublicPhraseDialog(
            value = phraseDraft,
            inputMessage = phraseInputMessage,
            isSaving = state.editor.isSaving,
            onValueChange = { next ->
                val sanitized = sanitizePublicPhrase(next)
                phraseDraft = sanitized
                phraseInputMessage = when {
                    sanitized.length >= ProfileUserphraseMaxLength && next.length > sanitized.length -> "Maximo 100 caracteres."
                    sanitized != next -> "Los emojis no estan permitidos."
                    else -> null
                }
            },
            onDismiss = {
                if (!state.editor.isSaving) {
                    showPhraseDialog = false
                    phraseInputMessage = null
                }
            },
            onConfirm = {
                val sanitized = sanitizePublicPhrase(phraseDraft).trim()
                phraseDraft = sanitized
                phraseInputMessage = when {
                    containsEmoji(sanitized) -> "Los emojis no estan permitidos."
                    sanitized.length > ProfileUserphraseMaxLength -> "Maximo 100 caracteres."
                    else -> null
                }
                if (phraseInputMessage == null) {
                    onUserphraseChange(sanitized)
                    onSaveInlineEdit()
                    showPhraseDialog = false
                }
            },
        )
    }

    if (showCoverMediaDialog) {
        CoverMediaPickerDialog(
            isSaving = state.editor.isSaving,
            onDismiss = {
                if (!state.editor.isSaving) {
                    showCoverMediaDialog = false
                }
            },
            onPickPhoto = {
                showCoverMediaDialog = false
                picker.pick(ProfileModuleId.Main, ProfileMediaUploadKind.Photo)
            },
            onPickVideo = {
                showCoverMediaDialog = false
                picker.pick(ProfileModuleId.Main, ProfileMediaUploadKind.Video)
            },
        )
    }
}

@Composable
private fun MainCoverFallback(displayName: String) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CoverSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = CoverTextPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CoverTextTile(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CoverSurface)
            .border(1.dp, CoverBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = CoverTextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = text,
            color = CoverTextPrimary,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoverIconRow(
    title: String,
    icons: List<ProfileCategoryIcon>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = CoverTextSecondary, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icons.forEachIndexed { index, icon ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CoverSurface)
                        .border(1.dp, CoverBorder, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverClubArtwork(
                        index = index,
                        fallbackLabel = icon.name.take(2).uppercase(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverPulseRow(
    config: ProfileVisualConfig,
) {
    val colors = listOf(
        config.colors.primaryArgb.toProfileColor(),
        config.colors.secondaryArgb.toProfileColor(),
        config.colors.secondaryArgb.toProfileColor().copy(alpha = 0.55f),
        config.colors.primaryArgb.toProfileColor().copy(alpha = 0.65f),
        Color.White.copy(alpha = 0.22f),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CoverSurface)
                    .border(1.dp, CoverBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun CoverMessageCard(
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CoverSurface)
            .border(1.dp, CoverBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            color = CoverTextPrimary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CoverMediaPickerDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0D13))
                .border(1.dp, Color(0xFF26344A), RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Actualizar portada",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Selecciona una foto o un video. El archivo no puede superar 15 MB y el video debe durar menos de 15 segundos.",
                    color = CoverTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CoverDialogActionChip(
                        text = "Foto",
                        enabled = !isSaving,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = onPickPhoto,
                    )
                    CoverDialogActionChip(
                        text = "Video",
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        onClick = onPickVideo,
                    )
                }
                CoverDialogActionChip(
                    text = if (isSaving) "Guardando..." else "Cancelar",
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun CoverDialogActionChip(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isPrimary) Color(0xFF111A24) else Color(0xFF0E121B))
            .border(
                1.dp,
                if (isPrimary) Color(0xFF35DBFF) else Color(0xFF26344A),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.56f),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

private fun ProfileVisualConfig.coverRenderSource(username: String): ProfileMediaRenderSource? {
    val main = module(ProfileModuleId.Main)
    val storagePath = mainProfilePhotoUrl?.takeIf { it.isNotBlank() } ?: main.media?.storagePath ?: return null
    return ProfileMediaRenderSource(
        storagePath = storagePath,
        thumbnailStoragePath = main.media?.thumbnailStoragePath,
        pendingPreviewBytes = null,
        isVideo = main.contentType == ProfileModuleContentType.Video || main.media?.mimeType?.startsWith("video/") == true,
        contentDescription = username,
    )
}

private fun ProfileVisualConfig.coverMediaContent(username: String): AppMediaContent? {
    val main = module(ProfileModuleId.Main)
    val storagePath = mainProfilePhotoUrl?.takeIf { it.isNotBlank() } ?: main.media?.storagePath ?: return null
    return AppMediaContent(
        url = main.media?.url?.takeIf { it.isNotBlank() } ?: storagePath,
        storagePath = storagePath,
        fileName = username,
        mimeType = main.media?.mimeType ?: "image/jpeg",
        sizeBytes = 0,
        thumbnailUrl = main.media?.thumbnailUrl ?: main.media?.thumbnailStoragePath,
    )
}

internal fun ProfileModule.toCoverRenderSource(): ProfileMediaRenderSource? {
    val media = media ?: return null
    return ProfileMediaRenderSource(
        storagePath = media.storagePath,
        thumbnailStoragePath = media.thumbnailStoragePath,
        pendingPreviewBytes = null,
        isVideo = contentType == ProfileModuleContentType.Video || media.mimeType.startsWith("video/"),
        contentDescription = profileLabel(),
    )
}

internal fun ProfileModule.toCoverMediaContent(): AppMediaContent? {
    val media = media ?: return null
    return AppMediaContent(
        url = media.url,
        storagePath = media.storagePath,
        fileName = profileLabel(),
        mimeType = media.mimeType,
        sizeBytes = 0,
        thumbnailUrl = media.thumbnailUrl ?: media.thumbnailStoragePath,
    )
}
