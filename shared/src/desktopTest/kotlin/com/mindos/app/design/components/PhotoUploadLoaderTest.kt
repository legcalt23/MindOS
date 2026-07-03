package com.mindos.app.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppStrings
import kotlin.test.Test

/**
 * Compose UI tests for the shared photo-upload loader, rendered on the desktop
 * (Skiko) test target. These pin the contract of the global upload loader:
 *  - the blocking overlay only shows the loader while `visible`
 *  - the underlying content always renders (the overlay never replaces it)
 *  - the caption defaults to "Subiendo foto..." and honours an override
 *  - a real progress value is surfaced as a percentage; none is invented
 *  - the inline variant renders its caption
 */
@OptIn(ExperimentalTestApi::class)
class PhotoUploadLoaderTest {

    private val backgroundMarker = "contenido-de-fondo"

    @Test
    fun overlay_visible_showsLoaderAndKeepsContent() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(visible = true) {
                Text(backgroundMarker)
            }
        }
        onNodeWithContentDescription(AppStrings.A11yPhotoUploadLoading).assertExists()
        // The content underneath is never replaced by the overlay.
        onNodeWithText(backgroundMarker).assertExists()
    }

    @Test
    fun overlay_notVisible_hidesLoader_butKeepsContent() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(visible = false) {
                Text(backgroundMarker)
            }
        }
        onNodeWithContentDescription(AppStrings.A11yPhotoUploadLoading).assertDoesNotExist()
        onNodeWithText(backgroundMarker).assertExists()
    }

    @Test
    fun overlay_showsDefaultMessage_whenNoneProvided() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(visible = true) { Text(backgroundMarker) }
        }
        onNodeWithText(AppStrings.PhotoUploadInProgress).assertExists()
    }

    @Test
    fun overlay_showsCustomMessage_whenProvided() = runComposeUiTest {
        val custom = "Subiendo tu avatar"
        setContent {
            PhotoUploadLoadingOverlay(visible = true, message = custom) { Text(backgroundMarker) }
        }
        onNodeWithText(custom).assertExists()
    }

    @Test
    fun overlay_showsPercent_onlyWhenProgressProvided() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(visible = true, progress = 0.42f) { Text(backgroundMarker) }
        }
        onNodeWithText("42%").assertExists()
    }

    @Test
    fun overlay_hidesPercent_whenNoProgress() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(visible = true) { Text(backgroundMarker) }
        }
        onNodeWithText("42%").assertDoesNotExist()
    }

    @Test
    fun inline_rendersMessage() = runComposeUiTest {
        val caption = "Subiendo foto de perfil"
        setContent {
            InlinePhotoUploadLoading(
                modifier = Modifier.size(120.dp),
                message = caption,
            )
        }
        onNodeWithText(caption).assertExists()
    }

    @Test
    fun inline_rendersPercent_whenProgressProvided() = runComposeUiTest {
        setContent {
            InlinePhotoUploadLoading(progress = 0.7f)
        }
        onNodeWithText("70%").assertExists()
    }

    @Test
    fun standaloneLoader_exposesContentDescription() = runComposeUiTest {
        setContent {
            Box(modifier = Modifier.size(64.dp)) { PhotoUploadLoader() }
        }
        onNodeWithContentDescription(AppStrings.A11yPhotoUploadLoading).assertExists()
    }

    @Test
    fun overlay_withInteractionAndBlurDisabled_stillShowsLoaderAndContent() = runComposeUiTest {
        setContent {
            PhotoUploadLoadingOverlay(
                visible = true,
                blockInteraction = false,
                blurEnabled = false,
            ) {
                Text(backgroundMarker)
            }
        }
        onNodeWithContentDescription(AppStrings.A11yPhotoUploadLoading).assertExists()
        onNodeWithText(backgroundMarker).assertExists()
    }
}
