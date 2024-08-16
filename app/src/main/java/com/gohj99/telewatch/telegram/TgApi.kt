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
import androidx.compose.runtime.mutableStateOf
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.main.add
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties
import java.util.concurrent.CountDownLatch

class TgApi(private val context: Context, private var chatsList: MutableState<List<Chat>>) {
    private var saveChatId = 1L
    private var saveChatList = mutableStateOf(emptyList<TdApi.Message>())
    private val client: Client = Client.create({ update -> handleUpdate(update) }, null, null)
    private val sharedPref = context.getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
    @Volatile private var isAuthorized: Boolean = false
    private val authLatch = CountDownLatch(1)
    private var isExitChatPage = true

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
            println("CheckDatabaseEncryptionKey result: $result")
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
            TdApi.UpdateDeleteMessages.CONSTRUCTOR -> handleDeleteMessages(update as TdApi.UpdateDeleteMessages)
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

    private fun handleDeleteMessages(update: TdApi.UpdateDeleteMessages) {
        val chatId = update.chatId
        val messageIds = update.messageIds
        println("Messages deleted in chat ID $chatId: $messageIds")

        if (chatId == saveChatId) {
            val mutableChatListSize = saveChatList.value.size
            val mutableChatList = saveChatList.value.toMutableList()
            for (messageId in messageIds) {
                val message = mutableChatList.find { it.id == messageId }
                if (message != null) {
                    // 更新保存的聊天列表
                    mutableChatList.remove(message)
                }
            }
            if (mutableChatListSize - mutableChatList.size <= 1) saveChatList.value =
                mutableChatList
        }

        // 更新聊天列表（别看了，我没写好的）
        /*CoroutineScope(Dispatchers.Main).launch {
            chatsList.value = chatsList.value.toMutableList().apply {
                // 从聊天列表中移除已删除的消息
                removeAll { chat -> chat.id == chatId && chat.message.id in messageIds }
            }

            // 更新保存的聊天列表
            if (chatId == saveChatId) {
                saveChatList.value = saveChatList.value.toMutableList().apply {
                    removeAll { it.id in messageIds }
                }
            }
        }*/
    }

    private fun handleNewMessage(update: TdApi.UpdateNewMessage) {
        val message = update.message
        println("New message received in chat ID ${message.chatId}")
        updateChatList(message)
    }

    private fun handleMessageContentUpdate(update: TdApi.UpdateMessageContent) {
        val chatId = update.chatId
        val messageId = update.messageId
        val newContent = update.newContent
        println("Message content updated in chat ID $chatId for message ID $messageId")

        /*CoroutineScope(Dispatchers.Main).launch {
            chatsList.value = chatsList.value.toMutableList().apply {
                val chatIndex = indexOfFirst { it.id == chatId }
                if (chatIndex >= 0) {
                    val updatedChat = get(chatIndex).copy(
                        message = if (newContent is TdApi.MessageText) {
                            val newMessageText =
                                if (newContent.text.text.length > 20) newContent.text.text.take(20) + "..." else newContent.text.text
                            newMessageText
                        } else context.getString(R.string.Unknown_Message)
                    )
                    removeAt(chatIndex)
                    add(0, updatedChat)
                }
            }
        }*/
    }

    private fun handleMessageEdited(update: TdApi.UpdateMessageEdited) {
        val chatId = update.chatId
        val messageId = update.messageId
        val editDate = update.editDate
        println("Message edited in chat ID $chatId for message ID $messageId at $editDate")

        if (chatId == saveChatId) {
            CoroutineScope(Dispatchers.IO).launch {
                // 异步获取消息的最新内容
                val getMessageRequest = TdApi.GetMessage(chatId, messageId)
                val result = sendRequest(getMessageRequest)
                if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                    val message = result as TdApi.Message

                    // 更新聊天列表中的消息
                    withContext(Dispatchers.Main) {
                        saveChatList.value = saveChatList.value.toMutableList().apply {
                            val messageIndex = indexOfFirst { it.id == messageId }
                            if (messageIndex >= 0) {
                                // 找到消息并替换内容
                                val updatedMessage = TdApi.Message().apply {
                                    this.chatId = message.chatId
                                    this.id = message.id
                                    this.date = message.date
                                    this.senderId = message.senderId
                                    this.content = message.content
                                    this.isOutgoing = message.isOutgoing
                                }
                                set(messageIndex, updatedMessage)
                            }
                        }
                    }
                } else {
                    println("Failed to get message content: $result")
                }
            }
        }

        // 更新聊天列表（别看了，我没写好的）
        /*CoroutineScope(Dispatchers.Main).launch {
            chatsList.value = chatsList.value.toMutableList().apply {
                val chatIndex = indexOfFirst { it.id == chatId }
                if (chatIndex >= 0) {
                    val updatedChat = get(chatIndex).copy(
                        message = if (saveChatList.value.any { it.id == messageId }) {
                            val editedMessage = saveChatList.value.first { it.id == messageId }
                            if (editedMessage.content is TdApi.MessageText) {
                                val newMessageText = if (editedMessage.content.text.text.length > 20) editedMessage.content.text.text.take(20) + "..." else editedMessage.content.text.text
                                newMessageText
                            } else context.getString(R.string.Unknown_Message)
                        } else get(chatIndex).message
                    )
                    removeAt(chatIndex)
                    add(0, updatedChat)
                }
            }
        }*/
    }

    private fun updateChatList(message: TdApi.Message) {
        val chatId = message.chatId
        val newMessageText = handleAllMessages(message)

        // 异步获取聊天标题
        CoroutineScope(Dispatchers.IO).launch {
            val chatResult = sendRequest(TdApi.GetChat(chatId))
            val chatTitle = when (chatResult.constructor) {
                TdApi.Chat.CONSTRUCTOR -> {
                    (chatResult as TdApi.Chat).title
                }

                else -> "Unknown Chat"
            }

            withContext(Dispatchers.Main) {
                if (chatId == saveChatId) {
                    // 将新消息添加到保存的聊天列表的前面
                    saveChatList.value = saveChatList.value.toMutableList().apply {
                        add(0, message) // 新消息存储在最前面
                    }
                }

                chatsList.value = chatsList.value.toMutableList().apply {
                    // 查找现有的聊天并更新
                    val existingChatIndex = indexOfFirst { it.id == chatId }
                    if (existingChatIndex >= 0) {
                        val updatedChat = get(existingChatIndex).copy(
                            message = newMessageText
                        )
                        removeAt(existingChatIndex)
                        add(0, updatedChat)
                    } else {
                        // 新增聊天到列表顶部
                        add(
                            0,
                            Chat(
                                id = chatId,
                                title = chatTitle, // 使用从TdApi获取的标题
                                message = newMessageText
                            )
                        )
                    }
                }
            }
        }
    }

    // 处理和简化消息
    private fun handleAllMessages(message: TdApi.Message): String {
        return when (val content = message.content) {
            is TdApi.MessageText -> if (content.text.text.length > 20) content.text.text.take(20) + "..." else content.text.text
            is TdApi.MessagePhoto -> context.getString(R.string.Photo)
            else -> context.getString(R.string.Unknown_Message)
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

     // 下载照片
     fun downloadThumbnailPhoto(file: TdApi.File, chatId: Long, completion: (Boolean) -> Unit) {
         //println("进入下载图片函数")
         if (file.local.isDownloadingCompleted) {
             //println("下载过直接返回")
             runBlocking {
                 reloadMessageById(chatId)
             }
             // 文件已经下载完成，直接返回
             completion(true)
         } else {
             //println("哦，貌似没下载过，那就开始下载吧")
             // 文件未下载，开始下载
             client.send(TdApi.DownloadFile(file.id, 1, 0, 0, true)) { response ->
                 when (response) {
                     is TdApi.Error -> {
                         // 下载失败
                         //println("下载失败")
                         completion(false)
                     }

                     is TdApi.File -> {
                         if (response.local.isDownloadingCompleted) {
                             // 下载完成
                             //println("下载完成，在" + response.local.path)
                             runBlocking {
                                 reloadMessageById(chatId)
                             }
                             completion(true)
                         } else {
                             // 下载未完成或失败
                             println("下载未完成")
                             completion(false)
                         }
                     }

                     else -> {
                         println("下载失败")
                         // 其他情况，下载失败
                         completion(false)
                     }
                 }
             }
         }
     }


    // 获取联系人
    fun getContacts(contacts: MutableState<List<Chat>>) {
        val request = TdApi.GetContacts()
        client.send(request) { result ->
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Error getting contacts: ${error.message}")
            } else if (result.constructor == TdApi.Users.CONSTRUCTOR) {
                val users = result as TdApi.Users
                val userIds = users.userIds

                // 异步获取每个用户的详细信息
                CoroutineScope(Dispatchers.IO).launch {
                    for (userId in userIds) {
                        val userResult = sendRequest(TdApi.GetUser(userId))
                        if (userResult.constructor == TdApi.User.CONSTRUCTOR) {
                            val user = userResult as TdApi.User
                            withContext(Dispatchers.Main) {
                                // 将用户添加到聊天列表
                                contacts.add(
                                    Chat(
                                        id = user.id,
                                        title = "${user.firstName} ${user.lastName}",
                                        message = ""
                                    )
                                )
                            }
                        } else if (userResult.constructor == TdApi.Error.CONSTRUCTOR) {
                            val error = userResult as TdApi.Error
                            println("Error getting user details for user ID $userId: ${error.message}")
                        }
                    }
                }
            } else {
                println("Unexpected result type: ${result.constructor}")
            }
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
        var allChatsLoaded = false
        while (!allChatsLoaded) {
            allChatsLoaded = loadChats(limit)
        }
    }

    // 加载聊天列表
    private suspend fun loadChats(limit: Int): Boolean = withContext(Dispatchers.IO) {
        val loadChats = TdApi.LoadChats(TdApi.ChatListMain(), limit)
        val result = sendRequest(loadChats)
        return@withContext when (result.constructor) {
            TdApi.Error.CONSTRUCTOR -> {
                val error = result as TdApi.Error
                println("Load Chats Error: ${error.message}")
                true // 发生错误，停止加载
            }
            TdApi.Chats.CONSTRUCTOR -> {
                val chats = result as TdApi.Chats
                fetchChatDetails(chats.chatIds.toList())
                chats.chatIds.isEmpty() // 如果获取的聊天ID为空，则表示所有聊天已加载完毕
            }
            else -> {
                println("Unexpected result: $result")
                true // 意外结果，停止加载
            }
        }
    }

    // 获取聊天详情
    private suspend fun fetchChatDetails(chatIds: List<Long>) = withContext(Dispatchers.IO) {
        for (chatId in chatIds) {
            val result = sendRequest(TdApi.GetChat(chatId))
            when (result.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    val error = result as TdApi.Error
                    println("Get Chat Details Error for chat ID $chatId: ${error.message}")
                }
                TdApi.Chat.CONSTRUCTOR -> {
                    val chat = result as TdApi.Chat
                    withContext(Dispatchers.Main) {
                        val lastMessage = chat.lastMessage
                        val message = if (lastMessage != null) {
                            handleAllMessages(lastMessage)
                        } else context.getString(R.string.Unknown_Message)
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

    // 根据消息id更新消息
    private suspend fun reloadMessageById(messageId: Long) {
        println("Reloading message")
        CoroutineScope(Dispatchers.IO).launch {
            // 创建一个请求来获取指定 ID 的消息
            val getMessageRequest = TdApi.GetMessage(saveChatId, messageId)
            val result = sendRequest(getMessageRequest)
            if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                val message = result as TdApi.Message

                // 使用重新加载的消息更新 saveChatList
                withContext(Dispatchers.Main) {
                    saveChatList.value = saveChatList.value.toMutableList().apply {
                        val index = indexOfFirst { it.id == messageId }
                        if (index >= 0) {
                            // 更新已存在的消息
                            set(index, message)
                        } else {
                            // 如果列表中没有此消息，则将其添加到列表的开头
                            add(0, message)
                        }
                    }
                }
            } else {
                println("Failed to reload message with ID $messageId: $result")
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

    // 发送请求并返回结果
    private suspend fun sendRequest(request: TdApi.Function): TdApi.Object =
        withContext(Dispatchers.IO) {
            val result = CompletableDeferred<TdApi.Object>()
            client.send(request) { result.complete(it) }
            return@withContext result.await()
    }

    // 退出聊天页面
    fun exitChatPage(){
        isExitChatPage = true
    }

    // 获取聊天记录
    fun getChatMessages(
        chatId: Long,
        chatList: MutableState<List<TdApi.Message>>
    ) {
        saveChatList = chatList
        saveChatId = chatId
        isExitChatPage = false

        // 定义一个内部函数用于异步递归获取消息
        fun fetchMessages(fromMessageId: Long) {
            val getChatMessages = TdApi.GetChatHistory().apply {
                this.chatId = chatId
                this.limit = 10 // 每次获取 10 条消息
                this.fromMessageId = fromMessageId
            }

            if (!isExitChatPage){
                client.send(getChatMessages) { result ->
                    //println("GetChatMessages result: $result")
                    if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                        val error = result as TdApi.Error
                        println("Get Chat Messages Error: ${error.message}")
                    } else {
                        val messages = result as TdApi.Messages
                        if (messages.messages.isNotEmpty()) {
                            val sortedMessages =
                                messages.messages.toList().sortedByDescending { it.date }
                            saveChatList.value = saveChatList.value.toMutableList().apply {
                                addAll(sortedMessages) // 将新消息添加到列表最后面
                            }
                            // 继续加载更旧的消息
                            fetchMessages(messages.messages.last().id)
                        }
                    }
                }
            }
        }

        // 从最新的消息开始获取
        fetchMessages(0)
    }

    // 关闭连接
    fun close() {
        println("Closing client")
        client.close()
    }
}
