package com.preat.peekaboo.ui.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object PeekabooColors {
    val background = Color(0xFFFFFFFF)
    val onBackground = Color(0xFF19191C)
    val uiLightBlack = Color(25, 25, 28).copy(alpha = 0.7f)
    val darkBackground = Color(0xFF19191C)
    val darkOnBackground = Color(0xFFFFFFFF)
}

@Suppress("FunctionName")
@Composable
fun PeekabooTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val colors =
        if (isDarkTheme) {
            MaterialTheme.colors.copy(
                background = PeekabooColors.darkBackground,
                onBackground = PeekabooColors.darkOnBackground,
            )
        } else {
            MaterialTheme.colors.copy(
                background = PeekabooColors.background,
                onBackground = PeekabooColors.onBackground,
            )
        }

    MaterialTheme(
        colors = colors,
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(letterSpacing = 0.sp)) {
            content()
        }
    }
}
