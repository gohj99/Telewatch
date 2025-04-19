/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

// 定义主题颜色
private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = Blue700,
    tertiary = Blue100,
    background = Color(0xFF000000)
)

// 定义 SharedPreferences 的 Key
private const val PREFS_NAME = "app_settings"
private const val KEY_TITLE_MEDIUM_FONT_SIZE = "title_medium_font_size"
private const val KEY_BODY_SMALL_FONT_SIZE = "body_small_font_size"
private const val KEY_BODY_MEDIUM_FONT_SIZE = "body_medium_font_size"
private const val KEY_GLOBAL_SCALE_FACTOR = "global_scale_factor" // 全局缩放因子 Key

// 默认值
private const val DEFAULT_TITLE_MEDIUM_FONT_SIZE = 14.3f
private const val DEFAULT_BODY_SMALL_FONT_SIZE = 13.3f
private const val DEFAULT_BODY_MEDIUM_FONT_SIZE = 13.5f
private const val DEFAULT_GLOBAL_SCALE_FACTOR = 1.0f // 默认不缩放

@Composable
fun TelewatchTheme(
    // 你可以添加参数来控制是否启用暗色模式等，这里暂时保持强制暗色
    // useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsSharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 1. 读取基础字体大小配置 (这些是未缩放前的基础值)
    val titleMediumBaseFontSize = settingsSharedPref.getFloat(KEY_TITLE_MEDIUM_FONT_SIZE, DEFAULT_TITLE_MEDIUM_FONT_SIZE)
    val bodySmallBaseFontSize = settingsSharedPref.getFloat(KEY_BODY_SMALL_FONT_SIZE, DEFAULT_BODY_SMALL_FONT_SIZE)
    val bodyMediumBaseFontSize = settingsSharedPref.getFloat(KEY_BODY_MEDIUM_FONT_SIZE, DEFAULT_BODY_MEDIUM_FONT_SIZE)

    // 2. 读取全局缩放因子配置
    //    你可以提供一个设置界面让用户调整这个值并保存到 SharedPreferences
    val globalScaleFactor = settingsSharedPref.getFloat(KEY_GLOBAL_SCALE_FACTOR, DEFAULT_GLOBAL_SCALE_FACTOR)

    // 3. 定义基础排版样式 (使用未缩放的字体大小)
    //    因为后续 LocalDensity 会统一处理缩放，这里不需要乘以 scaleFactor
    val customTypography = Typography(
        titleMedium = TextStyle(
            color = Color.White, // 保持颜色，或者根据 colorScheme 调整
            fontSize = titleMediumBaseFontSize.sp // 使用基础 sp 值
        ),
        bodyMedium = TextStyle(
            color = Color.White,
            fontSize = bodyMediumBaseFontSize.sp  // 使用基础 sp 值
        ),
        bodySmall = TextStyle(
            color = Color.White,
            fontSize = bodySmallBaseFontSize.sp  // 使用基础 sp 值
        )
        // 你可以根据需要定义更多文字样式...
    )

    // 4. 获取当前的 Density
    val currentDensity = LocalDensity.current

    // 5. 创建一个新的、经过缩放的 Density 对象
    //    同时乘以 density 和 fontScale，确保 dp 和 sp 单位都被缩放
    val scaledDensity = Density(
        density = currentDensity.density * globalScaleFactor,
        fontScale = currentDensity.fontScale * globalScaleFactor
    )

    // 6. 定义颜色方案 (这里仍然强制使用暗色)
    val colorScheme = DarkColorScheme // 或者根据 useDarkTheme 参数选择

    // 7. 使用 CompositionLocalProvider 提供新的、缩放后的 Density
    //    MaterialTheme 及其 content 中的所有 Composable 都会使用这个新的 Density
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = customTypography, // 应用基础排版样式
            content = content // content 中的所有 dp 和 sp 都会基于 scaledDensity 计算
        )
    }
}
