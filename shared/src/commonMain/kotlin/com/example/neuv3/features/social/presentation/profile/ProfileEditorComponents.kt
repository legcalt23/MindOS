package com.example.neuv3.features.social.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neuv3.features.social.domain.model.IdentityPosition
import com.example.neuv3.features.social.domain.model.ProfileColorTarget
import com.example.neuv3.features.social.domain.model.ProfileMediaUploadKind
import com.example.neuv3.features.social.domain.model.ProfileModule
import com.example.neuv3.features.social.domain.model.ProfileModuleContentType
import com.example.neuv3.features.social.domain.model.ProfileModuleId
import com.example.neuv3.features.social.domain.model.ProfileModuleSize
import com.example.neuv3.features.social.domain.model.ProfileSmallTextMaxLength
import com.example.neuv3.features.social.domain.model.ProfileVisualConfig
import kotlin.math.roundToInt

@Composable
internal fun ProfileEditorPanel(
    editor: ProfileEditorUiState,
    config: ProfileVisualConfig,
    picker: ProfileMediaPicker,
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
    onModuleCleared: (ProfileModuleId) -> Unit,
    onTextModuleChanged: (ProfileModuleId, String) -> Unit,
) {
    val enabled = !editor.isSaving
    val activeTarget = editor.activeTarget

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141A29), Color(0xFF0B0F18)),
                ),
            )
            .border(BorderStroke(1.dp, Color(0xFF283046)), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Editor del perfil",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = activeTarget?.editorTitle(config) ?: "Pulsa un bloque del perfil para abrir su editor.",
                    color = Color(0xFF9EA7BE),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (activeTarget != null) {
                EditorActionButton(
                    text = "Cerrar",
                    enabled = enabled,
                    compact = true,
                    onClick = onClearEditTarget,
                )
            }
        }

        if (activeTarget == null) {
            EditorSectionCard {
                Text(
                    text = "Selecciona la zona que quieres ajustar.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                TargetSelectorRow(config = config, enabled = enabled, onEditTargetSelected = onEditTargetSelected)
            }
        }

        when (activeTarget) {
            null -> Unit
            ProfileEditTarget.FotoPortada -> {
                EditorSectionCard {
                    TargetSelectorRow(config = config, enabled = enabled, onEditTargetSelected = onEditTargetSelected)
                }
                ModuleMediaTargetEditor(
                    label = "Fotoportada publica",
                    module = config.module(ProfileModuleId.Main),
                    picker = picker,
                    enabled = enabled,
                    allowText = false,
                    onClear = { onModuleCleared(ProfileModuleId.Main) },
                    onTextModuleChanged = onTextModuleChanged,
                )
            }

            ProfileEditTarget.Username -> {
                EditorSectionCard {
                    OutlinedTextField(
                        value = config.displayName,
                        onValueChange = onDisplayNameChange,
                        enabled = enabled,
                        label = { Text("Nombre visible") },
                        singleLine = true,
                        colors = editorFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = config.username.removePrefix("@"),
                        onValueChange = onUsernameChange,
                        enabled = enabled,
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        colors = editorFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Posicion del encabezado",
                        color = Color(0xFFB6BED4),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        EditorChoiceButton("Identidad arriba", config.identityPosition == IdentityPosition.Top, enabled) {
                            onIdentityPositionChange(IdentityPosition.Top)
                        }
                        EditorChoiceButton("Identidad abajo", config.identityPosition == IdentityPosition.Bottom, enabled) {
                            onIdentityPositionChange(IdentityPosition.Bottom)
                        }
                    }
                }
            }

            ProfileEditTarget.UserPhrase -> {
                EditorSectionCard {
                    OutlinedTextField(
                        value = config.userphrase,
                        onValueChange = onUserphraseChange,
                        enabled = enabled,
                        label = { Text("Frase publica") },
                        minLines = 3,
                        maxLines = 5,
                        colors = editorFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ProfileEditTarget.ThemeColors -> {
                ProfileColorEditor(
                    editor = editor,
                    config = config,
                    enabled = enabled,
                    onColorTargetSelected = onColorTargetSelected,
                    onColorApplied = onColorApplied,
                    onCustomColorChanged = onCustomColorChanged,
                    onToggleCustomColorPanel = onToggleCustomColorPanel,
                )
            }

            is ProfileEditTarget.Highlight -> {
                EditorSectionCard {
                    TargetSelectorRow(config = config, enabled = enabled, onEditTargetSelected = onEditTargetSelected)
                }
                ModuleMediaTargetEditor(
                    label = activeTarget.editorTitle(config),
                    module = config.module(activeTarget.moduleId),
                    picker = picker,
                    enabled = enabled,
                    allowText = true,
                    onClear = { onModuleCleared(activeTarget.moduleId) },
                    onTextModuleChanged = onTextModuleChanged,
                )
            }
        }
    }
}

@Composable
private fun TargetSelectorRow(
    config: ProfileVisualConfig,
    enabled: Boolean,
    onEditTargetSelected: (ProfileEditTarget) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorActionButton("Portada", enabled = enabled) { onEditTargetSelected(ProfileEditTarget.FotoPortada) }
        EditorActionButton("Identidad", enabled = enabled) { onEditTargetSelected(ProfileEditTarget.Username) }
        EditorActionButton("Frase", enabled = enabled) { onEditTargetSelected(ProfileEditTarget.UserPhrase) }
        EditorActionButton("Colores", enabled = enabled) { onEditTargetSelected(ProfileEditTarget.ThemeColors) }
        config.modules
            .filter { it.id != ProfileModuleId.Main }
            .forEach { module ->
                EditorActionButton(module.profileLabel(), enabled = enabled) {
                    onEditTargetSelected(ProfileEditTarget.Highlight(module.id))
                }
            }
    }
}

@Composable
private fun ModuleMediaTargetEditor(
    label: String,
    module: ProfileModule,
    picker: ProfileMediaPicker,
    enabled: Boolean,
    allowText: Boolean,
    onClear: () -> Unit,
    onTextModuleChanged: (ProfileModuleId, String) -> Unit,
) {
    EditorSectionCard {
        Text(label, color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            EditorActionButton("Foto", enabled = enabled) { picker.pick(module.id, ProfileMediaUploadKind.Photo) }
            EditorActionButton("Video <15s", enabled = enabled) { picker.pick(module.id, ProfileMediaUploadKind.Video) }
            if (allowText && module.size == ProfileModuleSize.Small) {
                EditorActionButton("Texto", enabled = enabled) {
                    onTextModuleChanged(module.id, module.text.orEmpty().ifBlank { "Nueva pieza visual" })
                }
            }
            EditorActionButton("Vaciar", enabled = enabled, compact = true, onClick = onClear)
        }

        if (allowText &&
            module.size == ProfileModuleSize.Small &&
            (module.contentType == ProfileModuleContentType.Text || !module.text.isNullOrBlank())
        ) {
            OutlinedTextField(
                value = module.text.orEmpty(),
                onValueChange = { onTextModuleChanged(module.id, it.take(ProfileSmallTextMaxLength)) },
                enabled = enabled,
                label = { Text("Texto del bloque") },
                minLines = 2,
                maxLines = 4,
                colors = editorFieldColors(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileColorEditor(
    editor: ProfileEditorUiState,
    config: ProfileVisualConfig,
    enabled: Boolean,
    onColorTargetSelected: (ProfileColorTarget) -> Unit,
    onColorApplied: (Long) -> Unit,
    onCustomColorChanged: (Long, ChromaCoreInteractionMode) -> Unit,
    onToggleCustomColorPanel: () -> Unit,
) {
    EditorSectionCard {
        Text(
            text = "Paleta del perfil",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditorChoiceButton("Primario", editor.selectedColorTarget == ProfileColorTarget.Primary, enabled) {
                onColorTargetSelected(ProfileColorTarget.Primary)
            }
            EditorChoiceButton("Secundario", editor.selectedColorTarget == ProfileColorTarget.Secondary, enabled) {
                onColorTargetSelected(ProfileColorTarget.Secondary)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomColorCircle(
                color = editor.customColorArgb.toProfileColor(),
                enabled = enabled,
                onClick = onToggleCustomColorPanel,
            )
            predefinedProfileColors.forEach { argb ->
                val selected = when (editor.selectedColorTarget) {
                    ProfileColorTarget.Primary -> config.colors.primaryArgb == argb
                    ProfileColorTarget.Secondary -> config.colors.secondaryArgb == argb
                }
                ColorCircle(color = argb.toProfileColor(), selected = selected, enabled = enabled) {
                    onColorApplied(argb)
                }
            }
        }

        if (editor.isCustomColorPanelVisible) {
            CustomColorSliders(
                argb = editor.customColorArgb,
                enabled = enabled,
                onColorChanged = { onCustomColorChanged(it, ChromaCoreInteractionMode.Precision) },
            )
        }
    }
}

private fun ProfileEditTarget.editorTitle(config: ProfileVisualConfig): String =
    when (this) {
        ProfileEditTarget.FotoPortada -> "Editar fotoportada"
        ProfileEditTarget.Username -> "Editar identidad"
        ProfileEditTarget.UserPhrase -> "Editar frase publica"
        ProfileEditTarget.ThemeColors -> "Editar atmosfera del perfil"
        is ProfileEditTarget.Highlight -> "Editar ${config.module(moduleId).profileLabel()}"
    }

@Composable
private fun EditorSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF101523), Color(0xFF0A0E16)),
                ),
            )
            .border(BorderStroke(1.dp, Color(0xFF20283A)), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun EditorChoiceButton(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) {
        Brush.horizontalGradient(listOf(Color(0xFF1C263D), Color(0xFF111827)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF0C1018), Color(0xFF0C1018)))
    }
    val borderColor = if (selected) Color(0xFF8AF1D6).copy(alpha = 0.76f) else Color(0xFF2A3144)

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.52f)
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun EditorActionButton(
    text: String,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.52f)
            .shadow(if (compact) 10.dp else 14.dp, shape)
            .widthIn(min = if (compact) 88.dp else 100.dp)
            .height(if (compact) 42.dp else 46.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF161B2D), Color(0xFF0F121E)),
                ),
            )
            .border(BorderStroke(1.dp, Color(0xFF2E385D)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ColorCircle(color: Color, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) Color(0xFF8AF1D6) else Color.White.copy(alpha = 0.48f),
                ),
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun CustomColorCircle(color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.68f)), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawLine(Color.White, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.4f)
            drawLine(Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2.4f)
        }
    }
}

@Composable
private fun CustomColorSliders(argb: Long, enabled: Boolean, onColorChanged: (Long) -> Unit) {
    val red = ((argb shr 16) and 0xFF).toInt()
    val green = ((argb shr 8) and 0xFF).toInt()
    val blue = (argb and 0xFF).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ColorSlider("R", red, enabled) { onColorChanged(argbFromRgb(it, green, blue)) }
        ColorSlider("G", green, enabled) { onColorChanged(argbFromRgb(red, it, blue)) }
        ColorSlider("B", blue, enabled) { onColorChanged(argbFromRgb(red, green, it)) }
    }
}

@Composable
private fun ColorSlider(label: String, value: Int, enabled: Boolean, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = Color(0xFFB6BED4), modifier = Modifier.width(18.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            enabled = enabled,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(value.toString(), color = Color(0xFFB6BED4), modifier = Modifier.width(34.dp))
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White.copy(alpha = 0.72f),
    focusedContainerColor = Color(0xFF0A0F18),
    unfocusedContainerColor = Color(0xFF0A0F18),
    disabledContainerColor = Color(0xFF0A0F18).copy(alpha = 0.7f),
    cursorColor = Color(0xFF8AF1D6),
    focusedBorderColor = Color(0xFF8AF1D6).copy(alpha = 0.72f),
    unfocusedBorderColor = Color(0xFF2A3144),
    disabledBorderColor = Color(0xFF2A3144).copy(alpha = 0.56f),
    focusedLabelColor = Color(0xFFE6EBF8),
    unfocusedLabelColor = Color(0xFF9EA7BE),
    disabledLabelColor = Color(0xFF9EA7BE).copy(alpha = 0.68f),
    focusedLeadingIconColor = Color.White,
    unfocusedLeadingIconColor = Color(0xFF9EA7BE),
)
