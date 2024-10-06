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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.IOException
import java.util.Properties
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

class TgApi(
    private val context: Context,
    private var chatsList: MutableState<List<Chat>>,
    private val UserId: String = "",
    private val topTitle: MutableState<String>
) {
    private var saveChatId = 1L
    private var saveChatList = mutableStateOf(emptyList<TdApi.Message>())
    private val client: Client = Client.create({ update -> handleUpdate(update) }, null, null)
    private val sharedPref = context.getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
    @Volatile private var isAuthorized: Boolean = false
    private val authLatch = CountDownLatch(1)
    private var isExitChatPage = true
    private var lastReadOutboxMessageId = mutableStateOf(0L)
    private var lastReadInboxMessageId = mutableStateOf(0L)

    init {
        // 获取应用外部数据目录
        val externalDir: File = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("Failed to get external directory.")
        // 获取API ID和API Hash
        val config = loadConfig(context)
        val tdapiId = config.getProperty("api_id").toInt()
        val tdapiHash = config.getProperty("api_hash")
        val parameters = TdApi.TdlibParameters().apply {
            databaseDirectory = externalDir.absolutePath + (if (UserId == "") "/tdlib" else {
                "/$UserId/tdlib"
            })
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
            TdApi.UpdateNewChat.CONSTRUCTOR -> handleNewChat(update as TdApi.UpdateNewChat)
            TdApi.UpdateConnectionState.CONSTRUCTOR -> handleConnectionUpdate(update as TdApi.UpdateConnectionState)
            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> handleChatReadInboxUpdate(update as TdApi.UpdateChatReadInbox)
            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> handleChatReadOutboxUpdate(update as TdApi.UpdateChatReadOutbox)
            // 其他更新
            else -> {
                //println("Received update: $update")
            }
        }
    }

    private fun handleChatReadInboxUpdate(update: TdApi.UpdateChatReadInbox) {
        val chatId = update.chatId
        if (chatId == saveChatId) {
            lastReadInboxMessageId.value = update.lastReadInboxMessageId
        }
    }

    private fun handleChatReadOutboxUpdate(update: TdApi.UpdateChatReadOutbox) {
        val chatId = update.chatId
        if (chatId == saveChatId) {
            lastReadOutboxMessageId.value = update.lastReadOutboxMessageId
        }
    }

    // 网络状态更新
    private fun handleConnectionUpdate(update: TdApi.UpdateConnectionState) {
        when (update.state.constructor) {
            TdApi.ConnectionStateReady.CONSTRUCTOR -> {
                // 已经成功连接到 Telegram 服务器
                topTitle.value = context.getString(R.string.HOME)
                println("TgApi: Connection Ready")
            }

            TdApi.ConnectionStateConnecting.CONSTRUCTOR -> {
                // 正在尝试连接到 Telegram 服务器
                topTitle.value = context.getString(R.string.Connecting)
                println("TgApi: Connecting")
            }

            TdApi.ConnectionStateConnectingToProxy.CONSTRUCTOR -> {
                // 正在尝试通过代理连接到 Telegram 服务器
                topTitle.value = context.getString(R.string.Connecting)
                println("TgApi: Connecting To Proxy")
            }

            TdApi.ConnectionStateUpdating.CONSTRUCTOR -> {
                // 正在更新 Telegram 数据库
                topTitle.value = context.getString(R.string.Update)
                println("TgApi: Updating")
            }

            TdApi.ConnectionStateWaitingForNetwork.CONSTRUCTOR -> {
                // 正在等待网络连接
                topTitle.value = context.getString(R.string.Offline)
                println("TgApi: Waiting For Network")
            }

            else -> {
                // 其他网络状态处理
            }
        }
    }

    // 处理授权状态更新
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

    // 处理删除消息
    private fun handleDeleteMessages(update: TdApi.UpdateDeleteMessages) {
        val chatId = update.chatId
        val messageIds = update.messageIds
        println("Messages deleted in chat ID $chatId: $messageIds")

        CoroutineScope(Dispatchers.IO).launch {
            val messageType = getMessageTypeById(messageIds[0], chatId)
            //println(messageType)
            if (messageType == null) {
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
                    reloadMessageById(messageIds[0])
                }

                // 更新聊天列表
                val chatResult = sendRequest(TdApi.GetChat(chatId))
                if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                    withContext(Dispatchers.Main) {
                        chatsList.value = chatsList.value.toMutableList().apply {
                            // 查找现有的聊天并更新
                            val existingChatIndex = indexOfFirst { it.id == chatId }
                            if (existingChatIndex >= 0) {
                                val updatedChat = get(existingChatIndex).copy(
                                    message = handleAllMessages((chatResult as TdApi.Chat).lastMessage)
                                )
                                removeAt(existingChatIndex)
                                add(0, updatedChat)
                            }
                        }
                    }
                }
            }
        }
    }

    // 处理获取到的新消息
    private fun handleNewMessage(update: TdApi.UpdateNewMessage) {
        val message = update.message
        println("New message received in chat ID ${message.chatId}\nmessageId ${message.id}")
        updateChatList(message)
    }

    // 处理消息内容更新
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

    // 处理消息内容更新
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

    // 处理消息编辑
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

        // 更新聊天列表
        CoroutineScope(Dispatchers.IO).launch {
            val chatResult = sendRequest(TdApi.GetChat(chatId))
            if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                withContext(Dispatchers.Main) {
                    chatsList.value = chatsList.value.toMutableList().apply {
                        // 查找现有的聊天并更新
                        val existingChatIndex = indexOfFirst { it.id == chatId }
                        if (existingChatIndex >= 0) {
                            val updatedChat = get(existingChatIndex).copy(
                                message = handleAllMessages((chatResult as TdApi.Chat).lastMessage)
                            )
                            removeAt(existingChatIndex)
                            add(0, updatedChat)
                        }
                    }
                }
            }
        }
    }

    // 处理新聊天
    private fun handleNewChat(update: TdApi.UpdateNewChat){
        //println(update)
        val newChat = update.chat
        val chatId = newChat.id
        //println(newChat.lastMessage)
        //println(newMessageText)

        // 异步获取聊天标题
        CoroutineScope(Dispatchers.IO).launch {
            val chatResult = sendRequest(TdApi.GetChat(chatId))
            if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                val chatTitle = (chatResult as TdApi.Chat).title
                val lastMessage = handleAllMessages((chatResult.lastMessage))
                withContext(Dispatchers.Main) {
                    chatsList.value = chatsList.value.toMutableList().apply {
                        // 查找现有的聊天并更新
                        val existingChatIndex = indexOfFirst { it.id == chatId }
                        if (existingChatIndex >= 0) {
                            val updatedChat = get(existingChatIndex).copy(
                                title = chatTitle,
                                message = lastMessage
                            )
                            removeAt(existingChatIndex)
                            add(0, updatedChat)
                        } else {
                            // 新增聊天到列表顶部
                            add(
                                Chat(
                                    id = chatId,
                                    title = chatTitle, // 使用从TdApi获取的标题
                                    message = lastMessage
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // 处理和简化消息
    private fun handleAllMessages(message: TdApi.Message?): String {
        if (message == null) return context.getString(R.string.Unknown_Message)
        return when (val content = message.content) {
            is TdApi.MessageText -> if (content.text.text.length > 20) content.text.text.take(20) + "..." else content.text.text
            is TdApi.MessagePhoto -> context.getString(R.string.Photo)
            is TdApi.MessageVideo -> context.getString(R.string.Video)
            is TdApi.MessageVoiceNote -> context.getString(R.string.Voice)
            is TdApi.MessageAnimation -> context.getString(R.string.Animation)
            else -> context.getString(R.string.Unknown_Message)
        }
    }

    // 加载配置
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

    // 获取应用版本
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // 下载文件
    fun downloadFile(
        file: TdApi.File,
        schedule: (String) -> Unit,
        completion: (Boolean, String?) -> Unit
    ) {
        // 判断文件是否已经下载完成
        if (file.local.isDownloadingCompleted) {
            // 文件已经下载完成，直接返回
            completion(true, file.local.path)
        } else {
            // 开始下载文件
            client.send(TdApi.DownloadFile(file.id, 1, 0, 0, true)) { response ->
                when (response) {
                    is TdApi.Error -> {
                        // 下载失败，回调completion
                        println("文件下载失败: ${response.message}")
                        completion(false, null)
                    }

                    is TdApi.File -> {
                        // 检查下载进度
                        val downloadProgress =
                            if (response.local.downloadedSize > 0 && response.expectedSize > 0) {
                                (response.local.downloadedSize * 100 / response.expectedSize).toString() + "%"
                            } else {
                                "未知进度"
                            }

                        // 回调schedule以更新进度
                        schedule(downloadProgress)

                        // 检查是否下载完成
                        if (response.local.isDownloadingCompleted) {
                            // 下载完成，回调completion并传递文件路径
                            println("文件下载完成: ${response.local.path}")
                            completion(true, response.local.path)
                        } else {
                            // 下载未完成，继续回调schedule直到下载完成
                            println("下载进行中: $downloadProgress")
                        }
                    }

                    else -> {
                        println("下载出现未知错误")
                        completion(false, null)
                    }
                }
            }
        }
    }

    // 下载照片
    fun downloadPhoto(file: TdApi.File, completion: (Boolean, String?) -> Unit) {
        //println("进入下载图片函数")
        if (file.local.isDownloadingCompleted) {
            //println("下载过直接返回")
            /*runBlocking {
                reloadMessageById(messageId)
            }*/
            // 文件已经下载完成，直接返回
            completion(true, file.local.path)
        } else {
            //println("哦，貌似没下载过，那就开始下载吧")
            // 文件未下载，开始下载
            client.send(TdApi.DownloadFile(file.id, 1, 0, 0, true)) { response ->
                when (response) {
                    is TdApi.Error -> {
                        // 下载失败
                        println("下载失败")
                        completion(false, null)
                    }

                    is TdApi.File -> {
                        if (response.local.isDownloadingCompleted) {
                            // 下载完成
                            //println("下载完成，在" + response.local.path)
                            completion(true, response.local.path)
                        } else {
                            // 下载未完成或失败
                            println("下载未完成")
                            completion(false, null)
                        }
                    }

                    else -> {
                        println("下载失败")
                        // 其他情况，下载失败
                        completion(false, null)
                    }
                }
            }
        }
    }

    // 获取lastReadOutboxMessageId
    fun getLastReadOutboxMessageId(): MutableState<Long> {
        return lastReadOutboxMessageId
    }

    // 获取lastReadOutboxMessageId
    fun getLastReadInboxMessageId(): MutableState<Long> {
        return lastReadInboxMessageId
    }

    // 获取用户名
    fun getUser(userId: Long, onResult: (String) -> Unit) {
        val getUserRequest = TdApi.GetUser(userId)

        client.send(getUserRequest, { result ->
            if (result is TdApi.User) {
                val fullName = "${result.firstName} ${result.lastName}".trim()
                onResult(fullName)
            } else {
                onResult("Unknown User")
            }
        })
    }

    // 获取聊天对象
    suspend fun getChat(chatId: Long): TdApi.Chat? {
        return suspendCancellableCoroutine { continuation ->
            val getChatRequest = TdApi.GetChat(chatId)

            // 发送异步请求
            client.send(getChatRequest, Client.ResultHandler { result ->
                if (result is TdApi.Chat) {
                    // 当结果是 TdApi.Chat 时，恢复协程并返回 Chat 对象
                    continuation.resume(result)
                } else {
                    // 在其他情况下，恢复协程并返回 null
                    continuation.resume(null)
                }
            })
        }
    }

    // 退出登录
    fun logOut() {
        client.send(TdApi.LogOut(), object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                when (result.constructor) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        println("Logged out successfully")
                    }

                    else -> {
                        println("Failed to log out: $result")
                    }
                }
            }
        })
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
                                // 检查是否已存在相同 ID 的联系人
                                val existingContacts = contacts.value.toMutableList()
                                val existingContactIndex =
                                    existingContacts.indexOfFirst { it.id == user.id }
                                if (existingContactIndex != -1) {
                                    // 替换原有的联系人
                                    existingContacts[existingContactIndex] = Chat(
                                        id = user.id,
                                        title = "${user.firstName} ${user.lastName}",
                                        message = ""
                                    )
                                } else {
                                    // 添加新联系人
                                    existingContacts.add(
                                        Chat(
                                            id = user.id,
                                            title = "${user.firstName} ${user.lastName}",
                                            message = ""
                                        )
                                    )
                                }
                                // 更新状态
                                contacts.value = existingContacts
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

    // 标记已读
    fun markMessagesAsRead(messageId: Long, messageThreadId: Long = 0L, forceRead: Boolean = true) {
        // 创建 ViewMessages 请求
        val viewMessagesRequest = TdApi.ViewMessages(
            saveChatId,
            messageThreadId,
            longArrayOf(messageId),
            forceRead
        )

        // 发送 ViewMessages 请求
        client.send(viewMessagesRequest) { response ->
            if (response is TdApi.Ok) {
                println("Messages successfully marked as read in chat ID $saveChatId")
            } else {
                println("Failed to mark messages as read: $response")
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

    // 加载聊天列表
    suspend fun loadChats(limit: Int = 15){
        val loadChats = TdApi.LoadChats(TdApi.ChatListMain(), limit)
        val result = sendRequest(loadChats)
        //println("LoadChats result: $result")
    }

    // 根据消息id更新消息
    suspend fun reloadMessageById(messageId: Long) {
        println("Reloading message")
        CoroutineScope(Dispatchers.IO).launch {
            // 创建一个请求来获取指定 ID 的消息
            val getMessageRequest = TdApi.GetMessage(saveChatId, messageId)
            val result = sendRequest(getMessageRequest)
            if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                val message = result as TdApi.Message

                println(message)

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

    // 获取当前用户 ID 的方法
    suspend fun getCurrentUser(): List<String> {
        val result = sendRequest(TdApi.GetMe())
        if (result.constructor == TdApi.User.CONSTRUCTOR) {
            val user = result as TdApi.User
            return listOf(user.id.toString(), "${user.firstName} ${user.lastName}")
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

    // 根据消息id获取消息
    suspend fun getMessageTypeById(messageId: Long, chatId: Long = saveChatId): TdApi.Message? {

        val getMessageRequest = TdApi.GetMessage(chatId, messageId)
        val result = sendRequest(getMessageRequest)

        if (result.constructor == TdApi.Message.CONSTRUCTOR) {
            val message = result as TdApi.Message
            return message
        } else {
            println("Failed to get message with ID $messageId: $result")
            return null
        }
    }

    // 关闭连接
    fun close() {
        println("Closing client")
        client.close()
    }
}
