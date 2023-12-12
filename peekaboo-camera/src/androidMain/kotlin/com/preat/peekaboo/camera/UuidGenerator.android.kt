package com.preat.peekaboo.camera

import java.util.UUID

actual fun createUUID(): String = UUID.randomUUID().toString()
