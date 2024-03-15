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
package com.preat.peekaboo.ui.gallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import androidx.core.os.bundleOf
import com.preat.peekaboo.ui.gallery.model.PeekabooMediaImage

private val projection =
    arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )

internal fun Context.createCursor(
    limit: Int,
    offset: Int,
): Cursor? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val bundle =
            bundleOf(
                ContentResolver.QUERY_ARG_OFFSET to offset,
                ContentResolver.QUERY_ARG_LIMIT to limit,
                ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(MediaStore.Images.Media.DATE_ADDED),
                ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            bundle,
            null,
        )
    } else {
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset",
            null,
        )
    }
}

internal fun Context.fetchPagePicture(
    limit: Int,
    offset: Int,
): List<PeekabooMediaImage> {
    val pictures = ArrayList<PeekabooMediaImage>()
    val cursor = createCursor(limit, offset)
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val displayName = it.getString(displayNameColumn)
            val contentUri =
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id,
                )

            pictures.add(
                PeekabooMediaImage(
                    id = id,
                    uri = contentUri,
                    name = displayName,
                ),
            )
        }
    }
    cursor?.close()
    return pictures
}
