/*
 * Copyright 2023 onseok
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
package com.preat.peekaboo.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun rememberCameraCaptureLauncher(
    scope: CoroutineScope,
    onResult: (ByteArray?) -> Unit,
    onCameraPermissionDenied: (() -> Unit)?,
): CameraCaptureLauncher {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    val delegate =
        remember {
            StrongReferenceDelegate(onResult)
        }

    return remember {
        CameraCaptureLauncher(
            onLaunch = {
                when (cameraAccess) {
                    CameraAccess.Undefined, CameraAccess.Denied -> {
                        requestCameraPermission { granted ->
                            if (granted) {
                                cameraAccess = CameraAccess.Authorized
                                launchCamera(delegate)
                            } else {
                                cameraAccess = CameraAccess.Denied
                                onCameraPermissionDenied?.invoke()
                            }
                        }
                    }
                    CameraAccess.Authorized -> {
                        launchCamera(delegate)
                    }
                }
            },
        )
    }
}

private class StrongReferenceDelegate(
    private val onResult: (ByteArray?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        image?.let {
            val imageBytes = UIImagePNGRepresentation(it)?.toByteArray()
            onResult(imageBytes)
        } ?: onResult(null)

        picker.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

private fun launchCamera(delegate: StrongReferenceDelegate) {
    dispatch_async(dispatch_get_main_queue()) {
        val imagePickerController =
            UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                cameraCaptureMode = UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
            }
        imagePickerController.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            imagePickerController,
            true,
            completion = null,
        )
    }
}

private fun requestCameraPermission(onPermissionResult: (Boolean) -> Unit) {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        onPermissionResult(granted)
    }
}

actual class CameraCaptureLauncher actual constructor(
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}

private sealed interface CameraAccess {
    data object Undefined : CameraAccess

    data object Denied : CameraAccess

    data object Authorized : CameraAccess
}

@OptIn(ExperimentalForeignApi::class)
private inline fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return byteArray
}
