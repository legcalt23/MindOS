package com.example.mindos.features.social.presentation.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindos.features.social.domain.model.AppMediaContent
import com.example.mindos.features.social.domain.model.ProfileLayoutSlot
import com.example.mindos.features.social.domain.model.ProfileModule
import com.example.mindos.features.social.domain.model.ProfileModuleContentType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileModuleSize
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverCubeCarousel(
    modules: List<ProfileModule>,
    onOpenMedia: (AppMediaContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val faces = remember(modules) {
        modules
            .filter { it.contentType != ProfileModuleContentType.Empty || !it.text.isNullOrBlank() || it.media != null }
            .ifEmpty { listOf(modules.firstOrNull() ?: emptyCarouselModule()) }
    }
    val shape = RoundedCornerShape(22.dp)

    if (faces.size <= 1) {
        Box(
            modifier = modifier
                .shadow(18.dp, shape)
                .clip(shape)
                .background(CoverSurface)
                .border(1.dp, CoverBorder, shape),
        ) {
            CoverCubeFace(
                module = faces.first(),
                faceFraction = 0f,
                onOpenMedia = onOpenMedia,
            )
        }
        return
    }

    val virtualPageCount = Int.MAX_VALUE
    val initialPage = remember(faces.size) {
        val midpoint = virtualPageCount / 2
        midpoint - (midpoint % faces.size)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount },
    )

    Box(
        modifier = modifier
            .shadow(18.dp, shape)
            .clip(shape)
            .background(CoverSurface)
            .border(1.dp, CoverBorder, shape),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { page ->
            val module = faces[page.floorMod(faces.size)]
            val rawFaceFraction = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val faceFraction = rawFaceFraction.coerceIn(-1f, 1f)
            val normalizedFraction = abs(faceFraction)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .graphicsLayer {
                        if (rawFaceFraction < -1f || rawFaceFraction > 1f) {
                            alpha = 0f
                            return@graphicsLayer
                        }
                        alpha = 1f
                        transformOrigin = TransformOrigin(0.5f, if (faceFraction > 0f) 0f else 1f)
                        rotationX = -90f * faceFraction
                        cameraDistance = 14f * density
                        shadowElevation = (22f * density) * (1f - normalizedFraction)
                        clip = true
                    },
            ) {
                CoverCubeFace(
                    module = module,
                    faceFraction = faceFraction,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}

@Composable
private fun CoverCubeFace(
    module: ProfileModule,
    faceFraction: Float,
    onOpenMedia: (AppMediaContent) -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val mediaContent = module.toCoverMediaContent()
    val mediaSource = module.toCoverRenderSource()
    val edgeAlpha = (0.18f * abs(faceFraction)).coerceIn(0f, 0.18f)
    val sheenAlpha = (0.07f * (1f - abs(faceFraction))).coerceIn(0f, 0.07f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(CoverSurface)
            .then(
                if (mediaContent != null) {
                    Modifier.clickable { onOpenMedia(mediaContent) }
                } else {
                    Modifier
                }
            ),
    ) {
        when {
            mediaSource != null -> {
                ProfilePlatformMediaPreview(
                    source = mediaSource,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) {
                    EmptyCubeFace()
                }
            }

            !module.text.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoverSurface, CoverBlack),
                            ),
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = module.text.orEmpty(),
                        color = CoverTextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp,
                        ),
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            else -> EmptyCubeFace()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = sheenAlpha),
                            Color.Transparent,
                            CoverBlack.copy(alpha = 0.16f + edgeAlpha),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(if (faceFraction >= 0f) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = edgeAlpha * 0.85f)),
        )
    }
}

@Composable
private fun EmptyCubeFace() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(CoverSurface, CoverBlack),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CoverBorder)
        )
    }
}

private fun emptyCarouselModule(): ProfileModule =
    ProfileModule(
        id = ProfileModuleId.SmallOne,
        size = ProfileModuleSize.Small,
        slot = ProfileLayoutSlot.SmallTopRight,
        contentType = ProfileModuleContentType.Empty,
    )

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
