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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDuoCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.kCMPixelFormat_32BGRA
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetDataSize
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.dataWithBytes
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.darwin.dispatch_queue_create
import platform.posix.memcpy

private val deviceTypes =
    listOf(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInDualWideCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInUltraWideCamera,
        AVCaptureDeviceTypeBuiltInDuoCamera,
    )

@Composable
actual fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    permissionDeniedContent: @Composable () -> Unit,
) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                cameraAccess = CameraAccess.Authorized
            }

            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                cameraAccess = CameraAccess.Denied
            }

            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(
                    mediaType = AVMediaTypeVideo,
                ) { success ->
                    cameraAccess = if (success) CameraAccess.Authorized else CameraAccess.Denied
                }
            }
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (cameraAccess) {
            CameraAccess.Undefined -> {
                // Waiting for the user to accept permission
            }

            CameraAccess.Denied -> {
                Box(modifier = modifier) {
                    permissionDeniedContent()
                }
            }

            CameraAccess.Authorized -> {
                AuthorizedCamera(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
actual fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
    onCapture: (byteArray: ByteArray?) -> Unit,
    onFrame: ((frame: ByteArray) -> Unit)?,
    permissionDeniedContent: @Composable () -> Unit,
) {
    val state =
        rememberPeekabooCameraState(
            initialCameraMode = cameraMode,
            onFrame = onFrame,
            onCapture = onCapture,
        )
    Box(
        modifier = modifier,
    ) {
        PeekabooCamera(
            state = state,
            modifier = modifier,
        )
        CompatOverlay(
            modifier = Modifier.fillMaxSize(),
            state = state,
            captureIcon = captureIcon,
            convertIcon = convertIcon,
            progressIndicator = progressIndicator,
        )
    }
}

@Composable
private fun CompatOverlay(
    modifier: Modifier,
    state: PeekabooCameraState,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        captureIcon(state::capture)
        convertIcon(state::toggleCamera)
        if (state.isCapturing) {
            progressIndicator()
        }
    }
}

@Composable
private fun BoxScope.AuthorizedCamera(
    cameraMode: CameraMode,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
    onCapture: (byteArray: ByteArray?) -> Unit,
) {
    var cameraReady by remember { mutableStateOf(false) }
    val camera: AVCaptureDevice? =
        remember {
            discoverySessionWithDeviceTypes(
                deviceTypes = deviceTypes,
                mediaType = AVMediaTypeVideo,
                position =
                    when (cameraMode) {
                        CameraMode.Front -> AVCaptureDevicePositionFront
                        CameraMode.Back -> AVCaptureDevicePositionBack
                    },
            ).devices.firstOrNull() as? AVCaptureDevice
        }

    if (camera != null) {
        RealDeviceCamera(
            camera = camera,
            onCameraReady = { cameraReady = true },
            captureIcon = captureIcon,
            convertIcon = convertIcon,
            progressIndicator = progressIndicator,
            onCapture = onCapture,
        )
    } else {
        Text(
            "Camera is not available on simulator. Please try to run on a real iOS device.",
            color = Color.White,
        )
    }

    if (!cameraReady) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        )
    }
}

@Composable
private fun AuthorizedCamera(
    state: PeekabooCameraState,
    modifier: Modifier = Modifier,
) {
    val camera: AVCaptureDevice? =
        remember {
            discoverySessionWithDeviceTypes(
                deviceTypes = deviceTypes,
                mediaType = AVMediaTypeVideo,
                position =
                    when (state.cameraMode) {
                        CameraMode.Front -> AVCaptureDevicePositionFront
                        CameraMode.Back -> AVCaptureDevicePositionBack
                    },
            ).devices.firstOrNull() as? AVCaptureDevice
        }

    if (camera != null) {
        RealDeviceCamera(
            state = state,
            camera = camera,
            modifier = modifier,
        )
    } else {
        Text(
            "Camera is not available on simulator. Please try to run on a real iOS device.",
            color = Color.White,
        )
    }

    if (!state.isCameraReady) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
private fun BoxScope.RealDeviceCamera(
    camera: AVCaptureDevice,
    onCameraReady: () -> Unit,
    captureIcon: @Composable (onClick: () -> Unit) -> Unit,
    convertIcon: @Composable (onClick: () -> Unit) -> Unit,
    progressIndicator: @Composable () -> Unit,
    onCapture: (byteArray: ByteArray?) -> Unit,
) {
    var isFrontCamera by remember { mutableStateOf(camera.position == AVCaptureDevicePositionFront) }
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    var actualOrientation by remember {
        mutableStateOf(
            AVCaptureVideoOrientationPortrait,
        )
    }
    var capturePhotoStarted by remember { mutableStateOf(false) }
    val photoCaptureDelegate =
        remember {
            object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
                override fun captureOutput(
                    output: AVCapturePhotoOutput,
                    didFinishProcessingPhoto: AVCapturePhoto,
                    error: NSError?,
                ) {
                    val photoData = didFinishProcessingPhoto.fileDataRepresentation()
                    if (photoData != null) {
                        var uiImage = UIImage(photoData)
                        if (uiImage.imageOrientation != UIImageOrientation.UIImageOrientationUp) {
                            UIGraphicsBeginImageContextWithOptions(
                                uiImage.size,
                                false,
                                uiImage.scale,
                            )
                            uiImage.drawInRect(
                                CGRectMake(
                                    x = 0.0,
                                    y = 0.0,
                                    width = uiImage.size.useContents { width },
                                    height = uiImage.size.useContents { height },
                                ),
                            )
                            val normalizedImage = UIGraphicsGetImageFromCurrentImageContext()
                            UIGraphicsEndImageContext()
                            uiImage = normalizedImage!!
                        }
                        val imageData = UIImagePNGRepresentation(uiImage)
                        val byteArray: ByteArray? = imageData?.toByteArray()
                        onCapture(byteArray)
                    }
                    capturePhotoStarted = false
                }
            }
        }

    val triggerCapture: () -> Unit = {
        capturePhotoStarted = true
        val photoSettings =
            AVCapturePhotoSettings.photoSettingsWithFormat(
                format = mapOf(pair = AVVideoCodecKey to AVVideoCodecTypeJPEG),
            )
        if (camera.position == AVCaptureDevicePositionFront) {
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.automaticallyAdjustsVideoMirroring = false
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.videoMirrored = true
        }
        capturePhotoOutput.capturePhotoWithSettings(
            settings = photoSettings,
            delegate = photoCaptureDelegate,
        )
    }

    val switchCamera: () -> Unit = {
        isFrontCamera = !isFrontCamera
    }

    val captureSession: AVCaptureSession =
        remember {
            AVCaptureSession().also { captureSession ->
                captureSession.sessionPreset = AVCaptureSessionPresetPhoto
                val captureDeviceInput: AVCaptureDeviceInput =
                    deviceInputWithDevice(device = camera, error = null)!!
                captureSession.addInput(captureDeviceInput)
                captureSession.addOutput(capturePhotoOutput)
            }
        }

    val cameraPreviewLayer =
        remember {
            AVCaptureVideoPreviewLayer(session = captureSession)
        }

    // Update captureSession with new camera configuration whenever isFrontCamera changed.
    LaunchedEffect(isFrontCamera) {
        val dispatchGroup = dispatch_group_create()
        captureSession.beginConfiguration()
        captureSession.inputs.forEach { captureSession.removeInput(it as AVCaptureInput) }

        val newCamera =
            discoverySessionWithDeviceTypes(
                deviceTypes,
                AVMediaTypeVideo,
                if (isFrontCamera) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack,
            ).devices.firstOrNull() as? AVCaptureDevice

        newCamera?.let {
            val newInput =
                AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as AVCaptureDeviceInput
            if (captureSession.canAddInput(newInput)) {
                captureSession.addInput(newInput)
            }
        }

        dispatch_group_enter(dispatchGroup)
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL)) {
            captureSession.startRunning()
            dispatch_group_leave(dispatchGroup)
        }
        captureSession.commitConfiguration()

        dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
            onCameraReady()
        }
    }

    DisposableEffect(Unit) {
        class OrientationListener : NSObject() {
            @Suppress("UNUSED_PARAMETER")
            @ObjCAction
            fun orientationDidChange(arg: NSNotification) {
                val cameraConnection = cameraPreviewLayer.connection
                if (cameraConnection != null) {
                    actualOrientation =
                        when (UIDevice.currentDevice.orientation) {
                            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                                AVCaptureVideoOrientationPortrait

                            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                                AVCaptureVideoOrientationLandscapeRight

                            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                                AVCaptureVideoOrientationLandscapeLeft

                            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                                AVCaptureVideoOrientationPortrait

                            else -> cameraConnection.videoOrientation
                        }
                    cameraConnection.videoOrientation = actualOrientation
                }
                capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                    ?.videoOrientation = actualOrientation
            }
        }

        val listener = OrientationListener()
        val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
        NSNotificationCenter.defaultCenter.addObserver(
            observer = listener,
            selector =
                NSSelectorFromString(
                    OrientationListener::orientationDidChange.name + ":",
                ),
            name = notificationName,
            `object` = null,
        )
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(
                observer = listener,
                name = notificationName,
                `object` = null,
            )
        }
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        background = Color.Black,
        factory = {
            val dispatchGroup = dispatch_group_create()
            val cameraContainer = UIView()
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            dispatch_group_enter(dispatchGroup)
            dispatch_async(
                dispatch_get_global_queue(
                    DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
                    0UL,
                ),
            ) {
                captureSession.startRunning()
                dispatch_group_leave(dispatchGroup)
            }
            dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                onCameraReady()
            }
            cameraContainer
        },
        onResize = { view: UIView, rect: CValue<CGRect> ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            cameraPreviewLayer.setFrame(rect)
            CATransaction.commit()
        },
    )
    // Call the triggerCapture lambda when the capture button is clicked
    captureIcon(triggerCapture)
    convertIcon(switchCamera)
    if (capturePhotoStarted) {
        progressIndicator()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun RealDeviceCamera(
    state: PeekabooCameraState,
    camera: AVCaptureDevice,
    modifier: Modifier,
) {
    val queue =
        remember {
            dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL)
        }
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    val videoOutput = remember { AVCaptureVideoDataOutput() }

    val photoCaptureDelegate =
        remember(state) { PhotoCaptureDelegate(state::stopCapturing, state::onCapture) }

    val frameAnalyzerDelegate =
        remember {
            CameraFrameAnalyzerDelegate(state.onFrame)
        }

    val triggerCapture: () -> Unit = {
        val photoSettings =
            AVCapturePhotoSettings.photoSettingsWithFormat(
                format = mapOf(pair = AVVideoCodecKey to AVVideoCodecTypeJPEG),
            )
        if (camera.position == AVCaptureDevicePositionFront) {
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.automaticallyAdjustsVideoMirroring = false
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.videoMirrored = true
        }
        capturePhotoOutput.capturePhotoWithSettings(
            settings = photoSettings,
            delegate = photoCaptureDelegate,
        )
    }

    SideEffect {
        state.triggerCaptureAnchor = triggerCapture
    }

    val captureSession: AVCaptureSession =
        remember {
            AVCaptureSession().also { captureSession ->
                captureSession.sessionPreset = AVCaptureSessionPresetPhoto
                val captureDeviceInput: AVCaptureDeviceInput =
                    deviceInputWithDevice(device = camera, error = null)!!
                captureSession.addInput(captureDeviceInput)
                captureSession.addOutput(capturePhotoOutput)

                if (captureSession.canAddOutput(videoOutput)) {
                    val captureQueue = dispatch_queue_create("sampleBufferQueue", attr = null)
                    videoOutput.setSampleBufferDelegate(frameAnalyzerDelegate, captureQueue)
                    videoOutput.alwaysDiscardsLateVideoFrames = true
                    videoOutput.videoSettings =
                        mapOf(
                            kCVPixelBufferPixelFormatTypeKey to kCMPixelFormat_32BGRA,
                        )
                    captureSession.addOutput(videoOutput)
                }
            }
        }

    val cameraPreviewLayer =
        remember {
            AVCaptureVideoPreviewLayer(session = captureSession)
        }

    // Update captureSession with new camera configuration whenever isFrontCamera changed.
    LaunchedEffect(state.cameraMode) {
        val dispatchGroup = dispatch_group_create()
        captureSession.beginConfiguration()
        captureSession.inputs.forEach { captureSession.removeInput(it as AVCaptureInput) }

        val newCamera =
            discoverySessionWithDeviceTypes(
                deviceTypes,
                AVMediaTypeVideo,
                if (state.cameraMode == CameraMode.Front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack,
            ).devices.firstOrNull() as? AVCaptureDevice

        newCamera?.let {
            val newInput =
                AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as AVCaptureDeviceInput
            if (captureSession.canAddInput(newInput)) {
                captureSession.addInput(newInput)
            }
        }

        captureSession.commitConfiguration()

        dispatch_group_enter(dispatchGroup)
        dispatch_async(queue) {
            captureSession.startRunning()
            dispatch_group_leave(dispatchGroup)
        }

        dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
            state.onCameraReady()
        }
    }

    DisposableEffect(cameraPreviewLayer, capturePhotoOutput, videoOutput, state) {
        val listener = OrientationListener(cameraPreviewLayer, capturePhotoOutput, videoOutput)
        val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
        NSNotificationCenter.defaultCenter.addObserver(
            observer = listener,
            selector =
                NSSelectorFromString(
                    OrientationListener::orientationDidChange.name + ":",
                ),
            name = notificationName,
            `object` = null,
        )
        onDispose {
            state.triggerCaptureAnchor = null
            NSNotificationCenter.defaultCenter.removeObserver(
                observer = listener,
                name = notificationName,
                `object` = null,
            )
        }
    }

    UIKitView(
        modifier = modifier,
        background = Color.Black,
        factory = {
            val dispatchGroup = dispatch_group_create()
            val cameraContainer = UIView()
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            dispatch_group_enter(dispatchGroup)
            dispatch_async(queue) {
                captureSession.startRunning()
                dispatch_group_leave(dispatchGroup)
            }
            dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
                state.onCameraReady()
            }
            cameraContainer
        },
        onResize = { view: UIView, rect: CValue<CGRect> ->
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            view.layer.setFrame(rect)
            cameraPreviewLayer.setFrame(rect)
            CATransaction.commit()
        },
    )
}

class OrientationListener(
    private val cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    private val capturePhotoOutput: AVCapturePhotoOutput,
    private val videoOutput: AVCaptureVideoDataOutput,
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        val cameraConnection = cameraPreviewLayer.connection
        val actualOrientation =
            when (UIDevice.currentDevice.orientation) {
                UIDeviceOrientation.UIDeviceOrientationPortrait ->
                    AVCaptureVideoOrientationPortrait

                UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                    AVCaptureVideoOrientationLandscapeRight

                UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                    AVCaptureVideoOrientationLandscapeLeft

                UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                    AVCaptureVideoOrientationPortrait

                else -> cameraConnection?.videoOrientation ?: AVCaptureVideoOrientationPortrait
            }
        if (cameraConnection != null) {
            cameraConnection.videoOrientation = actualOrientation
        }
        capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
            ?.videoOrientation = actualOrientation
        videoOutput.connectionWithMediaType(AVMediaTypeVideo)
            ?.videoOrientation = actualOrientation
    }
}

class CameraFrameAnalyzerDelegate(
    private val onFrame: ((frame: ByteArray) -> Unit)?,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCaptureOutput,
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        if (onFrame == null) return

        val imageBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        CVPixelBufferLockBaseAddress(imageBuffer, 0uL)
        val baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)
        val bufferSize = CVPixelBufferGetDataSize(imageBuffer)
        val data = NSData.dataWithBytes(bytes = baseAddress, length = bufferSize)
        CVPixelBufferUnlockBaseAddress(imageBuffer, 0uL)

        val bytes = data.toByteArray()
        onFrame.invoke(bytes)
    }
}

class PhotoCaptureDelegate(
    private val onCaptureEnd: () -> Unit,
    private val onCapture: (byteArray: ByteArray?) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        val photoData = didFinishProcessingPhoto.fileDataRepresentation()
        if (photoData != null) {
            var uiImage = UIImage(photoData)
            if (uiImage.imageOrientation != UIImageOrientation.UIImageOrientationUp) {
                UIGraphicsBeginImageContextWithOptions(
                    uiImage.size,
                    false,
                    uiImage.scale,
                )
                uiImage.drawInRect(
                    CGRectMake(
                        x = 0.0,
                        y = 0.0,
                        width = uiImage.size.useContents { width },
                        height = uiImage.size.useContents { height },
                    ),
                )
                val normalizedImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                uiImage = normalizedImage!!
            }
            val imageData = UIImagePNGRepresentation(uiImage)
            val byteArray: ByteArray? = imageData?.toByteArray()
            onCapture(byteArray)
        }
        onCaptureEnd()
    }
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
