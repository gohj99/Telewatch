/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.chat.SplashChatScreen
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

class ChatActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var chat: Chat? = null
    private var chatList = mutableStateOf(emptyList<TdApi.Message>())
    private var currentUserId = mutableStateOf(-1L) // 使用 MutableState 来持有当前用户 ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tgApi = TgApiManager.tgApi

        // 接收传递的 Chat 对象
        chat = intent.getParcelableExtra("chat")

        // 如果 chat 为 null，直接退出页面
        if (chat == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        // 异步获取当前用户 ID
        lifecycleScope.launch {
            tgApi?.let {
                currentUserId.value = it.getCurrentUserId()
            }
        }

        // chat 不为 null 时，获取聊天消息
        tgApi?.let {
            chatList.value = it.getChatMessages(chat!!.id, 10) ?: emptyList()
        }

        setContent {
            TelewatchTheme {
                SplashChatScreen(
                    chatTitle = chat!!.title,
                    chatList = chatList,
                    currentUserId = currentUserId.value // 传递当前用户 ID
                )
            }
        }
    }
}
