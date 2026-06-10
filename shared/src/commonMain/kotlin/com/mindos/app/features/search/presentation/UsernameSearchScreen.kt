package com.mindos.app.features.search.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mindos.app.features.search.domain.UsernameSearchUser
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UsernameSearchScreen(
    viewModel: UsernameSearchViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onUserSelected: (UsernameSearchUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onModeChanged(
            if (pagerState.currentPage == 0) UsernameSearchMode.Radar else UsernameSearchMode.ZeroUi,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBlack),
    ) {
        SearchAtmosphere(
            pageBlend = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            SearchTopBar(
                activePage = pagerState.currentPage,
                onBack = onBack,
                onModeSelected = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - (offset * 0.26f)
                            scaleX = 1f - (offset * 0.035f)
                            scaleY = 1f - (offset * 0.035f)
                        },
                ) {
                    if (page == 0) {
                        UsernameRadarMode(
                            state = state,
                            currentUserId = currentUserId,
                            onQueryChanged = viewModel::onQueryChanged,
                            onResultSelected = { user ->
                                viewModel.select(user, user.userId == currentUserId)
                                onUserSelected(user)
                            },
                        )
                    } else {
                        ZeroUiSearchMode(
                            state = state,
                            currentUserId = currentUserId,
                            onQueryChanged = viewModel::onQueryChanged,
                            onResultSelected = { user ->
                                viewModel.select(user, user.userId == currentUserId)
                                onUserSelected(user)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    activePage: Int,
    onBack: () -> Unit,
    onModeSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NeuMicroButton(text = "INICIO", onClick = onBack)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModePill(
                text = "RADAR",
                selected = activePage == 0,
                onClick = { onModeSelected(0) },
            )
            ModePill(
                text = "ZERO",
                selected = activePage == 1,
                onClick = { onModeSelected(1) },
            )
        }
    }
}

@Composable
private fun SearchAtmosphere(
    pageBlend: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(color = NeuBlack)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeuGreen.copy(alpha = 0.18f - pageBlend * 0.08f),
                    Color.Transparent,
                ),
                center = Offset(size.width * (0.22f + pageBlend * 0.34f), size.height * 0.24f),
                radius = size.minDimension * 0.72f,
            ),
        )
        drawLine(
            color = NeuGreen.copy(alpha = 0.08f),
            start = Offset(0f, size.height * (0.18f + pageBlend * 0.16f)),
            end = Offset(size.width, size.height * (0.04f + pageBlend * 0.22f)),
            strokeWidth = 1f,
        )
    }
}
