@file:Suppress("FunctionNaming", "LongMethod")

package com.mindos.app.features.personalidentity.signature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppElevation
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings
import com.mindos.app.features.personalidentity.model.PersonalSignature
import com.mindos.app.features.personalidentity.model.SignatureStroke

@Composable
fun SignatureManagerScreen(
    savedSignatures: List<PersonalSignature>,
    draftStrokes: List<SignatureStroke>,
    onDraftStrokesChanged: (List<SignatureStroke>) -> Unit,
    onSaveDraft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentPadding = PaddingValues(
            horizontal = AppSpacing.ScreenHorizontal,
            vertical = AppSpacing.L,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.L),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S)) {
                Text(
                    text = AppStrings.PersonalIdentitySignatureTitle,
                    color = AppColors.OnBackground,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = AppStrings.PersonalIdentitySignatureDescription,
                    color = AppColors.Mist,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = AppStrings.PersonalIdentitySignaturePrivacyNotice,
                    color = AppColors.Info,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            SignatureCaptureBox(
                strokes = draftStrokes,
                onStrokesChanged = onDraftStrokesChanged,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
            ) {
                OutlinedButton(
                    onClick = { onDraftStrokesChanged(draftStrokes.dropLast(1)) },
                    modifier = Modifier.weight(1f),
                    enabled = draftStrokes.isNotEmpty(),
                ) {
                    Text(AppStrings.PersonalIdentityUndo)
                }
                OutlinedButton(
                    onClick = { onDraftStrokesChanged(emptyList()) },
                    modifier = Modifier.weight(1f),
                    enabled = draftStrokes.isNotEmpty(),
                ) {
                    Text(AppStrings.PersonalIdentityClear)
                }
                Button(
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(1f),
                    enabled = draftStrokes.any { it.isDrawable },
                ) {
                    Text(AppStrings.Save)
                }
            }
        }

        item {
            Text(
                text = AppStrings.PersonalIdentitySavedSignatures,
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (savedSignatures.isEmpty()) {
            item {
                Text(
                    text = AppStrings.PersonalIdentityNoSavedSignatures,
                    color = AppColors.Mist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            items(savedSignatures, key = { it.id }) { signature ->
                SignatureListItem(signature = signature)
            }
        }
    }
}

@Composable
private fun SignatureListItem(
    signature: PersonalSignature,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceRaised),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.Low),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.S),
        ) {
            Text(
                text = signature.name,
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.titleSmall,
            )
            SignaturePreview(
                strokes = signature.strokes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.AvatarSizeLarge),
            )
            Text(
                text = AppStrings.PersonalIdentitySignatureReady,
                color = AppColors.Mist,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
