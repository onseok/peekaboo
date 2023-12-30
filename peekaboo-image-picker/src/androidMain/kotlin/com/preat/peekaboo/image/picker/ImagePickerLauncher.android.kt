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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.preat.peekaboo.image.picker.PeekabooBitmapCache.bitmapToByteArray
import com.preat.peekaboo.image.picker.SelectionMode.Companion.INFINITY
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLauncher(
    selectionMode: SelectionMode,
    scope: CoroutineScope,
    resizeOptions: ResizeOptions,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    return when (selectionMode) {
        SelectionMode.Single ->
            pickSingleImage(
                selectionMode = selectionMode,
                resizeOptions = resizeOptions,
                onResult = onResult,
            )

        is SelectionMode.Multiple ->
            pickMultipleImages(
                selectionMode = selectionMode,
                resizeOptions = resizeOptions,
                onResult = onResult,
            )
    }
}

@Composable
private fun pickSingleImage(
    selectionMode: SelectionMode,
    resizeOptions: ResizeOptions,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    var imagePickerLauncher: ImagePickerLauncher? = null
    val singleImagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    val resizedImage =
                        resizeImage(
                            context = context,
                            uri = uri,
                            width = resizeOptions.width,
                            height = resizeOptions.height,
                        )
                    if (resizedImage != null) {
                        onResult(listOf(resizedImage))
                    }
                }
                imagePickerLauncher?.markPhotoPickerInactive()
            },
        )

    imagePickerLauncher =
        remember {
            ImagePickerLauncher(
                selectionMode = selectionMode,
                onLaunch = {
                    singleImagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }

    return imagePickerLauncher
}

@Composable
private fun pickMultipleImages(
    selectionMode: SelectionMode.Multiple,
    resizeOptions: ResizeOptions,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    var imagePickerLauncher: ImagePickerLauncher? = null
    val maxSelection =
        if (selectionMode.maxSelection == INFINITY) {
            getMaxItems()
        } else {
            selectionMode.maxSelection
        }

    val multipleImagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxSelection),
            onResult = { uriList ->
                val imageBytesList =
                    uriList.mapNotNull { uri ->
                        resizeImage(
                            context = context,
                            uri = uri,
                            width = resizeOptions.width,
                            height = resizeOptions.height,
                        )
                    }
                if (imageBytesList.isNotEmpty()) {
                    onResult(imageBytesList)
                }
                imagePickerLauncher?.markPhotoPickerInactive()
            },
        )

    imagePickerLauncher =
        remember {
            ImagePickerLauncher(
                selectionMode = selectionMode,
                onLaunch = {
                    multipleImagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }

    return imagePickerLauncher
}

actual class ImagePickerLauncher actual constructor(
    selectionMode: SelectionMode,
    private val onLaunch: () -> Unit,
) {
    private var isPhotoPickerActive = false

    fun markPhotoPickerInactive() {
        isPhotoPickerActive = false
    }

    actual fun launch() {
        if (isPhotoPickerActive) return

        isPhotoPickerActive = true
        onLaunch()
    }
}

private fun resizeImage(
    context: Context,
    uri: Uri,
    width: Int,
    height: Int,
): ByteArray? {
    val cacheKey = "${uri}_w${width}_h$height"
    PeekabooBitmapCache.instance.get(cacheKey)?.let { cachedBitmap ->
        return bitmapToByteArray(cachedBitmap)
    }

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)

        var inSampleSize = 1
        while (options.outWidth / inSampleSize > width || options.outHeight / inSampleSize > height) {
            inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize

        context.contentResolver.openInputStream(uri)?.use { scaledInputStream ->
            BitmapFactory.decodeStream(scaledInputStream, null, options)?.let { scaledBitmap ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()
                    PeekabooBitmapCache.instance.put(cacheKey, scaledBitmap)
                    return byteArray
                }
            }
        }
    }
    return null
}
