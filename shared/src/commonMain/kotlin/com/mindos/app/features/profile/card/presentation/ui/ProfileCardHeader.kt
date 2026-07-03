@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "LongParameterList", "MaxLineLength")

package com.mindos.app.features.profile.card.presentation.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.features.profile.card.domain.FlagOrTeam
import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.ReputationVote
import com.mindos.app.features.profile.card.domain.SecondaryBadge
import com.mindos.app.features.profile.card.presentation.CardMedia
import com.mindos.app.features.social.presentation.profile.ProfileMediaRenderSource
import com.mindos.app.features.social.presentation.profile.ProfilePlatformMediaPreview

/** The profile identity card: identity, ranks, and the pegatina/insignia/flag row. */
@Composable
fun ProfileCard(
    state: ProfileCardState,
    accent: Color,
    cardImage: CardMedia?,
    onOpenCardBg: () -> Unit,
    onCyclePegatina: () -> Unit,
    onToggleInsignia: () -> Unit,
    onToggleFlagTeam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(state.cardBackground.toBrush())
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(26.dp))
            .clickable(onClick = onOpenCardBg),
    ) {
        // picked photo / GIF / video background (session-only bytes) + legibility scrim
        if (cardImage != null) {
            ProfilePlatformMediaPreview(
                source = ProfileMediaRenderSource(
                    storagePath = cardImage.storagePath,
                    thumbnailStoragePath = null,
                    pendingPreviewBytes = cardImage.bytes,
                    isVideo = cardImage.isVideo,
                    contentDescription = "Fondo de la tarjeta",
                    directUrl = cardImage.downloadUrl,
                ),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = {},
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x57080A12), Color(0xDB04060C))),
                ),
            )
        }
        // top-right warm glaze
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                    radius = 520f,
                ),
            ),
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IdentityBlock(state = state, accent = accent, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                SecondaryBadgeColumn(state.secondaryBadges)
                Spacer(Modifier.width(12.dp))
                GeneralRankEmblem(state = state, accent = accent)
            }
            Spacer(Modifier.height(13.dp))
            CardBottomRow(
                state = state,
                accent = accent,
                onCyclePegatina = onCyclePegatina,
                onToggleInsignia = onToggleInsignia,
                onToggleFlagTeam = onToggleFlagTeam,
            )
        }
    }
}

@Composable
private fun IdentityBlock(state: ProfileCardState, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "PERFIL",
            color = accent.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                state.username,
                color = ProfileCardTokens.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                state.accountNumber.toString(),
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NIVEL ${state.level}", color = ProfileCardTokens.TextFaint, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(3.dp).clip(RoundedCornerShape(50)).background(ProfileCardTokens.TextGhost))
            Spacer(Modifier.width(6.dp))
            Text(
                if (state.isOnline) "EN LÍNEA" else "DESCONECTADO",
                color = ProfileCardTokens.TextFaint,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
            if (state.isOnline) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(ProfileCardTokens.Online))
            }
        }
    }
}

@Composable
private fun SecondaryBadgeColumn(badges: List<SecondaryBadge>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        badges.forEach { badge ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(badge.baseArgb), Color(badge.edgeArgb)),
                            center = androidx.compose.ui.geometry.Offset(0.35f, 0.25f),
                            radius = 60f,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.glyph, fontSize = 14.sp, color = ProfileCardTokens.White)
            }
        }
    }
}

@Composable
private fun GeneralRankEmblem(state: ProfileCardState, accent: Color) {
    val glow by rememberInfiniteTransition(label = "rankGlow").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "rankGlowAlpha",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 70.dp, height = 80.dp), contentAlignment = Alignment.Center) {
            // outer plate
            Box(
                Modifier.fillMaxSize().clip(RankHexagon)
                    .background(Brush.linearGradient(listOf(Color(0xFF5B6790), Color(0xFF222A44)))),
            )
            // inner well
            Box(
                Modifier.padding(3.dp).fillMaxSize().clip(RankHexagon)
                    .background(Brush.radialGradient(listOf(Color(0xFF2B3354), Color(0xFF12182C))))
                    .border(1.5.dp, accent.copy(alpha = 0.85f), RankHexagon),
            )
            // chevrons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(Modifier.size(width = 26.dp, height = 11.dp).clip(ChevronUp).background(accent))
                Box(
                    Modifier.size(width = 26.dp, height = 9.dp).clip(ChevronUp)
                        .background(Brush.verticalGradient(listOf(accent, ProfileCardTokens.GoldDeep))),
                )
                Box(
                    Modifier.size(width = 26.dp, height = 9.dp).clip(ChevronUp).alpha(0.7f)
                        .background(Brush.verticalGradient(listOf(accent, ProfileCardTokens.GoldDeep))),
                )
            }
            // animated glow edge
            Box(Modifier.fillMaxSize().clip(RankHexagon).border(2.dp, accent.copy(alpha = glow), RankHexagon))
        }
        Spacer(Modifier.height(7.dp))
        Text(
            state.generalRank.label.uppercase(),
            color = ProfileCardTokens.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun CardBottomRow(
    state: ProfileCardState,
    accent: Color,
    onCyclePegatina: () -> Unit,
    onToggleInsignia: () -> Unit,
    onToggleFlagTeam: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // pegatina
        SlotButton(modifier = Modifier.weight(1f), onClick = onCyclePegatina) {
            Text(state.pegatinaGlyph, fontSize = 18.sp)
            Spacer(Modifier.width(7.dp))
            Text("PEGATINA", color = ProfileCardTokens.TextFaint, fontSize = 8.sp, letterSpacing = 1.sp)
        }
        // insignia
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (state.insigniaOn) accent.copy(alpha = 0.10f) else Color(0x38000000))
                .border(
                    1.dp,
                    if (state.insigniaOn) accent.copy(alpha = 0.35f) else ProfileCardTokens.HairlineSoft,
                    RoundedCornerShape(13.dp),
                )
                .clickable(onClick = onToggleInsignia),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 13.sp)
                Text("⚡", fontSize = 12.sp)
                Text("🔥", fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text("+${state.insigniaCount}", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        // flag / team
        Box(
            modifier = Modifier
                .weight(1.15f)
                .height(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, ProfileCardTokens.HairlineSoft, RoundedCornerShape(13.dp))
                .clickable(onClick = onToggleFlagTeam),
            contentAlignment = Alignment.Center,
        ) {
            if (state.flagOrTeam == FlagOrTeam.FLAG) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFAA151B)))
                        Box(Modifier.weight(2f).fillMaxWidth().background(Color(0xFFF1BF00)))
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFAA151B)))
                    }
                    Text(
                        "ESP",
                        color = ProfileCardTokens.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 5.dp),
                    )
                }
            } else {
                Box(
                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF13284F), Color(0xFF0A1731)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEU", color = Color(0xFFF5EFE0), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("EQUIPO", color = Color(0xFFC9A24B), fontSize = 7.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0x38000000))
            .border(1.dp, ProfileCardTokens.HairlineSoft, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

/** Friend / block / mute / reputation actions row. */
@Composable
fun ProfileActionsRow(
    state: ProfileCardState,
    accent: Color,
    onFriend: () -> Unit,
    onBlock: () -> Unit,
    onMute: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().height(46.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // friend
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
                .then(
                    if (state.isFriend) Modifier.background(Color(0x14FFFFFF))
                    else Modifier.background(Brush.verticalGradient(listOf(accent, ProfileCardTokens.GoldDeep))),
                )
                .clickable(onClick = onFriend),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.isFriend) "✓" else "+", color = if (state.isFriend) ProfileCardTokens.White else ProfileCardTokens.ScreenInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(7.dp))
                Text(
                    if (state.isFriend) "Amigos" else "Añadir",
                    color = if (state.isFriend) ProfileCardTokens.White else ProfileCardTokens.ScreenInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // block
        IconAction(selected = state.isBlocked, selectedColor = ProfileCardTokens.Negative, onClick = onBlock) {
            Text("⊘", fontSize = 17.sp, color = if (state.isBlocked) ProfileCardTokens.Negative else ProfileCardTokens.TextMuted)
        }
        // mute
        IconAction(selected = state.isMuted, selectedColor = accent, onClick = onMute, activeWhenUnselected = true) {
            Text(if (state.isMuted) "🔕" else "🔔", fontSize = 16.sp)
        }
        // reputation
        Row(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
                .border(1.dp, ProfileCardTokens.HairlineSoft, RoundedCornerShape(15.dp)),
        ) {
            ReputationHalf(
                modifier = Modifier.weight(1f),
                glyph = "👎",
                count = state.dislikeCount,
                active = state.reputation == ReputationVote.DISLIKE,
                activeBrush = Brush.verticalGradient(listOf(Color(0xFFE0556B), Color(0xFF8F2436))),
                idleColor = Color(0xFF1A0D10),
                onClick = onDislike,
            )
            Box(Modifier.width(1.dp).fillMaxSize().background(ProfileCardTokens.HairlineSoft))
            ReputationHalf(
                modifier = Modifier.weight(1f),
                glyph = "👍",
                count = state.likeCount,
                active = state.reputation == ReputationVote.LIKE,
                activeBrush = Brush.verticalGradient(listOf(Color(0xFF3FBF8F), Color(0xFF1E7A55))),
                idleColor = Color(0xFF0D1A14),
                onClick = onLike,
            )
        }
    }
}

@Composable
private fun IconAction(
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    activeWhenUnselected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val highlighted = if (activeWhenUnselected) !selected else selected
    Box(
        modifier = Modifier
            .width(48.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(if (highlighted) selectedColor.copy(alpha = 0.16f) else Color(0x0DFFFFFF))
            .border(
                1.dp,
                if (highlighted) selectedColor.copy(alpha = 0.4f) else ProfileCardTokens.HairlineSoft,
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ReputationHalf(
    glyph: String,
    count: Int,
    active: Boolean,
    activeBrush: Brush,
    idleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (active) Modifier.background(activeBrush) else Modifier.background(idleColor))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(glyph, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Text(formatThousands(count), color = ProfileCardTokens.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Spanish-style thousands grouping with '.' (e.g. 1284 → "1.284"). */
private fun formatThousands(value: Int): String {
    val digits = value.toString()
    val sb = StringBuilder()
    val len = digits.length
    for (i in 0 until len) {
        if (i > 0 && (len - i) % 3 == 0) sb.append('.')
        sb.append(digits[i])
    }
    return sb.toString()
}
