package com.preat.peekaboo.ui

internal sealed interface CameraAccess {
    data object Undefined : CameraAccess

    data object Denied : CameraAccess

    data object Authorized : CameraAccess
}
