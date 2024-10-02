/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = Blue700,
    tertiary = Blue100,
    background = Color(0xFF000000)
)

val CustomTypography = Typography(
    titleMedium = TextStyle(
        fontSize = 14.sp // 聊天标题字体大小
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp  // 聊天页面字体
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp  // 首页列表标题下文字大小
    )
)

@Composable
fun TelewatchTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // 强制使用亮色主题
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CustomTypography,
        content = content,
    )
}
