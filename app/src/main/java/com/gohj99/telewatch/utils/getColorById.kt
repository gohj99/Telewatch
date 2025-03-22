/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils
import androidx.compose.ui.graphics.Color

/**
 * 根据 TDLib 中的 ColorId 返回对应的颜色
 * 注意：此处 RGB 值仅为社区整理的非官方数据，实际可能会有所不同
 */
fun getColorById(colorId: Int): Color {
    return when (colorId) {
        0 -> Color(0xFF55A6EE)   // 蓝色
        1 -> Color(0xFFAF52DE)   // 紫色
        2 -> Color(0xFF3E5369)   // 灰色
        3 -> Color(0xFF7EC758)   // 绿色
        4 -> Color(0xFF4FBCDD)   // 淡蓝色
        5 -> Color(0xFF54A4EC)   // 蓝色
        6 -> Color(0xFF5AC8FA)   // 青色
        7 -> Color(0xFFFF2D55)   // 粉色
        8 -> Color(0xFF0040FF)   // 深蓝色
        9 ->Color(0xFFFF3B30)   // 红色
        else -> Color(0xFF55A6EE)  // 默认返回蓝色
    }
}
