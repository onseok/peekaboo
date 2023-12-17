package com.preat.peekaboo.ui

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
 */
@Suppress("FunctionName")
@Composable
expect fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode = CameraMode.Back,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit = {},
    progressIndicator: @Composable () -> Unit = {},
    onCapture: (byteArray: ByteArray?) -> Unit,
)
