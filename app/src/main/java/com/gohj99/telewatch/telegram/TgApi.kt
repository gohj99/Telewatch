/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.telegram

import android.content.Context
import android.os.Build
import androidx.compose.runtime.MutableState
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.main.add
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties
import java.util.concurrent.CountDownLatch

class TgApi(private val context: Context, private var chatsList: MutableState<List<Chat>>) {
    private val client: Client = Client.create({ update -> handleUpdate(update) }, null, null)
    private val sharedPref = context.getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
    @Volatile private var isAuthorized: Boolean = false
    private val authLatch = CountDownLatch(1)

    init {
        // 获取API ID和API Hash
        val config = loadConfig(context)
        val tdapiId = config.getProperty("api_id").toInt()
        val tdapiHash = config.getProperty("api_hash")
        val parameters = TdApi.TdlibParameters().apply {
            databaseDirectory = context.filesDir.absolutePath + "/tdlib"
            useMessageDatabase = true
            useSecretChats = true
            apiId = tdapiId
            apiHash = tdapiHash
            systemLanguageCode = context.resources.configuration.locales[0].language
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = getAppVersion(context)
            enableStorageOptimizer = true
        }
        client.send(TdApi.SetTdlibParameters(parameters)) { result ->
            println("SetTdlibParameters result: $result")
        }

        // 检查本地是否有加密密钥
        val encryptionKeyString = sharedPref.getString("encryption_key", null)
        val encryptionKey: TdApi.CheckDatabaseEncryptionKey = if (encryptionKeyString != null) {
            val keyBytes = encryptionKeyString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            TdApi.CheckDatabaseEncryptionKey(keyBytes)
        } else {
            throw IllegalStateException("Encryption key not found")
        }
        client.send(encryptionKey) { result ->
            //println("CheckDatabaseEncryptionKey result: $result")
        }

        // 等待授权状态更新
        try {
            authLatch.await()
        } catch (e: InterruptedException) {
            close()
            throw IllegalStateException("Interrupted while waiting for authorization", e)
        }

        if (!isAuthorized) {
            close()
            throw IllegalStateException("Failed to authorize")
        }
    }

    // 处理 TDLib 更新的函数
    private fun handleUpdate(update: TdApi.Object) {
        when (update.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> handleAuthorizationState(update as TdApi.UpdateAuthorizationState)
            TdApi.UpdateNewMessage.CONSTRUCTOR -> handleNewMessage(update as TdApi.UpdateNewMessage)
            TdApi.UpdateMessageContent.CONSTRUCTOR -> handleMessageContentUpdate(update as TdApi.UpdateMessageContent)
            TdApi.UpdateMessageEdited.CONSTRUCTOR -> handleMessageEdited(update as TdApi.UpdateMessageEdited)
            // 其他更新
            else -> {
                //println("Received update: $update")
            }
        }
    }

    private fun handleAuthorizationState(update: TdApi.UpdateAuthorizationState) {
        val authorizationState = update.authorizationState
        when (authorizationState.constructor) {
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                println("TgApi: Authorization Ready")
                isAuthorized = true
                authLatch.countDown()
            }

            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                println("TgApi: Authorization Closed")
                isAuthorized = false
                authLatch.countDown()
            }

            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                println("TgApi: Waiting for Phone Number")
                isAuthorized = false
                authLatch.countDown()
            }

            else -> {
                // 其他授权状态处理
            }
        }
    }

    private fun handleNewMessage(update: TdApi.UpdateNewMessage) {
        val message = update.message
        println("New message received in chat ID ${message.chatId}")
        updateChatList(message)
    }

    private fun handleMessageContentUpdate(update: TdApi.UpdateMessageContent) {
        val chatId = update.chatId
        val messageId = update.messageId
        //val newContent = update.newContent
        println("Message content updated in chat ID $chatId for message ID $messageId")
        // 更新消息内容逻辑
    }

    private fun handleMessageEdited(update: TdApi.UpdateMessageEdited) {
        val chatId = update.chatId
        val messageId = update.messageId
        val editDate = update.editDate
        println("Message edited in chat ID $chatId for message ID $messageId at $editDate")
        // 更新消息编辑状态逻辑
    }

    private fun updateChatList(message: TdApi.Message) {
        val chatId = message.chatId
        val newMessageText = when (val content = message.content) {
            is TdApi.MessageText -> {
                val text = content.text.text
                if (text.length > 20) text.take(20) + "..." else text
            }

            else -> context.getString(R.string.Unknown_Message)
        }

        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(message = newMessageText)
                removeAt(existingChatIndex)
                add(0, updatedChat)
            } else {
                // 新增聊天到列表顶部
                add(0, Chat(id = chatId, title = "New Chat", message = newMessageText))
            }
        }
    }

    private fun loadConfig(context: Context): Properties {
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

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // 发送消息
    fun sendMessage(chatId: Long, messageText: String): TdApi.Message? {
        var sentMessage: TdApi.Message? = null
        val message = TdApi.SendMessage().apply {
            this.chatId = chatId
            inputMessageContent = TdApi.InputMessageText().apply {
                text = TdApi.FormattedText().apply {
                    this.text = messageText
                }
            }
        }
        client.send(message) { result ->
            println("SendMessage result: $result")
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Send Message Error: ${error.message}")
            } else {
                sentMessage = result as TdApi.Message
                println("Message sent successfully")
            }
        }
        return sentMessage
    }

    // 获取聊天列表
    suspend fun getChats(limit: Int = 10) {
        val chatIds = getChatIds(limit)
        fetchChatDetails(chatIds)
    }

    private suspend fun getChatIds(limit: Int): List<Long> = withContext(Dispatchers.IO) {
        val chatIds = mutableListOf<Long>()
        val chatList = TdApi.GetChats().apply {
            this.limit = limit
        }
        val result = sendRequest(chatList)
        if (result.constructor == TdApi.Error.CONSTRUCTOR) {
            val error = result as TdApi.Error
            println("Get Chats Error: ${error.message}")
        } else {
            val chats = result as TdApi.Chats
            chatIds.addAll(chats.chatIds.toList())
            println("Chats: ${chats.chatIds.joinToString(", ")}")
        }
        return@withContext chatIds
    }

    private suspend fun fetchChatDetails(chatIds: List<Long>) =
        withContext(Dispatchers.IO) {
        for (chatId in chatIds) {
            //println("Sending request for chat ID: $chatId")
            val result = sendRequest(TdApi.GetChat(chatId))
            //println("Received result for chat ID $chatId: $result")
            when (result.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    val error = result as TdApi.Error
                    println("Get Chat Details Error for chat ID $chatId: ${error.message}")
                }

                TdApi.Chat.CONSTRUCTOR -> {
                    val chat = result as TdApi.Chat
                    //println("Chat Details for chat ID $chatId: $chat")
                    withContext(Dispatchers.Main) {
                        val lastMessage = chat.lastMessage
                        val message = if (lastMessage != null) {
                            val messageContent = lastMessage.content
                            if (messageContent is TdApi.MessageText) {
                                val text = messageContent.text.text.toString()
                                if (text.length > 20) text.take(20) + "..." else text
                            } else context.getString(R.string.Unknown_Message)
                        } else context.getString(R.string.Unknown_Message)
                        println(chat.id)
                        println(chat.title)
                        chatsList.add(
                            Chat(
                                id = chat.id,
                                title = chat.title,
                                message = message
                            )
                        )
                    }
                }

                else -> {
                    println("Unexpected result for chat ID $chatId: $result")
                }
            }
        }
        }

    // 添加获取当前用户 ID 的方法
    suspend fun getCurrentUserId(): Long {
        val result = sendRequest(TdApi.GetMe())
        if (result.constructor == TdApi.User.CONSTRUCTOR) {
            val user = result as TdApi.User
            return user.id
        } else {
            throw IllegalStateException("Failed to get current user ID")
        }
    }

    private suspend fun sendRequest(request: TdApi.Function): TdApi.Object =
        withContext(Dispatchers.IO) {
            val result = CompletableDeferred<TdApi.Object>()
            client.send(request) { result.complete(it) }
            return@withContext result.await()
    }

    // 获取聊天记录
    fun getChatMessages(
        chatId: Long,
        limit: Int = 10,
        chatList: MutableState<List<TdApi.Message>>
    ) {
        val messagesList = mutableListOf<TdApi.Message>()
        val getChatMessages = TdApi.GetChatHistory().apply {
            this.chatId = chatId
            this.limit = limit
        }
        client.send(getChatMessages) { result ->
            println("GetChatMessages result: $result")
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Get Chat Messages Error: ${error.message}")
            } else {
                val messages = result as TdApi.Messages
                messagesList.addAll(messages.messages.toList())
                chatList.value = messagesList
                println("Messages: ${messages.messages.joinToString(", ")}")
            }
        }
    }

    // 关闭连接
    fun close() {
        println("Closing client")
        client.close()
    }
}
