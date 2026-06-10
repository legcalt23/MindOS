package com.mindos.app.features.dm.presentation

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberRawImageBitmap(resourceName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(context, resourceName) {
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId == 0) {
            null
        } else {
            BitmapFactory.decodeResource(context.resources, resourceId)?.asImageBitmap()
        }
    }
}
