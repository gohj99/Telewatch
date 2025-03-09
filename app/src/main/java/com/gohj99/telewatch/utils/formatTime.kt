/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatTimestampToTime(unixTimestamp: Int): String {
    // 将 Unix 时间戳从 Int 转换为 Long，并转换为毫秒
    val date = Date(unixTimestamp.toLong() * 1000)
    // 定义时间格式
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    // 返回格式化的时间字符串
    return format.format(date)
}

fun formatDuration(duration: Int): String {
    val minutes = duration / 60
    val seconds = duration % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun formatTimestampToDate(unixTimestamp: Int): String {
    // 将时间戳转换为 Date 对象
    val date = Date(unixTimestamp.toLong() * 1000)

    // 获取当前年份和时间戳对应的年份
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val calendar = Calendar.getInstance().apply { time = date }
    val timestampYear = calendar.get(Calendar.YEAR)

    return if (timestampYear == currentYear) {
        // 当年份相同时，仅显示月和日。通过 skeleton 获取本地化的日期格式（例如：中文可能显示为 "M月d日"）
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd")
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    } else {
        // 当年份不同时，显示完整日期（包含年份），使用系统本地化中等格式
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(date)
    }
}
