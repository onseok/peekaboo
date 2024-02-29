package com.preat.peekaboo.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

actual class PeekabooCameraState(
    cameraMode: CameraMode,
    private val onCapture: (ByteArray?) -> Unit,
) {


    actual var isCameraReady: Boolean by mutableStateOf(false)

    actual var isCapturing: Boolean by mutableStateOf(false)

    actual var cameraMode: CameraMode by mutableStateOf(cameraMode)

    internal var triggerCaptureAnchor: (() -> Unit)? = null

    actual fun toggleCamera() {
        cameraMode = cameraMode.inverse()
    }

    actual fun capture() {
        isCapturing = true
        triggerCaptureAnchor?.invoke()
    }

    internal fun stopCapturing() {
        isCapturing = false
    }

    internal fun onCapture(image: ByteArray?) {
        onCapture.invoke(image)
    }

    internal fun onCameraReady() {
        isCameraReady = true
    }

}

@Composable
actual fun rememberPeekabooCameraState(
    initialCameraMode: CameraMode,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState {
    return remember(onCapture) { PeekabooCameraState(initialCameraMode, onCapture) }
}
