package com.mindos.app.features.personalidentity

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.design.AppStrings
import com.mindos.app.design.MindOSTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PersonalIdentityFeatureSmokeTest {
    @Test
    fun labScreen_rendersAndKeepsFutureHandwritingActionsDisabled() = runComposeUiTest {
        setContent {
            MindOSTheme {
                PersonalIdentityFeature.LabScreen()
            }
        }

        onNodeWithText(AppStrings.PersonalIdentityLabTitle).assertIsDisplayed()
        onNodeWithText(AppStrings.PersonalIdentitySignatureTitle).assertIsDisplayed()

        onNodeWithText(AppStrings.PersonalIdentityHandwritingSection).performClick()

        onNodeWithText(AppStrings.PersonalIdentityHandwritingTitle).assertIsDisplayed()
        onNodeWithText(AppStrings.PersonalIdentityPrepareTemplate).assertIsNotEnabled()
        onNodeWithText(AppStrings.PersonalIdentityImportImage).assertIsNotEnabled()
    }
}
