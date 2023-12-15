package com.preat.peekaboo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.preat.peekaboo.ui.icon.IconPhotoCamera

@Suppress("FunctionName")
@Composable
expect fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode = CameraMode.Back,
    captureIcon: ImageVector = IconPhotoCamera,
    onCapture: (byteArray: ByteArray?) -> Unit,
)
