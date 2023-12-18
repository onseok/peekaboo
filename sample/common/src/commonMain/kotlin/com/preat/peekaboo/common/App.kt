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
package com.preat.peekaboo.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.common.component.CircularButton
import com.preat.peekaboo.common.component.InstagramCameraButton
import com.preat.peekaboo.common.icon.IconCached
import com.preat.peekaboo.common.icon.IconClose
import com.preat.peekaboo.common.icon.IconWarning
import com.preat.peekaboo.common.style.PeekabooTheme
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import com.preat.peekaboo.ui.CameraMode
import com.preat.peekaboo.ui.PeekabooCamera
import kotlinx.coroutines.launch

@Suppress("FunctionName")
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf(listOf<ImageBitmap>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showCamera by remember { mutableStateOf(false) }

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
                if (showCamera) {
                    Box {
                        if (showCamera) {
                            PeekabooCamera(
                                modifier = Modifier.fillMaxSize(),
                                cameraMode = CameraMode.Back,
                                captureIcon = { onClick ->
                                    InstagramCameraButton(
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp),
                                        onClick = onClick,
                                    )
                                },
                                convertIcon = { onClick ->
                                    CircularButton(
                                        imageVector = IconCached,
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(bottom = 16.dp, end = 16.dp),
                                        onClick = onClick,
                                    )
                                },
                                progressIndicator = {
                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier
                                                .size(80.dp)
                                                .align(Alignment.Center),
                                        color = Color.White.copy(alpha = 0.7f),
                                        strokeWidth = 8.dp,
                                    )
                                },
                                onCapture = { byteArray ->
                                    byteArray?.let {
                                        images = listOf(it.toImageBitmap())
                                    } ?: scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message =
                                                "Unable to capture the image. Please try again.",
                                        )
                                    }
                                    showCamera = false
                                },
                                permissionDeniedContent = {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(color = MaterialTheme.colors.background),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = IconWarning,
                                            contentDescription = "Warning Icon",
                                            tint = MaterialTheme.colors.onBackground,
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Please grant the camera permission!",
                                            color = MaterialTheme.colors.onBackground,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                showCamera = false
                            },
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 16.dp, start = 16.dp),
                        ) {
                            Icon(
                                imageVector = IconClose,
                                contentDescription = "Back Button",
                                tint = Color.White,
                            )
                        }
                    }
                } else {
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
                }
            }
        }
    }
}
