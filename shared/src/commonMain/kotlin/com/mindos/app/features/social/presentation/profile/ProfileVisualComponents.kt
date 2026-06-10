package com.mindos.app.features.social.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindos.app.features.social.domain.model.IdentityPosition
import com.mindos.app.features.social.domain.model.ProfileCategoryIcon
import com.mindos.app.features.social.domain.model.ProfileLayoutSlot
import com.mindos.app.features.social.domain.model.ProfileMediaUpload
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.domain.model.ProfileVisualConfig

@Composable
internal fun ProfileVisualContainer(
    config: ProfileVisualConfig,
    pendingMedia: Map<ProfileModuleId, ProfileMediaUpload>,
    isEditing: Boolean,
    isOwner: Boolean,
    canFollow: Boolean,
    isFollowing: Boolean,
    onObserveClick: () -> Unit,
    onModuleMoved: (ProfileModuleId, ProfileLayoutSlot) -> Unit,
    onOpenMedia: (com.mindos.app.features.social.domain.model.AppMediaContent) -> Unit,
) {
    val primary = config.colors.primaryArgb.toProfileColor()
    val secondary = config.colors.secondaryArgb.toProfileColor()
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(primary)
            .border(BorderStroke(1.dp, secondary.copy(alpha = 0.72f)), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (config.identityPosition == IdentityPosition.Top) {
            ProfileIdentityBlock(config, isOwner, canFollow, isFollowing, primary, secondary, onObserveClick)
        }
        ProfileMediaCanvas(
            config = config,
            pendingMedia = pendingMedia,
            primary = primary,
            secondary = secondary,
            isEditing = isEditing,
            onModuleMoved = onModuleMoved,
            onOpenMedia = onOpenMedia,
        )
        if (config.identityPosition == IdentityPosition.Bottom) {
            ProfileIdentityBlock(config, isOwner, canFollow, isFollowing, primary, secondary, onObserveClick)
        }
    }
}

@Composable
private fun ProfileIdentityBlock(
    config: ProfileVisualConfig,
    isOwner: Boolean,
    canFollow: Boolean,
    isFollowing: Boolean,
    primary: Color,
    secondary: Color,
    onObserveClick: () -> Unit,
) {
    val circles = @Composable {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            config.categoryIcons.take(3).forEach { icon ->
                ProfileCategoryCircle(icon = icon, primary = primary, secondary = secondary)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        if (config.identityPosition == IdentityPosition.Bottom) circles()

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = config.displayName,
                color = secondary,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "@${config.username.removePrefix("@")}",
                color = secondary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.72f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            ObserveEyeButton(
                label = when {
                    isOwner -> "Propio"
                    isFollowing -> "Observando"
                    canFollow -> "Observar"
                    else -> "Ojo"
                },
                primary = primary,
                secondary = secondary,
                enabled = !isOwner && canFollow,
                onClick = onObserveClick,
            )
        }

        Text(
            text = "${config.observersCount} observadores",
            color = secondary.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (config.identityPosition == IdentityPosition.Top) circles()
    }
}

@Composable
private fun ObserveEyeButton(
    label: String,
    primary: Color,
    secondary: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(primary.copy(alpha = 0.88f), secondary)))
            .border(BorderStroke(1.dp, secondary.copy(alpha = 0.86f)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(17.dp)) {
            drawEye(color = if (enabled) Color.White else Color.White.copy(alpha = 0.72f))
        }
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun ProfileCategoryCircle(
    icon: ProfileCategoryIcon,
    primary: Color,
    secondary: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(secondary.copy(alpha = 0.13f))
            .border(BorderStroke(1.dp, secondary.copy(alpha = 0.84f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(21.dp)) {
            when (icon) {
                ProfileCategoryIcon.Art -> drawArtIcon(primary = primary, secondary = secondary)
                ProfileCategoryIcon.Atom -> drawAtomIcon(secondary)
                ProfileCategoryIcon.Cinema -> drawCinemaIcon(secondary)
            }
        }
    }
}

private fun DrawScope.drawEye(color: Color) {
    val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.06f, size.height * 0.5f)
        quadraticTo(size.width * 0.5f, -size.height * 0.05f, size.width * 0.94f, size.height * 0.5f)
        quadraticTo(size.width * 0.5f, size.height * 1.05f, size.width * 0.06f, size.height * 0.5f)
        close()
    }
    drawPath(path, color = color, style = stroke)
    drawCircle(color = color, radius = size.minDimension * 0.16f, center = center)
}

private fun DrawScope.drawArtIcon(primary: Color, secondary: Color) {
    drawCircle(secondary, radius = size.minDimension * 0.16f, center = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.34f))
    drawCircle(secondary.copy(alpha = 0.74f), radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f))
    drawCircle(secondary.copy(alpha = 0.62f), radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.7f, size.height * 0.42f))
    drawLine(
        color = secondary,
        start = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.74f),
        end = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.58f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round,
    )
    drawCircle(primary.copy(alpha = 0.8f), radius = size.minDimension * 0.09f, center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.58f))
}

private fun DrawScope.drawAtomIcon(color: Color) {
    val stroke = Stroke(width = 1.8f)
    drawCircle(color, radius = size.minDimension * 0.1f, center = center)
    repeat(3) { index ->
        rotate(degrees = index * 60f, pivot = center) {
            drawOval(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.14f, size.height * 0.34f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height * 0.32f),
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawCinemaIcon(color: Color) {
    val stroke = Stroke(width = 1.9f)
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.32f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.46f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        style = stroke,
    )
    drawCircle(color, radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.24f), style = stroke)
    drawCircle(color, radius = size.minDimension * 0.12f, center = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.24f), style = stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.52f), androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.52f), strokeWidth = 1.5f)
}
