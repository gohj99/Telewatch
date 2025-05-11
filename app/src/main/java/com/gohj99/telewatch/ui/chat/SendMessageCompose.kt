/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.ui.AutoScrollingText
import com.gohj99.telewatch.ui.InputBar
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.main.MessageView
import com.gohj99.telewatch.utils.telegram.editMessageText
import com.gohj99.telewatch.utils.telegram.handleAllMessages
import com.gohj99.telewatch.utils.telegram.sendMessage
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun SendMessageCompose(
    chatId: Long,
    inputText: MutableState<String>,
    planReplyMessage: MutableState<TdApi.Message?>,
    planReplyMessageSenderName: String,
    currentUserId: MutableState<Long>,
    planEditMessage: MutableState<TdApi.Message?>,
    planEditMessageText: MutableState<String>,
    listState: LazyListState,
    pagerState: PagerState,
    showUnknownMessageType: Boolean,
    chatTopics: Map<Long, String>,
    onLinkClick: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var item by remember { mutableStateOf(false) }

    if (planEditMessage.value != null) {
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        planReplyMessage.value = null
                        tgApi!!.replyMessage.value = null
                    }
                )
        )
        Text(
            text = stringResource(R.string.Edit),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(
                    start = 10.dp,
                    end = 5.dp,
                    top = 5.dp
                )
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        planEditMessage.value = null
                    }
                )
        )
    } else if (planReplyMessage.value != null) {
        // 将回复消息显示
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        planReplyMessage.value = null
                        tgApi!!.replyMessage.value = null
                    }
                )
        )
        Text(
            text = stringResource(R.string.Reply),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(
                    start = 10.dp,
                    end = 5.dp,
                    top = 5.dp
                )
                .fillMaxWidth()
        )
        var parentHeight by remember { mutableIntStateOf(0) }
        var stateDownloadDone = rememberSaveable { mutableStateOf(false) }
        var stateDownload = rememberSaveable { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .padding(
                    start = 5.dp,
                    end = 5.dp,
                    top = 5.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Color(0xFF3A4048),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF397DBC))
                        .width(8.dp) // 指定左边颜色宽度为 10.dp
                ) {
                    Spacer(Modifier.height((parentHeight/2).dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .onSizeChanged { size ->
                            parentHeight =
                                size.height // 获取父容器的高度
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                bottom = 5.dp,
                                start = 5.dp,
                                end = 5.dp
                            )
                    ) {
                        if (planReplyMessageSenderName == "") {
                            messageDrawer(
                                content = planReplyMessage.value!!.content,
                                onLinkClick = onLinkClick,
                                textColor = Color(0xFFFEFEFE),
                                stateDownload = stateDownload,
                                stateDownloadDone = stateDownloadDone,
                                showUnknownMessageType = showUnknownMessageType
                            )
                        } else {
                            // 用户名
                            AutoScrollingText(
                                text = planReplyMessageSenderName,
                                color = Color(0xFF66D3FE),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                            )
                            messageDrawer(
                                content = planReplyMessage.value!!.content,
                                onLinkClick = onLinkClick,
                                textColor = Color(0xFFFEFEFE),
                                stateDownload = stateDownload,
                                stateDownloadDone = stateDownloadDone,
                                showUnknownMessageType = showUnknownMessageType
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (planEditMessage.value != null) {
        InputBar(
            query = planEditMessageText.value,
            onQueryChange = { planEditMessageText.value = it },
            placeholder = stringResource(id = R.string.Write_message),
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 完成编辑消息按钮
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .padding(end = 10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // 左右对齐
            ) {
                // 换行按钮
                IconButton(
                    onClick = {
                        planEditMessageText.value += "\n"
                    },
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(45.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.enter_icon),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )
                }

                // 完成编辑按钮
                IconButton(
                    onClick = {
                        if (planEditMessage.value != null) {
                            tgApi?.editMessageText(
                                chatId = chatId,
                                messageId = planEditMessage.value!!.id,
                                message = TdApi.InputMessageText().apply {
                                    text = TdApi.FormattedText().apply {
                                        this.text = planEditMessageText.value
                                    }
                                }
                            )
                        }
                        planEditMessage.value = null
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier
                        .size(45.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.done_icon),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )
                }
            }
        }
    } else {
        // 消息主题选择
        if (chatTopics.keys.size > 0) {

        }

        InputBar(
            query = inputText.value,
            onQueryChange = { inputText.value = it },
            placeholder = stringResource(id = R.string.Write_message),
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 换行和发送消息按钮
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .padding(end = 10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween // 左右对齐
            ) {
                // 换行按钮
                IconButton(
                    onClick = {
                        inputText.value += "\n"
                    },
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(45.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.enter_icon),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )
                }

                // 发送按钮
                IconButton(
                    onClick = {
                        if (planReplyMessage.value == null) {
                            tgApi?.sendMessage(
                                chatId = chatId,
                                message = TdApi.InputMessageText().apply {
                                    text = TdApi.FormattedText().apply {
                                        this.text = inputText.value
                                    }
                                }
                            )
                        } else {
                            if (planReplyMessage.value!!.chatId != chatId) {
                                tgApi?.sendMessage(
                                    chatId = chatId,
                                    message = TdApi.InputMessageText().apply {
                                        text = TdApi.FormattedText().apply {
                                            this.text = inputText.value
                                        }
                                    },
                                    replyTo = TdApi.InputMessageReplyToExternalMessage(
                                        planReplyMessage.value!!.chatId,
                                        planReplyMessage.value!!.id, null
                                    )
                                )
                            } else {
                                tgApi?.sendMessage(
                                    chatId = chatId,
                                    message = TdApi.InputMessageText().apply {
                                        text = TdApi.FormattedText().apply {
                                            this.text = inputText.value
                                        }
                                    },
                                    replyTo = TdApi.InputMessageReplyToMessage(
                                        planReplyMessage.value!!.id, null
                                    )
                                )
                            }
                            planReplyMessage.value = null
                            tgApi!!.replyMessage.value = null
                        }
                        inputText.value = ""
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.size(45.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_custom_send),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    val forwardMessage = tgApi!!.forwardMessage
    if (forwardMessage.value != null) {
        val messageText =
            tgApi!!.handleAllMessages(message = forwardMessage.value, maxText = 100)
        val targetTitle =
            if (forwardMessage.value!!.chatId == currentUserId.value) stringResource(R.string.Saved_Messages) else
                tgApi!!.chatsList.value
                    .find { it.id == forwardMessage.value!!.chatId }
                    ?.title ?: stringResource(R.string.Unknown_chat) // 找不到时返回默认值

        Text(
            text = stringResource(R.string.Forward),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.clickable(
                onClick = {
                    tgApi!!.forwardMessage.value = null
                }
            )
        )
        MainCard(
            column = {
                Text(
                    text = targetTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                MessageView(message = messageText)
            },
            item = forwardMessage.value
        )
        // 转发消息部分发送按钮
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .padding(end = 10.dp)
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    tgApi?.sendMessage(
                        chatId = chatId,
                        message = TdApi.InputMessageForwarded().apply {  // 参数名改为message
                            copyOptions = null
                            fromChatId = forwardMessage.value!!.chatId
                            inGameShare = false
                            messageId = forwardMessage.value!!.id
                        }
                    )
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .size(45.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_custom_send),
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(80.dp))
}
