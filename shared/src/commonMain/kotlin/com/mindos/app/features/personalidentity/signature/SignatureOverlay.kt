@file:Suppress("FunctionNaming")

package com.mindos.app.features.personalidentity.signature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppSpacing
import com.mindos.app.features.personalidentity.model.PersonalSignature

/**
 * Visual-only overlay for future documents and exports. It is not a legal digital signature.
 */
@Composable
fun SignatureOverlay(
    signature: PersonalSignature?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (signature != null) {
            SignaturePreview(
                strokes = signature.strokes,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacing.M)
                    .width(AppSpacing.MinTouchTarget * OVERLAY_WIDTH_MULTIPLIER)
                    .height(AppSpacing.AvatarSizeLarge),
            )
        }
    }
}

private const val OVERLAY_WIDTH_MULTIPLIER = 3
