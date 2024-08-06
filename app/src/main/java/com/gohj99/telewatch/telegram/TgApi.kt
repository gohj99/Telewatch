package com.gohj99.telewatch.telegram

import android.content.Context
import android.os.Build
import androidx.compose.runtime.MutableState
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties
import java.util.concurrent.CountDownLatch

class TgApi(private val context: Context) {
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
            //println("SetTdlibParameters result: $result")
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
            throw IllegalStateException("Interrupted while waiting for authorization", e)
        }

        if (!isAuthorized) {
            throw IllegalStateException("Failed to authorize")
        }
    }

    // 处理 TDLib 更新的函数
    private fun handleUpdate(update: TdApi.Object) {
        when (update.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val authorizationState = (update as TdApi.UpdateAuthorizationState).authorizationState
                when (authorizationState.constructor) {
                    TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                        isAuthorized = true
                        authLatch.countDown()
                        println("TgApi: Authorization Ready")
                    }
                    TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                        isAuthorized = false
                        authLatch.countDown()
                    }
                    else -> {
                        // 其他状态不进行处理
                        //println("Authorization state: $authorizationState")
                    }
                }
            }
            // 处理其他更新...
            else -> {
                //println("Received update: $update")
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
    fun getChats(limit: Int = 10, chatsList: MutableState<MutableList<TdApi.Chat>>) {
        val chatIds = mutableListOf<Long>()
        val chatList = TdApi.GetChats().apply {
            this.limit = limit
        }
        client.send(chatList) { result ->
            println("GetChats result: $result")
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Get Chats Error: ${error.message}")
            } else {
                val chats = result as TdApi.Chats
                chatIds.addAll(chats.chatIds.toList())
                println("Chats: ${chats.chatIds.joinToString(", ")}")
                fetchChatDetails(chatIds, chatsList)
            }
        }
    }

    // 获取聊天详情
    private fun fetchChatDetails(chatIds: List<Long>, chatsList: MutableState<MutableList<TdApi.Chat>>) {
        for (chatId in chatIds) {
            println("Sending request for chat ID: $chatId")
            client.send(TdApi.GetChat(chatId)) { result ->
                println("Received result for chat ID $chatId: $result")
                when (result.constructor) {
                    TdApi.Error.CONSTRUCTOR -> {
                        val error = result as TdApi.Error
                        println("Get Chat Details Error for chat ID $chatId: ${error.message}")
                    }
                    TdApi.Chat.CONSTRUCTOR -> {
                        val chat = result as TdApi.Chat
                        println("Chat Details for chat ID $chatId: $chat")
                        chatsList.value.add(chat)
                    }
                    else -> {
                        println("Unexpected result for chat ID $chatId: $result")
                    }
                }
            }
        }
    }

    // 获取聊天记录
    fun getChatMessages(chatId: Long, limit: Int = 10): List<TdApi.Message> {
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
                println("Messages: ${messages.messages.joinToString(", ")}")
            }
        }
        return messagesList
    }

    // 关闭连接
    fun close() {
        println("Closing client")
        client.close()
    }
}