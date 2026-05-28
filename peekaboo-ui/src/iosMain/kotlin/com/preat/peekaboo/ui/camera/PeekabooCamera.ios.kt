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
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isTorchModeSupported
import platform.AVFoundation.position
import platform.AVFoundation.torchMode
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.kCMPixelFormat_32BGRA
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetDataSize
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
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
import platform.UIKit.UIColor
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

private fun preferredCamera(position: Long): AVCaptureDevice? =
    (
        discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = position,
        ).devices.firstOrNull()
            ?: discoverySessionWithDeviceTypes(
                deviceTypes = deviceTypes,
                mediaType = AVMediaTypeVideo,
                position = position,
            ).devices.firstOrNull()
    ) as? AVCaptureDevice

@Composable
actual fun PeekabooCamera(
    state: PeekabooCameraState,
    modifier: Modifier,
    captureAspectRatio: Float?,
    previewScaleType: CameraPreviewScaleType,
    previewOrientationMode: CameraPreviewOrientationMode,
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
                    captureAspectRatio = captureAspectRatio,
                    previewScaleType = previewScaleType,
                    previewOrientationMode = previewOrientationMode,
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
    onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
    captureAspectRatio: Float?,
    previewScaleType: CameraPreviewScaleType,
    previewOrientationMode: CameraPreviewOrientationMode,
    permissionDeniedContent: @Composable () -> Unit,
) {
    val state =
        rememberPeekabooCameraState(
            initialCameraMode = cameraMode,
            onFrame = onFrame,
            onScannerFrame = onScannerFrame,
            onCapture = onCapture,
        )
    Box(
        modifier = modifier,
    ) {
        PeekabooCamera(
            state = state,
            modifier = modifier,
            captureAspectRatio = captureAspectRatio,
            previewScaleType = previewScaleType,
            previewOrientationMode = previewOrientationMode,
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
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            captureIcon(state::capture)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            convertIcon(state::toggleCamera)
        }
        if (state.isCapturing) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                progressIndicator()
            }
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
            preferredCamera(
                position =
                    when (cameraMode) {
                        CameraMode.Front -> AVCaptureDevicePositionFront
                        CameraMode.Back -> AVCaptureDevicePositionBack
                    },
            )
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
    captureAspectRatio: Float? = null,
    previewScaleType: CameraPreviewScaleType = CameraPreviewScaleType.AspectFill,
    previewOrientationMode: CameraPreviewOrientationMode = CameraPreviewOrientationMode.FollowDevice,
) {
    val camera: AVCaptureDevice? =
        remember {
            preferredCamera(
                position =
                    when (state.cameraMode) {
                        CameraMode.Front -> AVCaptureDevicePositionFront
                        CameraMode.Back -> AVCaptureDevicePositionBack
                    },
            )
        }

    if (camera != null) {
        RealDeviceCamera(
            state = state,
            camera = camera,
            modifier = modifier,
            captureAspectRatio = captureAspectRatio,
            previewScaleType = previewScaleType,
            previewOrientationMode = previewOrientationMode,
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
            preferredCamera(if (isFrontCamera) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack)

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
        factory = {
            val cameraContainer = CameraContainerView()
            cameraContainer.backgroundColor = UIColor.blackColor
            cameraContainer.previewLayer = cameraPreviewLayer
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            cameraContainer
        },
    )
    DisposableEffect(captureSession) {
        onDispose {
            captureSession.stopRunning()
        }
    }
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
    captureAspectRatio: Float? = null,
    previewScaleType: CameraPreviewScaleType = CameraPreviewScaleType.AspectFill,
    previewOrientationMode: CameraPreviewOrientationMode = CameraPreviewOrientationMode.FollowDevice,
) {
    val queue =
        remember {
            dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL)
        }
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    val videoOutput = remember { AVCaptureVideoDataOutput() }

    // Defer-resolved holder so the delegate can read the latest preview-layer reference at
    // capture time (the layer is created below; we'll point the holder at it).
    val previewLayerHolder = remember { PreviewLayerHolder() }
    val photoCaptureDelegate =
        remember(state, captureAspectRatio) {
            PhotoCaptureDelegate(
                onCaptureEnd = state::stopCapturing,
                onCapture = state::onCapture,
                previewLayerProvider = if (captureAspectRatio != null) {
                    { previewLayerHolder.layer }
                } else {
                    { null }
                },
            )
        }

    val frameAnalyzerDelegate =
        remember(state.onFrame, state.onScannerFrame) {
            CameraFrameAnalyzerDelegate(
                onFrame = state.onFrame,
                onScannerFrame = state.onScannerFrame,
            )
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
                captureSession.sessionPreset =
                    if (state.onScannerFrame != null) {
                        AVCaptureSessionPresetHigh
                    } else {
                        AVCaptureSessionPresetPhoto
                    }
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
            AVCaptureVideoPreviewLayer(session = captureSession).also { layer ->
                previewLayerHolder.layer = layer
            }
        }

    // Update captureSession with new camera configuration whenever camera mode changes.
    LaunchedEffect(state.cameraMode) {
        captureSession.activeInputDevice()?.setTorchEnabled(false)
        state.isTorchEnabled = false
        state.isTorchAvailable = false
        val dispatchGroup = dispatch_group_create()
        captureSession.beginConfiguration()
        captureSession.inputs.forEach { captureSession.removeInput(it as AVCaptureInput) }

        val newCamera =
            preferredCamera(if (state.cameraMode == CameraMode.Front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack)

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
            applyVideoOrientation(
                cameraPreviewLayer = cameraPreviewLayer,
                capturePhotoOutput = capturePhotoOutput,
                videoOutput = videoOutput,
                forcedOrientation = previewOrientationMode.toForcedVideoOrientation(),
            )
            state.refreshTorchAvailability(captureSession)
            state.onCameraReady()
        }
    }

    LaunchedEffect(state.isTorchEnabled, state.cameraMode, captureSession) {
        val activeDevice = state.refreshTorchAvailability(captureSession)
        val shouldEnableTorch = state.isTorchAvailable && state.isTorchEnabled
        activeDevice?.setTorchEnabled(shouldEnableTorch)
    }

    DisposableEffect(cameraPreviewLayer, capturePhotoOutput, videoOutput, state, previewOrientationMode) {
        val listener =
            OrientationListener(
                cameraPreviewLayer = cameraPreviewLayer,
                capturePhotoOutput = capturePhotoOutput,
                videoOutput = videoOutput,
                forcedOrientation = previewOrientationMode.toForcedVideoOrientation(),
            )
        listener.applyCurrentOrientation()
        val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
        if (previewOrientationMode == CameraPreviewOrientationMode.FollowDevice) {
            NSNotificationCenter.defaultCenter.addObserver(
                observer = listener,
                selector =
                    NSSelectorFromString(
                        OrientationListener::orientationDidChange.name + ":",
                    ),
                name = notificationName,
                `object` = null,
            )
        }
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
        factory = {
            val cameraContainer = CameraContainerView()
            cameraContainer.backgroundColor = UIColor.blackColor
            cameraContainer.previewLayer = cameraPreviewLayer
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = previewScaleType.toAvLayerVideoGravity()
            cameraContainer
        },
    )
    DisposableEffect(captureSession) {
        onDispose {
            captureSession.activeInputDevice()?.setTorchEnabled(false)
            state.isTorchEnabled = false
            state.isTorchAvailable = false
            captureSession.stopRunning()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CameraContainerView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        val layer = previewLayer ?: return
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        layer.frame = bounds
        CATransaction.commit()
    }
}

class OrientationListener(
    private val cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    private val capturePhotoOutput: AVCapturePhotoOutput,
    private val videoOutput: AVCaptureVideoDataOutput,
    private val forcedOrientation: Long? = null,
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        applyCurrentOrientation()
    }

    fun applyCurrentOrientation() {
        applyVideoOrientation(cameraPreviewLayer, capturePhotoOutput, videoOutput, forcedOrientation)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureSession.activeInputDevice(): AVCaptureDevice? =
    (inputs.firstOrNull { it is AVCaptureDeviceInput } as? AVCaptureDeviceInput)?.device

@OptIn(ExperimentalForeignApi::class)
private fun PeekabooCameraState.refreshTorchAvailability(captureSession: AVCaptureSession): AVCaptureDevice? {
    val activeDevice = captureSession.activeInputDevice()
    val available = cameraMode == CameraMode.Back && (activeDevice?.hasTorch == true)
    isTorchAvailable = available
    if (!available && isTorchEnabled) {
        isTorchEnabled = false
    }
    return activeDevice
}

@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureDevice.setTorchEnabled(enabled: Boolean) {
    if (!hasTorch) return
    if (enabled && !isTorchModeSupported(AVCaptureTorchModeOn)) return
    if (!lockForConfiguration(null)) return
    try {
        torchMode =
            if (enabled) {
                AVCaptureTorchModeOn
            } else {
                AVCaptureTorchModeOff
            }
    } finally {
        unlockForConfiguration()
    }
}

private fun applyVideoOrientation(
    cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    capturePhotoOutput: AVCapturePhotoOutput,
    videoOutput: AVCaptureVideoDataOutput,
    forcedOrientation: Long?,
) {
    val cameraConnection = cameraPreviewLayer.connection
    val actualOrientation =
        forcedOrientation
            ?: when (UIDevice.currentDevice.orientation) {
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
    cameraConnection?.setVideoOrientationIfChanged(actualOrientation)
    capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
        ?.setVideoOrientationIfChanged(actualOrientation)
    videoOutput.connectionWithMediaType(AVMediaTypeVideo)
        ?.setVideoOrientationIfChanged(actualOrientation)
}

private fun AVCaptureConnection.setVideoOrientationIfChanged(orientation: Long) {
    if (videoOrientation != orientation) {
        videoOrientation = orientation
    }
}

class CameraFrameAnalyzerDelegate(
    private val onFrame: ((frame: ByteArray) -> Unit)?,
    private val onScannerFrame: ((frame: PeekabooCameraFrame) -> Unit)?,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCaptureOutput,
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        if (onFrame == null && onScannerFrame == null) return

        val imageBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        onScannerFrame?.invoke(
            PeekabooCameraFrame(
                sourcePixelBuffer = imageBuffer,
                metadata =
                    PeekabooFrameMetadata(
                        width = CVPixelBufferGetWidth(imageBuffer).toInt(),
                        height = CVPixelBufferGetHeight(imageBuffer).toInt(),
                        rotationDegrees = fromConnection.toRotationDegrees(),
                        timestampMillis = 0L,
                    ),
            ),
        )
        if (onFrame == null) return

        CVPixelBufferLockBaseAddress(imageBuffer, 0uL)
        val baseAddress = CVPixelBufferGetBaseAddress(imageBuffer)
        val bufferSize = CVPixelBufferGetDataSize(imageBuffer)
        val data = NSData.dataWithBytes(bytes = baseAddress, length = bufferSize)
        CVPixelBufferUnlockBaseAddress(imageBuffer, 0uL)

        val bytes = data.toByteArray()
        onFrame.invoke(bytes)
    }
}

/**
 * Mutable holder so the [PhotoCaptureDelegate] can read the latest preview-layer reference at
 * capture time without having to recreate the delegate when the layer is created.
 */
class PreviewLayerHolder {
    var layer: AVCaptureVideoPreviewLayer? = null
}

private fun AVCaptureConnection?.toRotationDegrees(): Int =
    when (this?.videoOrientation) {
        AVCaptureVideoOrientationLandscapeLeft -> 0
        AVCaptureVideoOrientationLandscapeRight -> 180
        AVCaptureVideoOrientationPortrait -> 90
        else -> 0
    }

private fun CameraPreviewScaleType.toAvLayerVideoGravity(): String? =
    when (this) {
        CameraPreviewScaleType.AspectFill -> AVLayerVideoGravityResizeAspectFill
        CameraPreviewScaleType.AspectFit -> AVLayerVideoGravityResizeAspect
    }

private fun CameraPreviewOrientationMode.toForcedVideoOrientation(): Long? =
    when (this) {
        CameraPreviewOrientationMode.FollowDevice -> null
        CameraPreviewOrientationMode.Portrait -> AVCaptureVideoOrientationPortrait
    }

class PhotoCaptureDelegate(
    private val onCaptureEnd: () -> Unit,
    private val onCapture: (byteArray: ByteArray?) -> Unit,
    private val previewLayerProvider: () -> AVCaptureVideoPreviewLayer? = { null },
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

            // Apple's recommended approach: convert the visible preview-layer rect to normalized
            // capture-output coordinates, then crop the captured image to that rect. This makes
            // the saved photo match what the preview layer was displaying.
            // https://developer.apple.com/documentation/avfoundation/avcapturevideopreviewlayer/1623501-metadataoutputrectconverted
            val previewLayer = previewLayerProvider()
            if (previewLayer != null) {
                cropImageToPreviewLayer(uiImage, previewLayer)?.let { uiImage = it }
            }

            val imageData = UIImagePNGRepresentation(uiImage)
            val byteArray: ByteArray? = imageData?.toByteArray()
            onCapture(byteArray)
        }
        onCaptureEnd()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun cropImageToPreviewLayer(
    image: UIImage,
    previewLayer: AVCaptureVideoPreviewLayer,
): UIImage? {
    val layerSize = previewLayer.bounds.useContents { size.width to size.height }
    if (layerSize.first <= 0.0 || layerSize.second <= 0.0) return null

    // Normalized rect in [0,1] of the captured image describing the visible preview area.
    // Apple's K/N name for `metadataOutputRectConverted(fromLayerRect:)` is
    // `metadataOutputRectOfInterestForRect(_:)` (the Objective-C selector).
    val outputRect = previewLayer.metadataOutputRectOfInterestForRect(previewLayer.bounds)
    val rectInfo = outputRect.useContents {
        listOf(origin.x, origin.y, size.width, size.height)
    }
    val originX = rectInfo[0]
    val originY = rectInfo[1]
    val rectWidth = rectInfo[2]
    val rectHeight = rectInfo[3]
    if (rectWidth <= 0.0 || rectHeight <= 0.0) return null

    val cgImage = image.CGImage ?: return null
    val pixelWidth = CGImageGetWidth(cgImage).toDouble()
    val pixelHeight = CGImageGetHeight(cgImage).toDouble()
    if (pixelWidth <= 0.0 || pixelHeight <= 0.0) return null

    val cropX = (originX * pixelWidth).coerceIn(0.0, pixelWidth - 1.0)
    val cropY = (originY * pixelHeight).coerceIn(0.0, pixelHeight - 1.0)
    val cropWidth = (rectWidth * pixelWidth).coerceAtMost(pixelWidth - cropX)
    val cropHeight = (rectHeight * pixelHeight).coerceAtMost(pixelHeight - cropY)
    if (cropWidth < 1.0 || cropHeight < 1.0) return null

    val cropRect = CGRectMake(cropX, cropY, cropWidth, cropHeight)
    val croppedCgImage = CGImageCreateWithImageInRect(cgImage, cropRect) ?: return null
    return UIImage.imageWithCGImage(croppedCgImage, image.scale, image.imageOrientation)
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
