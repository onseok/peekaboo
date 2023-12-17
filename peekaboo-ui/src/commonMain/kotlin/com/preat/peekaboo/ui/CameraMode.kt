package com.preat.peekaboo.ui

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
