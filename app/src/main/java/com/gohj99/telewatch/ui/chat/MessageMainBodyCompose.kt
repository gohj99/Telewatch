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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.utils.formatTimestampToTime
import org.drinkless.tdlib.TdApi

@Composable
fun MessageMainBodyCompose(
    message: TdApi.Message,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    alignment: Arrangement.Horizontal = Arrangement.End,
    textColor: Color = Color.White,
    backgroundColor: Color = Color(0xFF3A4048),
    stateDownload: MutableState<Boolean>,
    stateDownloadDone: MutableState<Boolean>,
    showUnknownMessageType: Boolean,
    selectMessage: MutableState<TdApi.Message>,
    isLongPressed: MutableState<Boolean>,
    lastReadOutboxMessageId: MutableState<Long>,
    lastReadInboxMessageId: MutableState<Long>,
    onLinkClick: (String) -> Unit,
    press: (TdApi.Message) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .background(
                    backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 1.dp
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            selectMessage.value = message
                            isLongPressed.value = true
                        },
                        onTap = {
                            if (!stateDownload.value) {
                                if (message.content is TdApi.MessageVideo) {
                                    val videoFile =
                                        (message.content as TdApi.MessageVideo).video.video
                                    if (!videoFile.local.isDownloadingCompleted) {
                                        tgApi!!.downloadFile(
                                            file = videoFile,
                                            schedule = { schedule ->
                                                println("下载进度: $schedule")
                                            },
                                            completion = { boolean, path ->
                                                println("下载完成情况: $boolean")
                                                println("下载路径: $path")
                                                stateDownload.value =
                                                    false
                                                stateDownloadDone.value =
                                                    true
                                            }
                                        )
                                        stateDownload.value = true
                                    }
                                }
                                press(message)
                            }
                        }
                    )
                }
        ) {
            Column {
                val content = message.content
                messageDrawer(
                    content = content,
                    onLinkClick = onLinkClick,
                    textColor = textColor,
                    stateDownload = stateDownload,
                    stateDownloadDone = stateDownloadDone,
                    showUnknownMessageType = showUnknownMessageType
                )

                Row(
                    modifier = modifier
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween // 两端对齐
                ) {
                    // 时间
                    Text(
                        text = if (message.editDate == 0) formatTimestampToTime(message.date)
                        else stringResource(id = R.string.edit) + " " + formatTimestampToTime(message.editDate),
                        modifier = modifier,
                        color = Color(0xFF6A86A3),
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 已读未读标识
                    // 确定消息是否为自己发的
                    if (message.isOutgoing) {
                        //println("read.message.id: ${chatObject.lastReadInboxMessageId}")
                        if (message.id <= lastReadOutboxMessageId.value) {
                            Image(
                                painter = painterResource(id = R.drawable.outgoing_read),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(19.4.dp, 12.dp) // 设置 Image 的大小
                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                    .padding(start = 3.8.dp)
                            )
                        } else if (message.id <= lastReadInboxMessageId.value) {
                            Image(
                                painter = painterResource(id = R.drawable.outgoing),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.2.dp, 11.dp) // 设置 Image 的大小
                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                    .padding(start = 3.5.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.sending),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.8.dp, 12.dp) // 设置 Image 的大小
                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                    .padding(start = 3.5.dp)
                            )
                        }
                    }

                    if (!isCurrentUser) {
                        val forwardInfo = message.forwardInfo
                        forwardInfo?.origin?.let { origin ->
                            if (origin is TdApi.MessageOriginChannel) {
                                // 署名
                                Text(
                                    text = origin.authorSignature,
                                    color = Color(0xFF6A86A3),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .weight(1f)
                                        .wrapContentWidth(Alignment.End) // 向右对齐
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}