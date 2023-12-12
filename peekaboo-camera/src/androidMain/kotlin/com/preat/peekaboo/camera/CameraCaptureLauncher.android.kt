package com.preat.peekaboo.camera

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberCameraCaptureLauncher(
    scope: CoroutineScope,
    onResult: (ByteArray?) -> Unit,
    onCameraPermissionDenied: (() -> Unit)?,
): CameraCaptureLauncher {
    val context = LocalContext.current
    val file = context.createImageFile()
    val uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file,
        )

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) {
                val imageBytes = file.readBytes()
                onResult(imageBytes)
            } else {
                onResult(null)
            }
        }

    val cameraPermissionState =
        rememberPermissionState(
            permission = Manifest.permission.CAMERA,
            onPermissionResult = {
                if (it) {
                    cameraLauncher.launch(uri)
                } else {
                    onCameraPermissionDenied?.invoke()
                }
            },
        )

    return remember {
        CameraCaptureLauncher(
            onLaunch = {
                cameraPermissionState.launchPermissionRequest()
            },
        )
    }
}

actual class CameraCaptureLauncher actual constructor(
    private val onLaunch: () -> Unit,
) {
    actual fun launch() {
        onLaunch()
    }
}

fun Context.createImageFile(): File {
    // Generate a file name using the current time
    val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
    // Add a unique identifier to prevent file name duplication
    val uniqueID = createUUID()
    val imageFileName = "${timeStamp}_$uniqueID"

    return File.createTempFile(
        imageFileName,
        ".jpg",
        externalCacheDir,
    )
}
