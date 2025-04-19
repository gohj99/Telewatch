/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.gohj99.telewatch.R
import org.drinkless.tdlib.TdApi

/**
 * Compose 函数，用于渲染带有 TdApi.TextEntity 格式的文本，并处理部分可点击实体。
 *
 * @param text 要渲染的原始文本字符串。
 * @param entities 适用于文本的 TdApi.TextEntity 数组。
 * @param onEntityClick 当一个可点击实体被点击时触发的回调。
 * 回调参数为被点击的文本片段和对应的 TdApi.TextEntityType 对象。
 */
@Composable
fun FormattedText(
    text: String,
    entities: Array<TdApi.TextEntity>?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    onEntityClick: (clickedText: String, entityType: TdApi.TextEntityType) -> Unit
) {
    // Ensure entities is not null and sorted by offset for correct application
    val sortedEntities = entities?.sortedBy { it.offset } ?: emptyList()

    val annotatedString = buildAnnotatedString {
        append(text)

        sortedEntities.forEach { entity ->
            val start = entity.offset
            val end = entity.offset + entity.length

            // Apply SpanStyle for formatting
            when (entity.type) {
                is TdApi.TextEntityTypeBold -> {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                }
                is TdApi.TextEntityTypeItalic -> {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                }
                is TdApi.TextEntityTypeUnderline -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
                is TdApi.TextEntityTypeStrikethrough -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                }
                is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode -> {
                    // Code and Pre might need specific background/padding, but basic style is monospace
                    addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
                    // Note: Pre and PreCode often have distinct formatting (like block display),
                    // which is harder to achieve with just SpanStyle in an inline Text.
                    // This example applies basic monospace font.
                }
                // Spoiler is explicitly excluded from processing per requirements
                // MediaTimestamp is explicitly excluded from processing per requirements
                // Cashtag is explicitly excluded from processing per requirements
                // CustomEmoji is explicitly excluded from processing per requirements
                // ExpandableBlockQuote is explicitly excluded from processing per requirements
                // Hashtag is explicitly excluded from processing per processing
                // PreCode is explicitly excluded from processing per requirements
                else -> {
                    // Default or no special style
                }
            }

            // Apply click annotation for specific types
            val isClickable = when (entity.type) {
                is TdApi.TextEntityTypeBankCardNumber,
                is TdApi.TextEntityTypeBlockQuote, // User listed this as potentially clickable
                is TdApi.TextEntityTypeBotCommand,
                is TdApi.TextEntityTypeEmailAddress,
                is TdApi.TextEntityTypeMention,
                is TdApi.TextEntityTypeMentionName,
                is TdApi.TextEntityTypePhoneNumber,
                is TdApi.TextEntityTypeTextUrl,
                is TdApi.TextEntityTypeUrl -> true
                else -> false
            }

            if (isClickable) {
                // Apply a style to indicate it's clickable (e.g., blue and underlined)
                addStyle(
                    SpanStyle(
                        color = colorResource(id = R.color.blue_dark), // Use theme color for links
                        textDecoration = TextDecoration.Underline
                    ), start, end
                )
                // Add a StringAnnotation to identify the clickable span and its type
                addStringAnnotation(
                    tag = "entity_click", // A tag to identify clickable spans
                    // Store the type info or an identifier. Storing the entity type class name string
                    // allows us to differentiate in the click handler.
                    annotation = entity.type::class.java.name,
                    start = start,
                    end = end
                )
                // Optionally, store the actual text for easier access in the click handler
                // addStringAnnotation(tag = "clicked_text", annotation = text.substring(start, end), start = start, end = end)
            }
        }
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            // Find annotations at the clicked offset
            annotatedString.getStringAnnotations(tag = "entity_click", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    // We found a clickable annotation. The annotation value is the entity type class name.
                    // Need to find the original entity based on the range and type name.
                    val clickedEntity = sortedEntities.firstOrNull {
                        it.offset == annotation.start &&
                                (it.offset + it.length) == annotation.end &&
                                it.type::class.java.name == annotation.item
                    }

                    clickedEntity?.let { entity ->
                        val clickedText = text.substring(entity.offset, entity.offset + entity.length)
                        onEntityClick(clickedText, entity.type)
                    }
                }
        },
        modifier = modifier,
        style = style
    )
}
