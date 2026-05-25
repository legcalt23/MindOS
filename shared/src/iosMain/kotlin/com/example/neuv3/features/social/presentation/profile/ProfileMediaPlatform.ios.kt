package com.example.neuv3.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.example.neuv3.features.social.domain.model.ProfileMediaUpload
import com.example.neuv3.features.social.domain.model.ProfileMediaUploadKind
import com.example.neuv3.features.social.domain.model.ProfileModuleId
import com.example.neuv3.features.social.domain.model.maxAllowedBytes
import com.example.neuv3.features.social.domain.model.maxAllowedBytesMessage

import platform.UIKit.*
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UniformTypeIdentifiers.*
import platform.objc.*
import kotlinx.coroutines.*

@Composable
actual fun rememberProfileMediaPicker(
    onPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onError: (String) -> Unit,
): ProfileMediaPicker {
    val scope = rememberCoroutineScope()
    
    return remember(onPicked, onError) {
        object : ProfileMediaPicker {
            override fun pick(moduleId: ProfileModuleId, kind: ProfileMediaUploadKind) {
                val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
                
                when (kind) {
                    ProfileMediaUploadKind.Photo, ProfileMediaUploadKind.Video -> {
                        val configuration = PHPickerConfiguration()
                        configuration.filter = if (kind == ProfileMediaUploadKind.Photo) PHPickerFilter.imagesFilter else PHPickerFilter.videosFilter
                        configuration.selectionLimit = 1
                        
                        val picker = PHPickerViewController(configuration)
                        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                                picker.dismissViewControllerAnimated(true, null)
                                val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                                val itemProvider = result.itemProvider
                                
                                val typeIdentifier = if (kind == ProfileMediaUploadKind.Photo) UTTypeImage.identifier else UTTypeMovie.identifier
                                
                                itemProvider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
                                    if (data != null) {
                                        scope.launch(Dispatchers.Main) {
                                            val upload = ProfileMediaUpload(
                                                kind = kind,
                                                bytes = data.toByteArray(),
                                                mimeType = if (kind == ProfileMediaUploadKind.Photo) "image/jpeg" else "video/mp4",
                                                durationMillis = null // Needs more logic to extract duration
                                            )
                                            if (upload.bytes.size.toLong() > kind.maxAllowedBytes(moduleId)) {
                                                onError(kind.maxAllowedBytesMessage(moduleId))
                                            } else {
                                                onPicked(moduleId, upload)
                                            }
                                        }
                                    } else {
                                        scope.launch(Dispatchers.Main) {
                                            onError(error?.localizedDescription ?: "Error cargando multimedia")
                                        }
                                    }
                                }
                            }
                        }
                        picker.delegate = delegate
                        rootController.presentViewController(picker, true, null)
                    }
                    ProfileMediaUploadKind.Audio, ProfileMediaUploadKind.File -> {
                        val types = if (kind == ProfileMediaUploadKind.Audio) listOf(UTTypeAudio) else listOf(UTTypeData)
                        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
                        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                                val data = NSData.dataWithContentsOfURL(url) ?: return
                                val upload = ProfileMediaUpload(
                                    kind = kind,
                                    bytes = data.toByteArray(),
                                    mimeType = if (kind == ProfileMediaUploadKind.Audio) "audio/mp4" else "application/octet-stream",
                                    originalName = url.lastPathComponent
                                )
                                if (upload.bytes.size.toLong() > kind.maxAllowedBytes(moduleId)) {
                                    onError(kind.maxAllowedBytesMessage(moduleId))
                                } else {
                                    onPicked(moduleId, upload)
                                }
                            }
                        }
                        picker.delegate = delegate
                        rootController.presentViewController(picker, true, null)
                    }
                }
            }
        }
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            platform.Foundation.memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}

@Composable
actual fun ProfilePlatformMediaPreview(
    source: ProfileMediaRenderSource,
    modifier: Modifier,
    contentScale: ContentScale,
    fallback: @Composable () -> Unit,
) {
    // Basic implementation for iOS preview using shared logic if possible
    fallback()
}
