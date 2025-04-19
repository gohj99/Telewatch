/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R

@Composable
fun LinkText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFEFEFE),
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    onLinkClick: ((String) -> Unit)? = null
) {
    if (text.isEmpty()) return

    val annotatedString = buildAnnotatedString {
        // 改进后的URL正则表达式（支持更广泛的域名格式）
        val urlRegex = Regex(
            """(?i)\b((?:https?|ftp)://)?(?:www\.|[\p{L}0-9-]+\.)*[\p{L}0-9-]+(?:\.[\p{L}0-9-]+)+(?:/[/\p{L}0-9-_.~?=%&@#]*)?\b"""
        )
        val usernameRegex = Regex(
            """(?<!\S)@(\w{4,})(?![\w/.-])"""
        )
        val boldRegex = Regex(
            """\*\*([^*]+)\*\*"""
        )

        var lastIndex = 0

        // 匹配优先级：URL > Bold > Username
        val allMatches = sequence {
            yieldAll(urlRegex.findAll(text))
            yieldAll(boldRegex.findAll(text))
            yieldAll(usernameRegex.findAll(text))
        }.sortedBy { it.range.first }

        allMatches.forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1

            // 处理未匹配的普通文本
            if (start > lastIndex) append(text.substring(lastIndex, start))

            when {
                match.value.matches(urlRegex) -> {
                    val fullUrl = when {
                        match.value.startsWith("www.") -> "https://${match.value}"
                        match.value.contains("://") -> match.value
                        else -> "https://${match.value}"
                    }
                    pushStringAnnotation("URL", fullUrl)
                    withStyle(SpanStyle(
                        color = colorResource(id = R.color.blue_dark),
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(match.value)
                    }
                    pop()
                }
                match.value.matches(boldRegex) -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                }
                match.value.matches(usernameRegex) -> {
                    val username = match.groupValues[1]
                    pushStringAnnotation("URL", "https://t.me/$username")
                    withStyle(SpanStyle(
                        color = colorResource(id = R.color.blue_dark),
                        textDecoration = TextDecoration.Underline
                    )) {
                        append("@$username")
                    }
                    pop()
                }
            }

            lastIndex = end
        }

        // 添加剩余文本
        if (lastIndex < text.length) append(text.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        style = style.copy(color = color),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { onLinkClick?.invoke(it.item) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> MainCard(
    column: @Composable () -> Unit,
    item: T,
    callback: (T) -> Unit = {},
    onLongClick: (T) -> Unit = {},
    color: Color = Color(0xFF404953),
    modifier: Modifier = Modifier.padding(start = 12.dp, top = 9.dp, end = 14.dp, bottom = 9.dp)
) {
    // 获取context
    val context = LocalContext.current
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val lineSpacing = settingsSharedPref.getFloat("Line_spacing", 5.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = lineSpacing.dp)
            .combinedClickable(
                onClick = { callback(item) }, // 处理点击
                onLongClick = { onLongClick(item) } // 处理长按
            ),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color // 设置 Card 的背景颜色
        )
    ) {
        Column(modifier = modifier) {
            column()
        }
    }
}
