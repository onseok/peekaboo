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
        SelectionMode.Single -> pickSingleImage(
            selectionMode = selectionMode,
            onResult = onResult
        )

        is SelectionMode.Multiple -> pickMultipleImages(
            selectionMode = selectionMode,
            onResult = onResult
        )
    }
}

@Composable
private fun pickSingleImage(
    selectionMode: SelectionMode,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                with(context.contentResolver) {
                    openInputStream(uri)?.use {
                        onResult(listOf(it.readBytes()))
                    }
                }
            }
        }
    )

    return remember {
        ImagePickerLauncher(
            selectionMode = selectionMode,
            onLaunch = {
                singleImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

@Composable
fun pickMultipleImages(
    selectionMode: SelectionMode.Multiple,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val maxSelection = if (selectionMode.maxSelection == INFINITY) {
        getMaxItems()
    } else {
        selectionMode.maxSelection
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxSelection),
        onResult = { uriList ->
            val imageBytesList = uriList.mapNotNull { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                }
            }
            if (imageBytesList.isNotEmpty()) {
                onResult(imageBytesList)
            }
        }
    )

    return remember {
        ImagePickerLauncher(
            selectionMode = selectionMode,
            onLaunch = {
                multipleImagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
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
