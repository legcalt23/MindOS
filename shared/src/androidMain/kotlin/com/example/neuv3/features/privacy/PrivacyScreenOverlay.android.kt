package com.example.neuv3.features.privacy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.example.neuv3.core.logging.safeErrorLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs

private val detectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
    .build()

@OptIn(ExperimentalGetImage::class)
@Composable
actual fun PrivacyScreenOverlay(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var targetProtectionLevel by remember { mutableFloatStateOf(0f) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        DisposableEffect(lifecycleOwner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val detector = FaceDetection.getClient(detectorOptions)
            var lastSeenFaceTime = System.currentTimeMillis()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        detector.process(image)
                            .addOnSuccessListener { faces ->
                                val now = System.currentTimeMillis()
                                if (faces.isNotEmpty()) {
                                    val face = faces[0]
                                    val eulerY = face.headEulerAngleY
                                    val eulerZ = face.headEulerAngleZ
                                    
                                    if (abs(eulerY) > 25 || abs(eulerZ) > 25 || faces.size > 1) {
                                        targetProtectionLevel = 0.95f
                                    } else if (abs(eulerY) > 15) {
                                        targetProtectionLevel = 0.5f 
                                    } else {
                                        targetProtectionLevel = 0f
                                        lastSeenFaceTime = now
                                    }
                                } else {
                                    if (now - lastSeenFaceTime > 1500) {
                                        targetProtectionLevel = 0.98f
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
                } catch(e: Exception) {
                    safeErrorLog("PrivacyOverlay", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    safeErrorLog("PrivacyOverlay", "Unbind failed", e)
                }
                cameraExecutor.shutdown()
                detector.close()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        val animatedProtection by animateFloatAsState(
            targetValue = targetProtectionLevel,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )

        if (animatedProtection > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = animatedProtection))
            )
        }
    }
}
