package com.preat.peekaboo.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher

@Suppress("FunctionName")
@Composable
fun App() {
    val scope = rememberCoroutineScope()

    val singleImagePicker =
        rememberImagePickerLauncher(
            selectionMode = SelectionMode.Single,
            scope = scope,
            onResult = { byteArrays ->
                byteArrays.firstOrNull()?.let {
                    // Process the selected images' ByteArrays.
                    println(it)
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
                byteArrays.forEach {
                    // Process the selected images' ByteArrays.
                    println(it)
                }
            },
        )

    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
        }
    }
}
