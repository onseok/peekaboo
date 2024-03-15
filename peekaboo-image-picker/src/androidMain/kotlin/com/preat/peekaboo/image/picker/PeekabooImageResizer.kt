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
import android.provider.OpenableColumns
import androidx.annotation.FloatRange
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal object PeekabooImageResizer {
    internal fun resizeImageAsync(
        context: Context,
        coroutineScope: CoroutineScope,
        uri: Uri,
        width: Int,
        height: Int,
        resizeThresholdBytes: Long,
        @FloatRange(from = 0.0, to = 1.0)
        compressionQuality: Double,
        filterOptions: FilterOptions,
        onResult: (ByteArray?) -> Unit,
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            if (getImageSize(context, uri) > resizeThresholdBytes) {
                val byteArray = resizeImage(context, uri, width, height, compressionQuality, filterOptions)
                withContext(Dispatchers.Main) {
                    onResult(byteArray)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(getOriginalImageByteArray(context, uri))
                }
            }
        }
    }

    private fun getImageSize(
        context: Context,
        uri: Uri,
    ): Int =
        context.contentResolver.query(uri, null, null, null, null).use { cursor ->
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
            cursor?.moveToFirst()
            sizeIndex?.let { cursor.getInt(it) } ?: 0
        }

    private fun getOriginalImageByteArray(
        context: Context,
        uri: Uri,
    ): ByteArray? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val rotatedBitmap = rotateImageIfRequired(context, bitmap, uri)

            ByteArrayOutputStream().apply {
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
            }.toByteArray()
        }
    }

    private suspend fun resizeImage(
        context: Context,
        uri: Uri,
        width: Int,
        height: Int,
        @FloatRange(from = 0.0, to = 1.0)
        compression: Double,
        filterOptions: FilterOptions,
    ): ByteArray? {
        val resizeCacheKey = "${uri}_w${width}_h$height"
        val filterCacheKey = "${resizeCacheKey}_$filterOptions"

        withContext(Dispatchers.IO) {
            PeekabooBitmapCache.instance.get(filterCacheKey)
        }?.let { cachedBitmap ->
            return PeekabooBitmapCache.bitmapToByteArray(cachedBitmap)
        }

        val resizedBitmap =
            withContext(Dispatchers.IO) {
                PeekabooBitmapCache.instance.get(resizeCacheKey)
            } ?: run {
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
                        BitmapFactory.decodeStream(scaledInputStream, null, options)
                            ?.also { bitmap ->
                                PeekabooBitmapCache.instance.put(resizeCacheKey, bitmap)
                            }
                    }
                }
            }

        resizedBitmap?.let {
            val rotatedBitmap = rotateImageIfRequired(context, it, uri)
            val filteredBitmap = applyFilter(rotatedBitmap, filterOptions)

            ByteArrayOutputStream().use { byteArrayOutputStream ->
                val validatedCompression = compression.coerceIn(0.0, 1.0)
                filteredBitmap.compress(Bitmap.CompressFormat.JPEG, (100 * validatedCompression).toInt(), byteArrayOutputStream)
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

        return Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
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
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.preScale(1.0f, -1.0f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1.0f, 1.0f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1.0f, 1.0f)
                matrix.postRotate(90f)
            }
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
