/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.ThumbnailChatPhoto
import com.gohj99.telewatch.utils.formatTimestampToDateAndTime
import com.gohj99.telewatch.utils.getColorById

@Composable
fun ChatViewMainCard(
    chat: Chat,
    callback: (Chat) -> Unit,
    currentUserId: MutableState<Long> = mutableStateOf(-1)
) {
    MainCard(
        column = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (chat.id == currentUserId.value) {
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .clip(CircleShape)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.saved_messages_icon),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                } else {
                    if (chat.chatPhoto != null) {
                        ThumbnailChatPhoto(chat.chatPhoto, 35, if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title)
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(35.dp), // 固定宽高为35dp
                            color = getColorById(chat.accentColorId),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) { // 居中显示文本
                                Text(
                                    text = (if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title)[0].toString().uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Column {
                    Spacer(Modifier.height(1.5.dp))
                    Text(
                        text = if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis // 过长省略号
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 时间：使用 weight 占据剩余空间，允许换行显示
                        if (chat.lastMessageTime != -1) {
                            Text(
                                text = formatTimestampToDateAndTime(chat.lastMessageTime),
                                color = Color(0xFF728AA5),
                                fontSize = 10.5.sp,
                                lineHeight = 10.5.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                        }
                        // 未读指示器：始终完整显示，不受时间文本影响
                        if (chat.unreadCount > 0) {
                            Surface(
                                modifier = Modifier.wrapContentSize(),
                                color = if (chat.needNotification) Color(0xFF3F81BB) else Color(0xFF49617A),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = chat.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 4.6.dp, vertical = 1.3.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            if (chat.lastMessage.isNotEmpty()) {
                MessageView(message = chat.lastMessage)
            }

        },
        item = chat,
        callback = { callback(chat) },
        modifier = Modifier.padding(start = 10.dp, top = 6.dp, end = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun MessageView(message: androidx.compose.ui.text.AnnotatedString) {
    var currentMessage by remember { mutableStateOf(message) }

    // 如果消息更新了，才重新设置状态
    if (currentMessage != message) {
        currentMessage = message
    }

    if (currentMessage.isNotEmpty()) {
        Text(
            text = currentMessage,
            color = Color(0xFF728AA5),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // 过长省略号
        )
    }
}
