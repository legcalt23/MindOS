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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mindos.design.AppColors
import com.example.mindos.features.profile.presentation.state.CustomLinkEditState
import com.example.mindos.features.profile.presentation.state.EmojiPickerUiState
import com.example.mindos.features.social.presentation.profile.ProfileMediaRenderSource
import com.example.mindos.features.social.presentation.profile.ProfilePlatformMediaPreview

private const val EmojiPerRow = 6

/** Picker that sets the username prefix emoji (or resets it to "@"). */
@Composable
fun EmojiPrefixPickerDialog(
    state: EmojiPickerUiState,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        DialogSurface {
            Text(
                text = "Elige un emoji para tu nombre",
                color = AppColors.ProfileTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            state.options.chunked(EmojiPerRow).forEach { rowEmojis ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowEmojis.forEach { emoji ->
                        EmojiCell(emoji = emoji, onClick = { onSelect(emoji) }, modifier = Modifier.weight(1f))
                    }
                    repeat(EmojiPerRow - rowEmojis.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DialogChip(text = "Usar @", primary = false, onClick = onReset)
                Spacer(modifier = Modifier.width(10.dp))
                DialogChip(text = "Cerrar", primary = false, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semanticsLabel("Emoji $emoji"),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

/** Add/edit form for a custom link slot (URL + 1:1 custom icon). */
@Composable
fun CustomLinkEditDialog(
    state: CustomLinkEditState,
    onUrlChange: (String) -> Unit,
    onPickIcon: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!state.isSaving) onDismiss() }) {
        DialogSurface {
            Text(
                text = "Añade un enlace web con icono personalizado",
                color = AppColors.ProfileTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                enabled = !state.isSaving,
                singleLine = true,
                label = { Text("Enlace web") },
                placeholder = { Text("https://mindos.app") },
                shape = RoundedCornerShape(16.dp),
                colors = dialogFieldColors(),
                isError = state.validationError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            IconUploadRow(state = state, onPickIcon = onPickIcon)
            state.validationError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = AppColors.ErrorSoft, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DialogChip(text = "Eliminar", primary = false, enabled = !state.isSaving, onClick = onClear)
                Spacer(modifier = Modifier.width(10.dp))
                DialogChip(text = "Cancelar", primary = false, enabled = !state.isSaving, onClick = onDismiss)
                Spacer(modifier = Modifier.width(10.dp))
                DialogChip(
                    text = if (state.isSaving) "Guardando..." else "Guardar",
                    primary = true,
                    enabled = !state.isSaving,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun IconUploadRow(state: CustomLinkEditState, onPickIcon: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.DialogField)
            .border(0.5.dp, AppColors.StrokeDialog, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onPickIcon)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, AppColors.ComposerBlue, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val source = iconPreviewSource(state)
            if (source != null) {
                ProfilePlatformMediaPreview(
                    source = source,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Crop,
                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.ComposerBlue) }
            } else {
                Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.ComposerBlue)
            }
        }
        Column {
            Text(
                text = if (state.hasIcon) "Cambiar icono" else "Subir icono personalizado",
                color = AppColors.ProfileTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Imagen cuadrada · relación 1:1",
                color = AppColors.IceBlue,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun DialogSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.DialogBackground)
            .border(1.dp, AppColors.StrokeDialog, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun DialogChip(
    text: String,
    primary: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (primary) AppColors.DialogActionPrimary else AppColors.DialogActionSecondary)
            .border(1.dp, if (primary) AppColors.CyanFocus else AppColors.StrokeDialog, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.ProfileTextPrimary else AppColors.ProfileTextPrimary.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppColors.DialogField,
    unfocusedContainerColor = AppColors.DialogField,
    disabledContainerColor = AppColors.DialogField,
    focusedBorderColor = AppColors.CyanFocus,
    unfocusedBorderColor = AppColors.StrokeDialog,
    disabledBorderColor = AppColors.StrokeDialog,
    errorBorderColor = AppColors.ErrorSoft,
    focusedTextColor = AppColors.ProfileTextPrimary,
    unfocusedTextColor = AppColors.ProfileTextPrimary,
    focusedLabelColor = AppColors.IceBlue,
    unfocusedLabelColor = AppColors.IceBlue,
    cursorColor = AppColors.CyanFocus,
)

private fun iconPreviewSource(state: CustomLinkEditState): ProfileMediaRenderSource? {
    val bytes = state.pickedIconBytes ?: state.existingIconBytes
    val reference = state.existingIconReference
    if ((bytes == null || bytes.isEmpty()) && reference.isNullOrBlank()) return null
    return ProfileMediaRenderSource(
        storagePath = reference,
        thumbnailStoragePath = null,
        pendingPreviewBytes = bytes,
        isVideo = false,
        contentDescription = "Icono de enlace",
        directUrl = reference,
        thumbnailUrl = null,
    )
}
