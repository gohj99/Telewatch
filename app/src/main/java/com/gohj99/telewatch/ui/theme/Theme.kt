/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

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
    val context = LocalContext.current
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val titleMediumFontSize = settingsSharedPref.getFloat("title_medium_font_size", 13f)
    val bodySmallFontSize = settingsSharedPref.getFloat("body_small_font_size", 14f)
    val bodyMediumFontSize = settingsSharedPref.getFloat("body_medium_font_size", 12f)

    val customTypography = Typography(
        titleMedium = TextStyle(
            color = Color.White,
            fontSize = titleMediumFontSize.sp // 聊天标题字体大小
        ),
        bodyMedium = TextStyle(
            color = Color.White,
            fontSize = bodyMediumFontSize.sp  // 聊天页面字体
        ),
        bodySmall = TextStyle(
            color = Color.White,
            fontSize = bodySmallFontSize.sp  // 首页列表标题下文字大小
        )
    )

    val colorScheme = DarkColorScheme // 强制使用亮色主题

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content,
    )
}
