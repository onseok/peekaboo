package com.preat.peekaboo.camera

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFUUIDCreate
import platform.CoreFoundation.CFUUIDCreateString
import platform.Foundation.CFBridgingRelease

@OptIn(ExperimentalForeignApi::class)
actual fun createUUID(): String = CFBridgingRelease(CFUUIDCreateString(null, CFUUIDCreate(null))) as String
