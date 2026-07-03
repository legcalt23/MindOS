@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "MaxLineLength", "CyclomaticComplexMethod")

package com.mindos.app.features.profile.card.presentation.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.components.PhotoUploadLoader
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.ProfilePanel
import com.mindos.app.features.profile.card.domain.ProfilePost
import com.mindos.app.features.profile.card.domain.PostKind
import com.mindos.app.features.profile.card.presentation.ProfileCardLoadStatus
import com.mindos.app.features.profile.card.presentation.ProfileCardMediaStatus
import com.mindos.app.features.profile.card.presentation.ProfileCardStateHolder
import com.mindos.app.features.social.domain.model.ProfileMediaUploadKind
import com.mindos.app.features.social.domain.model.ProfileModuleId
import com.mindos.app.features.social.presentation.profile.rememberProfileMediaPicker

private enum class OpenSheet { NONE, COLOR, CARD_BG, PREVIEW }

private enum class PickTarget { CARD, CANVAS }

/**
 * The MindBoost application profile card (design: Perfil.dc.html). This is the
 * social/identity surface (NOT the cognitive "brain" profile, which is a separate
 * screen to the left). Renders full-bleed; the phone bezel in the design is a
 * Cloud Design preview artifact only.
 */
@Composable
fun ProfileCardScreen(
    holder: ProfileCardStateHolder,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = holder.state
    val accent = ProfileCardTokens.accent(state.accentArgb)

    var openSheet by remember { mutableStateOf(OpenSheet.NONE) }
    var colorTarget by remember { mutableStateOf(ColorTarget.SCREEN) }
    var previewPost by remember { mutableStateOf<ProfilePost?>(null) }

    fun closeSheets() {
        openSheet = OpenSheet.NONE
    }

    // Real photo / GIF / video picking (reuses the social media picker — system
    // picker, no extra runtime permission; image/* includes GIF).
    var pickTarget by remember { mutableStateOf<PickTarget?>(null) }
    val mediaPicker = rememberProfileMediaPicker(
        onPicked = { _, upload ->
            val target = pickTarget
            pickTarget = null
            when (target) {
                PickTarget.CARD -> {
                    holder.setCardImage(upload.bytes, upload.mimeType)
                    closeSheets()
                }
                PickTarget.CANVAS -> holder.addCanvasMedia(upload.bytes, upload.mimeType)
                null -> Unit
            }
        },
        onError = { pickTarget = null },
    )
    fun pickMedia(target: PickTarget) {
        pickTarget = target
        mediaPicker.pick(ProfileModuleId.Main, ProfileMediaUploadKind.Photo)
    }

    PlatformBackHandler(enabled = true) {
        if (openSheet != OpenSheet.NONE) closeSheets() else onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(state.screenBackground.toBrush())) {
        // bottom vignette for depth (reliable vertical gradient)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))),
            ),
        )

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // customize-bg hint
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color(0x0FFFFFFF))
                        .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(100))
                        .clickable { colorTarget = ColorTarget.SCREEN; openSheet = OpenSheet.COLOR }
                        .padding(horizontal = 13.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(
                            Brush.sweepGradient(
                                listOf(Color(0xFFE0556B), Color(0xFFE8C77A), Color(0xFF3FBF8F), Color(0xFF4F7CFF), Color(0xFFE0556B)),
                            ),
                        ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("COLOR PERSONALIZADO", color = ProfileCardTokens.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            ProfileCard(
                state = state,
                accent = accent,
                cardImage = holder.mediaFor((state.cardBackground as? BackgroundPaint.Image)?.ref),
                onOpenCardBg = { openSheet = OpenSheet.CARD_BG },
                onCyclePegatina = holder::cyclePegatina,
                onToggleInsignia = holder::toggleInsignia,
                onToggleFlagTeam = holder::toggleFlagTeam,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            val bgStatus = holder.backgroundStatus
            if (bgStatus != ProfileCardMediaStatus.Idle && bgStatus != ProfileCardMediaStatus.Saved) {
                Spacer(Modifier.height(8.dp))
                BackgroundMediaStatusBar(
                    status = bgStatus,
                    accent = accent,
                    onRetry = holder::retryBackgroundMedia,
                    onDiscard = holder::discardBackgroundMedia,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            ProfileActionsRow(
                state = state,
                accent = accent,
                onFriend = holder::toggleFriend,
                onBlock = holder::toggleBlock,
                onMute = holder::toggleMute,
                onLike = holder::setLike,
                onDislike = holder::setDislike,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(13.dp))
            // content zone (shared box) — direct switch (no Crossfade) so the panel always paints
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x0DFFFFFF))
                    .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(20.dp)),
            ) {
                when (state.activePanel) {
                    ProfilePanel.CANVAS -> CanvasPanel(
                        state = state,
                        accent = accent,
                        mediaFor = holder::mediaFor,
                        onSelect = holder::selectCanvasItem,
                        onMove = holder::moveCanvasItem,
                        onResize = holder::resizeCanvasItem,
                        onFront = holder::bringToFront,
                        onBack = holder::sendToBack,
                        onRemove = holder::removeCanvasItem,
                        failedItemIds = holder.canvasUploadFailures,
                        onRetry = holder::retryCanvasMedia,
                        onAdd = { pickMedia(PickTarget.CANVAS) },
                    )
                    ProfilePanel.POSTS -> PostsPanel(
                        posts = state.posts,
                        accent = accent,
                        onOpenPost = { previewPost = it; openSheet = OpenSheet.PREVIEW },
                    )
                    ProfilePanel.STATS -> StatsPanel(state = state, accent = accent)
                }
            }

            // dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfilePanel.entries.forEach { panel ->
                    val active = state.activePanel == panel
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (active) 22.dp else 8.dp)
                            .clip(RoundedCornerShape(100))
                            .background(if (active) accent else Color(0x40FFFFFF))
                            .clickable { holder.selectPanel(panel) },
                    )
                }
            }

            // sponsor
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(11.dp)).background(Color(0x08FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(11.dp)).padding(7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("SPONSOR", color = ProfileCardTokens.TextGhost, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.navigationBarsPadding())
        }

        // ── overlays ──
        SheetScrim(visible = openSheet == OpenSheet.COLOR || openSheet == OpenSheet.CARD_BG, onClose = ::closeSheets)

        Box(Modifier.align(Alignment.BottomCenter)) {
            ColorPickerSheet(
                visible = openSheet == OpenSheet.COLOR,
                target = colorTarget,
                initialArgb = if (colorTarget == ColorTarget.SCREEN) state.accentArgb else cardSolidArgb(state),
                recent = state.recentColors,
                saved = state.savedColors,
                onApply = { argb ->
                    if (colorTarget == ColorTarget.SCREEN) holder.applyAccentAndScreen(argb) else holder.applyCardSolid(argb)
                    closeSheets()
                },
                onSaveColor = holder::saveColor,
                onPickGradient = { stops, kind -> holder.applyScreenGradient(stops, kind); closeSheets() },
                onClose = ::closeSheets,
            )
            CardBackgroundSheet(
                visible = openSheet == OpenSheet.CARD_BG,
                username = state.username,
                hasMedia = state.cardBackground is BackgroundPaint.Image,
                onOpenSolid = { colorTarget = ColorTarget.CARD; openSheet = OpenSheet.COLOR },
                onPickImage = { pickMedia(PickTarget.CARD) },
                onReset = { holder.resetCardBackground(); closeSheets() },
                onClose = ::closeSheets,
            )
        }

        PostPreviewModal(
            visible = openSheet == OpenSheet.PREVIEW,
            title = previewPost.previewTitle(),
            meta = previewPost.previewMeta(),
            gradient = previewPost?.let { it.gradientStart to it.gradientEnd },
            isVideo = previewPost?.kind == PostKind.VIDEO,
            onClose = ::closeSheets,
        )

        // Loading overlay: covers the default-content flash while restoring from Firestore.
        if (holder.loadStatus == ProfileCardLoadStatus.LoadingRemoteProfile) {
            ProfileCardLoadingOverlay(accent = accent, background = state.screenBackground.toBrush())
        }
    }
}

/** Opaque loading veil shown while the card restores from Firestore. */
@Composable
private fun ProfileCardLoadingOverlay(accent: Color, background: Brush) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("Cargando tu perfil…", color = ProfileCardTokens.TextMuted, fontSize = 12.sp)
        }
    }
}

private fun cardSolidArgb(state: com.mindos.app.features.profile.card.domain.ProfileCardState): Long =
    (state.cardBackground as? com.mindos.app.features.profile.card.domain.BackgroundPaint.Solid)?.argb ?: state.accentArgb

private fun ProfilePost?.previewTitle(): String = when (this?.kind) {
    PostKind.VIDEO -> "Vídeo"
    PostKind.GIF -> "GIF animado"
    PostKind.IMAGE -> "Imagen"
    null -> ""
}

private fun ProfilePost?.previewMeta(): String =
    if (this == null) "" else "Publicación ${index + 1} · hace ${index + 1} d"

/** Explicit background-media status (uploading / saving / error with retry+discard). */
@Composable
private fun BackgroundMediaStatusBar(
    status: ProfileCardMediaStatus,
    accent: Color,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message: String
    val isError: Boolean
    when (status) {
        ProfileCardMediaStatus.Uploading -> { message = "Subiendo fondo…"; isError = false }
        ProfileCardMediaStatus.Saving -> { message = "Publicando…"; isError = false }
        is ProfileCardMediaStatus.UploadFailed -> { message = status.message; isError = true }
        is ProfileCardMediaStatus.SaveFailed -> { message = status.message; isError = true }
        ProfileCardMediaStatus.Idle, ProfileCardMediaStatus.Saved -> { message = ""; isError = false }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (isError) Color(0x1FE0556B) else Color(0x14FFFFFF))
            .border(
                1.dp,
                if (isError) Color(0x66E0556B) else ProfileCardTokens.HairlineSoft,
                RoundedCornerShape(13.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == ProfileCardMediaStatus.Uploading) {
            // Background image is uploading (local preview already shown) → the
            // dedicated non-blocking photo-upload loader from the global system.
            PhotoUploadLoader(size = 26.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            message,
            color = if (isError) ProfileCardTokens.Negative else ProfileCardTokens.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        if (isError) {
            Text(
                "Reintentar",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRetry).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Descartar",
                color = ProfileCardTokens.TextFaint,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onDiscard).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
