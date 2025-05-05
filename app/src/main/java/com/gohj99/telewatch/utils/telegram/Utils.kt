/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.gohj99.telewatch.R
import org.drinkless.tdlib.TdApi
import java.io.IOException
import java.util.Properties

// 处理和简化消息
fun TgApi.handleAllMessages(
    message: TdApi.Message? = null,
    messageContext: TdApi.MessageContent? = null,
    maxText: Int = 64
): AnnotatedString {
    val content: TdApi.MessageContent = messageContext ?: message?.content
    ?: return buildAnnotatedString { append(context.getString(R.string.Unknown_Message)) }

    return when (content) {
        is TdApi.MessageText -> buildAnnotatedString {
            val text = content.text.text.replace('\n', ' ')
            append(if (text.length > maxText) text.take(maxText) + "..." else text)
        }
        is TdApi.MessagePhoto -> buildAnnotatedString {
            // 将 Photo 文本设置为蓝色
            withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                append(context.getString(R.string.Photo))
            }
            append(" ")
            val caption = content.caption.text.replace('\n', ' ')
            append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
        }
        is TdApi.MessageVideo -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                append(context.getString(R.string.Video))
            }
            append(" ")
            val caption = content.caption.text.replace('\n', ' ')
            append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
        }
        is TdApi.MessageVoiceNote -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                append(context.getString(R.string.Voice))
            }
            append(" ")
            val caption = content.caption.text.replace('\n', ' ')
            append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
        }
        is TdApi.MessageAnimation -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                append(context.getString(R.string.Animation))
            }
            append(" ")
            val caption = content.caption.text.replace('\n', ' ')
            append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
        }
        is TdApi.MessageDocument -> buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                append(context.getString(R.string.File))
            }
            append(" ")
            val caption = content.document.fileName.replace('\n', ' ') + content.caption.text.replace('\n', ' ')
            append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
        }
        is TdApi.MessageAnimatedEmoji -> buildAnnotatedString {
            if (content.emoji.isEmpty()) append(context.getString(R.string.Unknown_Message))
            else append(content.emoji)
        }
        is TdApi.MessageSticker -> buildAnnotatedString {
            if (content.sticker.emoji.isEmpty()) append(context.getString(R.string.Unknown_Message))
            else append(content.sticker.emoji)
        }
        else -> buildAnnotatedString { append(context.getString(R.string.Unknown_Message)) }
    }
}

// 加载配置
internal fun loadConfig(context: Context): Properties {
    val properties = Properties()
    try {
        val inputStream = context.assets.open("config.properties")
        inputStream.use { properties.load(it) }
    } catch (e: IOException) {
        e.printStackTrace()
        // 处理异常，例如返回默认配置或通知用户
    }
    return properties
}

// 获取应用版本
fun TgApi.getAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName
    } catch (e: Exception) {
        "1.0.0"
    }.toString()
}

// 获取lastReadOutboxMessageId
fun TgApi.getLastReadOutboxMessageId(): MutableState<Long> {
    return lastReadOutboxMessageId
}

// 获取lastReadOutboxMessageId
fun TgApi.getLastReadInboxMessageId(): MutableState<Long> {
    return lastReadInboxMessageId
}

// 为 TdApi.Message 增加可选参数的 copy() 扩展
fun TdApi.Message.copy(
    id: Long = this.id,
    senderId: TdApi.MessageSender = this.senderId,
    chatId: Long = this.chatId,
    sendingState: TdApi.MessageSendingState? = this.sendingState,
    schedulingState: TdApi.MessageSchedulingState? = this.schedulingState,
    isOutgoing: Boolean = this.isOutgoing,
    isPinned: Boolean = this.isPinned,
    isFromOffline: Boolean = this.isFromOffline,
    canBeSaved: Boolean = this.canBeSaved,
    hasTimestampedMedia: Boolean = this.hasTimestampedMedia,
    isChannelPost: Boolean = this.isChannelPost,
    isTopicMessage: Boolean = this.isTopicMessage,
    containsUnreadMention: Boolean = this.containsUnreadMention,
    date: Int = this.date,
    editDate: Int = this.editDate,
    forwardInfo: TdApi.MessageForwardInfo? = this.forwardInfo,
    importInfo: TdApi.MessageImportInfo? = this.importInfo,
    interactionInfo: TdApi.MessageInteractionInfo? = this.interactionInfo,
    unreadReactions: Array<TdApi.UnreadReaction> = this.unreadReactions,
    factCheck: TdApi.FactCheck? = this.factCheck,
    replyTo: TdApi.MessageReplyTo? = this.replyTo,
    messageThreadId: Long = this.messageThreadId,
    savedMessagesTopicId: Long = this.savedMessagesTopicId,
    selfDestructType: TdApi.MessageSelfDestructType? = this.selfDestructType,
    selfDestructIn: Double = this.selfDestructIn,
    autoDeleteIn: Double = this.autoDeleteIn,
    viaBotUserId: Long = this.viaBotUserId,
    senderBusinessBotUserId: Long = this.senderBusinessBotUserId,
    senderBoostCount: Int = this.senderBoostCount,
    authorSignature: String = this.authorSignature,
    mediaAlbumId: Long = this.mediaAlbumId,
    effectId: Long = this.effectId,
    hasSensitiveContent: Boolean = this.hasSensitiveContent,
    restrictionReason: String = this.restrictionReason,
    content: TdApi.MessageContent = this.content,
    replyMarkup: TdApi.ReplyMarkup? = this.replyMarkup
): TdApi.Message = TdApi.Message(
    id,
    senderId,
    chatId,
    sendingState,
    schedulingState,
    isOutgoing,
    isPinned,
    isFromOffline,
    canBeSaved,
    hasTimestampedMedia,
    isChannelPost,
    isTopicMessage,
    containsUnreadMention,
    date,
    editDate,
    forwardInfo,
    importInfo,
    interactionInfo,
    unreadReactions,
    factCheck,
    replyTo,
    messageThreadId,
    savedMessagesTopicId,
    selfDestructType,
    selfDestructIn,
    autoDeleteIn,
    viaBotUserId,
    senderBusinessBotUserId,
    senderBoostCount,
    authorSignature,
    mediaAlbumId,
    effectId,
    hasSensitiveContent,
    restrictionReason,
    content,
    replyMarkup
)
