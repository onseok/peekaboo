package com.preat.peekaboo.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import com.preat.peekaboo.image.picker.toImageBitmap
import com.preat.peekaboo.ui.BackButton
import com.preat.peekaboo.ui.PeekabooCamera
import com.preat.peekaboo.ui.style.PeekabooTheme
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
                                onCapture = { byteArray ->
                                    byteArray?.let {
                                        images = listOf(it.toImageBitmap())
                                    } ?: scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message =
                                                "Error occurred.",
                                        )
                                    }
                                    showCamera = false
                                },
                            )
                        }
                        TopLayout(
                            alignLeftContent = {
                                BackButton {
                                    showCamera = false
                                }
                            },
                            alignRightContent = {},
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(images) { image ->
                            Image(
                                bitmap = image,
                                contentDescription = "Selected Image",
                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
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
                        Text("Capture Camera Image")
                    }
                }
            }
        }
    }
}
