package com.gohj99.telewatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = Blue700,
    tertiary = Blue100,
    background = Color(0xFF000000)
)

@Composable
fun TelewatchTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // 强制使用亮色主题
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
