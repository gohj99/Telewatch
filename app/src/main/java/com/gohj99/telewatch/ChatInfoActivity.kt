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
import com.gohj99.telewatch.ui.SplashChatInfoScreen
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.utils.telegram.TgApi
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi

class ChatInfoActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var chat: Chat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tgApi = TgApiManager.tgApi

        // 接收传递的 Chat 对象
        chat = intent.getParcelableExtra("chat")

        // 如果 chat 为 null，直接退出页面
        if (chat == null) {
            finish()
            return
        }

        chat?.let { safeChat ->
            var chatObject: TdApi.Chat?  // 在外部声明变量

            runBlocking {
                chatObject = tgApi!!.getChat(safeChat.id)  // 在 runBlocking 中赋值
            }

            //println("获取到的chatObject")
            //println(chatObject)
            // 这里可以使用 chatObject，因为它在 runBlocking 块外声明了
            chatObject?.let { itChatObject ->
                setContent {
                    TelewatchTheme {
                        SplashChatInfoScreen(itChatObject)
                    }
                }
            }
        }
    }
}
