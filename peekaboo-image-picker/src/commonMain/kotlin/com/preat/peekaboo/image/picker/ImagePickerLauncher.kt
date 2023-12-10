package com.preat.peekaboo.image.picker

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

@Composable
expect fun rememberImagePickerLauncher(
    selectionMode: SelectionMode = SelectionMode.Single,
    scope: CoroutineScope?,
    onResult: (List<ByteArray>) -> Unit,
): ImagePickerLauncher

sealed class SelectionMode {
    data object Single : SelectionMode()

    data class Multiple(val maxSelection: Int = INFINITY) : SelectionMode()

    companion object {
        const val INFINITY = 0
    }
}

expect class ImagePickerLauncher(
    selectionMode: SelectionMode,
    onLaunch: () -> Unit,
) {
    fun launch()
}
