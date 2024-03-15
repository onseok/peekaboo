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
package com.preat.peekaboo.ui.gallery

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cash.paging.compose.LazyPagingItems
import app.cash.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.preat.peekaboo.ui.gallery.model.PeekabooMediaImage
import com.preat.peekaboo.ui.gallery.repository.PeekabooGalleryRepositoryImpl
import com.preat.peekaboo.ui.gallery.viewmodel.PeekabooGalleryViewModel
import com.preat.peekaboo.ui.gallery.viewmodel.PeekabooGalleryViewModelFactory
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@ExperimentalPeekabooGalleryApi
@Composable
actual fun PeekabooGallery(
    modifier: Modifier,
    state: GalleryPickerState,
    lazyGridState: LazyGridState,
    backgroundColor: Color,
    header: @Composable () -> Unit,
    progressIndicator: @Composable () -> Unit,
    permissionDeniedContent: @Composable () -> Unit,
    onImageSelected: (ByteArray?) -> Unit,
) {
    val context = LocalContext.current

    val storagePermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            rememberPermissionState(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }

    val peekabooGalleryViewModel: PeekabooGalleryViewModel =
        viewModel(
            factory =
                PeekabooGalleryViewModelFactory(
                    PeekabooGalleryRepositoryImpl(
                        context,
                    ),
                ),
        )

    val lazyPeekabooGalleryImages: LazyPagingItems<PeekabooMediaImage> =
        peekabooGalleryViewModel.getImages().collectAsLazyPagingItems()

    Box(modifier = modifier.background(backgroundColor)) {
        if (storagePermissionState.status.isGranted) {
            Column(modifier = modifier.background(backgroundColor)) {
                header()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.columns),
                    state = lazyGridState,
                    contentPadding = PaddingValues(horizontal = state.contentPadding.dp),
                    horizontalArrangement = Arrangement.spacedBy(state.itemSpacing.dp),
                    verticalArrangement = Arrangement.spacedBy(state.itemSpacing.dp),
                ) {
                    items(
                        count = lazyPeekabooGalleryImages.itemCount,
                        key = { index ->
                            lazyPeekabooGalleryImages.peek(index)?.id ?: index
                        },
                    ) { index ->
                        lazyPeekabooGalleryImages[index]?.let { photo ->
                            val bitmap = getOriginalImageByteArray(context, photo.uri)?.toBitmap()
                            bitmap?.let {
                                Card(
                                    shape = RoundedCornerShape(state.cornerSize.dp),
                                    modifier = Modifier.aspectRatio(1f),
                                    onClick = {
                                        onImageSelected(
                                            getOriginalImageByteArray(
                                                context,
                                                photo.uri,
                                            ),
                                        )
                                    },
                                ) {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.clip(shape = RoundedCornerShape(state.cornerSize.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (storagePermissionState.status.shouldShowRationale) {
                permissionDeniedContent()
            } else {
                LaunchedEffect(Unit) {
                    storagePermissionState.launchPermissionRequest()
                }
            }
        }
    }
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

private fun ByteArray.toBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}
