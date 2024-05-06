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

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

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
    permissionDeniedContent: @Composable () -> Unit,
) {
    val state =
        rememberPeekabooCameraState(
            initialCameraMode = cameraMode,
            onFrame = onFrame,
            onCapture = onCapture,
        )
    Box(
        modifier = modifier,
    ) {
        PeekabooCamera(
            state = state,
            modifier = modifier,
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
        captureIcon(state::capture)
        convertIcon(state::toggleCamera)
        if (state.isCapturing) {
            progressIndicator()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    permissionDeniedContent: @Composable () -> Unit,
) {
    val cameraPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            CameraWithGrantedPermission(
                state = state,
                modifier = modifier,
            )
        }
        is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                LaunchedEffect(Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
            } else {
                Box(modifier = modifier) {
                    permissionDeniedContent()
                }
            }
        }
    }
}

@Composable
private fun CameraWithGrantedPermission(
    state: PeekabooCameraState,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider: ProcessCameraProvider? by loadCameraProvider(context)

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageAnalyzer =
        remember(state.onFrame) {
            state.onFrame?.let { onFrame ->
                val analyzer =
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                analyzer.apply {
                    setAnalyzer(backgroundExecutor) { imageProxy ->
                        val imageBytes = imageProxy.toByteArray()
                        onFrame(imageBytes)
                    }
                }
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

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(state.cameraMode, cameraProvider, imageAnalyzer) {
        if (cameraProvider != null) {
            state.onCameraReady()
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *listOfNotNull(
                    preview,
                    imageCapture,
                    imageAnalyzer,
                ).toTypedArray(),
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
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

private fun ImageProxy.toByteArray(): ByteArray {
    val rotationDegrees = imageInfo.rotationDegrees
    val bitmap = toBitmap()

    // Rotate the image if necessary
    val rotatedData =
        if (rotationDegrees != 0) {
            bitmap.rotate(rotationDegrees)
        } else {
            bitmap.toByteArray()
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
