@file:Suppress("FunctionNaming", "LongMethod")

package com.mindos.app.features.personalidentity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings
import com.mindos.app.features.personalidentity.handwriting.HandwritingLabScreen
import com.mindos.app.features.personalidentity.model.HandwritingTemplateSpec
import com.mindos.app.features.personalidentity.model.PersonalSignature
import com.mindos.app.features.personalidentity.model.SignatureStroke
import com.mindos.app.features.personalidentity.model.normalizedAndSimplified
import com.mindos.app.features.personalidentity.signature.SignatureManagerScreen

private enum class PersonalIdentitySection {
    Signature,
    Handwriting,
}

@Composable
fun PersonalIdentityLabScreen(
    onExportHandwritingTemplateRequested: ((HandwritingTemplateSpec) -> Unit)?,
    onImportHandwritingTemplateRequested: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableStateOf(PersonalIdentitySection.Signature) }
    val savedSignatures = remember { mutableStateListOf<PersonalSignature>() }
    var draftStrokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.ScreenHorizontal,
                    top = AppSpacing.L,
                    end = AppSpacing.ScreenHorizontal,
                    bottom = AppSpacing.S,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.M),
        ) {
            Text(
                text = AppStrings.PersonalIdentityLabTitle,
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = AppStrings.PersonalIdentityInternalStatus,
                color = AppColors.Mist,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
            ) {
                SectionButton(
                    text = AppStrings.PersonalIdentitySignatureSection,
                    selected = selectedSection == PersonalIdentitySection.Signature,
                    onClick = { selectedSection = PersonalIdentitySection.Signature },
                    modifier = Modifier.weight(1f),
                )
                SectionButton(
                    text = AppStrings.PersonalIdentityHandwritingSection,
                    selected = selectedSection == PersonalIdentitySection.Handwriting,
                    onClick = { selectedSection = PersonalIdentitySection.Handwriting },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (selectedSection) {
            PersonalIdentitySection.Signature -> SignatureManagerScreen(
                savedSignatures = savedSignatures,
                draftStrokes = draftStrokes,
                onDraftStrokesChanged = { draftStrokes = it },
                onSaveDraft = {
                    val ordinal = savedSignatures.size + 1
                    val signature = PersonalSignature(
                        id = "session_signature_$ordinal",
                        name = "${AppStrings.PersonalIdentitySignatureNamePrefix} $ordinal",
                        strokes = draftStrokes,
                    ).normalizedAndSimplified()

                    if (signature.isUsable) {
                        savedSignatures += signature
                        draftStrokes = emptyList()
                    }
                },
                modifier = Modifier.weight(1f),
            )

            PersonalIdentitySection.Handwriting -> HandwritingLabScreen(
                onExportTemplateRequested = onExportHandwritingTemplateRequested,
                onImportTemplateImageRequested = onImportHandwritingTemplateRequested,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    }
}
