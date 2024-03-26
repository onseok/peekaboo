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
package com.preat.peekaboo.ui.gallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.preat.peekaboo.ui.gallery.model.PeekabooMediaImage
import com.preat.peekaboo.ui.gallery.repository.PeekabooGalleryRepository
import kotlinx.coroutines.flow.Flow

internal class PeekabooGalleryViewModel(
    private val peekabooGalleryRepository: PeekabooGalleryRepository,
) : ViewModel() {
    fun getImages(): Flow<PagingData<PeekabooMediaImage>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = 10,
                    initialLoadSize = 10,
                    enablePlaceholders = true,
                ),
        ) {
            peekabooGalleryRepository.getPicturePagingSource()
        }.flow.cachedIn(viewModelScope)
}
