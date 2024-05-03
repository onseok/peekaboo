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
import androidx.compose.ui.Modifier

/**
 * `PeekabooCamera` is a composable function that provides a customizable camera UI within a Compose Multiplatform application.
 * It allows for the display of a camera preview, along with custom capture and convert buttons, and an optional progress indicator during photo capture.
 *
 * @param modifier The [Modifier] applied to the camera UI component for layout customization.
 * @param cameraMode The initial camera mode (front or back). Default is [CameraMode.Back].
 * @param captureIcon A composable lambda for the capture button. It takes an `onClick` lambda that triggers the image capture process.
 * @param convertIcon An optional composable lambda for a button to toggle the camera mode. It takes an `onClick` lambda for switching the camera.
 * @param progressIndicator An optional composable lambda displayed during photo capture processing.
 * @param onCapture A lambda called when a photo is captured, providing the photo as a ByteArray or null if the capture fails.
 * @param permissionDeniedContent An optional composable lambda that provides content to be displayed when camera permission is denied.
 *        This allows users to define a custom UI to inform or guide the user when camera access has been denied. The content can be
 *        informative text, an image, a button to redirect the user to settings, or any other composable content. This lambda will be invoked
 *        within the PeekabooCamera composable scope, replacing the camera preview with the user-defined UI.
 *
 * Note: If `permissionDeniedContent` is not used, and the camera permission is denied, PeekabooCamera will render an empty view. To avoid this,
 * it's recommended to implement a separate permission check logic before rendering the PeekabooCamera composable. This way, you can ensure the
 * camera permission is granted before the camera UI is displayed, or handle the permission denial appropriately outside of the PeekabooCamera scope.
 */
@Composable
expect fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode = CameraMode.Back,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit = {},
    progressIndicator: @Composable () -> Unit = {},
    onCapture: (byteArray: ByteArray?) -> Unit,
    onFrame: ((frame: ByteArray) -> Unit)? = null,
    permissionDeniedContent: @Composable () -> Unit = {},
)

/**
 * `PeekabooCamera` is a composable function that provides a customizable camera UI within a Compose Multiplatform application.
 * It allows for the display of a camera preview, along with custom capture and convert buttons, and an optional progress indicator during photo capture.
 *
 * @param state The [PeekabooCameraState] to control camera.
 * @param modifier The [Modifier] applied to the camera UI component for layout customization.
 * @param permissionDeniedContent An optional composable lambda that provides content to be displayed when camera permission is denied.
 *        This allows users to define a custom UI to inform or guide the user when camera access has been denied. The content can be
 *        informative text, an image, a button to redirect the user to settings, or any other composable content. This lambda will be invoked
 *        within the PeekabooCamera composable scope, replacing the camera preview with the user-defined UI.
 *
 * Note: If `permissionDeniedContent` is not used, and the camera permission is denied, PeekabooCamera will render an empty view. To avoid this,
 * it's recommended to implement a separate permission check logic before rendering the PeekabooCamera composable. This way, you can ensure the
 * camera permission is granted before the camera UI is displayed, or handle the permission denial appropriately outside of the PeekabooCamera scope.
 */
@Composable
expect fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    permissionDeniedContent: @Composable () -> Unit = {},
)
