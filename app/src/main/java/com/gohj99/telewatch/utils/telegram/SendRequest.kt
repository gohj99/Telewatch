/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.buildAnnotatedString
import com.gohj99.telewatch.model.Chat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

/*
 * 包含sendRequest请求函数功能注释区：
 *
 * 1. sendRequest
 *   作用: 通用TDLib请求发送器，支持自动重试机制。处理响应并返回结果对象，自动处理404等错误
 *
 * 2. deleteMessageById
 *   作用: 根据消息ID删除指定消息，操作结果通过日志输出
 *
 * 3. loadChats
 *   作用: 加载指定数量的聊天列表（默认15条），用于初始化或刷新聊天界面
 *
 * 4. reloadMessageById
 *   作用: 强制重新加载指定消息并更新本地消息列表，支持消息位置调整
 *
 * 5. createPrivateChat
 *   作用: 创建与指定用户的私密聊天会话，返回Chat对象
 *
 * 6. getChatFolderInfo
 *   作用: 获取指定ID的聊天分组配置信息
 *
 * 7. getCurrentUser
 *   作用: 获取当前登录用户的ID和姓名信息，带缓存机制
 *
 * 8. deleteChat
 *   作用: 删除指定聊天并重置当前选中聊天ID
 *
 * 9. getMessageTypeById
 *   作用: 根据消息ID获取完整消息对象及其类型信息
 *
 * 10. addNewChat
 *    作用: 强制加载新聊天到列表，包含聊天类型判断（频道/群组/私聊）和UI更新
 *
 * 11. getBasicGroup/getSupergroup 系列
 *    作用: 获取基础群组/超级群组的详细信息（基础信息+完整信息）
 *
 * 12. searchPublicChats
 *    作用: 根据关键词搜索公开聊天/频道，结果更新到可观察状态列表
 *
 * 13. getUser 系列
 *    作用: 获取用户基础信息+完整档案信息
 *
 * 14. 代理管理系列 (removeProxy/disableProxy/enableProxy/getProxy)
 *    作用: 代理服务器的增删改查和状态管理
 *
 * 15. getContacts
 *    作用: 获取联系人列表并维护可观察状态
 *
 * 16. markMessagesAsRead
 *    作用: 标记消息为已读，支持指定消息或最后一条消息的自动识别
 */

// 发送请求并返回结果
internal suspend fun <R : TdApi.Object> TgApi.sendRequest(
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

// 根据消息id删除消息
fun TgApi.deleteMessageById(messageId: Long) {
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

// 加载聊天列表
suspend fun TgApi.loadChats(limit: Int = 15){
    val loadChats = TdApi.LoadChats(TdApi.ChatListFolder(0), limit)
    try {
        val result = sendRequest(loadChats)
        println("LoadChats result: $result")
    } catch (e: Exception) {
        println("LoadChats request failed: ${e.message}")
    }
}

// 根据消息id更新消息
fun TgApi.reloadMessageById(messageId: Long) {
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

suspend fun TgApi.createPrivateChat(userId: Long) : TdApi.Chat? {
    try {
        return sendRequest(TdApi.CreatePrivateChat(userId, false))
    } catch (e: Exception) {
        println("Error create private chat: ${e.message}")
        return null
    }
}

// 获取分组信息
internal suspend fun TgApi.getChatFolderInfo(chatFolderId: Int): TdApi.ChatFolder? {
    try {
        val result = sendRequest(TdApi.GetChatFolder(chatFolderId))
        return result
    } catch (e: Exception) {
        println("Error getting chat folders: ${e.message}")
    }
    return null
}

// 获取当前用户 ID 的方法
suspend fun TgApi.getCurrentUser(): List<String>? {
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

// 删除聊天
suspend fun TgApi.deleteChat(chatId: Long){
    saveChatId = -1L
    sendRequest(TdApi.DeleteChat(chatId))
}

// 根据消息id获取消息
suspend fun TgApi.getMessageTypeById(messageId: Long, chatId: Long = saveChatId): TdApi.Message? {
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

// 强制加载消息
internal fun TgApi.addNewChat(chatId: Long){
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

// 获取群组信息
suspend fun TgApi.getBasicGroup(id: Long): TdApi.BasicGroup? {
    try {
        val getResult = sendRequest(TdApi.GetBasicGroup(id))
        return getResult
    } catch (e: Exception) {
        println("getBasicGroup request failed: ${e.message}")
        return null
    }
}

// 获取群组详细信息
suspend fun TgApi.getBasicGroupFullInfo(id: Long): TdApi.BasicGroupFullInfo? {
    try {
        val getResult = sendRequest(TdApi.GetBasicGroupFullInfo(id))
        return getResult
    } catch (e: Exception) {
        println("getBasicGroupFullInfo request failed: ${e.message}")
        return null
    }
}

// 获取超级群组信息
suspend fun TgApi.getSupergroup(id: Long): TdApi.Supergroup? {
    try {
        val getResult = sendRequest(TdApi.GetSupergroup(id))
        return getResult
    } catch (e: Exception) {
        println("getSupergroup request failed: ${e.message}")
        return null
    }
}

// 获取超级群组详细信息
suspend fun TgApi.getSupergroupFullInfo(id: Long): TdApi.SupergroupFullInfo? {
    try {
        val getResult = sendRequest(TdApi.GetSupergroupFullInfo(id))
        return getResult
    } catch (e: Exception) {
        println("getSupergroupFullInfo request failed: ${e.message}")
        return null
    }
}

// 按用户名搜索公共聊天
suspend fun TgApi.searchPublicChats(
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

// 获取用户
suspend fun TgApi.getUser(id: Long): TdApi.User? {
    try {
        val getResult = sendRequest(TdApi.GetUser(id))
        return getResult
    } catch (e: Exception) {
        println("GetUser request failed: ${e.message}")
        return null
    }
}

// 获取用户详细信息
suspend fun TgApi.getUserFullInfo(id: Long): TdApi.UserFullInfo? {
    try {
        val getResult = sendRequest(TdApi.GetUserFullInfo(id))
        return getResult
    } catch (e: Exception) {
        println("GetUser request failed: ${e.message}")
        return null
    }
}

// 删除代理
suspend fun TgApi.removeProxy(proxyId: Int) : TdApi.Ok? {
    try {
        return sendRequest(TdApi.RemoveProxy(proxyId))
    } catch (e: Exception) {
        println("RemoveProxy request failed: ${e.message}")
        return null
    }
}

// 停用代理
suspend fun TgApi.disableProxy() : TdApi.Ok? {
    try {
        return sendRequest(TdApi.DisableProxy())
    } catch (e: Exception) {
        println("DisableProxy request failed: ${e.message}")
        return null
    }
}

// 启用代理
suspend fun TgApi.enableProxy(proxyId: Int) : TdApi.Ok? {
    try {
        return sendRequest(TdApi.EnableProxy(proxyId))
    } catch (e: Exception) {
        println("DisableProxy request failed: ${e.message}")
        return null
    }
}

// 获取代理信息
suspend fun TgApi.getProxy() : TdApi.Proxies? {
    try {
        val getResult = sendRequest(TdApi.GetProxies())
        //println(getResult)
        return getResult
    } catch (e: Exception) {
        println("GetUser request failed: ${e.message}")
        return null
    }
}

// 获取联系人
fun TgApi.getContacts(contacts: MutableState<List<Chat>>) {
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
fun TgApi.markMessagesAsRead(messageId: Long? = null, chatId: Long = saveChatId, forceRead: Boolean = false) {
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

// 获取聊天主题信息
suspend fun TgApi.getForumTopics(chatId: Long): TdApi.ForumTopics? {
    try {
        val getResult = sendRequest(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100))
        return getResult
    } catch (e: Exception) {
        println("GetChatTheme request failed: ${e.message}")
        return null
    }
}
