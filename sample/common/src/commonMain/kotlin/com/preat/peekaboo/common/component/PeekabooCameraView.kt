/*
 * Copyright 2024 onseok
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
package com.preat.peekaboo.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.preat.peekaboo.common.icon.IconCached
import com.preat.peekaboo.common.icon.IconClose
import com.preat.peekaboo.common.icon.IconWarning
import com.preat.peekaboo.ui.camera.PeekabooCamera
import com.preat.peekaboo.ui.camera.rememberPeekabooCameraState

@Composable
internal fun PeekabooCameraView(
    modifier: Modifier = Modifier,
    onCapture: (ByteArray?) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberPeekabooCameraState(onCapture = onCapture)
    Box(modifier = modifier) {
        PeekabooCamera(
            state = state,
            modifier = Modifier.fillMaxSize(),
            permissionDeniedContent = {
                PermissionDenied(
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
        CameraOverlay(
            isCapturing = state.isCapturing,
            onBack = onBack,
            onCapture = { state.capture() },
            onConvert = { state.toggleCamera() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PermissionDenied(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(color = MaterialTheme.colors.background),
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
}

@Composable
private fun CameraOverlay(
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onConvert: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        IconButton(
            onClick = onBack,
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
        if (isCapturing) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(80.dp)
                        .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 8.dp,
            )
        }
        CircularButton(
            imageVector = IconCached,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp),
            onClick = onConvert,
        )
        InstagramCameraButton(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            onClick = onCapture,
        )
    }
}
