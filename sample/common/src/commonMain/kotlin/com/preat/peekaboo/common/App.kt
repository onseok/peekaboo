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
package com.preat.peekaboo.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.common.component.PeekabooCameraView
import com.preat.peekaboo.common.component.PeekabooGalleryView
import com.preat.peekaboo.common.style.PeekabooTheme
import com.preat.peekaboo.image.picker.FilterOptions
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf(listOf<ImageBitmap>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    var showGallery by rememberSaveable { mutableStateOf(false) }

    val singleImagePicker =
        rememberImagePickerLauncher(
            selectionMode = SelectionMode.Single,
            scope = scope,
            onResult = { byteArrays ->
                byteArrays.firstOrNull()?.let {
                    images = listOf(it.toImageBitmap())
                }
            },
        )

    val multipleImagePicker =
        rememberImagePickerLauncher(
            // Optional: Set a maximum selection limit, e.g., SelectionMode.Multiple(maxSelection = 5).
            // Default: No limit, depends on system's maximum capacity.
            selectionMode = SelectionMode.Multiple(maxSelection = 5),
            scope = scope,
            // Resize options are customizable. Default is set to 800 x 800 pixels.
            resizeOptions = ResizeOptions(width = 1200, height = 1200, compressionQuality = 1.0),
            // Default is 'Default', which applies no filter.
            // Other available options: GrayScale, Sepia, Invert.
            filterOptions = FilterOptions.GrayScale,
            onResult = { byteArrays ->
                images =
                    byteArrays.map { byteArray ->
                        byteArray.toImageBitmap()
                    }
            },
        )

    PeekabooTheme {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    showCamera -> {
                        PeekabooCameraView(
                            modifier = Modifier.fillMaxSize(),
                            onBack = { showCamera = false },
                            onCapture = { byteArray ->
                                byteArray?.let {
                                    images = listOf(it.toImageBitmap())
                                }
                                showCamera = false
                            },
                        )
                    }
                    showGallery -> {
                        PeekabooGalleryView(
                            modifier = Modifier.fillMaxSize(),
                            onBack = { showGallery = false },
                            onImageSelected = { byteArray ->
                                byteArray?.let {
                                    images = listOf(it.toImageBitmap())
                                }
                                showGallery = false
                            },
                        )
                    }
                    else -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            items(images) { image ->
                                Image(
                                    bitmap = image,
                                    contentDescription = "Selected Image",
                                    modifier =
                                        Modifier
                                            .size(200.dp)
                                            .clip(shape = RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                singleImagePicker.launch()
                            },
                        ) {
                            Text("Pick Single Image")
                        }
                        Button(
                            onClick = {
                                multipleImagePicker.launch()
                            },
                        ) {
                            Text("Pick Multiple Images")
                        }
                        Button(
                            onClick = {
                                showCamera = true
                            },
                        ) {
                            Text("Capture Image from Camera")
                        }
                        Button(
                            onClick = {
                                showGallery = true
                            },
                        ) {
                            Text("Pick Image from Gallery")
                        }
                    }
                }
            }
        }
    }
}
