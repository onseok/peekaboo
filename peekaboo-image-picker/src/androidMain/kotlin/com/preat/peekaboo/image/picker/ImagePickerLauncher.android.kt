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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.preat.peekaboo.image.picker.SelectionMode.Companion.INFINITY
import kotlinx.coroutines.CoroutineScope

@Composable
actual fun rememberImagePickerLauncher(
    selectionMode: SelectionMode,
    scope: CoroutineScope?,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    return when (selectionMode) {
        SelectionMode.Single ->
            pickSingleImage(
                selectionMode = selectionMode,
                onResult = onResult,
            )

        is SelectionMode.Multiple ->
            pickMultipleImages(
                selectionMode = selectionMode,
                onResult = onResult,
            )
    }
}

@Composable
private fun pickSingleImage(
    selectionMode: SelectionMode,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current

    val singleImagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    with(context.contentResolver) {
                        openInputStream(uri)?.use {
                            onResult(listOf(it.readBytes()))
                        }
                    }
                }
            },
        )

    return remember {
        ImagePickerLauncher(
            selectionMode = selectionMode,
            onLaunch = {
                singleImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
fun pickMultipleImages(
    selectionMode: SelectionMode.Multiple,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
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
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                if (imageBytesList.isNotEmpty()) {
                    onResult(imageBytesList)
                }
            },
        )

    return remember {
        ImagePickerLauncher(
            selectionMode = selectionMode,
            onLaunch = {
                multipleImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

actual class ImagePickerLauncher actual constructor(
    selectionMode: SelectionMode,
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}
