package com.mindos.app.features.social.presentation.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileSettingsScreen(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditProfileCoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080A12)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProfileSettingsHeader(
                title = "Configuración del perfil",
                onBackClick = onBackClick,
            )

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                ProfileSettingsActionButton(
                    text = "Editar perfil",
                    onClick = onEditProfileClick,
                    isPrimary = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileSettingsActionButton(
                    text = "Editar portada del perfil",
                    onClick = onEditProfileCoverClick,
                    isPrimary = false,
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingsHeader(
    title: String,
    onBackClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "ProfileSettingsBackScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.CenterStart)
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF131624))
                .border(1.dp, Color(0xFF2A2F45).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBackClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ProfileSettingsActionButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "ProfileSettingsButtonScale",
    )
    val shape = RoundedCornerShape(20.dp)

    val backgroundModifier = if (isPrimary) {
        Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF161B2D), Color(0xFF0F121E)),
            ),
        )
    } else {
        Modifier.background(Color(0xFF0C0E17))
    }

    val borderModifier = if (isPrimary) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2E385D), Color(0xFF1A1F35)),
            ),
            shape = shape,
        )
    } else {
        Modifier.border(width = 1.dp, color = Color(0xFF171A29), shape = shape)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .scale(scale)
            .clip(shape)
            .then(backgroundModifier)
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isPrimary) Color.White else Color(0xFFB5B9D6),
            fontSize = 16.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}
