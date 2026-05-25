package com.example.neuv3.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.objc.NSObject

@Composable
actual fun rememberPlatformFilePicker(
    onPicked: (PlatformFile) -> Unit,
    onError: (String) -> Unit
): PlatformFilePicker {
    val scope = rememberCoroutineScope()

    return remember(onPicked, onError) {
        object : PlatformFilePicker {
            private var activeDelegate: NSObject? = null

            override fun pick(kind: FileKind) {
                val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return

                when (kind) {
                    FileKind.Photo, FileKind.Video -> {
                        val configuration = PHPickerConfiguration()
                        configuration.filter = if (kind == FileKind.Photo) PHPickerFilter.imagesFilter else PHPickerFilter.videosFilter
                        configuration.selectionLimit = 1

                        val picker = PHPickerViewController(configuration)
                        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                                picker.dismissViewControllerAnimated(true, null)
                                activeDelegate = null
                                val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                                val itemProvider = result.itemProvider

                                val typeIdentifier = if (kind == FileKind.Photo) UTTypeImage.identifier else UTTypeMovie.identifier

                                itemProvider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
                                    if (data != null) {
                                        scope.launch(Dispatchers.Main) {
                                            processPickedData(data, kind, null, onPicked)
                                        }
                                    } else {
                                        scope.launch(Dispatchers.Main) {
                                            onError(error?.localizedDescription ?: "Error cargando multimedia")
                                        }
                                    }
                                }
                            }
                        }
                        activeDelegate = delegate
                        picker.delegate = delegate
                        rootController.presentViewController(picker, true, null)
                    }
                    FileKind.Audio, FileKind.File -> {
                        val types = if (kind == FileKind.Audio) listOf(UTTypeAudio) else listOf(UTTypeData)
                        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
                        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                                activeDelegate = null
                                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                                val data = NSData.dataWithContentsOfURL(url) ?: return
                                
                                scope.launch(Dispatchers.Main) {
                                    processPickedData(data, kind, url.lastPathComponent, onPicked)
                                }
                            }
                        }
                        activeDelegate = delegate
                        picker.delegate = delegate
                        rootController.presentViewController(picker, true, null)
                    }
                }
            }
        }
    }
}

private suspend fun processPickedData(
    data: NSData,
    kind: FileKind,
    fileName: String?,
    onPicked: (PlatformFile) -> Unit
) {
    val bytes = data.toByteArray()
    val name = fileName ?: when (kind) {
        FileKind.Photo -> "image.jpg"
        FileKind.Video -> "video.mp4"
        FileKind.Audio -> "audio.m4a"
        FileKind.File -> "file"
    }
    val mimeType = when (kind) {
        FileKind.Photo -> "image/jpeg"
        FileKind.Video -> "video/mp4"
        FileKind.Audio -> "audio/mp4"
        FileKind.File -> "application/octet-stream"
    }

    var durationMs: Long? = null
    var thumbnailBytes: ByteArray? = null

    if (kind == FileKind.Video) {
        // To extract metadata on iOS from NSData, we need to write it to a temp file first
        // because AVAsset needs a URL.
        val tempDir = NSTemporaryDirectory()
        val tempUrl = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent(NSUUID().UUIDString() + ".mp4")!!
        data.writeToURL(tempUrl, true)

        val asset = AVURLAsset.assetWithURL(tempUrl) as AVAsset
        durationMs = (asset.duration.useContents { value.toDouble() / timescale.toDouble() } * 1000).toLong()

        val imageGenerator = AVAssetImageGenerator.assetImageGeneratorWithAsset(asset)
        imageGenerator.appliesPreferredTrackTransform = true
        val time = CMTimeMake(0, 1)
        val imageRef = imageGenerator.copyCGImageAtTime(time, null, null)
        if (imageRef != null) {
            val uiImage = UIImage.imageWithCGImage(imageRef)
            val jpegData = UIImageJPEGRepresentation(uiImage, 0.8)
            if (jpegData != null) {
                thumbnailBytes = jpegData.toByteArray()
            }
        }
        
        // Clean up temp file
        NSFileManager.defaultManager.removeItemAtURL(tempUrl, null)
    }

    onPicked(
        PlatformFile(
            bytes = bytes,
            name = name,
            mimeType = mimeType,
            size = bytes.size.toLong(),
            durationMs = durationMs,
            thumbnailBytes = thumbnailBytes
        )
    )
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}
