package com.preat.peekaboo.ui.camera

internal fun CameraMode.inverse(): CameraMode {
    return when (this) {
        CameraMode.Back -> CameraMode.Front
        CameraMode.Front -> CameraMode.Back
    }
}

internal fun CameraMode.id(): Int {
    return when (this) {
        CameraMode.Back -> 0
        CameraMode.Front -> 1
    }
}

internal fun cameraModeFromId(id: Int): CameraMode {
    return when (id) {
        0 -> CameraMode.Back
        1 -> CameraMode.Front
        else -> throw IllegalArgumentException("CameraMode with id=${id} does not exists")
    }
}
