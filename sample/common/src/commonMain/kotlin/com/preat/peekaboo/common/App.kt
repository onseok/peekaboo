package com.preat.peekaboo.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher

@Composable
fun App() {

    val singleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = rememberCoroutineScope(),
        onResult = { byteArrays ->
            println(byteArrays)
        }
    )

    val multipleImagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Multiple,
        scope = rememberCoroutineScope(),
        onResult = { byteArrays ->
            println(byteArrays)
        }
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = {
                    singleImagePicker.launch()
                }
            ) {
                Text("Pick Single Image")
            }
            TextButton(
                onClick = {
                    multipleImagePicker.launch()
                }
            ) {
                Text("Pick Multiple Images")
            }
        }
    }
}