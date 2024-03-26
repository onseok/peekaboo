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
import kotlinx.coroutines.CoroutineScope

private const val DEFAULT_RESIZE_IMAGE_WIDTH = 800
private const val DEFAULT_RESIZE_IMAGE_HEIGHT = 800
private const val DEFAULT_RESIZE_THRESHOLD_BYTES = 1048576L // 1MB

@Composable
expect fun rememberImagePickerLauncher(
    selectionMode: SelectionMode = SelectionMode.Single,
    scope: CoroutineScope,
    resizeOptions: ResizeOptions = ResizeOptions(),
    filterOptions: FilterOptions = FilterOptions.Default,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher

sealed class SelectionMode {
    data object Single : SelectionMode()

    data class Multiple(val maxSelection: Int = INFINITY) : SelectionMode()

    companion object {
        const val INFINITY = 0
    }
}

data class ResizeOptions(
    val width: Int = DEFAULT_RESIZE_IMAGE_WIDTH,
    val height: Int = DEFAULT_RESIZE_IMAGE_HEIGHT,
    val resizeThresholdBytes: Long = DEFAULT_RESIZE_THRESHOLD_BYTES,
    @FloatRange(from = 0.0, to = 1.0)
    val compressionQuality: Double = 1.0,
)

sealed interface FilterOptions {
    data object Default : FilterOptions

    data object GrayScale : FilterOptions

    data object Sepia : FilterOptions

    data object Invert : FilterOptions
}

expect class ImagePickerLauncher(
    selectionMode: SelectionMode,
    onLaunch: () -> Unit,
) {
    fun launch()
}
