/*
 * Copyright 2024 onseok
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

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
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
    internal var onFrame: ((frame: ByteArray) -> Unit)?,
    internal var onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
    internal var onCapture: (ByteArray?) -> Unit,
) {
    actual var isCameraReady: Boolean by mutableStateOf(false)

    actual var isCapturing: Boolean by mutableStateOf(false)

    actual var cameraMode: CameraMode by mutableStateOf(cameraMode)

    actual var isTorchAvailable: Boolean by mutableStateOf(false)

    actual var isTorchEnabled: Boolean by mutableStateOf(false)

    internal var triggerCaptureAnchor: (() -> Unit)? = null

    actual fun toggleCamera() {
        isTorchEnabled = false
        isTorchAvailable = false
        cameraMode = cameraMode.inverse()
    }

    actual fun setTorchActive(enabled: Boolean) {
        isTorchEnabled = enabled && isTorchAvailable
    }

    actual fun toggleTorch() {
        setTorchActive(!isTorchEnabled)
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

    companion object {
        fun saver(
            onFrame: ((frame: ByteArray) -> Unit)?,
            onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
            onCapture: (ByteArray?) -> Unit,
        ): Saver<PeekabooCameraState, Int> {
            return Saver(
                save = {
                    it.cameraMode.id()
                },
                restore = {
                    PeekabooCameraState(
                        cameraMode = cameraModeFromId(it),
                        onFrame = onFrame,
                        onScannerFrame = onScannerFrame,
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
    onFrame: ((frame: ByteArray) -> Unit)?,
    onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
    onCapture: (ByteArray?) -> Unit,
): PeekabooCameraState {
    return rememberSaveable(
        saver = PeekabooCameraState.saver(onFrame, onScannerFrame, onCapture),
    ) { PeekabooCameraState(initialCameraMode, onFrame, onScannerFrame, onCapture) }.apply {
        this.onFrame = onFrame
        this.onScannerFrame = onScannerFrame
        this.onCapture = onCapture
    }
}

actual class PeekabooCameraFrame internal constructor(
    private val imageProxy: ImageProxy,
    actual val metadata: PeekabooFrameMetadata,
) {
    private var retainedForAsyncAnalysis = false
    private var released = false
    private var cachedBitmap: Bitmap? = null

    val bitmap: Bitmap
        get() {
            check(!released) { "Camera frame was released before bitmap conversion" }
            return cachedBitmap ?: imageProxy.toBitmap().also { cachedBitmap = it }
        }

    actual fun retainForAsyncAnalysis() {
        retainedForAsyncAnalysis = true
    }

    actual fun releaseAfterAsyncAnalysis() {
        if (released) return
        cachedBitmap?.recycle()
        cachedBitmap = null
        imageProxy.close()
        retainedForAsyncAnalysis = false
        released = true
    }

    internal fun releaseIfNotRetained() {
        if (!retainedForAsyncAnalysis) {
            releaseAfterAsyncAnalysis()
        }
    }
}
