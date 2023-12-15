package com.preat.peekaboo.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.preat.peekaboo.ui.icon.IconPhotoCamera
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

private val executor = Executors.newSingleThreadExecutor()

@Suppress("FunctionName")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode,
    onCapture: (byteArray: ByteArray?) -> Unit,
) {
    val cameraPermissionState =
        rememberPermissionState(
            android.Manifest.permission.CAMERA,
        )
    when (cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            CameraWithGrantedPermission(modifier, cameraMode, onCapture)
        }
        is PermissionStatus.Denied -> {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun CameraWithGrantedPermission(
    modifier: Modifier,
    cameraMode: CameraMode,
    onCapture: (byteArray: ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    var isFrontCamera by rememberSaveable {
        mutableStateOf(
            when (cameraMode) {
                CameraMode.Front -> true
                CameraMode.Back -> false
            },
        )
    }
    val cameraSelector =
        remember(isFrontCamera) {
            val lensFacing =
                if (isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
        }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(isFrontCamera) {
        cameraProvider =
            suspendCoroutine<ProcessCameraProvider> { continuation ->
                ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                    cameraProvider.addListener(
                        {
                            continuation.resume(cameraProvider.get())
                        },
                        executor,
                    )
                }
            }
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    var capturePhotoStarted by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .pointerInput(isFrontCamera) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount.absoluteValue > 50.0) {
                            isFrontCamera = !isFrontCamera
                        }
                    }
                },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        CircularButton(
            imageVector = IconPhotoCamera,
            modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
            enabled = !capturePhotoStarted,
        ) {
            capturePhotoStarted = true
            imageCapture.takePicture(
                executor,
                object : OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val buffer = image.planes[0].buffer
                        val data = buffer.toByteArray()

                        // Rotate the image if necessary
                        val rotatedData =
                            if (rotationDegrees != 0) {
                                rotateImage(data, rotationDegrees)
                            } else {
                                data
                            }

                        image.close()
                        onCapture(rotatedData)
                        capturePhotoStarted = false
                    }
                },
            )
        }
        if (capturePhotoStarted) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp).align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 8.dp,
            )
        }
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind() // Rewind the buffer to zero
    val data = ByteArray(remaining())
    get(data) // Copy the buffer into a byte array
    return data // Return the byte array
}

private fun rotateImage(
    data: ByteArray,
    degrees: Int,
): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val outputStream = ByteArrayOutputStream()
    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return outputStream.toByteArray()
}
