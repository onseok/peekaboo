/*
 * Copyright 2023-2024 onseok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.preat.peekaboo.ui.camera

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Rational
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val executor = Executors.newSingleThreadExecutor()

@Composable
actual fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
    onCapture: (byteArray: ByteArray?) -> Unit,
    onFrame: ((frame: ByteArray) -> Unit)?,
    onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
    captureAspectRatio: Float?,
    previewScaleType: CameraPreviewScaleType,
    previewOrientationMode: CameraPreviewOrientationMode,
    permissionDeniedContent: @Composable () -> Unit,
) {
    val state =
        rememberPeekabooCameraState(
            initialCameraMode = cameraMode,
            onFrame = onFrame,
            onScannerFrame = onScannerFrame,
            onCapture = onCapture,
        )
    Box(
        modifier = modifier,
    ) {
        PeekabooCamera(
            state = state,
            modifier = modifier,
            captureAspectRatio = captureAspectRatio,
            previewScaleType = previewScaleType,
            previewOrientationMode = previewOrientationMode,
        )
        CompatOverlay(
            modifier = Modifier.fillMaxSize(),
            state = state,
            captureIcon = captureIcon,
            convertIcon = convertIcon,
            progressIndicator = progressIndicator,
        )
    }
}

@Composable
private fun CompatOverlay(
    modifier: Modifier,
    state: PeekabooCameraState,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            captureIcon(state::capture)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            convertIcon(state::toggleCamera)
        }
        if (state.isCapturing) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                progressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    captureAspectRatio: Float?,
    previewScaleType: CameraPreviewScaleType,
    previewOrientationMode: CameraPreviewOrientationMode,
    permissionDeniedContent: @Composable () -> Unit,
) {
    val cameraPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            CameraWithGrantedPermission(
                state = state,
                modifier = modifier,
                captureAspectRatio = captureAspectRatio,
                previewScaleType = previewScaleType,
                previewOrientationMode = previewOrientationMode,
            )
        }
        is PermissionStatus.Denied -> {
            // Always request permission on first encounter (shouldShowRationale is false
            // on first ask, so we must still request it)
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
            Box(modifier = modifier) {
                permissionDeniedContent()
            }
        }
    }
}

@Composable
private fun CameraWithGrantedPermission(
    state: PeekabooCameraState,
    modifier: Modifier,
    captureAspectRatio: Float?,
    previewScaleType: CameraPreviewScaleType,
    @Suppress("UNUSED_PARAMETER") previewOrientationMode: CameraPreviewOrientationMode,
) {
    val context = LocalContext.current
    // Use Activity as lifecycle owner to ensure camera works inside Dialogs
    // (Dialog's LocalLifecycleOwner may not be in RESUMED state)
    val lifecycleOwner: LifecycleOwner = remember(context) {
        context.findLifecycleOwner()
    }
    val cameraProvider: ProcessCameraProvider? by loadCameraProvider(context)

    val preview = remember { Preview.Builder().build() }
    val previewView =
        remember {
            PreviewView(context).apply {
                scaleType =
                    when (previewScaleType) {
                        CameraPreviewScaleType.AspectFill -> PreviewView.ScaleType.FILL_CENTER
                        CameraPreviewScaleType.AspectFit -> PreviewView.ScaleType.FIT_CENTER
                    }
            }
        }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageAnalyzer: ImageAnalysis? =
        remember(state.onFrame, state.onScannerFrame) {
            if (state.onFrame != null || state.onScannerFrame != null) {
                val analyzer =
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                analyzer.apply {
                    setAnalyzer(backgroundExecutor) { imageProxy ->
                        val scannerFrame = state.onScannerFrame
                        val legacyFrame = state.onFrame
                        if (scannerFrame != null) {
                            val frame =
                                PeekabooCameraFrame(
                                    imageProxy = imageProxy,
                                    metadata =
                                        PeekabooFrameMetadata(
                                            width = imageProxy.width,
                                            height = imageProxy.height,
                                            rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                                            timestampMillis = imageProxy.imageInfo.timestamp / 1_000_000L,
                                        ),
                                )
                            try {
                                scannerFrame(frame)
                            } finally {
                                frame.releaseIfNotRetained()
                            }
                        } else if (legacyFrame != null) {
                            val imageBytes = imageProxy.toByteArray()
                            legacyFrame(imageBytes)
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            } else {
                null
            }
        }

    val cameraSelector =
        remember(state.cameraMode) {
            val lensFacing =
                when (state.cameraMode) {
                    CameraMode.Front -> {
                        CameraSelector.LENS_FACING_FRONT
                    }
                    CameraMode.Back -> {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            CameraSelector.Builder().requireLensFacing(lensFacing).build()
        }
    val boundCamera = remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { boundCamera.value?.cameraControl?.enableTorch(false) }
            boundCamera.value = null
            state.isTorchAvailable = false
            state.setTorchActive(false)
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(state.cameraMode, cameraProvider, imageAnalyzer, captureAspectRatio) {
        if (cameraProvider != null) {
            state.onCameraReady()
            runCatching { boundCamera.value?.cameraControl?.enableTorch(false) }
            boundCamera.value = null
            cameraProvider?.unbindAll()
            // CameraX official guidance: bind preview + capture (and analyzer if any) through a
            // UseCaseGroup that shares a ViewPort. Both use cases then share the same crop rect,
            // so the saved photo matches what was visible in the preview surface.
            // https://developer.android.com/reference/kotlin/androidx/camera/core/ViewPort
            val useCaseGroupBuilder = UseCaseGroup.Builder().addUseCase(preview).addUseCase(imageCapture)
            imageAnalyzer?.let { useCaseGroupBuilder.addUseCase(it) }
            if (captureAspectRatio != null && captureAspectRatio > 0f) {
                val rotation = preview.targetRotation
                val rational = aspectRatioToRational(captureAspectRatio)
                val viewPort =
                    ViewPort.Builder(rational, rotation)
                        .setScaleType(ViewPort.FILL_CENTER)
                        .build()
                useCaseGroupBuilder.setViewPort(viewPort)
            }
            val camera =
                cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                useCaseGroupBuilder.build(),
            )
            boundCamera.value = camera
            val hasTorch = state.cameraMode == CameraMode.Back && (camera?.cameraInfo?.hasFlashUnit() == true)
            state.isTorchAvailable = hasTorch
            if (!hasTorch) {
                state.setTorchActive(false)
            }
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } else {
            boundCamera.value = null
            state.isTorchAvailable = false
            state.setTorchActive(false)
        }
    }

    LaunchedEffect(boundCamera.value, state.isTorchEnabled, state.cameraMode, state.isTorchAvailable) {
        val camera = boundCamera.value ?: return@LaunchedEffect
        val shouldEnableTorch =
            state.cameraMode == CameraMode.Back &&
                state.isTorchAvailable &&
                state.isTorchEnabled
        runCatching {
            camera.cameraControl.enableTorch(shouldEnableTorch)
        }
    }

    SideEffect {
        val triggerCapture = {
            imageCapture.takePicture(
                executor,
                ImageCaptureCallback(state::onCapture, state::stopCapturing),
            )
        }
        state.triggerCaptureAnchor = triggerCapture
    }

    DisposableEffect(state) {
        onDispose {
            state.triggerCaptureAnchor = null
        }
    }
    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

/**
 * Convert a `width / height` ratio to a [Rational] suitable for [ViewPort.Builder].
 *
 * CameraX expects the rational in the natural orientation of the target rotation. Since our
 * preview surfaces use the device's portrait orientation by default, we keep the same `w:h`
 * convention as the rest of the app.
 */
internal fun aspectRatioToRational(aspectRatio: Float): Rational {
    // Multiply by 10_000 to retain precision for ratios like 63 / 88 = 0.71590...
    val numerator = (aspectRatio * 10_000f).roundToInt().coerceAtLeast(1)
    val denominator = 10_000
    val divisor = gcd(numerator, denominator)
    return Rational(numerator / divisor, denominator / divisor)
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

class ImageCaptureCallback(
    private val onCapture: (byteArray: ByteArray?) -> Unit,
    private val stopCapturing: () -> Unit,
) : OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
        val imageBytes = image.toByteArray()
        onCapture(imageBytes)
        stopCapturing()
    }
}

/**
 * Convert an [ImageProxy] to JPEG bytes, honoring both the EXIF rotation and any crop rect set
 * by CameraX (e.g. via the use-case group's ViewPort).
 */
private fun ImageProxy.toByteArray(): ByteArray {
    val rotationDegrees = imageInfo.rotationDegrees
    val bitmap = toBitmap()

    // CameraX exposes the requested ViewPort area via `cropRect`. Apply it BEFORE rotation so the
    // saved bytes contain only the rectangle that was visible in the preview surface.
    val cropped =
        runCatching {
            val rect = cropRect
            if (rect.width() in 1..bitmap.width && rect.height() in 1..bitmap.height &&
                (rect.left != 0 || rect.top != 0 || rect.width() != bitmap.width || rect.height() != bitmap.height)
            ) {
                Bitmap.createBitmap(
                    bitmap,
                    max(0, rect.left),
                    max(0, rect.top),
                    min(rect.width(), bitmap.width - max(0, rect.left)),
                    min(rect.height(), bitmap.height - max(0, rect.top)),
                ).also { if (it != bitmap) bitmap.recycle() }
            } else {
                bitmap
            }
        }.getOrDefault(bitmap)

    val rotatedData =
        if (rotationDegrees != 0) {
            cropped.rotate(rotationDegrees)
        } else {
            cropped.toByteArray()
        }
    close()

    return rotatedData
}

private fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}

private fun Bitmap.rotate(degrees: Int): ByteArray {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    return rotatedBitmap.toByteArray()
}

/** Walk the Context wrapper chain to find a LifecycleOwner (typically the Activity). */
private fun Context.findLifecycleOwner(): LifecycleOwner {
    var ctx: Context = this
    while (ctx !is LifecycleOwner) {
        ctx = (ctx as? ContextWrapper)?.baseContext
            ?: throw IllegalStateException("No LifecycleOwner found in Context chain")
    }
    return ctx
}
