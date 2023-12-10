package com.preat.peekaboo.image.picker

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore

internal fun isSystemPickerAvailable(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // getExtension is seen as part of Android Tiramisu only while the SdkExtensions
        // have been added on Android R
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
    } else {
        false
    }
}

@SuppressLint("NewApi", "ClassVerificationFailure")
internal fun getMaxItems() =
    if (isSystemPickerAvailable()) {
        MediaStore.getPickImagesMaxLimit()
    } else {
        Integer.MAX_VALUE
    }
