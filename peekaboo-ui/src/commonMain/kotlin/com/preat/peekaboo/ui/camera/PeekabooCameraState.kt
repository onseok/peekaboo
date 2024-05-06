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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Create and [remember] a [PeekabooCameraState]
 * @param initialCameraMode The initial camera mode (front or back). Default is [CameraMode.Back]. Changes does not affect state. To toggle use [PeekabooCameraState.toggleCamera]
 * @param onCapture A lambda called when a photo is captured, providing the photo as a ByteArray or null if the capture fails.
 */
@Composable
expect fun rememberPeekabooCameraState(
    initialCameraMode: CameraMode = CameraMode.Back,
    onFrame: ((frame: ByteArray) -> Unit)? = null,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState

/**
 * State of [PeekabooCamera]. Contains states relating to camera control.
 */
@Stable
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
