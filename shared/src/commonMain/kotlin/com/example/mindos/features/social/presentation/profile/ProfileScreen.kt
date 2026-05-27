package com.example.mindos.features.social.presentation.profile

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.design.AppColors
import com.example.mindos.features.social.domain.feed.GlobalFeedAttachment
import com.example.mindos.features.social.domain.feed.GlobalFeedAttachmentType
import com.example.mindos.features.social.domain.feed.GlobalFeedPost
import com.example.mindos.features.social.domain.feed.GlobalFeedPostType
import com.example.mindos.features.social.domain.feed.GlobalFeedTextAlign
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.domain.model.IdentityPosition
import com.example.mindos.features.social.domain.model.ProfileColorTarget
import com.example.mindos.features.social.domain.model.ProfileLayoutSlot
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileModuleContentType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileUserphraseMaxLength
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.model.VisibleUserProfile
import com.example.mindos.features.social.domain.model.toAppMediaContent
import com.example.mindos.features.social.presentation.state.ProfileActionState
import com.example.mindos.features.social.presentation.state.ProfileUiState

private val ProfileBlack = Color.Black
private val ProfileSurface = AppColors.Background
private val ProfilePhraseSurface = AppColors.SurfaceRaised
private val ProfileStroke = AppColors.Stroke
private val ProfileTextMuted = AppColors.TextMuted
private val ProfileError = AppColors.ErrorSoft
private val ProfileSegmentBackground = AppColors.SurfaceDark
private val ProfileSegmentBorder = AppColors.StrokeSegment
private val ProfileSegmentSelected = AppColors.TextSelected
private val ProfileEditButton = AppColors.NeuGreenSoft
private val ProfileEditButtonText = AppColors.OnBright
private val ProfileDialogBackground = AppColors.DialogBackground
private val ProfileDialogStroke = AppColors.StrokeDialog
private val ProfileDialogField = AppColors.DialogField

private const val ProfilePublicationSlotCount = 9

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    postsState: ProfilePublicationsUiState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenCreator: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onEnterEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onSaveInlineEdit: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onUserphraseChange: (String) -> Unit,
    onIdentityPositionChange: (IdentityPosition) -> Unit,
    onColorTargetSelected: (ProfileColorTarget) -> Unit,
    onColorApplied: (Long) -> Unit,
    onCustomColorChanged: (Long, ChromaCoreInteractionMode) -> Unit,
    onToggleCustomColorPanel: () -> Unit,
    onEditTargetSelected: (ProfileEditTarget) -> Unit,
    onClearEditTarget: () -> Unit,
    onChromaConfirm: () -> Unit,
    onChromaCancel: () -> Unit,
    onModuleMoved: (ProfileModuleId, ProfileLayoutSlot) -> Unit,
    onModuleCleared: (ProfileModuleId) -> Unit,
    onTextModuleChanged: (ProfileModuleId, String) -> Unit,
    onMediaPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onMediaPickerError: (String) -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    onOpenNows: () -> Unit,
    showHeader: Boolean = true,
    showEditorPanel: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val config = state.visualConfig ?: state.editor.draft
    val isOwner = state.details?.relationshipState?.isSelf == true
    val isEditing = isOwner && state.editor.isEditing
    val picker = rememberProfileMediaPicker(
        onPicked = { moduleId, upload ->
            onMediaPicked(moduleId, upload)
            if (moduleId == ProfileModuleId.Main && upload.kind == ProfileMediaUploadKind.Photo) {
                onSaveInlineEdit()
            }
        },
        onError = onMediaPickerError,
    )
    var showPhraseDialog by rememberSaveable { mutableStateOf(false) }
    var phraseDraft by rememberSaveable { mutableStateOf("") }
    var phraseInputMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileBlack),
    ) {
        if (config == null) {
            EmptyProfileUnavailable()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .then(if (showHeader) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 24.dp),
            ) {
                if (showHeader) {
                    ProfileStandaloneTopBar(
                        isOwner = isOwner,
                        onBack = onBack,
                        onOpenOptions = onOpenOptions,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (isOwner) {
                    ProfileEditToggleButton(
                        idleLabel = "Editar perfil",
                        isEditing = isEditing,
                        isSaving = state.editor.isSaving,
                        onEnterEdit = onEnterEdit,
                        onExitEdit = onCancelEdit,
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                RestoredProfileHero(
                    state = state,
                    config = config,
                    isEditing = isEditing,
                    picker = picker,
                    onOpenPhraseEditor = {
                        phraseDraft = config.userphrase
                        phraseInputMessage = null
                        showPhraseDialog = true
                    },
                    onOpenMedia = onOpenMedia,
                    onOpenNows = onOpenNows,
                )

                val messages = buildList {
                    when (val action = state.actionState) {
                        is ProfileActionState.Failed -> add(action.message)
                        else -> Unit
                    }
                    state.editor.feedbackMessage?.let(::add)
                    state.editor.errorMessage?.let(::add)
                    state.errorMessage?.let(::add)
                    postsState.errorMessage?.let(::add)
                }

                if (messages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        messages.forEach { ProfileMessage(it) }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                ProfilePublicationsGrid(
                    postsState = postsState,
                    isOwner = isOwner,
                    onOpenCreator = onOpenCreator,
                    onOpenMedia = onOpenMedia,
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
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

    }
}

@Composable
private fun ProfileStandaloneTopBar(
    isOwner: Boolean,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        StaticProfileSegmentedControl(
            modifier = Modifier
                .align(Alignment.Center)
                .width(280.dp)
                .height(56.dp),
        )

        IconButton(
            onClick = onOpenOptions,
            enabled = isOwner,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Opciones",
                tint = if (isOwner) Color.White else ProfileTextMuted,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun StaticProfileSegmentedControl(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(ProfileSegmentBackground)
            .border(1.dp, ProfileSegmentBorder, RoundedCornerShape(30.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(ProfileSegmentSelected),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Portada",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Perfil",
                    color = AppColors.Background,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun ProfileEditToggleButton(
    idleLabel: String,
    isEditing: Boolean,
    isSaving: Boolean,
    onEnterEdit: () -> Unit,
    onExitEdit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .height(44.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = AppColors.NeuCyan,
                    ambientColor = AppColors.NeuCyan
                )
                .clip(CircleShape)
                .background(AppColors.SurfaceButton)
                .border(1.dp, AppColors.NeuCyan, CircleShape)
                .clickable(enabled = !isSaving, onClick = if (isEditing) onExitEdit else onEnterEdit)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = when {
                    isSaving -> "GUARDANDO..."
                    isEditing -> "SALIR EDICIÓN"
                    else -> idleLabel.uppercase()
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RestoredProfileHero(
    state: ProfileUiState,
    config: ProfileVisualConfig,
    isEditing: Boolean,
    picker: ProfileMediaPicker,
    onOpenPhraseEditor: () -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    onOpenNows: () -> Unit,
) {
    val profile = state.details?.profile
    val username = profile?.handle?.value?.takeIf { it.isNotBlank() } ?: config.username.removePrefix("@")
    val displayName = profile?.displayName?.value?.takeIf { it.isNotBlank() } ?: config.displayName
    val photoSource = profile.profilePhotoRenderSource(config, username)
    val photoMedia = profile.profilePhotoMediaContent(config, username)
    val phrase = config.userphrase.trim()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(156.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(156.dp)
                    .height(278.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ProfileSurface)
                    .border(1.dp, ProfileStroke, RoundedCornerShape(16.dp))
                    .let { base ->
                        if (photoMedia != null) {
                            base.clickable { onOpenMedia(photoMedia) }
                        } else {
                            base
                        }
                    },
            ) {
                if (photoSource != null) {
                    ProfilePlatformMediaPreview(
                        source = photoSource,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    ) {
                        MainProfilePhotoFallback(displayName)
                    }
                } else {
                    MainProfilePhotoFallback(displayName)
                }

                if (isEditing) {
                    EditableOverlayButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { picker.pick(ProfileModuleId.Main, ProfileMediaUploadKind.Photo) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "@${username.removePrefix("@")}",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (displayName.isNotBlank()) {
                    Text(
                        text = displayName,
                        color = ProfileTextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileXCardsCarousel(onOpenNows = onOpenNows)
            PublicPhraseCard(
                phrase = phrase,
                isEditing = isEditing,
                onEditClick = onOpenPhraseEditor,
            )
        }
    }
}

@Composable
private fun MainProfilePhotoFallback(displayName: String) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfileXCardsCarousel(
    onOpenNows: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp),
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(162.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(ProfileSurface)
                    .border(1.dp, ProfileStroke, RoundedCornerShape(15.dp))
                    .clickable(onClick = onOpenNows),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "X",
                        color = AppColors.SurfaceDark,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicPhraseCard(
    phrase: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ProfilePhraseSurface)
            .border(1.dp, ProfileStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = phrase,
            color = AppColors.TextPrimarySoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )

        if (isEditing) {
            EditableOverlayButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = onEditClick,
            )
        }
    }
}

@Composable
internal fun EditPublicPhraseDialog(
    value: String,
    inputMessage: String?,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ProfileDialogBackground)
                .border(1.dp, ProfileDialogStroke, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Editar frase pública",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isSaving,
                    minLines = 2,
                    maxLines = 3,
                    label = { Text("Frase pública") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ProfileDialogField,
                        unfocusedContainerColor = ProfileDialogField,
                        disabledContainerColor = ProfileDialogField,
                        focusedBorderColor = AppColors.CyanFocus,
                        unfocusedBorderColor = ProfileDialogStroke,
                        disabledBorderColor = ProfileDialogStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = AppColors.IceBlue,
                        unfocusedLabelColor = AppColors.IceBlue,
                        disabledLabelColor = AppColors.IceBlue.copy(alpha = 0.7f),
                        cursorColor = AppColors.CyanFocus,
                    ),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = inputMessage.orEmpty(),
                                color = if (inputMessage != null) ProfileError else Color.Transparent,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${value.length}/$ProfileUserphraseMaxLength",
                                color = AppColors.IceBlue,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogActionChip(
                        text = "Cancelar",
                        enabled = !isSaving,
                        onClick = onDismiss,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DialogActionChip(
                        text = if (isSaving) "Guardando..." else "Confirmar",
                        enabled = !isSaving,
                        isPrimary = true,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogActionChip(
    text: String,
    enabled: Boolean,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isPrimary) AppColors.DialogActionPrimary else AppColors.DialogActionSecondary)
            .border(
                width = 1.dp,
                color = if (isPrimary) AppColors.CyanFocus else ProfileDialogStroke,
                shape = shape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

private sealed interface ProfilePublicationTile {
    data class Post(val post: GlobalFeedPost) : ProfilePublicationTile
    data object Add : ProfilePublicationTile
    data object Empty : ProfilePublicationTile
}

@Composable
private fun ProfilePublicationsGrid(
    postsState: ProfilePublicationsUiState,
    isOwner: Boolean,
    onOpenCreator: () -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
) {
    val tiles = buildPublicationTiles(postsState.posts, isOwner)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (postsState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        }

        tiles.chunked(3).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTiles.forEach { tile ->
                    PublicationGridTile(
                        tile = tile,
                        onOpenCreator = onOpenCreator,
                        onOpenMedia = onOpenMedia,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicationGridTile(
    tile: ProfilePublicationTile,
    onOpenCreator: () -> Unit,
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileSurface)
            .border(1.dp, ProfileStroke, RoundedCornerShape(12.dp)),
    ) {
        when (tile) {
            ProfilePublicationTile.Add -> PublicationAddTile(onOpenCreator)
            ProfilePublicationTile.Empty -> PublicationEmptyTile()
            is ProfilePublicationTile.Post -> PublicationPostPreview(tile.post, onOpenMedia)
        }
    }
}

@Composable
private fun PublicationPostPreview(
    post: GlobalFeedPost,
    onOpenMedia: (AppMediaContent) -> Unit,
) {
    val attachment = post.attachments.firstOrNull()
    val openable = primaryOpenableContent(post)

    when {
        attachment != null && attachment.type != GlobalFeedAttachmentType.Audio -> {
            val source = attachment.toPublicationRenderSource(post.authorUsername)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { base -> if (openable != null) base.clickable { onOpenMedia(openable) } else base },
            ) {
                ProfilePlatformMediaPreview(
                    source = source,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) {
                    PublicationEmptyTile()
                }
                if (attachment.type == GlobalFeedAttachmentType.Video) {
                    VideoBadge(
                        secondary = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        post.postType == GlobalFeedPostType.TEXT -> PublicationTextPreview(post)
        post.postType == GlobalFeedPostType.AUDIO -> PublicationAudioPreview(post)
        else -> PublicationEmptyTile()
    }
}

@Composable
private fun PublicationTextPreview(post: GlobalFeedPost) {
    val align = when (post.textStyle?.textAlign ?: GlobalFeedTextAlign.Start) {
        GlobalFeedTextAlign.Start -> TextAlign.Start
        GlobalFeedTextAlign.Center -> TextAlign.Center
        GlobalFeedTextAlign.End -> TextAlign.End
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(post.textStyle?.backgroundPrimaryArgb ?: 0xFF070707),
                        Color(post.textStyle?.backgroundSecondaryArgb ?: 0xFF151515),
                    ),
                ),
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = post.text.orEmpty(),
            color = Color(post.textStyle?.textColorArgb ?: 0xFFF4FFF9),
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = align,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PublicationAudioPreview(post: GlobalFeedPost) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.SurfaceDark, AppColors.Background),
                ),
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "AUDIO",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = post.description ?: "d90a77j3.wav",
                color = ProfileTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PublicationAddTile(
    onOpenCreator: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onOpenCreator),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PublicationAddArtwork(modifier = Modifier.size(60.dp))
            Text(
                text = "AÑADIR",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PublicationEmptyTile() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileSurface),
    )
}

@Composable
private fun ProfileMessage(
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ProfileError.copy(alpha = 0.1f))
            .border(1.dp, ProfileError.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = ProfileError,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyProfileUnavailable() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileSurface)
            .border(1.dp, ProfileStroke, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("Perfil no disponible", color = Color.White)
    }
}

@Composable
internal fun NowsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showImpatientOverlay by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileBlack),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "NOWS",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ESPERA IMPACIENTE.",
                color = ProfileTextMuted,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(AppColors.SurfaceDark)
                    .border(1.dp, ProfileStroke, RoundedCornerShape(999.dp))
                    .clickable { showImpatientOverlay = !showImpatientOverlay }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
            }
        }
    }
}

private fun buildPublicationTiles(posts: List<GlobalFeedPost>, isOwner: Boolean): List<ProfilePublicationTile> {
    val contentTiles = buildList {
        if (isOwner) add(ProfilePublicationTile.Add)
        addAll(posts.take(ProfilePublicationSlotCount).map { ProfilePublicationTile.Post(it) })
    }
    return contentTiles.take(ProfilePublicationSlotCount).let { used ->
        used + List((ProfilePublicationSlotCount - used.size).coerceAtLeast(0)) { ProfilePublicationTile.Empty }
    }
}

private fun VisibleUserProfile?.profilePhotoRenderSource(
    config: ProfileVisualConfig,
    username: String,
): ProfileMediaRenderSource? {
    return config.mainProfileRenderSource(username) ?: this?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatar ->
        ProfileMediaRenderSource(
            storagePath = null,
            thumbnailStoragePath = null,
            pendingPreviewBytes = null,
            isVideo = false,
            contentDescription = username,
            directUrl = avatar,
            thumbnailUrl = avatar,
        )
    }
}

private fun VisibleUserProfile?.profilePhotoMediaContent(
    config: ProfileVisualConfig,
    username: String,
): AppMediaContent? {
    return config.mainProfileMediaContent(username) ?: this?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatar ->
        AppMediaContent(
            url = avatar,
            storagePath = avatar,
            fileName = username,
            mimeType = "image/*",
            sizeBytes = 0L,
            thumbnailUrl = avatar,
        )
    }
}

private fun ProfileVisualConfig.mainProfileRenderSource(username: String): ProfileMediaRenderSource? {
    val main = module(ProfileModuleId.Main)
    val storagePath = mainProfilePhotoUrl?.takeIf { it.isNotBlank() } ?: main.media?.storagePath ?: return null
    return ProfileMediaRenderSource(
        storagePath = storagePath,
        thumbnailStoragePath = main.media?.thumbnailStoragePath,
        pendingPreviewBytes = null,
        isVideo = main.contentType == ProfileModuleContentType.Video || main.media?.mimeType?.startsWith("video/") == true,
        contentDescription = username,
        directUrl = main.media?.url,
        thumbnailUrl = main.media?.thumbnailUrl,
    )
}

private fun ProfileVisualConfig.mainProfileMediaContent(username: String): AppMediaContent? {
    val main = module(ProfileModuleId.Main)
    val media = main.media ?: return null
    val storagePath = mainProfilePhotoUrl?.takeIf { it.isNotBlank() } ?: media.storagePath
    return AppMediaContent(
        url = media.url,
        storagePath = storagePath,
        fileName = username,
        mimeType = media.mimeType,
        sizeBytes = 0L,
        thumbnailUrl = media.thumbnailUrl,
    )
}

private fun GlobalFeedAttachment.toPublicationRenderSource(username: String): ProfileMediaRenderSource =
    ProfileMediaRenderSource(
        storagePath = storagePath,
        thumbnailStoragePath = null,
        pendingPreviewBytes = null,
        isVideo = type == GlobalFeedAttachmentType.Video || mimeType.startsWith("video/"),
        contentDescription = username,
        directUrl = url,
        thumbnailUrl = thumbnailUrl,
    )

private fun primaryOpenableContent(post: GlobalFeedPost): AppMediaContent? =
    post.attachments.firstOrNull()?.toAppMediaContent()

@Composable
private fun VideoBadge(
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.18f)
                lineTo(size.width * 0.78f, size.height * 0.5f)
                lineTo(size.width * 0.28f, size.height * 0.82f)
                close()
            }
            drawPath(path, color = secondary)
        }
    }
}
