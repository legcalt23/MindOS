@file:Suppress("FunctionNaming")

package com.mindos.app.features.personalidentity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.features.personalidentity.model.HandwritingTemplateSpec
import com.mindos.app.features.personalidentity.ui.PersonalIdentityLabScreen

/**
 * Internal-only entry point for the experimental personal identity lab.
 *
 * Deliberately not registered in public navigation, Home, Settings, or onboarding.
 */
object PersonalIdentityFeature {
    @Composable
    fun LabScreen(
        modifier: Modifier = Modifier,
        onExportHandwritingTemplateRequested: ((HandwritingTemplateSpec) -> Unit)? = null,
        onImportHandwritingTemplateRequested: (() -> Unit)? = null,
    ) {
        PersonalIdentityLabScreen(
            onExportHandwritingTemplateRequested = onExportHandwritingTemplateRequested,
            onImportHandwritingTemplateRequested = onImportHandwritingTemplateRequested,
            modifier = modifier,
        )
    }
}
