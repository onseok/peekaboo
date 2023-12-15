package com.preat.peekaboo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Suppress("FunctionName")
@Composable
expect fun PeekabooCamera(
    modifier: Modifier,
    onCapture: (byteArray: ByteArray?) -> Unit,
)
