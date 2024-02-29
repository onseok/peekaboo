package com.preat.peekaboo.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


/**
 * Create and [remember] a [PeekabooCameraState]
 * @param initialCameraMode The initial camera mode (front or back). Default is [CameraMode.Back]. Changes does not affect state. To toggle use [PeekabooCameraState.toggleCamera]
 * @param onCapture A lambda called when a photo is captured, providing the photo as a ByteArray or null if the capture fails.
 */
@Composable
expect fun rememberPeekabooCameraState(
    initialCameraMode: CameraMode = CameraMode.Back,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState


/**
 * State of [PeekabooCamera]. Contains states relating to camera control.
 */
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
