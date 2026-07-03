@file:Suppress("FunctionNaming")

package com.mindos.app.features.personalidentity.handwriting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppStrings
import com.mindos.app.features.personalidentity.model.HandwritingFontProfile

/**
 * Preview surface for a future generated font without pretending that generation exists today.
 */
@Composable
fun HandwritingPreviewText(
    text: String,
    profile: HandwritingFontProfile,
    modifier: Modifier = Modifier,
    generatedFontFamily: FontFamily? = null,
) {
    val state = if (profile.isComplete && generatedFontFamily != null) {
        profile.name
    } else {
        AppStrings.PersonalIdentityFontPending
    }

    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = state },
        color = AppColors.OnBackground,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = generatedFontFamily ?: FontFamily.Default,
    )
}
