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
package com.preat.peekaboo.image.picker

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.setValue
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.darwin.NSObject
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify
import platform.posix.memcpy

@Composable
actual fun rememberImagePickerLauncher(
    selectionMode: SelectionMode,
    scope: CoroutineScope,
    resizeOptions: ResizeOptions,
    filterOptions: FilterOptions,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val delegate =
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>,
            ) {
                picker.dismissViewControllerAnimated(flag = true, completion = null)
                @Suppress("UNCHECKED_CAST")
                val results = didFinishPicking as List<PHPickerResult>

                val dispatchGroup = dispatch_group_create()
                val imageData = mutableListOf<ByteArray>()

                for (result in results) {
                    dispatch_group_enter(dispatchGroup)
                    result.itemProvider.loadDataRepresentationForTypeIdentifier(
                        typeIdentifier = "public.image",
                    ) { nsData, _ ->
                        scope.launch(Dispatchers.Main) {
                            nsData?.let {
                                val image = UIImage.imageWithData(it)
                                val resizedImage =
                                    image?.fitInto(
                                        resizeOptions.width,
                                        resizeOptions.height,
                                        resizeOptions.resizeThresholdBytes,
                                        resizeOptions.compressionQuality,
                                        filterOptions,
                                    )
                                val bytes = resizedImage?.toByteArray(resizeOptions.compressionQuality)
                                if (bytes != null) {
                                    imageData.add(bytes)
                                }
                                dispatch_group_leave(dispatchGroup)
                            }
                        }
                    }
                }

                dispatch_group_notify(dispatchGroup, platform.darwin.dispatch_get_main_queue()) {
                    scope.launch(Dispatchers.Main) {
                        onResult(imageData)
                    }
                }
            }
        }

    return remember {
        ImagePickerLauncher(
            selectionMode = selectionMode,
            onLaunch = {
                val pickerController = createPHPickerViewController(delegate, selectionMode)
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                    pickerController,
                    true,
                    null,
                )
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toByteArray(compressionQuality: Double): ByteArray {
    val validCompressionQuality = compressionQuality.coerceIn(0.0, 1.0)
    val jpegData = UIImageJPEGRepresentation(this, validCompressionQuality)!!
    return ByteArray(jpegData.length.toInt()).apply {
        memcpy(this.refTo(0), jpegData.bytes, jpegData.length)
    }
}
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun UIImage.fixImageOrientation(): UIImage {
    if (this.imageOrientation == UIImageOrientation.UIImageOrientationUp) return this

    UIGraphicsBeginImageContext(size = this.size)
    this.drawAtPoint(CGPointMake(0.0, 0.0))
    val fixedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return fixedImage ?: this
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.fitInto(
    maxWidth: Int,
    maxHeight: Int,
    resizeThresholdBytes: Long,
    @FloatRange(from = 0.0, to = 1.0)
    compressionQuality: Double,
    filterOptions: FilterOptions,
): UIImage {
    val fixedImage = runBlocking { this@fitInto.fixImageOrientation() }
    val imageData = fixedImage.toByteArray(compressionQuality)
    if (imageData.size > resizeThresholdBytes) {
        val originalWidth = fixedImage.size.useContents { width }
        val originalHeight = fixedImage.size.useContents { height }
        val originalRatio = originalWidth / originalHeight

        val targetRatio = maxWidth.toDouble() / maxHeight.toDouble()
        val scale =
            if (originalRatio > targetRatio) {
                maxWidth.toDouble() / originalWidth
            } else {
                maxHeight.toDouble() / originalHeight
            }

        val newWidth = originalWidth * scale
        val newHeight = originalHeight * scale

        val targetSize = CGSizeMake(newWidth, newHeight)
        val resizedImage = fixedImage.resize(targetSize)

        return applyFilterToUIImage(resizedImage, filterOptions)
    } else {
        return applyFilterToUIImage(fixedImage, filterOptions)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.resize(targetSize: CValue<CGSize>): UIImage {
    UIGraphicsBeginImageContextWithOptions(targetSize, false, 0.0)
    this.drawInRect(
        CGRectMake(
            0.0,
            0.0,
            targetSize.useContents { width },
            targetSize.useContents { height },
        ),
    )
    val newImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return newImage!!
}

private fun createPHPickerViewController(
    delegate: PHPickerViewControllerDelegateProtocol,
    selection: SelectionMode,
): PHPickerViewController {
    val pickerViewController =
        PHPickerViewController(
            configuration =
                when (selection) {
                    is SelectionMode.Multiple ->
                        PHPickerConfiguration().apply {
                            setSelectionLimit(selectionLimit = selection.maxSelection.toLong())
                            setFilter(filter = PHPickerFilter.imagesFilter)
                            setSelection(selection = PHPickerConfigurationSelectionOrdered)
                        }
                    SelectionMode.Single ->
                        PHPickerConfiguration().apply {
                            setSelectionLimit(selectionLimit = 1)
                            setFilter(filter = PHPickerFilter.imagesFilter)
                            setSelection(selection = PHPickerConfigurationSelectionOrdered)
                        }
                },
        )
    pickerViewController.delegate = delegate
    return pickerViewController
}

@OptIn(ExperimentalForeignApi::class)
private fun applyFilterToUIImage(
    image: UIImage,
    filterOptions: FilterOptions,
): UIImage {
    val ciImage = CIImage.imageWithCGImage(image.CGImage)

    val filteredCIImage =
        when (filterOptions) {
            FilterOptions.GrayScale -> {
                CIFilter.filterWithName("CIPhotoEffectNoir")?.apply {
                    setValue(ciImage, forKey = "inputImage")
                }?.outputImage
            }
            FilterOptions.Sepia -> {
                CIFilter.filterWithName("CISepiaTone")?.apply {
                    setValue(ciImage, forKey = "inputImage")
                    setValue(0.8, forKey = "inputIntensity")
                }?.outputImage
            }
            FilterOptions.Invert -> {
                CIFilter.filterWithName("CIColorInvert")?.apply {
                    setValue(ciImage, forKey = "inputImage")
                }?.outputImage
            }
            FilterOptions.Default -> ciImage
        }

    val context = CIContext.contextWithOptions(null)
    return filteredCIImage?.let {
        val filteredCGImage = context.createCGImage(it, fromRect = it.extent())
        UIImage.imageWithCGImage(filteredCGImage)
    } ?: image
}

actual class ImagePickerLauncher actual constructor(
    selectionMode: SelectionMode,
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}
