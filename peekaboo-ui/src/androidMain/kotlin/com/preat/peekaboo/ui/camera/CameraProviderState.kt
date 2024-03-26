package com.preat.peekaboo.ui.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun loadCameraProvider(context: Context): State<ProcessCameraProvider?> {
    return produceState<ProcessCameraProvider?>(null, context) {
        value = withContext(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
            ProcessCameraProvider.getInstance(context).await()
        }
    }
}
