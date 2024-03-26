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
package com.preat.peekaboo.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSPredicate
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalMaterialApi::class)
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
    val imageFlow = remember { MutableSharedFlow<List<UIImage>>(replay = 1) }
    val images by imageFlow.collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.background(backgroundColor)) {
        if (checkPhotoLibraryAuthorization()) {
            LaunchedEffect(Unit) {
                fetchImagesFromGalleryAsFlow().collectLatest { fetchedImages ->
                    imageFlow.emit(fetchedImages)
                    isLoading = false
                }
            }

            if (isLoading) {
                progressIndicator()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.columns),
                    state = lazyGridState,
                    contentPadding = PaddingValues(horizontal = state.contentPadding.dp),
                    horizontalArrangement = Arrangement.spacedBy(state.itemSpacing.dp),
                    verticalArrangement = Arrangement.spacedBy(state.itemSpacing.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { header() }
                    items(images.size) { index ->
                        val imageByteArray = images[index].toByteArray()
                        imageByteArray?.toImageBitmap()?.let {
                            Card(
                                shape = RoundedCornerShape(state.cornerSize.dp),
                                modifier =
                                    Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(state.cornerSize.dp)),
                                onClick = {
                                    onImageSelected(imageByteArray)
                                },
                            ) {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            permissionDeniedContent()
        }
    }
}

private fun CoroutineScope.fetchImagesFromGalleryAsFlow(): Flow<List<UIImage>> =
    flow {
        val images = fetchImagesFromGallery()
        emit(images)
    }.catch {
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

@OptIn(ExperimentalForeignApi::class)
private suspend fun fetchImagesFromGallery(): List<UIImage> =
    withContext(Dispatchers.Default) {
        val fetchOptions =
            PHFetchOptions().apply {
                predicate = NSPredicate.predicateWithFormat("mediaType = %d", PHAssetMediaTypeImage)
            }
        val photos = PHAsset.fetchAssetsWithOptions(fetchOptions)
        (0 until photos.count.toInt()).mapNotNull { index ->
            val asset = photos.objectAtIndex(index.toULong()) as PHAsset
            asset.getAssetThumbnail(CGSizeMake(asset.pixelWidth.toDouble(), asset.pixelHeight.toDouble()))
        }
    }

@OptIn(ExperimentalForeignApi::class)
private suspend fun PHAsset.getAssetThumbnail(targetSize: CValue<CGSize>): UIImage? =
    withContext(Dispatchers.Default) {
        var image: UIImage? = null
        val options =
            PHImageRequestOptions().apply {
                setSynchronous(true)
                setNetworkAccessAllowed(true)
            }
        PHImageManager.defaultManager().requestImageForAsset(
            this@getAssetThumbnail,
            targetSize,
            PHImageContentModeAspectFill,
            options,
        ) { uiImage, _ ->
            image = uiImage
        }
        image
    }

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toByteArray(): ByteArray? {
    val jpegData = UIImageJPEGRepresentation(this, 1.0) ?: return null
    return ByteArray(jpegData.length.toInt()).apply {
        memcpy(this.refTo(0), jpegData.bytes, jpegData.length)
    }
}

private fun ByteArray.toImageBitmap(): ImageBitmap = Image.makeFromEncoded(this).toComposeImageBitmap()

private fun checkPhotoLibraryAuthorization(): Boolean {
    val status = PHPhotoLibrary.authorizationStatus()
    return status == PHAuthorizationStatusAuthorized
}
