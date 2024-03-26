/*
 * Copyright 2023-2024 onseok
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
package com.preat.peekaboo.ui.camera

/**
 * Represents the camera modes available in the `PeekabooCamera` composable.
 * This sealed class is used to define whether the front or back camera should be used.
 *
 * `CameraMode` allows developers to specify the initial camera facing direction.
 */
sealed class CameraMode {
    /**
     * Represents the front-facing camera mode.
     * Use this mode to utilize the device's front camera in the PeekabooCamera composable.
     */
    data object Front : CameraMode()

    /**
     * Represents the back-facing camera mode.
     * Use this mode to utilize the device's rear camera in the PeekabooCamera composable.
     */
    data object Back : CameraMode()
}

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
        else -> throw IllegalArgumentException("CameraMode with id=$id does not exists")
    }
}
