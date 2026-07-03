@file:Suppress("FunctionNaming") // Compose UI function uses PascalCase.

package com.mindos.app.features.mindboost.profile.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mindos.app.features.mindboost.profile.presentation.rememberMindBoostBrainProfileStateHolder
import kotlinx.coroutines.launch

/**
 * Own-profile pager: page 0 is the app profile card; swiping LEFT reveals page 1, the cognitive
 * "brain profile" (Fase 2A), wired to the real encrypted module signals. The brain profile's back
 * gesture animates back to the card. The card is provided as a slot so Home keeps building it as before.
 */
@Composable
fun ProfileWithBrainProfilePager(
    userId: String,
    modifier: Modifier = Modifier,
    card: @Composable () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        if (page == 0) {
            Box(Modifier.fillMaxSize()) { card() }
        } else {
            val holder = rememberMindBoostBrainProfileStateHolder(userId)
            LaunchedEffect(holder) { holder.start() }
            MindBoostBrainProfileScreen(
                model = holder.uiModel,
                isLoading = holder.isLoading,
                onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                onRefresh = holder::refresh,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
