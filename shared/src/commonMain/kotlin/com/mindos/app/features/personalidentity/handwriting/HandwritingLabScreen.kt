@file:Suppress("FunctionNaming", "LongMethod")

package com.mindos.app.features.personalidentity.handwriting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppElevation
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings
import com.mindos.app.features.personalidentity.model.HandwritingFontProfile
import com.mindos.app.features.personalidentity.model.HandwritingTemplateSpec

@Composable
fun HandwritingLabScreen(
    modifier: Modifier = Modifier,
    onExportTemplateRequested: ((HandwritingTemplateSpec) -> Unit)? = null,
    onImportTemplateImageRequested: (() -> Unit)? = null,
) {
    val templateSpec = remember { HandwritingTemplateSpec() }
    var previewText by remember { mutableStateOf(AppStrings.PersonalIdentityHandwritingPreview) }
    val placeholderProfile = remember {
        HandwritingFontProfile(
            id = "handwriting_lab_preview",
            name = AppStrings.PersonalIdentityHandwritingProfileName,
            templateSpec = templateSpec,
        )
    }

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
                    text = AppStrings.PersonalIdentityHandwritingTitle,
                    color = AppColors.OnBackground,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = AppStrings.PersonalIdentityHandwritingDescription,
                    color = AppColors.Mist,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
            ) {
                Button(
                    onClick = { onExportTemplateRequested?.invoke(templateSpec) },
                    modifier = Modifier.weight(1f),
                    enabled = onExportTemplateRequested != null,
                ) {
                    Text(AppStrings.PersonalIdentityPrepareTemplate)
                }
                OutlinedButton(
                    onClick = { onImportTemplateImageRequested?.invoke() },
                    modifier = Modifier.weight(1f),
                    enabled = onImportTemplateImageRequested != null,
                ) {
                    Text(AppStrings.PersonalIdentityImportImage)
                }
            }
        }

        item {
            OutlinedTextField(
                value = previewText,
                onValueChange = { previewText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(AppStrings.PersonalIdentityPreviewInputLabel) },
                singleLine = false,
            )
        }

        item {
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
                        text = AppStrings.PersonalIdentityPreviewTitle,
                        color = AppColors.OnBackground,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    HandwritingPreviewText(
                        text = previewText,
                        profile = placeholderProfile,
                    )
                    Text(
                        text = AppStrings.PersonalIdentityFontPending,
                        color = AppColors.Mist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            Text(
                text = AppStrings.PersonalIdentityCanonicalTemplate,
                color = AppColors.OnBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        itemsIndexed(templateSpec.rows) { index, row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.Card,
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.XS),
                ) {
                    Text(
                        text = "${AppStrings.PersonalIdentityTemplateRowPrefix} ${index + 1}",
                        color = AppColors.Mist,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = row,
                        color = AppColors.OnBackground,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
