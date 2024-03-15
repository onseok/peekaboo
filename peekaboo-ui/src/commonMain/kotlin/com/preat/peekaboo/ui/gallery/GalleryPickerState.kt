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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

private const val DEFAULT_CONTENT_PADDING = 4
private const val DEFAULT_ITEM_SPACING = 4
private const val DEFAULT_CORNER_SIZE = 0
private const val DEFAULT_COLUMNS = 3

/**
 * Remembers the state for a gallery picker, providing values for UI customization.
 *
 * @param contentPadding The horizontal padding applied to the whole content of the gallery grid.
 * @param itemSpacing The spacing between individual items in the gallery grid, applied both horizontally and vertically.
 * @param cornerSize The corner size for the card items in the gallery.
 * @param columns The number of columns in the gallery grid.
 */
@Composable
fun rememberGalleryPickerState(
    contentPadding: Int = DEFAULT_CONTENT_PADDING,
    itemSpacing: Int = DEFAULT_ITEM_SPACING,
    cornerSize: Int = DEFAULT_CORNER_SIZE,
    columns: Int = DEFAULT_COLUMNS,
): GalleryPickerState =
    rememberSaveable(saver = GalleryPickerState.Saver) {
        GalleryPickerState(
            contentPadding = contentPadding,
            itemSpacing = itemSpacing,
            cornerSize = cornerSize,
            columns = columns,
        )
    }

@Stable
class GalleryPickerState(
    val contentPadding: Int,
    val itemSpacing: Int,
    val cornerSize: Int,
    val columns: Int,
) {
    internal companion object {
        val Saver: Saver<GalleryPickerState, *> =
            listSaver(
                save = {
                    listOf<Any>(
                        it.contentPadding,
                        it.itemSpacing,
                        it.cornerSize,
                        it.columns,
                    )
                },
                restore = {
                    GalleryPickerState(
                        contentPadding = it[0] as Int,
                        itemSpacing = it[1] as Int,
                        cornerSize = it[2] as Int,
                        columns = it[3] as Int,
                    )
                },
            )
    }
}
