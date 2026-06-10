package com.mindos.app.features.element_selection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mindos.app.features.element_selection.domain.ElementMaster
import com.mindos.app.features.element_selection.presentation.ElementSelectionStateHolder
import com.mindos.app.features.element_selection.presentation.rememberElementSelectionStateHolder
import kotlin.math.roundToInt

@Composable
fun ElementSelectionScreen(
    onElementSelected: (ElementMaster) -> Unit,
    modifier: Modifier = Modifier,
    stateHolder: ElementSelectionStateHolder = rememberElementSelectionStateHolder(),
) {
    val uiState = stateHolder.uiState
    val selectedElement = uiState.selectedElement
    val time = 0f
    val impact = 0f
    val parallax = Offset.Zero

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060816)),
    ) {
        SanctuaryBackground(
            selectedElement = selectedElement,
            time = time,
            impact = impact,
            parallax = parallax,
            modifier = Modifier.fillMaxSize(),
        )

        if (selectedElement != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedElement.type) {
                        detectTapGestures(onTap = { stateHolder.onOutsideTapped() })
                    },
            )
        }

        OrbArc(
            uiState = uiState,
            time = time,
            impact = impact,
            onTap = stateHolder::onOrbTapped,
            onLongPressStarted = stateHolder::onLongPressStarted,
            onLongPressEnded = stateHolder::onLongPressEnded,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
        )

        SkyLoreText(
            element = selectedElement,
            time = time,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .zIndex(3f),
        )

        ElementalButton(
            element = selectedElement,
            time = time,
            onClick = onElementSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 26.dp)
                .zIndex(3f),
        )
    }
}

@Composable
private fun OrbArc(
    uiState: com.mindos.app.features.element_selection.presentation.ElementSelectionUiState,
    time: Float,
    impact: Float,
    onTap: (ElementMaster) -> Unit,
    onLongPressStarted: (ElementMaster) -> Unit,
    onLongPressEnded: (ElementMaster) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val compactWidth = maxWidth < 420.dp
        val compactHeight = maxHeight < 720.dp
        val orbSize = if (compactWidth) 86.dp else 98.dp
        val orbPx = with(density) { orbSize.toPx() }
        val expanded = uiState.selectedElement != null
        val visualPrimary = uiState.selectedElement ?: uiState.masters.getOrNull(2) ?: uiState.masters.firstOrNull()
        val secondaryMasters = remember(uiState.masters, visualPrimary?.type) {
            uiState.masters.filter { it.type != visualPrimary?.type }
        }
        val mainY = when {
            expanded && compactHeight -> 0.56f
            expanded -> 0.51f
            compactHeight -> 0.54f
            else -> 0.50f
        }

        uiState.masters.forEachIndexed { index, master ->
            val isSelected = uiState.selectedElement?.type == master.type
            val isVisualPrimary = visualPrimary?.type == master.type
            val secondaryIndex = secondaryMasters.indexOfFirst { it.type == master.type }.coerceAtLeast(0)
            val anchor = CeremonyAnchors[secondaryIndex.coerceIn(0, CeremonyAnchors.lastIndex)]
            val centerX = if (isVisualPrimary) 0.50f else anchor.x
            val centerY = if (isVisualPrimary) mainY else anchor.y + if (compactHeight) 0.025f else 0f
            val targetX = widthPx * centerX - orbPx * 0.50f
            val targetY = heightPx * centerY - orbPx * 0.50f
            val targetScale = when {
                isSelected && compactWidth -> 1.72f
                isSelected -> 1.88f
                isVisualPrimary && expanded -> 1.02f
                isVisualPrimary -> 1.28f
                expanded -> 0.78f
                else -> 0.88f
            }
            val targetAlpha = when {
                isSelected -> 1f
                expanded && isVisualPrimary -> 0.54f
                expanded -> 0.48f
                isVisualPrimary -> 0.96f
                else -> 0.84f
            }
            val targetBlur = if (expanded && !isSelected) 2.2f else 0f
            val x = targetX
            val y = targetY
            val scale = targetScale
            val alpha = targetAlpha
            val blur = targetBlur

            OrbComponent(
                master = master,
                time = time + index * 0.043f,
                selected = isSelected,
                intense = uiState.intenseElement == master.type,
                impact = if (isSelected) impact else 0f,
                onTap = onTap,
                onLongPressStarted = onLongPressStarted,
                onLongPressEnded = onLongPressEnded,
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(orbSize)
                    .zIndex(
                        when {
                            isSelected -> 4f
                            isVisualPrimary -> 2.5f
                            else -> 1f - index * 0.01f
                        },
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        shadowElevation = if (isSelected) 46f else 18f
                        ambientShadowColor = master.baseColor.let(::Color)
                        spotShadowColor = master.baseColor.let(::Color)
                    }
                    .then(if (blur > 0.1f) Modifier.blur(blur.dp) else Modifier),
            )
        }
    }
}

private data class OrbAnchor(
    val x: Float,
    val y: Float,
)

private val CeremonyAnchors = listOf(
    OrbAnchor(x = 0.24f, y = 0.39f),
    OrbAnchor(x = 0.76f, y = 0.39f),
    OrbAnchor(x = 0.29f, y = 0.66f),
    OrbAnchor(x = 0.71f, y = 0.66f),
)
