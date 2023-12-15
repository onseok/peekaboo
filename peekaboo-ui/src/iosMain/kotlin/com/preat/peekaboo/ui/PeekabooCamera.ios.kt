package com.preat.peekaboo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
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
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
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
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
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
import platform.darwin.NSObject
import platform.posix.memcpy

private val deviceTypes =
    listOf(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInDualWideCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInUltraWideCamera,
        AVCaptureDeviceTypeBuiltInDuoCamera,
    )

@Suppress("FunctionName")
@Composable
actual fun PeekabooCamera(
    modifier: Modifier,
    cameraMode: CameraMode,
    captureIcon: ImageVector,
    onCapture: (byteArray: ByteArray?) -> Unit,
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
                Text("Camera access denied", color = Color.White)
            }

            CameraAccess.Authorized -> {
                AuthorizedCamera(cameraMode, captureIcon, onCapture)
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun BoxScope.AuthorizedCamera(
    cameraMode: CameraMode,
    captureIcon: ImageVector,
    onCapture: (byteArray: ByteArray?) -> Unit,
) {
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
        RealDeviceCamera(camera, captureIcon, onCapture)
    } else {
        Text(
            """
            Camera is not available on simulator.
            Please try to run on a real iOS device.
            """.trimIndent(),
            color = Color.White,
        )
    }
}

@Suppress("FunctionName")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
private fun BoxScope.RealDeviceCamera(
    camera: AVCaptureDevice,
    captureIcon: ImageVector,
    onCapture: (byteArray: ByteArray?) -> Unit,
) {
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

                @OptIn(ExperimentalForeignApi::class)
                private fun UIImage.fitInto(px: Int): UIImage {
                    val targetScale =
                        maxOf(
                            px.toFloat() / size.useContents { width },
                            px.toFloat() / size.useContents { height },
                        )
                    val newSize =
                        size.useContents { CGSizeMake(width * targetScale, height * targetScale) }
                    return resize(newSize)
                }

                @OptIn(ExperimentalForeignApi::class)
                private fun UIImage.resize(targetSize: CValue<CGSize>): UIImage {
                    val currentSize = this.size
                    val widthRatio =
                        targetSize.useContents { width } / currentSize.useContents { width }
                    val heightRatio =
                        targetSize.useContents { height } / currentSize.useContents { height }

                    val newSize: CValue<CGSize> =
                        if (widthRatio > heightRatio) {
                            CGSizeMake(
                                width = currentSize.useContents { width } * heightRatio,
                                height = currentSize.useContents { height } * heightRatio,
                            )
                        } else {
                            CGSizeMake(
                                width = currentSize.useContents { width } * widthRatio,
                                height = currentSize.useContents { height } * widthRatio,
                            )
                        }
                    val newRect =
                        CGRectMake(
                            x = 0.0,
                            y = 0.0,
                            width = newSize.useContents { width },
                            height = newSize.useContents { height },
                        )
                    UIGraphicsBeginImageContextWithOptions(
                        size = newSize,
                        opaque = false,
                        scale = 1.0,
                    )
                    this.drawInRect(newRect)
                    val newImage = UIGraphicsGetImageFromCurrentImageContext()
                    UIGraphicsEndImageContext()

                    return newImage!!
                }
            }
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
            val cameraContainer = UIView()
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            captureSession.startRunning()
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
    CircularButton(
        imageVector = captureIcon,
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(36.dp),
        enabled = !capturePhotoStarted,
    ) {
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
    if (capturePhotoStarted) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .size(80.dp)
                    .align(Alignment.Center),
            color = Color.White.copy(alpha = 0.7f),
            strokeWidth = 8.dp,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
inline fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return byteArray
}
