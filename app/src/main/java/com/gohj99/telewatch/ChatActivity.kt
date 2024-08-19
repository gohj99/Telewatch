/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.chat.SplashChatScreen
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

class ChatActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var chat: Chat? = null
    private var chatList = mutableStateOf(emptyList<TdApi.Message>())

    @SuppressLint("AutoboxingStateCreation")
    private var currentUserId = mutableStateOf(-1L) // 使用 MutableState 来持有当前用户 ID

    override fun onDestroy() {
        super.onDestroy()
        TgApiManager.tgApi?.exitChatPage()
    }

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

        // 清空旧的聊天消息
        chatList.value = emptyList()

        // 异步获取当前用户 ID 和聊天记录
        lifecycleScope.launch {
            currentUserId.value = tgApi!!.getCurrentUserId()
            tgApi!!.getChatMessages(chat!!.id, chatList) // 异步加载全部聊天消息
        }

        setContent {
            TelewatchTheme {
                SplashChatScreen(
                    chatTitle = chat!!.title,
                    chatList = chatList,
                    currentUserId = currentUserId.value,
                    sendCallback = { messageText ->
                        tgApi?.sendMessage(
                            chatId = chat!!.id,
                            messageText = messageText
                        )
                    },
                    press = { message ->
                        println("点击触发")
                        println(message.id)
                        when (message.content) {
                            is TdApi.MessageText -> {
                                println("文本消息")
                            }

                            is TdApi.MessagePhoto -> {
                                println("图片消息")
                                val intent = Intent(this, ImgViewActivity::class.java)
                                intent.putExtra("messageId", message.id)
                                startActivity(intent)
                            }

                            is TdApi.MessageVideo -> {
                                println("视频消息")
                            }

                            is TdApi.MessageVoiceNote -> {
                                println("语音消息")
                            }

                            is TdApi.MessageAnimation -> {
                                println("动画消息")
                            }
                        }
                    },
                    longPress = { select, message ->
                        println("长按触发")
                        println(message)
                        when (select) {
                            "ReloadMessage" -> {
                                tgApi!!.reloadMessageById(message.id)
                                Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show()
                                return@SplashChatScreen "OK"
                            }
                            "GetMessage" -> {
                                return@SplashChatScreen tgApi!!.getMessageTypeById(message.id)?.let { messageType ->
                                    val gson = Gson()
                                    val messageJson = gson.toJson(messageType)
                                    formatJson(messageJson)
                                } ?: "error"
                                /*val gson = Gson()
                                val messageJson = gson.toJson(message)
                                return@SplashChatScreen formatJson(messageJson)*/
                            }
                            else -> return@SplashChatScreen "NotFind"
                        }
                    }
                )
            }
        }
    }
    fun formatJson(jsonString: String): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonElement = gson.fromJson(jsonString, Any::class.java)
        return gson.toJson(jsonElement)
    }
}
