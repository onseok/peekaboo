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
package com.preat.peekaboo.ui.gallery.datasource

import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import com.preat.peekaboo.ui.gallery.model.PeekabooMediaImage

internal class PeekabooGalleryDataSource(
    private val onFetch: (limit: Int, offset: Int) -> List<PeekabooMediaImage>,
) : PagingSource<Int, PeekabooMediaImage>() {
    override fun getRefreshKey(state: PagingState<Int, PeekabooMediaImage>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PeekabooMediaImage> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize
        val pictures = onFetch.invoke(pageSize, pageNumber * pageSize)
        val prevKey = if (pageNumber > 0) pageNumber.minus(1) else null
        val nextKey = if (pictures.isNotEmpty()) pageNumber.plus(1) else null

        return LoadResult.Page(
            data = pictures,
            prevKey = prevKey,
            nextKey = nextKey,
        )
    }
}
