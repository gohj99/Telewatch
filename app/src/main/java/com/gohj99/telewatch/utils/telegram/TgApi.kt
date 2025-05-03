/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.content.edit
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.model.ChatMessagesSave
import com.gohj99.telewatch.utils.generateChatTitleIconBitmap
import com.gohj99.telewatch.utils.notification.drawableToBitmap
import com.gohj99.telewatch.utils.notification.sendChatMessageNotification
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.InputMessageContent
import java.io.File
import java.io.IOException
import java.util.Properties
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

class TgApi(
    private val context: Context,
    var chatsList: MutableState<List<Chat>>,
    private val userId: String = "",
    private val topTitle: MutableState<String>,
    private val chatsFoldersList: MutableState<List<TdApi.ChatFolder>>,
    private val onPaused: MutableState<Boolean>
) {
    var saveChatId = 0L
    var replyMessage = mutableStateOf<TdApi.Message?>(null)
    var updateFileCallBackList = mutableMapOf<Int, (TdApi.File) -> Unit>()
    private var saveChatMessagesList = mutableMapOf<Long, ChatMessagesSave>() //聊天在后台时更新
    private var saveChatList = mutableStateOf(emptyList<TdApi.Message>()) // 保存的聊天列表，前台更新
    private var saveChatIdList = mutableMapOf<Long, MutableList<Long>>()
    private val client: Client = Client.create({ update -> handleUpdate(update) }, null, null)
    private val sharedPref = context.getSharedPreferences("LoginPref", MODE_PRIVATE)
    private val settingsSharedPref = context.getSharedPreferences("app_settings", MODE_PRIVATE)
    @Volatile private var isAuthorized: Boolean = false
    private val authLatch = CountDownLatch(1)
    private var isExitChatPage = true
    private var lastReadOutboxMessageId = mutableStateOf(0L)
    private var lastReadInboxMessageId = mutableStateOf(0L)
    private var currentUser: List<String> = emptyList()
    var forwardMessage: MutableState<TdApi.Message?> = mutableStateOf(null)
    var chatReadList = mutableStateMapOf<Long, Int>()


    init {
        // 获取应用外部数据目录
        val externalDir: File = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("Failed to get external directory.")
        // 获取API ID和API Hash
        val config = loadConfig(context)
        val tdapiId = config.getProperty("api_id").toInt()
        val tdapiHash = config.getProperty("api_hash")
        val encryptionKeyString = sharedPref.getString("encryption_key", null)
        client.send(TdApi.SetTdlibParameters().apply {
            databaseDirectory = externalDir.absolutePath + (if (userId == "") "/tdlib" else {
                "/$userId/tdlib"
            })
            useMessageDatabase = true
            useSecretChats = true
            apiId = tdapiId
            apiHash = tdapiHash
            systemLanguageCode = context.resources.configuration.locales[0].language
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = getAppVersion(context)
            useSecretChats = false
            useMessageDatabase = true
            databaseEncryptionKey = encryptionKeyString?.chunked(2)?.map { it.toInt(16).toByte() }
                ?.toByteArray()
                ?: throw IllegalStateException("Encryption key not found")
        }) { result ->
            println("SetTdlibParameters result: $result")
            if (result is TdApi.Error) {
                throw IllegalStateException(result.message)
            }
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

        client.send(TdApi.GetMe()) {
            if (it is TdApi.User) {
                val user = it
                currentUser = listOf(user.id.toString(), "${user.firstName} ${user.lastName}")
            }
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
            TdApi.UpdateChatFolders.CONSTRUCTOR -> handleChatFoldersUpdate(update as TdApi.UpdateChatFolders)
            TdApi.UpdateChatTitle.CONSTRUCTOR -> handleChatTitleUpdate(update as TdApi.UpdateChatTitle)
            TdApi.UpdateUser.CONSTRUCTOR -> handleUpdateUser(update as TdApi.UpdateUser)
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> handleChatLastMessageUpdate(update as TdApi.UpdateChatLastMessage)
            TdApi.UpdateChatPosition.CONSTRUCTOR -> handleChatPositionUpdate(update as TdApi.UpdateChatPosition)
            TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> handleMessageSendSucceededUpdate(update as TdApi.UpdateMessageSendSucceeded)
            TdApi.UpdateFile.CONSTRUCTOR -> handleFileUpdate(update as TdApi.UpdateFile)
            TdApi.UpdateChatPhoto.CONSTRUCTOR -> handleChatPhotoUpdate(update as TdApi.UpdateChatPhoto)
            TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> handleChatUnreadMentionCountUpdate(update as TdApi.UpdateChatUnreadMentionCount)
            TdApi.UpdateUnreadChatCount.CONSTRUCTOR -> handleUnreadChatCountUpdate(update as TdApi.UpdateUnreadChatCount)
            TdApi.UpdateChatNotificationSettings.CONSTRUCTOR -> handleChatNotificationSettingsUpdate(update as TdApi.UpdateChatNotificationSettings)
            TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> handleChatDraftMessageUpdate(update as TdApi.UpdateChatDraftMessage)
            // 其他更新
            else -> {
                Log.d("TdApiUpdate","Received update: $update")
            }
        }
    }

    // 更新欲输入
    private fun handleChatDraftMessageUpdate(update: TdApi.UpdateChatDraftMessage) {
        val chatId = update.chatId
        val draftMessage = update.draftMessage
        println("Chat draft message updated in chat ID $chatId: $draftMessage")
        val inputMessageText = draftMessage?.inputMessageText
        if (inputMessageText is TdApi.InputMessageText) {
            val lastMessageText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.red)))) {
                    append(context.getString(R.string.Draft))
                }
                append(" ")
                val caption = inputMessageText.text.text.replace('\n', ' ')
                append(if (caption.length > 64) caption.take(20) + "..." else caption)
            }

            val date = draftMessage.date
            val order = update.positions.find { it.list is TdApi.ChatListMain }?.order

            // 更新聊天列表
            chatsList.value = chatsList.value.toMutableList().apply {
                // 查找现有的聊天并更新
                val existingChatIndex = indexOfFirst { it.id == chatId }
                if (existingChatIndex >= 0) {
                    var updatedChat = get(existingChatIndex)
                    if (order != null) {
                        updatedChat = get(existingChatIndex).copy(
                            lastMessageDraft = updatedChat.lastMessage,
                            lastMessageTimeDraft = updatedChat.lastMessageTime,
                            orderDraft = updatedChat.order,
                            lastMessage = lastMessageText,
                            lastMessageTime = date,
                            order = order
                        )
                    }
                    removeAt(existingChatIndex)
                    add(0, updatedChat)
                }
            }
        } else if (draftMessage == null) {
            chatsList.value = chatsList.value.toMutableList().apply {
                // 查找现有的聊天并更新
                val existingChatIndex = indexOfFirst { it.id == chatId }
                if (existingChatIndex >= 0) {
                    var updatedChat = get(existingChatIndex)
                    updatedChat = get(existingChatIndex).copy(
                        lastMessage = updatedChat.lastMessageDraft,
                        lastMessageTime = updatedChat.lastMessageTimeDraft,
                        order = updatedChat.orderDraft
                    )
                    removeAt(existingChatIndex)
                    add(0, updatedChat)
                }
            }
        }
    }

    // 更新通知
    private fun handleChatNotificationSettingsUpdate(update: TdApi.UpdateChatNotificationSettings) {
        val chatId = update.chatId
        val notificationSettings = update.notificationSettings
        val needNotification = notificationSettings.muteFor == 0

        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    needNotification = needNotification
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
    }

    // 更新未读聊天数量
    private fun handleUnreadChatCountUpdate(update: TdApi.UpdateUnreadChatCount) {
        val unreadCount = update.unreadCount
        //println("Unread count updated in unreadCount $unreadCount: $unreadCount")

        // 更新聊天列表
        /*
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    unreadCount = unreadCount
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
         */
    }

    // 更新聊天未读提及数量
    private fun handleChatUnreadMentionCountUpdate(update: TdApi.UpdateChatUnreadMentionCount) {
        val chatId = update.chatId
        val unreadMentionCount = update.unreadMentionCount
        println("Unread mention count updated in chat ID $chatId: $unreadMentionCount")

        // 更新聊天列表
        /*
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    unreadCount = unreadMentionCount
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
         */
    }

    // 聊天图片更新
    private fun handleChatPhotoUpdate(update: TdApi.UpdateChatPhoto) {
        val chatId = update.chatId
        val photoFile = update.photo?.small
        println("Chat photo updated in chat ID $chatId")

        // 更新聊天列表
        if (photoFile != null) {
            chatsList.value = chatsList.value.toMutableList().apply {
                // 查找现有的聊天并更新
                val existingChatIndex = indexOfFirst { it.id == chatId }
                if (existingChatIndex >= 0) {
                    val updatedChat = get(existingChatIndex).copy(
                        chatPhoto = photoFile
                    )
                    removeAt(existingChatIndex)
                    add(0, updatedChat)
                }
            }
        }
    }

    // 文件更新
    private fun handleFileUpdate(update: TdApi.UpdateFile) {
        val file = update.file
        //println("${file.id} file\n isDownloadingCompleted: ${file.local.isDownloadingCompleted}\n downloadedSize: ${file.local.downloadedSize}")

        updateFileCallBackList[file.id]?.invoke(file)
    }

    // 消息发送成功
    private fun handleMessageSendSucceededUpdate(update: TdApi.UpdateMessageSendSucceeded) {
        val chatId = update.message.chatId
        val oldMessageId = update.oldMessageId
        val newMessageId = update.message.id
        if (chatId == saveChatId) {
            saveChatList.value = saveChatList.value.toMutableList().apply {
                val messageIndex = indexOfFirst { it.id == oldMessageId }
                if (messageIndex >= 0) {
                    set(messageIndex, update.message)
                }
            }
            saveChatIdList[chatId]?.remove(oldMessageId)
            saveChatIdList[chatId]?.add(newMessageId)
        } else if (chatId in saveChatMessagesList) {
            saveChatMessagesList[chatId]?.messages?.replaceAll {
                if (it.id == oldMessageId) {
                    update.message
                } else {
                    it
                }
            }
            saveChatIdList[chatId]?.remove(oldMessageId)
            saveChatIdList[chatId]?.add(newMessageId)
        }
    }

    // 未读消息更新
    private fun handleChatReadInboxUpdate(update: TdApi.UpdateChatReadInbox) {
        val chatId = update.chatId
        val unreadCount = update.unreadCount
        if (chatId == saveChatId) {
            lastReadInboxMessageId.value = update.lastReadInboxMessageId
        } else if (chatId in saveChatMessagesList) {
            saveChatMessagesList[chatId]?.lastReadInboxMessageId = update.lastReadInboxMessageId
        }
        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    unreadCount = unreadCount
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }

        // 更新未读数量列表
        chatReadList[chatId] = unreadCount
    }

    private fun handleChatReadOutboxUpdate(update: TdApi.UpdateChatReadOutbox) {
        val chatId = update.chatId
        if (chatId == saveChatId) {
            lastReadOutboxMessageId.value = update.lastReadOutboxMessageId
        } else if (chatId in saveChatMessagesList) {
            saveChatMessagesList[chatId]?.lastReadOutboxMessageId = update.lastReadOutboxMessageId
        }
    }

    // 更新用户信息
    private fun handleUpdateUser(update: TdApi.UpdateUser) {
        val user = update.user
        val chatId = update.user.id

        var isBot = false
        var chatTitle = user.firstName + " " + user.lastName
        when (user.type) {
            is TdApi.UserTypeDeleted -> chatTitle = context.getString(R.string.Deleted_User)
            is TdApi.UserTypeUnknown -> chatTitle = context.getString(R.string.Unknown_chat)
            is TdApi.UserTypeBot -> isBot = true
        }

        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    title = chatTitle,
                    isBot = isBot
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
    }

    // 更新聊天标题
    private fun handleChatTitleUpdate(update: TdApi.UpdateChatTitle) {
        val chatId = update.chatId
        val title = update.title
        println("Chat title updated in chat ID $chatId: $title")

        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    title = title
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
    }

    // 获取聊天文件夹
    private fun handleChatFoldersUpdate(update: TdApi.UpdateChatFolders) {
        chatsFoldersList.value = emptyList()
        update.chatFolders?.let { chatFolders ->
            CoroutineScope(Dispatchers.IO).launch {
                // 将每个异步任务放入列表
                val foldersInfo = chatFolders.map { chatFolder ->
                    async { getChatFolderInfo(chatFolder.id) }
                }.awaitAll() // 等待所有异步任务完成

                // 过滤非空结果，并按顺序更新 chatsFoldersList
                chatsFoldersList.value = foldersInfo.filterNotNull()
            }
        }
    }

    // 网络状态更新
    private fun handleConnectionUpdate(update: TdApi.UpdateConnectionState) {
        when (update.state.constructor) {
            TdApi.ConnectionStateReady.CONSTRUCTOR -> {
                // 已经成功连接到 Telegram 服务器
                topTitle.value = ""
                println("TgApi: Connection Ready")
                // 更新通知
                if (settingsSharedPref.getBoolean("Use_Notification", false)) {
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                //println(token)
                                settingsSharedPref.edit(commit = false) {
                                    putString("Token_Notification", token)
                                }
                                TgApiManager.tgApi?.setFCMToken(token) { id ->
                                    settingsSharedPref.edit(commit = false) {
                                        putLong("Id_Notification", id)
                                    }
                                    //println(id)
                                }
                            }
                        }
                }
            }

            TdApi.ConnectionStateConnecting.CONSTRUCTOR -> {
                // 正在尝试连接到 Telegram 服务器
                topTitle.value = context.getString(R.string.Connecting)
                println("TgApi: Connecting")
            }

            TdApi.ConnectionStateConnectingToProxy.CONSTRUCTOR -> {
                // 正在尝试通过代理连接到 Telegram 服务器
                topTitle.value = context.getString(R.string.Connecting_Proxy)
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
        //println("Messages deleted in chat ID $chatId: $messageIds")

        if (chatId == saveChatId) {
            val mutableChatList = saveChatList.value.toMutableList()
            //println(mutableChatList)
            messageIds.forEach { messageId ->
                val message = mutableChatList.find { it.id == messageId }
                if (message != null) {
                    // 更新保存的聊天列表
                    mutableChatList.remove(message)
                    saveChatIdList[chatId]?.remove(message.id)
                }
            }
            saveChatList.value = mutableChatList
            //reloadMessageById(messageIds[0])
        } else if (chatId in saveChatMessagesList) {
            messageIds.forEach { messageId ->
                val message = saveChatMessagesList[chatId]?.messages?.find { it.id == messageId }
                if (message != null) {
                    // 更新保存的聊天列表
                    saveChatMessagesList[chatId]?.messages?.remove(message)
                    saveChatIdList[chatId]?.remove(message.id)
                }
            }
        }
    }

    // 处理获取到的新消息
    private fun handleNewMessage(update: TdApi.UpdateNewMessage) {
        val message = update.message
        //println("New message received in chat ID ${message.chatId}\nmessageId ${message.id}")
        val chatId = message.chatId

        if (chatId !in saveChatIdList || message.id !in (saveChatIdList[chatId] ?: emptyList())) {
            if (chatId == saveChatId) {
                // 将新消息添加到保存的聊天列表的前面
                saveChatList.value = saveChatList.value.toMutableList().apply {
                    add(0, message) // 新消息存储在最前面
                }
                saveChatIdList[chatId]?.add(message.id)
            } else if (chatId in saveChatMessagesList) {
                // 将新消息添加到保存的聊天列表的前面
                saveChatMessagesList[chatId]?.messages?.add(message)
                saveChatIdList[chatId]?.add(message.id)
            }
        }

        // 消息通知
        if (onPaused.value) {
            fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            }
            // 如果是已经打开的聊天，则不处理
            CoroutineScope(Dispatchers.IO).launch {
                if (chatId != saveChatId) {
                    // 如果是自己发的内容，则不处理
                    if (message.senderId is TdApi.MessageSenderUser) {
                        if ((message.senderId as TdApi.MessageSenderUser).userId != userId.toLong()) {
                            // 判断是否启用通知
                            if (settingsSharedPref.getBoolean("Use_Notification", false)) {
                                val chat = chatsList.value.find { it.id == chatId }
                                if (chat != null && chat.needNotification) {
                                    val chatTitle = chat.title
                                    val accentColorId = chat.accentColorId
                                    val isGroup = chat.isGroup
                                    // 获取聊天图片
                                    var bmp = drawableToBitmap(context, R.mipmap.ic_launcher)!!
                                    val photoFile = chat.chatPhoto
                                    if (photoFile?.local?.isDownloadingCompleted == true) {
                                        val filePath = photoFile.local.path
                                        val file = File(filePath)
                                        if (file.exists()) {
                                            // 这里可以处理图片文件，例如显示或使用
                                            loadBitmapFromUri(context.contentResolver, Uri.fromFile(file))?.let {
                                                bmp = it
                                            }
                                        }
                                    } else {
                                        // 使用默认图标
                                        bmp = generateChatTitleIconBitmap(
                                            context,
                                            chatTitle,
                                            accentColorId
                                        )
                                    }
                                    // 获取发送者名称
                                    var senderName = chatTitle
                                    if (isGroup) {
                                        when (val senderId = message.senderId) {
                                            is TdApi.MessageSenderUser -> {
                                                val userId = senderId.userId
                                                val userResult = sendRequest(TdApi.GetUser(userId))
                                                if (userResult is TdApi.User) {
                                                    senderName = "${userResult.firstName} ${userResult.lastName}"
                                                }
                                            }
                                            is TdApi.MessageSenderChat -> {
                                                // 处理群组消息的发送者
                                                if (senderId.chatId == chatId) {
                                                    senderName = chatTitle
                                                } else {
                                                    val itChat = tgApi?.getChat(senderId.chatId)
                                                    itChat.let {
                                                        senderName = it!!.title
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    context.sendChatMessageNotification(
                                        title = chatTitle,
                                        message = handleAllMessages(messageContext = message.content).toString(),
                                        senderName = senderName,
                                        conversationId = chatId.toString(),
                                        timestamp = message.date * 1000L,
                                        isGroupChat = isGroup,
                                        chatIconBitmap = bmp // 这里可以传入群组图标的 Uri
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 处理消息内容更新
    private fun handleMessageContentUpdate(update: TdApi.UpdateMessageContent) {
        val chatId = update.chatId
        val message = update.newContent
        val messageId = update.messageId

        // 更新聊天列表
        /*
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                val updatedChat = get(existingChatIndex).copy(
                    lastMessage = handleAllMessages(messageContext = message)
                )
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }

         */

        if (chatId == saveChatId) {
            saveChatList.value = saveChatList.value.toMutableList().apply {
                val messageIndex = indexOfFirst { it.id == messageId }
                if (messageIndex >= 0) {
                    // 合并旧消息的元数据和新内容
                    val updatedMessage = get(messageIndex).apply {
                        content = message
                    }
                    set(messageIndex, updatedMessage)
                }
            }
        } else if (chatId in saveChatMessagesList) {
            saveChatMessagesList[chatId]?.messages?.replaceAll {
                if (it.id == messageId) {
                    it.apply {
                        content = message
                    }
                } else {
                    it
                }
            }
        }

        //val newContent = update.newContent
        //println("Message content updated in chat ID $chatId for message ID $messageId")

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

    // 更新最后一条消息内容
    private fun handleChatLastMessageUpdate(update: TdApi.UpdateChatLastMessage) {
        val chatId = update.chatId
        val lastMessage = update.lastMessage
        val lastMessageText = handleAllMessages(lastMessage)
        val position = update.positions.find { it.list is TdApi.ChatListMain }

        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            val order = position?.order
            if (existingChatIndex >= 0) {
                val updatedChat =
                    if (order != null) {
                        get(existingChatIndex).copy(
                            order = order,
                            isPinned = position.isPinned,
                            lastMessage = lastMessageText,
                            lastMessageTime = lastMessage?.date ?: -1
                        )
                    } else {
                        get(existingChatIndex).copy(
                            lastMessage = lastMessageText,
                            lastMessageTime = lastMessage?.date ?: -1
                        )
                    }
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }

    }

    // 处理消息编辑
    private fun handleMessageEdited(update: TdApi.UpdateMessageEdited) {
        val chatId = update.chatId
        val messageId = update.messageId
        val editDate = update.editDate
        println("Message edited in chat ID $chatId for message ID $messageId at $editDate")

        if (chatId == saveChatId) {
            saveChatList.value = saveChatList.value.toMutableList().apply {
                val messageIndex = indexOfFirst { it.id == messageId }
                if (messageIndex >= 0) {
                    // 合并旧消息的元数据和新内容
                    val updatedMessage = get(messageIndex).apply {
                        date = editDate
                    }
                    set(messageIndex, updatedMessage)
                }
            }
        } else if (chatId in saveChatMessagesList) {
            saveChatMessagesList[chatId]?.messages?.replaceAll {
                if (it.id == messageId) {
                    it.apply {
                        date = editDate
                    }
                } else {
                    it
                }
            }
        }
    }

    // 消息排序
    private fun handleChatPositionUpdate(update: TdApi.UpdateChatPosition) {
        val chatId = update.chatId
        val position = update.position
        //println("Chat position updated in chat ID $chatId: $position")

        // 更新聊天列表
        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                var updatedChat = get(existingChatIndex).copy()
                if (position.list is TdApi.ChatListMain) {
                    if (position.order == 0L) {
                        updatedChat = get(existingChatIndex).copy(
                            isArchiveChatPin = position.isPinned
                        )
                    } else {
                        updatedChat = get(existingChatIndex).copy(
                            order = position.order,
                            isPinned = position.isPinned,
                            isArchiveChatPin = null
                        )
                    }
                } else if (position.list is TdApi.ChatListArchive) {
                    if (position.order == 0L) {
                        updatedChat = get(existingChatIndex).copy(
                            isArchiveChatPin = null
                        )
                    } else {
                        updatedChat = get(existingChatIndex).copy(
                            isArchiveChatPin = position.isPinned
                        )
                    }
                }
                removeAt(existingChatIndex)
                add(0, updatedChat)
            }
        }
    }

    // 处理新聊天
    private fun handleNewChat(update: TdApi.UpdateNewChat){
        //println("New chat received: ${update.chat}")
        val newChat = update.chat
        val chatId = newChat.id

        //println(newChat.lastMessage)
        //println(newChat)
        //println(newChat.positions.firstOrNull()?.isPinned ?: false)

        var isPinned = newChat.positions.find { it.list is TdApi.ChatListMain }?.isPinned ?: false
        var isRead = newChat.isMarkedAsUnread
        var isBot = false
        var isChannel = false
        var isGroup = false
        var isPrivateChat = false
        var chatTitle = newChat.title
        val photoFile = newChat.photo?.small
        val accentColorId = newChat.accentColorId
        val unreadCount = newChat.unreadCount
        val needNotification = newChat.notificationSettings.muteFor == 0

        isRead = newChat.isMarkedAsUnread
        when (val messageType = newChat.type) {
            is TdApi.ChatTypeSupergroup -> {
                if (messageType.isChannel) {
                    isChannel = true
                } else {
                    isGroup = true
                }
            }
            is TdApi.ChatTypeBasicGroup -> {
                isGroup = true
            }
            is TdApi.ChatTypePrivate -> {
                isPrivateChat = true
            }
        }

        chatsList.value = chatsList.value.toMutableList().apply {
            // 查找现有的聊天并更新
            val existingChatIndex = indexOfFirst { it.id == chatId }
            if (existingChatIndex >= 0) {
                // 如果存在该聊天，更新并移动到顶部
                val updatedChat = get(existingChatIndex).copy(
                    id = chatId,
                    title = chatTitle,
                    accentColorId = accentColorId,
                    unreadCount = unreadCount,
                    chatPhoto = photoFile,
                    needNotification = needNotification,
                    isPinned = isPinned,
                    isRead = isRead,
                    isBot = isBot,
                    isChannel = isChannel,
                    isGroup = isGroup,
                    isPrivateChat = isPrivateChat
                )
                removeAt(existingChatIndex)  // 移除旧的聊天
                add(0, updatedChat)  // 将更新后的聊天添加到顶部
            } else {
                // 如果不存在该聊天，添加到列表末尾
                add(
                    Chat(
                        id = chatId,
                        title = chatTitle,
                        accentColorId = accentColorId,
                        unreadCount = unreadCount,
                        chatPhoto = photoFile,
                        needNotification = needNotification,
                        isPinned = isPinned,
                        isRead = isRead,
                        isBot = isBot,
                        isChannel = isChannel,
                        isGroup = isGroup,
                        isPrivateChat = isPrivateChat
                    )
                )

            }
        }

        // 更新未读数量列表
        chatReadList[chatId] = unreadCount

        if (isPrivateChat) {
            // 判断是不是机器人
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val user = sendRequest(TdApi.GetUser(chatId))
                    user?.let {
                        when (user.type) {
                            is TdApi.UserTypeDeleted -> chatTitle = context.getString(R.string.Deleted_User)
                            is TdApi.UserTypeUnknown -> chatTitle = context.getString(R.string.Unknown_chat)
                            is TdApi.UserTypeBot -> isBot = true
                        }
                        // 更新聊天列表
                        withContext(Dispatchers.Main) {
                            chatsList.value = chatsList.value.toMutableList().apply {
                                // 查找现有的聊天并更新
                                val existingChatIndex = indexOfFirst { it.id == chatId }
                                if (existingChatIndex >= 0) {
                                    val updatedChat = get(existingChatIndex).copy(
                                        title = chatTitle,
                                        isBot = isBot
                                    )
                                    removeAt(existingChatIndex)
                                    add(0, updatedChat)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("GetUser failed: ${e.message}")
                }
            }
        }
    }

    // 强制加载消息
    private fun addNewChat(chatId: Long){
        // 异步获取聊天标题
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatResult = sendRequest(TdApi.GetChat(chatId))
                if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {

                    var isBot = false
                    var isChannel = false
                    var isGroup = false
                    var isPrivateChat = false
                    var havePositions = true
                    val accentColorId = chatResult.accentColorId
                    val needNotification = chatResult.notificationSettings.muteFor == 0

                    if (chatResult.positions.isEmpty()) havePositions = false
                    val isPinned = chatResult.positions.find { it.list is TdApi.ChatListMain }?.isPinned ?: false
                    val chatTitle = chatResult.title
                    val lastMessageTime = chatResult.lastMessage?.date ?: -1
                    val lastMessage = handleAllMessages(chatResult.lastMessage)
                    val isRead = chatResult.isMarkedAsUnread
                    when (val messageType = chatResult.type) {
                        is TdApi.ChatTypeSupergroup -> {
                            if (messageType.isChannel) {
                                isChannel = true
                            } else {
                                isGroup = true
                            }
                        }
                        is TdApi.ChatTypeBasicGroup -> {
                            isGroup = true
                        }
                        is TdApi.ChatTypePrivate -> {
                            isPrivateChat = true
                            val userResult = sendRequest(TdApi.GetUser(chatResult.id))
                            if (userResult.type is TdApi.UserTypeBot) {
                                isBot = true
                            }
                        }
                    }

                    // 加入聊天列表
                    withContext(Dispatchers.Main) {
                        chatsList.value = chatsList.value.toMutableList().apply {
                            // 查找现有的聊天并更新
                            val existingChatIndex = indexOfFirst { it.id == chatId }
                            if (existingChatIndex >= 0) {
                                // 如果存在该聊天，更新并移动到顶部
                                val updatedChat = get(existingChatIndex).copy(
                                    title = chatTitle,
                                    lastMessage = lastMessage,
                                    needNotification = needNotification,
                                    lastMessageTime = lastMessageTime
                                )
                                removeAt(existingChatIndex)  // 移除旧的聊天
                                add(0, updatedChat)  // 将更新后的聊天添加到顶部
                            } else {
                                // 如果不存在该聊天，添加到列表末尾
                                if (havePositions) {
                                    add(
                                        Chat(
                                            id = chatId,
                                            title = chatTitle, // 使用从 TdApi 获取的标题
                                            accentColorId = accentColorId,
                                            lastMessage = lastMessage,
                                            needNotification = needNotification,
                                            isPinned = isPinned,
                                            isRead = isRead,
                                            isBot = isBot,
                                            isChannel = isChannel,
                                            isGroup = isGroup,
                                            isPrivateChat = isPrivateChat,
                                            lastMessageTime = lastMessageTime
                                        )
                                    )
                                }

                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("GetChat request failed (handleNewChat): ${e.message}")
            }
        }
    }

    // 处理和简化消息
    fun handleAllMessages(
        message: TdApi.Message? = null,
        messageContext: TdApi.MessageContent? = null,
        maxText: Int = 64
    ): AnnotatedString {
        val content: TdApi.MessageContent = messageContext ?: message?.content
        ?: return buildAnnotatedString { append(context.getString(R.string.Unknown_Message)) }

        return when (content) {
            is TdApi.MessageText -> buildAnnotatedString {
                val text = content.text.text.replace('\n', ' ')
                append(if (text.length > maxText) text.take(maxText) + "..." else text)
            }
            is TdApi.MessagePhoto -> buildAnnotatedString {
                // 将 Photo 文本设置为蓝色
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                    append(context.getString(R.string.Photo))
                }
                append(" ")
                val caption = content.caption.text.replace('\n', ' ')
                append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
            }
            is TdApi.MessageVideo -> buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                    append(context.getString(R.string.Video))
                }
                append(" ")
                val caption = content.caption.text.replace('\n', ' ')
                append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
            }
            is TdApi.MessageVoiceNote -> buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                    append(context.getString(R.string.Voice))
                }
                append(" ")
                val caption = content.caption.text.replace('\n', ' ')
                append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
            }
            is TdApi.MessageAnimation -> buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                    append(context.getString(R.string.Animation))
                }
                append(" ")
                val caption = content.caption.text.replace('\n', ' ')
                append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
            }
            is TdApi.MessageDocument -> buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(context.getColor(R.color.blue)))) {
                    append(context.getString(R.string.File))
                }
                append(" ")
                val caption = content.document.fileName.replace('\n', ' ') + content.caption.text.replace('\n', ' ')
                append(if (caption.length > maxText) caption.take(maxText) + "..." else caption)
            }
            is TdApi.MessageAnimatedEmoji -> buildAnnotatedString {
                if (content.emoji.isEmpty()) append(context.getString(R.string.Unknown_Message))
                else append(content.emoji)
            }
            is TdApi.MessageSticker -> buildAnnotatedString {
                if (content.sticker.emoji.isEmpty()) append(context.getString(R.string.Unknown_Message))
                else append(content.sticker.emoji)
            }
            else -> buildAnnotatedString { append(context.getString(R.string.Unknown_Message)) }
        }
    }

    // 获取FCM接受到的消息的相应账号
    fun getPushReceiverId(payload: String, callback: (Long) -> Unit) {
        client.send(TdApi.GetPushReceiverId(payload)) { receiverId ->
            if (receiverId is TdApi.PushReceiverId) {
                callback(receiverId.id)
            }
        }
    }

    // 处理加密消息
    fun processPushNotification(payload: String) {
        client.send(TdApi.ProcessPushNotification(payload)) {}
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
    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }.toString()
    }

    // 获取归档会话
    fun getArchiveChats() {
        client.send(TdApi.GetChats(TdApi.ChatListArchive(), 1000)) { response ->
            if (response is TdApi.Chats) {
                val chatIds = response.chatIds
                chatIds.forEach { chatId ->
                    chatsList.value = chatsList.value.toMutableList().apply {
                        // 查找现有的聊天并更新
                        val existingChatIndex = indexOfFirst { it.id == chatId }
                        if (existingChatIndex >= 0) {
                            // 如果存在该聊天，更新并移动到顶部
                            if (get(existingChatIndex).isArchiveChatPin != true) {
                                val updatedChat = get(existingChatIndex).copy(
                                    id = chatId,
                                    needNotification = false,
                                    isArchiveChatPin = false
                                )
                                removeAt(existingChatIndex)  // 移除旧的聊天
                                add(0, updatedChat)  // 将更新后的聊天添加到顶部
                            }
                        } else {
                            // 如果不存在该聊天，添加到列表末尾
                            add(
                                Chat(
                                    id = chatId,
                                    title = context.getString(R.string.loading),
                                    accentColorId = 2,
                                    needNotification = false,
                                    isArchiveChatPin = false
                                )
                            )

                        }
                    }
                }
            }
        }
    }

    // 获取消息链接
    fun getMessageLink(
        messageId: Long,
        chatId: Long = saveChatId,
        callback: (TdApi.MessageLink?) -> Unit
    ) {
        client.send(TdApi.GetMessageLink(
            chatId,
            messageId,
            0,
            false,
            false
        )) { response ->
            //println(response)
            if (response is TdApi.MessageLink) {
                println("Message link: ${response.link}")
                callback.invoke(response)
            } else if (response is TdApi.Error) {
                if (response.code == 400) {
                    println("Error message: ${response.message}")
                    callback.invoke(null)
                }
            }
        }
    }

    // 下载文件
    fun downloadFile(
        file: TdApi.File,
        schedule: (TdApi.File) -> Unit = {},
        completion: (Boolean, String?) -> Unit = { _, _ -> },
        priority: Int = 1,
        synchronous: Boolean = true
    ) {
        // 判断文件是否已经下载完成
        if (file.local.isDownloadingCompleted) {
            // 文件已经下载完成，直接返回
            completion(true, file.local.path)
        } else {
            // 添加进度更新
            if (schedule != {}) {
                updateFileCallBackList[file.id] = { file ->
                    schedule(file)
                }
            }

            // 开始下载文件
            client.send(TdApi.DownloadFile(
                file.id, // fileId: 文件 ID，类型 int
                priority, // priority: 下载优先级，1-32，类型 int
                0, // offset: 下载起始位置，类型 long
                0, // limit: 下载的字节数限制，0 表示无限制，类型 long
                synchronous // synchronous: 是否异步，类型 boolean
            )) { response ->
                when (response) {
                    is TdApi.Error -> {
                        // 下载失败，回调completion
                        println("文件下载失败: ${response.message}")
                        completion(false, null)
                    }

                    is TdApi.File -> {
                        // 回调schedule以更新进度
                        schedule(response)

                        // 检查是否下载完成
                        if (response.local.isDownloadingCompleted) {
                            //println("测试代码执行")
                            var file = File(response.local.path)
                            CoroutineScope(Dispatchers.IO).launch {
                                while (!file.exists() || file.length() == 0L) {
                                    delay(500) // 启动一个协程来调用 delay
                                    file = File(response.local.path)
                                }
                                withContext(Dispatchers.Main) {
                                    println("文件下载完成: ${response.local.path}")
                                    completion(true, response.local.path)
                                }
                            }
                        } else {
                            println("下载进行中: ${response.local.downloadedSize}")
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

    // 删除正在下载的文件
    fun cancelDownloadFile(fileId: Int, callback: () -> Unit) {
        //println("删除文件 $fileId")
        client.send(TdApi.CancelDownloadFile(
            fileId,
            false
        )) { response ->
            //println(response)
            if (response is TdApi.Ok) {
                callback.invoke()
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

    fun searchPublicChat(username: String, callback: (TdApi.Chat?) -> Unit) {
        // 异步搜索公共聊天
        client.send(TdApi.SearchPublicChat(username)) { response ->
            if (response is TdApi.Chat) {
                callback(response)
            } else {
                callback(null)
            }
        }
    }

    // 获取群组信息
    suspend fun getBasicGroup(id: Long): TdApi.BasicGroup? {
        try {
            val getResult = sendRequest(TdApi.GetBasicGroup(id))
            return getResult
        } catch (e: Exception) {
            println("getBasicGroup request failed: ${e.message}")
            return null
        }
    }

    // 获取群组详细信息
    suspend fun getBasicGroupFullInfo(id: Long): TdApi.BasicGroupFullInfo? {
        try {
            val getResult = sendRequest(TdApi.GetBasicGroupFullInfo(id))
            return getResult
        } catch (e: Exception) {
            println("getBasicGroupFullInfo request failed: ${e.message}")
            return null
        }
    }

    // 获取超级群组信息
    suspend fun getSupergroup(id: Long): TdApi.Supergroup? {
        try {
            val getResult = sendRequest(TdApi.GetSupergroup(id))
            return getResult
        } catch (e: Exception) {
            println("getSupergroup request failed: ${e.message}")
            return null
        }
    }

    // 获取超级群组详细信息
    suspend fun getSupergroupFullInfo(id: Long): TdApi.SupergroupFullInfo? {
        try {
            val getResult = sendRequest(TdApi.GetSupergroupFullInfo(id))
            return getResult
        } catch (e: Exception) {
            println("getSupergroupFullInfo request failed: ${e.message}")
            return null
        }
    }

    // 按用户名搜索公共聊天
    suspend fun searchPublicChats(
        query: String,
        searchList: MutableState<List<Chat>>
    ) {
        println("查询中")
        val searchResult = sendRequest(TdApi.SearchPublicChats(query))
        searchResult.let {
            if (it is TdApi.Chats) {
                // 搜索成功
                println("搜索成功")
                //println(searchResult)
                searchList.value = emptyList()
                searchList.value = withContext(Dispatchers.IO) {
                    searchResult.chatIds.map { id ->
                        var isPinned = false
                        var isRead = false
                        var isBot = false
                        var isChannel = false
                        var isGroup = false
                        var isPrivateChat = false
                        var chatTitle = "error"
                        var lastMessageTime = -1
                        var lastMessage = buildAnnotatedString {}
                        var accentColorId = 2
                        try {
                            val chatResult = sendRequest(TdApi.GetChat(id))
                            if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                                println(chatResult)
                                isPinned = chatResult.positions.firstOrNull()?.isPinned ?: false
                                chatTitle = chatResult.title
                                lastMessage = handleAllMessages(chatResult.lastMessage)
                                isRead = chatResult.isMarkedAsUnread
                                accentColorId = chatResult.accentColorId
                                lastMessageTime = chatResult.lastMessage?.date ?: -1
                                when (val messageType = chatResult.type) {
                                    is TdApi.ChatTypeSupergroup -> {
                                        if (messageType.isChannel) {
                                            isChannel = true
                                        } else {
                                            isGroup = true
                                        }
                                    }

                                    is TdApi.ChatTypeBasicGroup -> {
                                        isGroup = true
                                    }

                                    is TdApi.ChatTypePrivate -> {
                                        isPrivateChat = true
                                        val userResult =
                                            sendRequest(TdApi.GetUser(chatResult.id))
                                        if (userResult.type is TdApi.UserTypeBot) {
                                            isBot = true
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("GetChat request failed (handleNewChat): ${e.message}")
                        }
                        Chat(
                            id = id,
                            title = chatTitle,
                            accentColorId = accentColorId,
                            lastMessage = lastMessage,
                            lastMessageTime = lastMessageTime,
                            isPinned = isPinned,
                            isRead = isRead,
                            isBot = isBot,
                            isChannel = isChannel,
                            isGroup = isGroup,
                            isPrivateChat = isPrivateChat
                        )
                    }
                }
            }
        }
    }

    // 发送加入聊天请求
    fun joinChat(chatId: Long, reInit: () -> Unit) {
        client.send(TdApi.JoinChat(chatId)) { result ->
            if (result is TdApi.Ok) {
                // 加入聊天成功
                //println("Joined the chat successfully")
                addNewChat(chatId)
                reInit()
            } else {
                // 加入聊天失败
                println("Failed to join chat")
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
    fun getUserName(userId: Long, onResult: (String) -> Unit) {
        val getUserRequest = TdApi.GetUser(userId)

        client.send(getUserRequest) { result ->
            if (result is TdApi.User) {
                val fullName = "${result.firstName} ${result.lastName}".trim()
                onResult(fullName)
            } else {
                onResult("Unknown User")
            }
        }
    }

    // 获取用户
    suspend fun getUser(id: Long): TdApi.User? {
        try {
            val getResult = sendRequest(TdApi.GetUser(id))
            return getResult
        } catch (e: Exception) {
            println("GetUser request failed: ${e.message}")
            return null
        }
    }

    // 获取用户详细信息
    suspend fun getUserFullInfo(id: Long): TdApi.UserFullInfo? {
        try {
            val getResult = sendRequest(TdApi.GetUserFullInfo(id))
            return getResult
        } catch (e: Exception) {
            println("GetUser request failed: ${e.message}")
            return null
        }
    }

    // 删除代理
    suspend fun removeProxy(proxyId: Int) : TdApi.Ok? {
        try {
            return sendRequest(TdApi.RemoveProxy(proxyId))
        } catch (e: Exception) {
            println("RemoveProxy request failed: ${e.message}")
            return null
        }
    }

    // 停用代理
    suspend fun disableProxy() : TdApi.Ok? {
        try {
            return sendRequest(TdApi.DisableProxy())
        } catch (e: Exception) {
            println("DisableProxy request failed: ${e.message}")
            return null
        }
    }

    // 启用代理
    suspend fun enableProxy(proxyId: Int) : TdApi.Ok? {
        try {
            return sendRequest(TdApi.EnableProxy(proxyId))
        } catch (e: Exception) {
            println("DisableProxy request failed: ${e.message}")
            return null
        }
    }

    // 获取代理信息
    suspend fun getProxy() : TdApi.Proxies? {
        try {
            val getResult = sendRequest(TdApi.GetProxies())
            //println(getResult)
            return getResult
        } catch (e: Exception) {
            println("GetUser request failed: ${e.message}")
            return null
        }
    }

    // 添加代理
    fun addProxy(server: String, port: Int, type: TdApi.ProxyType, enable: Boolean = true) {
        try {
            val addProxyRequest = TdApi.AddProxy(
                server,
                port,
                enable,
                type
            )
            client.send(addProxyRequest) { result ->
                when (result) {
                    is TdApi.Ok -> {
                        println("Proxy added successfully")
                    }
                    is TdApi.Error -> {
                        println("Failed to add proxy: ${result.message}")
                    }
                    else -> {
                        println("Unexpected response type")
                    }
                }
            }
        } catch (e: Exception) {
            println("SetProxy request failed: ${e.message}")
        }
    }

    // 获取聊天对象
    suspend fun getChat(chatId: Long): TdApi.Chat? {
        return suspendCancellableCoroutine { continuation ->
            val getChatRequest = TdApi.GetChat(chatId)

            // 发送异步请求
            client.send(getChatRequest) { result ->
                if (result is TdApi.Chat) {
                    // 当结果是 TdApi.Chat 时，恢复协程并返回 Chat 对象
                    continuation.resume(result)
                } else {
                    // 在其他情况下，恢复协程并返回 null
                    continuation.resume(null)
                }
            }
        }
    }

    // 退出登录
    fun logOut() {
        client.send(TdApi.LogOut()) { result ->
            when (result.constructor) {
                TdApi.Ok.CONSTRUCTOR -> {
                    println("Logged out successfully")
                }

                else -> {
                    println("Failed to log out: $result")
                }
            }
        }
    }

    // 获取联系人
    fun getContacts(contacts: MutableState<List<Chat>>) {
        val request = TdApi.GetContacts()
        client.send(request) { result ->
            when (result.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    val error = result as TdApi.Error
                    println("Error getting contacts: ${error.message}")
                }
                TdApi.Users.CONSTRUCTOR -> {
                    val users = result as TdApi.Users
                    val userIds = users.userIds

                    // 异步获取每个用户的详细信息
                    CoroutineScope(Dispatchers.IO).launch {
                        for (userId in userIds) {
                            try {
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
                                                title = "${user.firstName} ${user.lastName}"
                                            )
                                        } else {
                                            // 添加新联系人
                                            existingContacts.add(
                                                Chat(
                                                    id = user.id,
                                                    title = "${user.firstName} ${user.lastName}"
                                                )
                                            )
                                        }
                                        // 更新状态
                                        contacts.value = existingContacts
                                    }
                                } else {
                                    println("Unexpected result type for user ID $userId: ${userResult.javaClass.name}")
                                }
                            } catch (e: Exception) {
                                println("GetUser request failed: ${e.message}")
                            }
                        }
                    }
                }
                else -> {
                    println("Unexpected result type: ${result.constructor}")
                }
            }
        }
    }

    // 标记已读
    fun markMessagesAsRead(messageId: Long? = null, chatId: Long = saveChatId, forceRead: Boolean = false) {
        if (messageId != null) {
            // 创建 ViewMessages 请求
            val viewMessagesRequest = TdApi.ViewMessages(
                chatId,
                longArrayOf(messageId),
                null,
                forceRead
            )

            // 发送 ViewMessages 请求
            client.send(viewMessagesRequest) { response ->
                if (response is TdApi.Ok) {
                    //println("Messages successfully marked as read in chat ID $saveChatId")
                } else {
                    println("Failed to mark messages as read: $response")
                }
            }
        } else {
            // 异步执行
            CoroutineScope(Dispatchers.IO).launch {
                // 获取消息 ID
                try {
                    val chatResult = sendRequest(TdApi.GetChat(chatId))
                    val messageId = chatResult.lastMessage?.id

                    if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                        // 创建 ViewMessages 请求
                        val viewMessagesRequest = messageId?.let {
                            TdApi.ViewMessages(
                                chatId,
                                longArrayOf(it),
                                null,
                                forceRead
                            )
                        }

                        // 发送 ViewMessages 请求
                        client.send(viewMessagesRequest) { response ->
                            if (response is TdApi.Ok) {
                                println("Messages successfully marked as read in chat ID $chatId")
                            } else {
                                println("Failed to mark messages as read: $response")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("HandleNewChat failed: ${e.message}")
                }
            }
        }
    }

    // 获取分组信息
    private suspend fun getChatFolderInfo(chatFolderId: Int): TdApi.ChatFolder? {
        try {
            val result = sendRequest(TdApi.GetChatFolder(chatFolderId))
            return result
        } catch (e: Exception) {
            println("Error getting chat folders: ${e.message}")
        }
        return null
    }

    suspend fun createPrivateChat(userId: Long) : TdApi.Chat? {
        try {
            return sendRequest(TdApi.CreatePrivateChat(userId, false))
        } catch (e: Exception) {
            println("Error create private chat: ${e.message}")
            return null
        }
    }

    // 发送消息
    fun sendMessage(chatId: Long, message: InputMessageContent, replyTo: TdApi.InputMessageReplyTo? = null) {
        val message = TdApi.SendMessage().apply {
            this.chatId = chatId
            this.replyTo = replyTo
            inputMessageContent = message
        }
        client.send(message) { result ->
            //println("SendMessage result: $result")
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Send Message Error: ${error.message}")
            } else {
                println("Message sent successfully")
            }
        }
    }

    // 修改消息文本
    fun editMessageText(chatId: Long, messageId: Long, message: InputMessageContent) {
        client.send(TdApi.EditMessageText(
            chatId,
            messageId,
            null,
            message
        )) { result ->
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                val error = result as TdApi.Error
                println("Edit Message Error: ${error.message}")
            } else {
                println("Message edited successfully")
            }
        }
    }

    fun setFCMToken(token: String = "", callback: (Long) -> Unit = {}) {
        client.send(
            TdApi.RegisterDevice(
                TdApi.DeviceTokenFirebaseCloudMessaging(
                    token,
                    true
                ),
                null
            )
        ) { result ->
            if (result is TdApi.PushReceiverId) {
                println("FCM token set successfully")
                sharedPref.edit(commit = false) {
                    putLong("userPushReceiverId", result.id)
                }
                callback(result.id)
            } else {
                println("Failed to set FCM token: $result")
            }
        }
    }

    // 加载聊天列表
    suspend fun loadChats(limit: Int = 15){
        val loadChats = TdApi.LoadChats(TdApi.ChatListFolder(0), limit)
        try {
            val result = sendRequest(loadChats)
            println("LoadChats result: $result")
        } catch (e: Exception) {
            println("LoadChats request failed: ${e.message}")
        }
    }

    // 根据消息id删除消息
    fun deleteMessageById(messageId: Long) {
        println("Deleting message")
        runBlocking {
            // 创建一个请求来删除指定 ID 的消息
            val getMessageRequest = TdApi.DeleteMessages(saveChatId, longArrayOf(messageId) , true)
            try {
                val result = sendRequest(getMessageRequest)
                if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                    println("Message deleted successfully")
                } else {
                    println("Failed to reload message with ID $messageId: $result")
                }
            } catch (e: Exception) {
                println("DeleteMessage request failed: ${e.message}")
            }
        }
    }

    // 根据消息id更新消息
    fun reloadMessageById(messageId: Long) {
        println("Reloading message")
        CoroutineScope(Dispatchers.IO).launch {
            // 创建一个请求来获取指定 ID 的消息
            val getMessageRequest = TdApi.GetMessage(saveChatId, messageId)
            try {
                val result = sendRequest(getMessageRequest)
                if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                    val message = result as TdApi.Message

                    //println(message)

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
                                saveChatIdList[saveChatId]?.add(0, messageId)
                            }
                        }
                    }
                } else {
                    println("Failed to reload message with ID $messageId: $result")
                }
            } catch (e: Exception) {
                println("GetMessage request failed: ${e.message}")
            }
        }
    }

    // 获取当前用户 ID 的方法
    suspend fun getCurrentUser(): List<String>? {
        if (currentUser.isEmpty()) {
            try {
                val result = sendRequest(TdApi.GetMe())
                if (result.constructor == TdApi.User.CONSTRUCTOR) {
                    val user = result as TdApi.User
                    currentUser = listOf(user.id.toString(), "${user.firstName} ${user.lastName}")
                    return currentUser
                } else {
                    throw IllegalStateException("Failed to get current user ID")
                }
            } catch (e: Exception) {
                println("GetMe request failed: ${e.message}")
                return null
            }
        } else {
            client.send(TdApi.GetMe()) {
                if (it is TdApi.User) {
                    val user = it
                    currentUser = listOf(user.id.toString(), "${user.firstName} ${user.lastName}")
                }
            }
            return currentUser
        }
    }

    // 发送请求并返回结果
    private suspend fun <R : TdApi.Object> sendRequest(
        request: TdApi.Function<R>,
        retryCount: Int = 3 // 重试次数限制
    ): R = withContext(Dispatchers.IO) {
        val result = CompletableDeferred<R>()
        client.send(request) { response ->
            when (response) {
                is TdApi.Error -> {
                    if (response.code == 404) {
                        // 错误码是 404，直接抛出异常
                        result.completeExceptionally(
                            Exception("TDLib error 404: ${response.message}")
                        )
                    } else if (retryCount > 0) {
                        // 错误码不是 404，并且还可以重试，递归调用 sendRequest
                        launch {
                            try {
                                val retryResult = sendRequest(request, retryCount - 1)
                                result.complete(retryResult)
                            } catch (e: Exception) {
                                result.completeExceptionally(e)
                            }
                        }
                    } else {
                        // 超过重试次数，抛出异常
                        result.completeExceptionally(
                            Exception("TDLib error: ${response.message}")
                        )
                    }
                }
                else -> {
                    // 成功时，完成请求
                    @Suppress("UNCHECKED_CAST")
                    result.complete(response as R)
                }
            }
        }
        return@withContext result.await()
    }

    // 进入聊天页面
    fun openChatPage(openChatId: Long, chatList: MutableState<List<TdApi.Message>>) {
        client.send(TdApi.OpenChat(openChatId)) { result ->
            if (result.constructor == TdApi.Ok.CONSTRUCTOR) {
                println("Opened chat page successfully, ChatId: $openChatId")
            } else {
                println("Failed to open chat page: $result")
            }
        }
        if (saveChatId != 0L && saveChatId != -1L && saveChatId != openChatId){
            saveChatMessagesList = saveChatMessagesList.toMutableMap().apply {
                put(saveChatId, ChatMessagesSave(
                    saveChatList.value.toMutableList(),
                    lastReadInboxMessageId.value,
                    lastReadOutboxMessageId.value
                ))
            }
        }
        saveChatId = openChatId
        val oldChatList = saveChatMessagesList[openChatId]
        if (oldChatList == null) {
            saveChatList = chatList
            lastReadInboxMessageId.value = 0L
            lastReadOutboxMessageId.value = 0L
        } else {
            saveChatList.value = oldChatList.messages
            lastReadInboxMessageId.value = oldChatList.lastReadInboxMessageId
            lastReadOutboxMessageId.value = oldChatList.lastReadOutboxMessageId
        }
        isExitChatPage = false
    }

    // 退出聊天页面
    fun exitChatPage(draftMessage: TdApi.DraftMessage? = null) {
        val closeChatId = saveChatId
        client.send(TdApi.SetChatDraftMessage(closeChatId, 0, draftMessage)) { result ->
            if (result.constructor == TdApi.Ok.CONSTRUCTOR) {
                println("Set draft message successfully, ChatId: $closeChatId")
            } else {
                println("Failed to set draft message: $result")
            }
        }
        client.send(TdApi.CloseChat(closeChatId)) { result ->
            if (result.constructor == TdApi.Ok.CONSTRUCTOR) {
                println("Closed chat page successfully, ChatId: $closeChatId")
            } else {
                println("Failed to close chat page: $result")
            }
        }
        if (closeChatId != 0L && closeChatId != -1L){
            saveChatMessagesList = saveChatMessagesList.toMutableMap().apply {
                remove(closeChatId)
            }
        }
        isExitChatPage = true
        saveChatId = 0L
    }

    // 删除聊天
    suspend fun deleteChat(chatId: Long){
        saveChatId = -1L
        sendRequest(TdApi.DeleteChat(chatId))
    }

    // 获取消息
    fun fetchMessages(fromMessageId: Long = saveChatList.value.lastOrNull()?.id ?: -1L, nowChatId: Long = saveChatId, limit: Int = 10) {
        //println("fetchMessages启动")
        //println(saveChatId)
        if (fromMessageId != -1L) {
            val getChatMessages = TdApi.GetChatHistory().apply {
                this.chatId = nowChatId
                this.limit = limit // 每次获取 limit 条消息
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
                            val sortedMessages = messages.messages
                                .toList()
                                .sortedByDescending { it.date }
                                .filterNot { message ->
                                    saveChatList.value.any { it.id == message.id }
                                }

                            saveChatList.value = saveChatList.value.toMutableList().apply {
                                if (nowChatId == saveChatId) {
                                    addAll(sortedMessages) // 将新消息添加到列表最后面
                                } else {
                                    println("Discarded messages: $sortedMessages")
                                }
                            }
                            // 继续加载更旧的消息
                            if (fromMessageId == 0L) {
                                fetchMessages(messages.messages.last().id)
                            }
                            //println(messages.messages.last().id)
                        }
                    }
                }
            }
        }
    }

    // 根据消息id获取消息
    suspend fun getMessageTypeById(messageId: Long, chatId: Long = saveChatId): TdApi.Message? {
        val getMessageRequest = TdApi.GetMessage(chatId, messageId)

        try {
            val result = sendRequest(getMessageRequest)
            if (result.constructor == TdApi.Message.CONSTRUCTOR) {
                val message = result as TdApi.Message
                //println("GetMessage result: $message")
                return message
            } else {
                println("Failed to get message with ID $messageId: $result")
                return null
            }
        } catch (e: Exception) {
            return null
        }
    }

    // 关闭连接
    fun close() {
        println("Closing client")
        runBlocking {
            sendRequest(TdApi.Close())
        }
    }
}
