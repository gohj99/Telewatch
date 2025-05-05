/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.ui.AutoScrollingText
import com.gohj99.telewatch.ui.animateScrollToItemCentered
import com.gohj99.telewatch.utils.telegram.createPrivateChat
import com.gohj99.telewatch.utils.telegram.getChat
import com.gohj99.telewatch.utils.telegram.getMessageTypeById
import com.gohj99.telewatch.utils.telegram.getUserName
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun MessageReplyCompose(
    message: TdApi.Message,
    chatId: Long,
    chatTitle: String,
    isCurrentUser: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    alignment: Arrangement.Horizontal = Arrangement.End,
    textColor: Color,
    stateDownload: MutableState<Boolean>,
    stateDownloadDone: MutableState<Boolean>,
    showUnknownMessageType: Boolean,
    chatList: MutableState<List<TdApi.Message>>,
    senderNameMap: MutableState<MutableMap<Long, String?>>,
    onLinkClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 保存和恢复MessageContent
    val MessageContentSaver = Saver<TdApi.MessageContent, Any>(
        save = { content ->
            when {
                // 优先处理文本类型
                content is TdApi.MessageText -> mapOf(
                    "type" to "MessageText",
                    "text" to content.text.text
                )
                // 其他类型统一使用ID引用
                else -> {
                    val cache = MessageCache.put(content)
                    mapOf("type" to "MessageReference", "cache" to cache)
                }
            }
        },
        restore = { value ->
            when ((value as Map<String, *>)["type"]) {
                "MessageText" -> TdApi.MessageText(
                    TdApi.FormattedText(value["text"] as String, emptyArray()),
                    null, null
                )
                "MessageReference" -> {
                    val cache = value["cache"] as Int
                    MessageCache.get(cache) ?: TdApi.MessageText(
                        TdApi.FormattedText("error", emptyArray()),
                        null, null
                    )
                }
                else -> TdApi.MessageText(
                    TdApi.FormattedText("error", emptyArray()),
                    null, null
                )
            }
        }
    )

    if (message.replyTo != null) {
        var senderName by rememberSaveable { mutableStateOf("") }
        var messagePosition by remember { mutableStateOf(-1) }
        val replyTo = message.replyTo
        if (replyTo is TdApi.MessageReplyToMessage) {
            var content by rememberSaveable(stateSaver = MessageContentSaver) { mutableStateOf<TdApi.MessageContent>(
                TdApi.MessageText(
                    TdApi.FormattedText(
                        context.getString(R.string.loading),
                        emptyArray()
                    ),
                    null,
                    null
                )
            )}
            LaunchedEffect(replyTo.chatId) {
                if (replyTo.origin != null) {
                    //println(replyTo.origin)
                    when (val origin = replyTo.origin) {
                        is TdApi.MessageOriginChannel -> {
                            if (origin.authorSignature != "") senderName = origin.authorSignature
                            else {
                                val chat = tgApi?.getChat(origin.chatId)
                                chat?.let {
                                    senderName = it.title
                                }
                            }
                        }
                        is TdApi.MessageOriginChat -> {
                            if (origin.authorSignature != "") senderName = origin.authorSignature
                            else {
                                val chat = tgApi?.getChat(origin.senderChatId)
                                chat?.let {
                                    senderName = it.title
                                }
                            }
                        }
                        is TdApi.MessageOriginHiddenUser -> {
                            senderName = origin.senderName
                        }
                        is TdApi.MessageOriginUser -> {
                            val chat = tgApi?.createPrivateChat(origin.senderUserId)
                            chat?.let {
                                senderName = it.title
                            }
                        }
                    }
                }
            }
            //println(replyTo.content)
            LaunchedEffect(replyTo.content) {
                if (replyTo.content != null) {
                    //println("replyTo.content: ${replyTo.content}")
                    content = replyTo.content!!
                } else {
                    if (replyTo.chatId == 0L) {
                        if (replyTo.quote != null) {
                            if (replyTo.quote!!.text != null) {
                                if (replyTo.quote!!.text.text != "") {
                                    content = TdApi.MessageText(
                                        TdApi.FormattedText(
                                            replyTo.quote!!.text.text,
                                            emptyArray()
                                        ),
                                        null,
                                        null
                                    )
                                }
                            }
                        } else {
                            content = TdApi.MessageText(
                                TdApi.FormattedText(
                                    context.getString(R.string.empty_message),
                                    emptyArray()
                                ),
                                null,
                                null
                            )
                        }
                    } else if (replyTo.chatId == chatId) {
                        var replyMessage = chatList.value.find { it.id == replyTo.messageId }
                        if (replyMessage == null) replyMessage = tgApi?.getMessageTypeById(replyTo.messageId)
                        else messagePosition = chatList.value.indexOfFirst {
                            it.id == replyTo.messageId
                        }
                        if (replyMessage != null) {
                            chatList.value.find { it.id == replyTo.messageId }?.let {
                                content = it.content
                            }

                            content = replyMessage.content

                            // 用户名称
                            //println(replyMessage.senderId)
                            if (replyMessage.senderId != null) {
                                val senderId = replyMessage.senderId
                                if (senderId is TdApi.MessageSenderUser){
                                    senderId.userId.let { senderUserId ->
                                        if (senderUserId in senderNameMap.value) {
                                            senderName = senderNameMap.value[senderUserId]!!
                                        } else {
                                            tgApi?.getUserName(senderUserId) { user ->
                                                senderName = user
                                                senderNameMap.value[senderUserId] = user
                                            }
                                        }
                                    }
                                } else if (senderId is TdApi.MessageSenderChat) {
                                    if (senderId.chatId == chatId) {
                                        senderName = chatTitle
                                    } else {
                                        val chat = tgApi?.getChat(senderId.chatId)
                                        chat?.let {
                                            senderName = it.title
                                        }
                                    }
                                }
                            }
                        } else {
                            content = TdApi.MessageText(
                                TdApi.FormattedText(
                                    context.getString(R.string.Deleted_message),
                                    emptyArray()
                                ),
                                null,
                                null
                            )
                        }
                    } else {
                        val chat = tgApi?.getChat(replyTo.chatId)
                        if (chat != null) {
                            val replyMessage = tgApi?.getMessageTypeById(replyTo.messageId, replyTo.chatId)
                            replyMessage?.let { content = it.content }
                        } else {
                            content = TdApi.MessageText(
                                TdApi.FormattedText(
                                    context.getString(R.string.empty_message),
                                    emptyArray()
                                ),
                                null,
                                null
                            )
                        }
                    }
                }
            }

            var parentHeight by remember { mutableIntStateOf(0) }

            Box (
                modifier = Modifier.clickable(
                    onClick = {
                        if (messagePosition != -1) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(messagePosition)
                            }
                        }
                    }
                )
            ) {
                if (isCurrentUser) {
                    Row(
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    if (messagePosition != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(
                                                messagePosition
                                            )
                                        }
                                    }
                                }
                            )
                            .padding(
                                start = 5.dp,
                                end = 5.dp,
                                top = 5.dp
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = alignment
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        if (messagePosition != -1) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(
                                                    messagePosition
                                                )
                                            }
                                        }
                                    }
                                )
                                .background(
                                    Color(0xFF3A4048),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            if (messagePosition != -1) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItemCentered(
                                                        messagePosition + 1
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    .weight(1f, fill = false)
                                    .fillMaxHeight()
                                    .onSizeChanged { size ->
                                        parentHeight =
                                            size.height // 获取父容器的高度
                                    },
                            ) {
                                // 回复正文部分
                                if (senderName != "") {
                                    Column(
                                        modifier = Modifier
                                            .clickable(
                                                onClick = {
                                                    if (messagePosition != -1) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(
                                                                messagePosition
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(
                                                bottom = 5.dp,
                                                start = 5.dp,
                                                end = 5.dp
                                            ),
                                        horizontalAlignment = Alignment.End // 文字右对齐
                                    ) {
                                        // 用户名
                                        AutoScrollingText(
                                            text = senderName,
                                            color = Color(0xFF66D3FE),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                        )

                                        // 回复消息内容
                                        messageDrawer(
                                            content = content,
                                            onLinkClick = onLinkClick,
                                            textColor = textColor,
                                            stateDownload = stateDownload,
                                            stateDownloadDone = stateDownloadDone,
                                            showUnknownMessageType = showUnknownMessageType
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .clickable(
                                                onClick = {
                                                    if (messagePosition != -1) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(
                                                                messagePosition
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(5.dp),
                                        horizontalAlignment = Alignment.End // 文字右对齐
                                    ) {
                                        // 消息内容
                                        messageDrawer(
                                            content = content,
                                            onLinkClick = onLinkClick,
                                            textColor = textColor,
                                            stateDownload = stateDownload,
                                            stateDownloadDone = stateDownloadDone,
                                            showUnknownMessageType = showUnknownMessageType
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            if (messagePosition != -1) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItemCentered(
                                                        messagePosition + 1
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    .background(Color(0xFF397DBC))
                                    .width(8.dp)
                                    .fillMaxHeight()
                            ) {
                                Spacer(Modifier.height((parentHeight/2).dp)) // 保持Spacer，虽然在这里作用不大
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    if (messagePosition != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(
                                                messagePosition
                                            )
                                        }
                                    }
                                }
                            )
                            .padding(
                                start = 5.dp,
                                end = 5.dp,
                                top = 5.dp
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = alignment
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        if (messagePosition != -1) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(
                                                    messagePosition
                                                )
                                            }
                                        }
                                    }
                                )
                                .background(
                                    Color(0xFF3A4048),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            if (messagePosition != -1) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItemCentered(
                                                        messagePosition + 1
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    .background(Color(0xFF397DBC))
                                    .width(8.dp) // 指定左边颜色宽度为 10.dp
                            ) {
                                Spacer(Modifier.height((parentHeight/2).dp))
                            }
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        onClick = {
                                            if (messagePosition != -1) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItemCentered(
                                                        messagePosition + 1
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    .fillMaxHeight()
                                    .onSizeChanged { size ->
                                        parentHeight =
                                            size.height // 获取父容器的高度
                                    }
                            ) {
                                // 回复正文部分
                                if (senderName != "") {
                                    Column(
                                        modifier = Modifier
                                            .clickable(
                                                onClick = {
                                                    if (messagePosition != -1) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(
                                                                messagePosition
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(
                                                bottom = 5.dp,
                                                start = 5.dp,
                                                end = 5.dp
                                            )
                                    ) {
                                        // 用户名
                                        AutoScrollingText(
                                            text = senderName,
                                            color = Color(0xFF66D3FE),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                        )

                                        // 回复消息内容
                                        messageDrawer(
                                            content = content,
                                            onLinkClick = onLinkClick,
                                            textColor = textColor,
                                            stateDownload = stateDownload,
                                            stateDownloadDone = stateDownloadDone,
                                            showUnknownMessageType = showUnknownMessageType
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clickable(
                                                onClick = {
                                                    if (messagePosition != -1) {
                                                        coroutineScope.launch {
                                                            listState.animateScrollToItem(
                                                                messagePosition
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(5.dp)
                                    ) {
                                        messageDrawer(
                                            content = content,
                                            onLinkClick = onLinkClick,
                                            textColor = textColor,
                                            stateDownload = stateDownload,
                                            stateDownloadDone = stateDownloadDone,
                                            showUnknownMessageType = showUnknownMessageType
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}