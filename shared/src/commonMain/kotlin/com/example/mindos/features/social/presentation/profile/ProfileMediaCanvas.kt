package com.example.mindos.features.social.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.features.social.domain.model.ProfileLayoutSlot
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModule
import com.example.mindos.features.social.domain.model.ProfileModuleContentType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileModuleSize
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun ProfileMediaCanvas(
    config: ProfileVisualConfig,
    pendingMedia: Map<ProfileModuleId, ProfileMediaUpload>,
    primary: Color,
    secondary: Color,
    isEditing: Boolean,
    onModuleMoved: (ProfileModuleId, ProfileLayoutSlot) -> Unit,
    onOpenMedia: (com.example.mindos.features.social.domain.model.AppMediaContent) -> Unit,
) {
    val density = LocalDensity.current
    var draggingModuleId by remember { mutableStateOf<ProfileModuleId?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(324.dp)
            .heightIn(min = 286.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(secondary.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, secondary.copy(alpha = 0.38f)), RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        val canvasWidth = maxWidth
        val canvasHeight = 304.dp
        val anchors = remember(canvasWidth, canvasHeight) { buildModuleAnchors(canvasWidth, canvasHeight) }
        val anchorsPx = anchors.mapValues { (_, anchor) -> anchor.toPx(density) }

        Box(modifier = Modifier.size(canvasWidth, canvasHeight)) {
            config.modules.forEach { module ->
                val anchor = anchors.getValue(module.slot)
                val anchorPx = anchorsPx.getValue(module.slot)
                val activeOffset = if (draggingModuleId == module.id) dragOffset else Offset.Zero
                ProfileModuleSurface(
                    module = module,
                    pendingUpload = pendingMedia[module.id],
                    primary = primary,
                    secondary = secondary,
                    isEditing = isEditing,
                    onOpenMedia = onOpenMedia,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (anchorPx.left + activeOffset.x).roundToInt(),
                                y = (anchorPx.top + activeOffset.y).roundToInt(),
                            )
                        }
                        .size(anchor.width, anchor.height)
                        .pointerInput(isEditing, module.id, module.slot) {
                            if (!isEditing) return@pointerInput
                            detectDragGestures(
                                onDragStart = {
                                    draggingModuleId = module.id
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingModuleId = null
                                    dragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    val currentRect = anchorsPx.getValue(module.slot)
                                    val finalCenter = currentRect.center + dragOffset
                                    onModuleMoved(module.id, nearestDropSlot(module.id, finalCenter, anchorsPx))
                                    draggingModuleId = null
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun ProfileModuleSurface(
    module: ProfileModule,
    pendingUpload: ProfileMediaUpload?,
    primary: Color,
    secondary: Color,
    isEditing: Boolean,
    onOpenMedia: (com.example.mindos.features.social.domain.model.AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(primary.copy(alpha = if (module.size == ProfileModuleSize.Large) 0.78f else 0.64f))
            .border(BorderStroke(if (module.size == ProfileModuleSize.Large) 1.6.dp else 1.dp, secondary), shape)
            .let { 
                if (!isEditing && module.contentType != ProfileModuleContentType.Empty && module.contentType != ProfileModuleContentType.Text) {
                    it.clickable { 
                        module.toAppMediaContent()?.let { content -> onOpenMedia(content) }
                    }
                } else it
            },
    ) {
        val source = module.toRenderSource(pendingUpload)
        if (module.contentType == ProfileModuleContentType.Text && module.text != null) {
            ProfileTextModule(text = module.text, secondary = secondary, modifier = Modifier.fillMaxSize().padding(12.dp))
        } else if (source != null) {
            ProfilePlatformMediaPreview(source = source, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) {
                ProfileEmptyModule(module = module, secondary = secondary, isEditing = isEditing)
            }
            if (source.isVideo) VideoBadge(secondary = secondary, modifier = Modifier.align(Alignment.Center))
        } else {
            ProfileEmptyModule(module = module, secondary = secondary, isEditing = isEditing)
        }

        if (isEditing) {
            Text(
                text = module.profileLabel(),
                color = secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.32f))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProfileTextModule(text: String, secondary: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = secondary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileEmptyModule(module: ProfileModule, secondary: Color, isEditing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(secondary.copy(alpha = 0.07f))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                isEditing && module.size == ProfileModuleSize.Large -> "Foto o video principal"
                isEditing -> "Foto, video o texto"
                module.size == ProfileModuleSize.Large -> "Módulo principal"
                else -> "Modulo visual"
            },
            color = secondary.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VideoBadge(secondary: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, secondary.copy(alpha = 0.9f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.12f)
                lineTo(size.width * 0.84f, size.height * 0.5f)
                lineTo(size.width * 0.24f, size.height * 0.88f)
                close()
            }
            drawPath(path, color = secondary)
        }
    }
}

private data class ModuleAnchor(val slot: ProfileLayoutSlot, val x: Dp, val y: Dp, val width: Dp, val height: Dp)

private data class ModuleRectPx(val left: Float, val top: Float, val width: Float, val height: Float) {
    val center: Offset
        get() = Offset(left + width / 2f, top + height / 2f)
}

private fun buildModuleAnchors(maxWidth: Dp, maxHeight: Dp): Map<ProfileLayoutSlot, ModuleAnchor> {
    val gap = 10.dp
    val largeWidth = (maxWidth - gap) * 0.64f
    val smallWidth = maxWidth - gap - largeWidth
    val smallHeight = (maxHeight - gap) / 2f
    return mapOf(
        ProfileLayoutSlot.LargeLeft to ModuleAnchor(ProfileLayoutSlot.LargeLeft, 0.dp, 0.dp, largeWidth, maxHeight),
        ProfileLayoutSlot.LargeRight to ModuleAnchor(ProfileLayoutSlot.LargeRight, smallWidth + gap, 0.dp, largeWidth, maxHeight),
        ProfileLayoutSlot.SmallTopLeft to ModuleAnchor(ProfileLayoutSlot.SmallTopLeft, 0.dp, 0.dp, smallWidth, smallHeight),
        ProfileLayoutSlot.SmallBottomLeft to ModuleAnchor(ProfileLayoutSlot.SmallBottomLeft, 0.dp, smallHeight + gap, smallWidth, smallHeight),
        ProfileLayoutSlot.SmallTopRight to ModuleAnchor(ProfileLayoutSlot.SmallTopRight, largeWidth + gap, 0.dp, smallWidth, smallHeight),
        ProfileLayoutSlot.SmallBottomRight to ModuleAnchor(ProfileLayoutSlot.SmallBottomRight, largeWidth + gap, smallHeight + gap, smallWidth, smallHeight),
    )
}

private fun ModuleAnchor.toPx(density: Density): ModuleRectPx =
    with(density) { ModuleRectPx(x.toPx(), y.toPx(), width.toPx(), height.toPx()) }

private fun nearestDropSlot(
    moduleId: ProfileModuleId,
    finalCenter: Offset,
    anchors: Map<ProfileLayoutSlot, ModuleRectPx>,
): ProfileLayoutSlot {
    val allowedSlots = if (moduleId == ProfileModuleId.Main) {
        listOf(ProfileLayoutSlot.LargeLeft, ProfileLayoutSlot.LargeRight)
    } else {
        ProfileLayoutSlot.entries.filter { it.acceptsSmall }
    }
    return allowedSlots.minBy { slot ->
        val center = anchors.getValue(slot).center
        sqrt((center.x - finalCenter.x).pow(2) + (center.y - finalCenter.y).pow(2))
    }
}

private fun ProfileModule.toRenderSource(upload: ProfileMediaUpload?): ProfileMediaRenderSource? {
    upload?.let {
        val isVideo = it.kind == ProfileMediaUploadKind.Video
        return ProfileMediaRenderSource(
            storagePath = null,
            thumbnailStoragePath = null,
            pendingPreviewBytes = if (isVideo) it.thumbnailBytes else it.bytes,
            isVideo = isVideo,
            contentDescription = profileLabel(),
        )
    }
    val media = media ?: return null
    return ProfileMediaRenderSource(
        storagePath = media.storagePath,
        thumbnailStoragePath = media.thumbnailStoragePath,
        pendingPreviewBytes = null,
        isVideo = contentType == ProfileModuleContentType.Video || media.mimeType.startsWith("video/"),
        contentDescription = profileLabel(),
    )
}

private fun ProfileModule.toAppMediaContent(): com.example.mindos.features.social.domain.model.AppMediaContent? {
    val media = media ?: return null
    return com.example.mindos.features.social.domain.model.AppMediaContent(
        url = media.url,
        storagePath = media.storagePath,
        fileName = profileLabel(),
        mimeType = media.mimeType,
        sizeBytes = 0, // Profile modules don't always have size in the current model
        thumbnailUrl = media.thumbnailUrl ?: media.thumbnailStoragePath
    )
}
