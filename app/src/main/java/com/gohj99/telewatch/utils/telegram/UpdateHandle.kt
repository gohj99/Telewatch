/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.content.edit
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.utils.generateChatTitleIconBitmap
import com.gohj99.telewatch.utils.notification.drawableToBitmap
import com.gohj99.telewatch.utils.notification.sendChatMessageNotification
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException

/*
 * 更新函数功能注释区：
 *
 * 1. handleAuthorizationState - 处理授权状态更新
 *   (处理 TdApi.UpdateAuthorizationState，管理登录/登出状态变更)
 *
 * 2. handleNewMessage - 处理新消息接收
 *   (处理 TdApi.UpdateNewMessage，管理消息存储、通知触发逻辑)
 *
 * 3. handleMessageContentUpdate - 处理消息内容更新
 *   (处理 TdApi.UpdateMessageContent，更新本地消息缓存内容)
 *
 * 4. handleMessageEdited - 处理消息编辑事件
 *   (处理 TdApi.UpdateMessageEdited，更新消息编辑时间和内容)
 *
 * 5. handleDeleteMessages - 处理消息删除事件
 *   (处理 TdApi.UpdateDeleteMessages，清理本地消息缓存)
 *
 * 6. handleNewChat - 处理新聊天创建
 *   (处理 TdApi.UpdateNewChat，管理聊天列表更新和分类逻辑)
 *
 * 7. handleConnectionUpdate - 处理网络连接状态变化
 *   (处理 TdApi.UpdateConnectionState，管理网络状态提示和FCM令牌更新)
 *
 * 8. handleChatReadInboxUpdate - 处理收件箱已读状态更新
 *   (处理 TdApi.UpdateChatReadInbox，更新未读消息计数)
 *
 * 9. handleChatFoldersUpdate - 处理聊天文件夹更新
 *   (处理 TdApi.UpdateChatFolders，异步加载文件夹结构)
 *
 * 10. handleChatTitleUpdate - 处理聊天标题变更
 *   (处理 TdApi.UpdateChatTitle，更新聊天列表标题显示)
 *
 * 11. handleUpdateUser - 处理用户信息更新
 *   (处理 TdApi.UpdateUser，更新用户类型和显示名称)
 *
 * 12. handleChatLastMessageUpdate - 处理最后消息更新
 *   (处理 TdApi.UpdateChatLastMessage，维护聊天列表最新消息预览)
 *
 * 13. handleChatPositionUpdate - 处理聊天排序变更
 *   (处理 TdApi.UpdateChatPosition，管理置顶/归档等排序逻辑)
 *
 * 14. handleMessageSendSucceededUpdate - 处理消息发送成功
 *   (处理 TdApi.UpdateMessageSendSucceeded，更新消息发送状态)
 *
 * 15. handleFileUpdate - 处理文件下载状态更新
 *   (处理 TdApi.UpdateFile，管理文件下载回调)
 *
 * 16. handleChatPhotoUpdate - 处理聊天图片变更
 *   (处理 TdApi.UpdateChatPhoto，更新聊天头像显示)
 *
 * 17. handleChatNotificationSettingsUpdate - 处理通知设置变更
 *   (处理 TdApi.UpdateChatNotificationSettings，管理消息免打扰状态)
 *
 * 18. handleChatDraftMessageUpdate - 处理草稿消息更新
 *   (处理 TdApi.UpdateChatDraftMessage，维护聊天草稿状态显示)
 */

// 处理授权状态更新
internal fun TgApi.handleAuthorizationState(update: TdApi.UpdateAuthorizationState) {
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

// 新消息更新
internal fun TgApi.handleNewMessage(update: TdApi.UpdateNewMessage) {
    val message = update.message
    //println("New message received in chat ID ${message.chatId}\n messageId ${message.id}")
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
internal fun TgApi.handleMessageContentUpdate(update: TdApi.UpdateMessageContent) {
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
}

// 处理消息编辑
internal fun TgApi.handleMessageEdited(update: TdApi.UpdateMessageEdited) {
    val chatId = update.chatId
    val messageId = update.messageId
    val editDate = update.editDate
    println("Message edited in chat ID $chatId for message ID $messageId at $editDate")

    if (chatId == saveChatId) {
        saveChatList.value = saveChatList.value.toMutableList().apply {
            val messageIndex = indexOfFirst { it.id == messageId }
            if (messageIndex >= 0) {
                // 合并旧消息的元数据和新内容
                val updatedMessage = get(messageIndex).copy(editDate = editDate)
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

// 处理删除消息
internal fun TgApi.handleDeleteMessages(update: TdApi.UpdateDeleteMessages) {
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

// 处理新聊天
internal fun TgApi.handleNewChat(update: TdApi.UpdateNewChat){
    //println("New chat received: ${update.chat}")
    val newChat = update.chat
    val chatId = newChat.id

    //println(newChat.lastMessage)
    //println(newChat)
    //println(newChat.positions.firstOrNull()?.isPinned ?: false)

    var isPinned = newChat.positions.find { it.list is TdApi.ChatListMain }?.isPinned == true
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

// 网络状态更新
internal fun TgApi.handleConnectionUpdate(update: TdApi.UpdateConnectionState) {
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
                            tgApi?.setFCMToken(token) { id ->
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

// 未读消息更新
internal fun TgApi.handleChatReadInboxUpdate(update: TdApi.UpdateChatReadInbox) {
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

// 已读消息更新
internal fun TgApi.handleChatReadOutboxUpdate(update: TdApi.UpdateChatReadOutbox) {
    val chatId = update.chatId
    if (chatId == saveChatId) {
        lastReadOutboxMessageId.value = update.lastReadOutboxMessageId
    } else if (chatId in saveChatMessagesList) {
        saveChatMessagesList[chatId]?.lastReadOutboxMessageId = update.lastReadOutboxMessageId
    }
}

// 获取聊天文件夹
internal fun TgApi.handleChatFoldersUpdate(update: TdApi.UpdateChatFolders) {
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

// 更新聊天标题
internal fun TgApi.handleChatTitleUpdate(update: TdApi.UpdateChatTitle) {
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

// 更新用户信息
internal fun TgApi.handleUpdateUser(update: TdApi.UpdateUser) {
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

// 更新最后一条消息内容
internal fun TgApi.handleChatLastMessageUpdate(update: TdApi.UpdateChatLastMessage) {
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

// 消息排序
internal fun TgApi.handleChatPositionUpdate(update: TdApi.UpdateChatPosition) {
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
                updatedChat = if (position.order == 0L) {
                    get(existingChatIndex).copy(
                        isArchiveChatPin = position.isPinned
                    )
                } else {
                    get(existingChatIndex).copy(
                        order = position.order,
                        isPinned = position.isPinned,
                        isArchiveChatPin = null
                    )
                }
            } else if (position.list is TdApi.ChatListArchive) {
                updatedChat = if (position.order == 0L) {
                    get(existingChatIndex).copy(
                        isArchiveChatPin = null
                    )
                } else {
                    get(existingChatIndex).copy(
                        isArchiveChatPin = position.isPinned
                    )
                }
            }
            removeAt(existingChatIndex)
            add(0, updatedChat)
        }
    }
}

// 消息发送成功
internal fun TgApi.handleMessageSendSucceededUpdate(update: TdApi.UpdateMessageSendSucceeded) {
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

// 文件更新
internal fun TgApi.handleFileUpdate(update: TdApi.UpdateFile) {
    val file = update.file
    //println("${file.id} file\n isDownloadingCompleted: ${file.local.isDownloadingCompleted}\n downloadedSize: ${file.local.downloadedSize}")

    updateFileCallBackList[file.id]?.invoke(file)
}

// 聊天图片更新
internal fun TgApi.handleChatPhotoUpdate(update: TdApi.UpdateChatPhoto) {
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

// 更新通知
internal fun TgApi.handleChatNotificationSettingsUpdate(update: TdApi.UpdateChatNotificationSettings) {
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

// 更新欲输入
internal fun TgApi.handleChatDraftMessageUpdate(update: TdApi.UpdateChatDraftMessage) {
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
