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
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import com.preat.peekaboo.image.picker.PeekabooBitmapCache.bitmapToByteArray
import com.preat.peekaboo.image.picker.SelectionMode.Companion.INFINITY
import kotlinx.coroutines.CoroutineScope
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePickerLauncher(
    selectionMode: SelectionMode,
    scope: CoroutineScope,
    resizeOptions: ResizeOptions,
    filterOptions: FilterOptions,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    return when (selectionMode) {
        SelectionMode.Single ->
            pickSingleImage(
                selectionMode = selectionMode,
                resizeOptions = resizeOptions,
                filterOptions = filterOptions,
                onResult = onResult,
            )

        is SelectionMode.Multiple ->
            pickMultipleImages(
                selectionMode = selectionMode,
                resizeOptions = resizeOptions,
                filterOptions = filterOptions,
                onResult = onResult,
            )
    }
}

@Composable
private fun pickSingleImage(
    selectionMode: SelectionMode,
    resizeOptions: ResizeOptions,
    filterOptions: FilterOptions,
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
                            filterOptions = filterOptions,
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
    filterOptions: FilterOptions,
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
                            filterOptions = filterOptions,
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
    filterOptions: FilterOptions,
): ByteArray? {
    val resizeCacheKey = "${uri}_w${width}_h$height"
    val filterCacheKey = "${resizeCacheKey}_$filterOptions"

    PeekabooBitmapCache.instance.get(filterCacheKey)?.let { cachedBitmap ->
        return bitmapToByteArray(cachedBitmap)
    }

    val resizedBitmap =
        PeekabooBitmapCache.instance.get(resizeCacheKey) ?: run {
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
                    BitmapFactory.decodeStream(scaledInputStream, null, options)?.also { bitmap ->
                        PeekabooBitmapCache.instance.put(resizeCacheKey, bitmap)
                    }
                }
            }
        }

    resizedBitmap?.let {
        val rotatedBitmap = rotateImageIfRequired(context, it, uri)
        val filteredBitmap = applyFilter(rotatedBitmap, filterOptions)

        ByteArrayOutputStream().use { byteArrayOutputStream ->
            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            PeekabooBitmapCache.instance.put(filterCacheKey, filteredBitmap)
            return byteArray
        }
    }

    return null
}

private fun applyFilter(
    originalBitmap: Bitmap,
    filterOptions: FilterOptions,
): Bitmap {
    val colorMatrix = ColorMatrix()
    when (filterOptions) {
        FilterOptions.Default -> return originalBitmap
        FilterOptions.GrayScale -> colorMatrix.setSaturation(0f)
        FilterOptions.Sepia -> {
            colorMatrix.setSaturation(0f)
            val sepiaMatrix =
                ColorMatrix().apply {
                    setScale(1f, 0.95f, 0.82f, 1f)
                }
            colorMatrix.postConcat(sepiaMatrix)
        }
        FilterOptions.Invert -> {
            colorMatrix.set(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
    }

    val paint =
        Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

    return Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
    }
}

private fun rotateImageIfRequired(
    context: Context,
    bitmap: Bitmap,
    uri: Uri,
): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
    val exif = ExifInterface(inputStream)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
