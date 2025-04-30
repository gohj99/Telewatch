/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import org.drinkless.tdlib.TdApi

@Composable
fun messageDrawer(
    onLinkClick: (String) -> Unit,
    content: TdApi.MessageContent,
    stateDownload: MutableState<Boolean>,
    stateDownloadDone: MutableState<Boolean>,
    textColor: Color,
    showUnknownMessageType: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val onEntityClick = { clickedText: String, entityType: TdApi.TextEntityType ->
        // 根据 entityType 执行不同的操作
        when (entityType) {
            is TdApi.TextEntityTypeUrl -> {
                // 打开链接
                // context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickedText)))
                println("Clicked URL: $clickedText")
                onLinkClick(clickedText)
            }
            is TdApi.TextEntityTypeTextUrl -> {
                // 打开 TextUrl 中包含的链接
                val url = entityType.url // 对于 TextUrl，需要获取其内部的 url 字段
                try {
                    // val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    // context.startActivity(intent)
                    println("Attempted to open TextUrl: $url (display text: $clickedText)")
                    onLinkClick(url)
                } catch (e: Exception) {
                    println("Failed to open TextUrl: $e")
                    // 显示错误提示
                }
            }
            is TdApi.TextEntityTypeBotCommand -> {
                // 执行机器人命令的逻辑，例如发送消息 "/start"
                // 通常需要将这个命令发送回 Telegram API
                println("Bot Command clicked: $clickedText. Implement sending command.")
                copyText(clickedText, context)
            }
            is TdApi.TextEntityTypeMention -> {
                println("Clicked Mention: $clickedText")
                if (clickedText.startsWith("@")) {
                    onLinkClick("https://t.me/${clickedText}")
                }
            }
            is TdApi.TextEntityTypeHashtag -> {
                // 处理点击话题标签的逻辑，例如搜索该话题
                println("Hashtag clicked: $clickedText. Implement searching hashtag.")
            }
            is TdApi.TextEntityTypeEmailAddress -> {
                // 处理点击邮箱地址的逻辑，例如打开邮件应用
                val emailUri = Uri.fromParts("mailto", clickedText, null)
                try {
                    val intent = Intent(Intent.ACTION_SENDTO, emailUri)
                    context.startActivity(intent)
                    println("Attempted to send email to: $clickedText")
                } catch (e: Exception) {
                    println("Failed to open email app: $e")
                }
            }
            is TdApi.TextEntityTypePhoneNumber -> {
                // 处理点击电话号码的逻辑，例如打开拨号应用
                val telUri = Uri.fromParts("tel", clickedText.replace("[^\\d+]".toRegex(), ""), null) // 移除除数字和+号外的字符
                try {
                    val intent = Intent(Intent.ACTION_DIAL, telUri)
                    context.startActivity(intent)
                    println("Attempted to dial number: $clickedText")
                } catch (e: Exception) {
                    println("Failed to open dialer: $e")
                }
            }
            is TdApi.TextEntityTypeBankCardNumber -> {
                // 处理点击银行卡号的逻辑，例如复制到剪贴板
                println("Bank Card Number clicked: $clickedText. Implement copy to clipboard.")
                copyText(clickedText, context)
            }
            is TdApi.TextEntityTypeBlockQuote -> {
                // 处理点击 BlockQuote 的逻辑 (如果它确实需要点击处理)
                println("Block Quote clicked: $clickedText. Implement blockquote specific action if any.")
            }
            else -> {
                // 默认处理或者不做任何事情
                println("Clicked on entity type without specific handler: ${entityType::class.java.simpleName}")
            }
        }
    }

    when (content) {
        is TdApi.MessageText -> {
            SelectionContainer {
                FormattedText(
                    text = content.text.text,
                    entities = content.text.entities,
                    modifier = modifier,
                    style = MaterialTheme.typography.bodyMedium,
                    onEntityClick = onEntityClick
                )
            }
        }
        is TdApi.MessagePhoto -> {
            val thumbnail = content.photo.sizes.minByOrNull { it.width * it.height }
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.photo,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = Color(0xFFFEFEFE),
                    modifier = modifier
                )
            } else {
                // 处理没有缩略图的情况
                Text(
                    text = stringResource(id = R.string.No_thumbnail_available),
                    color = Color(0xFFFEFEFE),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier
                )
            }
            // 图片文字
            content.caption?.text?.let {
                SelectionContainer {
                    FormattedText(
                        text = it,
                        entities = content.caption.entities,
                        modifier = modifier,
                        style = MaterialTheme.typography.bodyMedium,
                        onEntityClick = onEntityClick
                    )
                }
            }
        }
        is TdApi.MessageVideo -> {
            val thumbnail = content.video.thumbnail
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    ThumbnailImage(
                        thumbnail = thumbnail.file,
                        imageWidth = thumbnail.width,
                        imageHeight = thumbnail.height,
                        textColor = Color(0xFFFEFEFE),
                        modifier = modifier
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize() // 覆盖层与图片大小一致
                            .background(Color.Black.copy(alpha = 0.5f)) // 设置半透明黑色背景
                    )
                } else {
                    // 处理没有缩略图的情况
                    Text(
                        text = stringResource(id = R.string.No_thumbnail_available),
                        color = Color(0xFFFEFEFE),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }

                if (stateDownload.value) SplashLoadingScreen()
                val videoFile = content.video.video
                if (videoFile.local.isDownloadingCompleted) {
                    stateDownloadDone.value = true
                }
                if (stateDownloadDone.value) {
                    Image(
                        painter = painterResource(id = R.drawable.play),
                        contentDescription = null,
                        modifier = modifier
                            .align(Alignment.Center)
                            .size(36.dp) // 设置图标大小为 24dp
                    )
                } else {
                    if (!stateDownload.value) {
                        Image(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = null,
                            modifier = modifier
                                .align(Alignment.Center)
                                .size(36.dp) // 设置图标大小为 24dp
                        )
                    }
                }
            }

            // 视频文字
            content.caption?.text?.let {
                SelectionContainer {
                    FormattedText(
                        text = it,
                        entities = content.caption.entities,
                        modifier = modifier,
                        style = MaterialTheme.typography.bodyMedium,
                        onEntityClick = onEntityClick
                    )
                }
            }
        }
        // GIF信息
        is TdApi.MessageAnimation -> {
            val thumbnail = content.animation.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    modifier = modifier
                )
            }

            // GIF文字
            content.caption?.text?.let {
                SelectionContainer {
                    FormattedText(
                        text = it,
                        entities = content.caption.entities,
                        modifier = modifier,
                        style = MaterialTheme.typography.bodyMedium,
                        onEntityClick = onEntityClick
                    )
                }
            }
        }
        // 表情消息
        is TdApi.MessageAnimatedEmoji -> {
            val emoji = content.emoji
            val thumbnail = content.animatedEmoji.sticker?.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    loadingText = emoji,
                    modifier = modifier
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }
            }
        }
        // 贴纸表情消息
        is TdApi.MessageSticker -> {
            val emoji = content.sticker.emoji
            val thumbnail = content.sticker.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    loadingText = emoji,
                    modifier = modifier
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }
            }
        }
        // 语音消息
        is TdApi.MessageVoiceNote -> {
            MessageVideoNote(
                messageVideoNote = content,
                modifier = modifier
            )
        }
        // 文件消息
        is TdApi.MessageDocument -> {
            MessageFile(
                content = content,
                stateDownload = stateDownload,
                stateDownloadDone = stateDownloadDone,
                modifier = modifier
            )

            // 文件文字
            content.caption?.text?.let {
                SelectionContainer {
                    FormattedText(
                        text = it,
                        entities = content.caption.entities,
                        modifier = modifier,
                        style = MaterialTheme.typography.bodyMedium,
                        onEntityClick = onEntityClick
                    )
                }
            }
        }
        else -> {
            SelectionContainer {
                Text(
                    text = stringResource(id = R.string.Unknown_Message) + if (showUnknownMessageType) "\nType: TdApi." + getMessageContentTypeName(content) else "",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier
                )
            }
        }
    }
}
