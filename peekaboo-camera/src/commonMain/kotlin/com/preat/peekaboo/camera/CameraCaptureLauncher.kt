package com.preat.peekaboo.camera

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

@Composable
expect fun rememberCameraCaptureLauncher(
    scope: CoroutineScope,
    onResult: (ByteArray?) -> Unit,
    onCameraPermissionDenied: (() -> Unit)?,
): CameraCaptureLauncher

expect class CameraCaptureLauncher(
    onLaunch: () -> Unit,
) {
    fun launch()
}
