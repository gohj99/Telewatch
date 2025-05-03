/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.AutoScrollingText
import org.drinkless.tdlib.TdApi

@Composable
fun UserNameCompose(
    message: TdApi.Message,
    chatId: Long,
    chatTitle: String,
    isCurrentUser: Boolean,
    selectMessage: MutableState<TdApi.Message>,
    isLongPressed: MutableState<Boolean>,
    senderNameMap: MutableState<MutableMap<Long, String?>>,
    goToChat: (Chat) -> Unit
) {
    if (!isCurrentUser) {
        var senderName by rememberSaveable { mutableStateOf("") }
        val senderId = message.senderId
        //println("senderId: $senderId")
        if (senderId.constructor == TdApi.MessageSenderUser.CONSTRUCTOR){
            val senderUser = senderId as TdApi.MessageSenderUser
            //println("senderUser: $senderUser")
            senderUser.userId.let {
                AutoScrollingText(
                    text = senderName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                if (senderUser.userId != chatId) {
                                    goToChat(
                                        Chat(
                                            id = senderUser.userId,
                                            title = senderName
                                        )
                                    )
                                }
                            }, onLongPress = {
                                selectMessage.value = message
                                isLongPressed.value = true
                            })
                        }
                )

                LaunchedEffect(message.senderId) {
                    if (it in senderNameMap.value) {
                        senderName = senderNameMap.value[it]!!
                    } else {
                        tgApi?.getUserName(it) { user ->
                            senderName = user
                            senderNameMap.value[it] = user
                        }
                    }
                }
            }
        } else if (senderId.constructor == TdApi.MessageSenderChat.CONSTRUCTOR) {
            val senderChat = senderId as TdApi.MessageSenderChat
            //println("senderChat: $senderChat")
            senderChat.chatId.let { itChatId ->
                AutoScrollingText(
                    text = senderName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 10.dp, end = 5.dp)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    selectMessage.value = message
                                    isLongPressed.value = true
                                }
                            )
                        }
                )

                LaunchedEffect(message.senderId) {
                    if (senderId.chatId == chatId) {
                        senderName = chatTitle
                    } else {
                        val itChat = tgApi?.getChat(itChatId)
                        itChat.let {
                            senderName = it!!.title
                        }
                    }
                }
            }
        }
    }
}