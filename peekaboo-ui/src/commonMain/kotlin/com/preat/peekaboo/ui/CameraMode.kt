package com.preat.peekaboo.ui

sealed class CameraMode {
    data object Front : CameraMode()

    data object Back : CameraMode()
}
