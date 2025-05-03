/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

/**
 * 根据聊天标题的第一个字母和颜色 ID 生成一个默认聊天图标的 Bitmap
 *
 * @param context 用于访问资源和显示指标
 * @param title 聊天标题，用于获取第一个字母
 * @param accentColorId 用于获取背景颜色
 * @return 生成的 Bitmap
 */
fun generateChatTitleIconBitmap(
    context: Context,
    title: String,
    accentColorId: Int
): Bitmap {
    val density = context.resources.displayMetrics.density

    // 转换为像素
    val sizeDp = 35f // 从 35.dp 获取值
    val textSizeSp = 18f // 从 18.sp 获取值

    val sizePx = (sizeDp * density).toInt()
    val textSizePx = (textSizeSp * density).toInt()

    // 创建一个可变的 Bitmap
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)

    // --- 绘制圆形背景 ---
    val circlePaint = Paint().apply {
        color = getColorById(accentColorId).toArgb() // 使用 getColorById 获取颜色并转换为 ARGB
        style = Paint.Style.FILL
        isAntiAlias = true // 启用抗锯齿
    }

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val radius = sizePx / 2f // 半径就是一半的尺寸

    canvas.drawCircle(centerX, centerY, radius, circlePaint)

    // --- 绘制文本 ---
    val textPaint = Paint().apply {
        color = Color.White.toArgb() // 文字颜色为白色
        textSize = textSizePx.toFloat() // 文字大小
        textAlign = Paint.Align.CENTER // 设置文本对齐方式为中心
        isAntiAlias = true
        // 您可能还需要设置字体，Compose 默认使用系统字体
        // typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // 确定要显示的文本（第一个大写字母），处理空字符串情况
    val text = title
        .takeIf { it.isNotEmpty() } // 如果标题不为空
        ?.get(0) // 获取第一个字符
        ?.uppercaseChar() // 转换为大写字符
        ?.toString() // 转换为字符串
        ?: "?" // 如果标题为空，使用 "?" 作为默认文本

    // 计算文本的基线位置，使其在垂直方向上居中
    // 对于 Paint.Align.CENTER，drawText 的 x 坐标是水平中心，y 坐标是基线位置
    // 文本高度 = ascent + descent (ascent 是负数)
    // 文本的半高 = (descent - ascent) / 2
    // 垂直居中的 y 坐标（基于中心）= (文本的半高) - descent
    // 基线 y 坐标 = centerY + 垂直居中的 y 坐标（基于中心）
    val fontMetrics = textPaint.fontMetrics
    val textBaseLineY = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f


    canvas.drawText(text, centerX, textBaseLineY, textPaint)

    return bitmap
}
