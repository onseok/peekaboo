package com.preat.peekaboo.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
actual class PeekabooCameraState(
    cameraMode: CameraMode,
    private val onCapture: (ByteArray?) -> Unit,
) {

    actual var isCameraReady: Boolean by mutableStateOf(false)

    internal var triggerCaptureAnchor: (() -> Unit)? = null

    actual var isCapturing: Boolean by mutableStateOf(false)

    actual var cameraMode: CameraMode by mutableStateOf(cameraMode)

    actual fun toggleCamera() {
        cameraMode = cameraMode.inverse()
    }

    actual fun capture() {

    }

    internal fun stopCapturing() {
        isCapturing = false
    }

    internal fun onCapture(image: ByteArray?) {
        onCapture.invoke(image)
    }

    fun onCameraReady() {
        isCameraReady = true
    }

    companion object {

        fun saver(onCapture: (ByteArray?) -> Unit): Saver<PeekabooCameraState, Int> {
            return Saver(
                save = {
                    it.cameraMode.id()
                },
                restore = {
                    PeekabooCameraState(
                        cameraMode = cameraModeFromId(it),
                        onCapture = onCapture,
                    )
                },
            )
        }
    }

}

@Composable
actual fun rememberPeekabooCameraState(
    initialCameraMode: CameraMode,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState {
    return rememberSaveable(
        onCapture,
        saver = PeekabooCameraState.saver(onCapture),
    ) { PeekabooCameraState(initialCameraMode, onCapture) }
}
