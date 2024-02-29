package com.preat.peekaboo.ui.camera

import androidx.compose.runtime.Composable


@Composable
expect fun rememberPeekabooCameraState(
    initialCameraMode: CameraMode = CameraMode.Back,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState

expect class PeekabooCameraState {

    var isCameraReady: Boolean
        internal set

    var isCapturing: Boolean
        internal set

    var cameraMode: CameraMode
        internal set

    fun toggleCamera()

    fun capture()

}
