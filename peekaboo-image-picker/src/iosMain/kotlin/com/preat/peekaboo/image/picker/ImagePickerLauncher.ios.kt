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
package com.preat.peekaboo.image.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
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
                                val resizedImage = image?.fitInto(resizeOptions.width, resizeOptions.height)
                                val bytes = resizedImage?.toByteArray()
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
private fun UIImage.toByteArray(): ByteArray {
    val jpegData = UIImageJPEGRepresentation(this, 1.0)!!
    return ByteArray(jpegData.length.toInt()).apply {
        memcpy(this.refTo(0), jpegData.bytes, jpegData.length)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.fitInto(
    width: Int,
    height: Int,
): UIImage {
    val targetSize = CGSizeMake(width.toDouble(), height.toDouble())
    return this.resize(targetSize)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.resize(targetSize: CValue<CGSize>): UIImage {
    UIGraphicsBeginImageContextWithOptions(targetSize, false, 0.0)
    this.drawInRect(CGRectMake(0.0, 0.0, targetSize.useContents { width }, targetSize.useContents { height }))
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

actual class ImagePickerLauncher actual constructor(
    selectionMode: SelectionMode,
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}
