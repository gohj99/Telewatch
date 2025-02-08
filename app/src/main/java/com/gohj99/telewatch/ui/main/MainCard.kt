/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun LinkText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFEFEFE),
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    onLinkClick: ((String) -> Unit)? = null
) {
    if (text.isNotEmpty()) {
        val annotatedString = buildAnnotatedString {
            val urlRegex = Regex(
                "(?i)\\b((https?|ftp)://[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,63}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)?)"
            )

            val usernameRegex = Regex("(?<!\\w)@(\\w{4,})(?!\\w)")
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*") // Regex for bold text

            var lastIndex = 0

            (urlRegex.findAll(text) + usernameRegex.findAll(text) + boldRegex.findAll(text))
                .sortedBy { it.range.first }
                .forEach { result ->
                    val start = result.range.first
                    val end = result.range.last + 1

                    if (start >= lastIndex) {
                        append(text.substring(lastIndex, start))
                    }

                    when {
                        result.value.matches(urlRegex) -> {
                            pushStringAnnotation(tag = "URL", annotation = result.value)
                            withStyle(style = SpanStyle(color = Color(0xFF2397D3), textDecoration = TextDecoration.Underline)) {
                                append(result.value)
                            }
                            pop()
                        }
                        result.value.matches(usernameRegex) -> {
                            val nowUrl = "https://t.me/${result.value.trimStart('@')}"
                            pushStringAnnotation(tag = "URL", annotation = nowUrl)
                            withStyle(style = SpanStyle(color = Color(0xFF2397D3), textDecoration = TextDecoration.Underline)) {
                                append(result.value)
                            }
                            pop()
                        }
                        result.value.matches(boldRegex) -> {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(result.groups[1]?.value ?: "") // Append the text inside the asterisks
                            }
                        }
                        else -> {
                            append(result.value)
                        }
                    }

                    lastIndex = end
                }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }

        ClickableText(
            text = annotatedString,
            style = style.copy(color = color),
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onLinkClick?.invoke(annotation.item)
                    }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> MainCard(
    column: @Composable () -> Unit,
    item: T,
    callback: (T) -> Unit = {},
    onLongClick: (T) -> Unit = {},
    color: Color = Color(0xFF404953)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .combinedClickable(
                onClick = { callback(item) }, // 处理点击
                onLongClick = { onLongClick(item) } // 处理长按
            ),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color // 设置 Card 的背景颜色
        )
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 9.dp, end = 14.dp, bottom = 9.dp)) {
            column()
        }
    }
}
