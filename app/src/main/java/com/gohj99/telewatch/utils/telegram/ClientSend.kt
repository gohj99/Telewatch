/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import androidx.core.content.edit
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.Chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.InputMessageContent
import java.io.File
import kotlin.coroutines.resume

/*
 * 包含Client请求函数功能注释区：
 *
 * 1. getPushReceiverId()
 *    - 作用: 通过FCM推送payload获取对应的账号ID
 *
 * 2. processPushNotification()
 *    - 作用: 处理加密的推送通知内容
 *
 * 3. getArchiveChats()
 *    - 作用: 获取归档聊天列表并更新UI（自动管理聊天项的排序和状态）
 *
 * 4. getMessageLink()
 *    - 作用: 生成指定消息的公开分享链接
 *
 * 5. downloadFile()
 *    - 作用: 通用文件下载器（支持进度回调、断点续传、异步/同步模式）
 *
 * 6. cancelDownloadFile()
 *    - 作用: 取消正在进行的文件下载任务
 *
 * 7. downloadPhoto()
 *    - 作用: 照片专用下载器（简化版的downloadFile封装）
 *
 * 8. searchPublicChat()
 *    - 作用: 根据用户名搜索公开的频道/群组
 *
 * 9. joinChat()
 *    - 作用: 加入指定聊天并触发UI刷新
 *
 * 10. getUserName()
 *    - 作用: 通过用户ID获取用户全名（firstName + lastName）
 *
 * 11. addProxy()
 *    - 作用: 添加网络代理服务器配置
 *
 * 12. getChat()（挂起函数）
 *    - 作用: 协程方式获取聊天详细信息
 *
 * 13. logOut()
 *    - 作用: 执行账号登出操作
 *
 * 14. sendMessage()
 *    - 作用: 发送消息到指定聊天（支持消息回复）
 *
 * 15. editMessageText()
 *    - 作用: 修改已发送消息的文本内容
 *
 * 16. setFCMToken()
 *    - 作用: 注册/更新设备的FCM推送令牌
 */

// 获取FCM接受到的消息的相应账号
fun TgApi.getPushReceiverId(payload: String, callback: (Long) -> Unit) {
    client.send(TdApi.GetPushReceiverId(payload)) { receiverId ->
        if (receiverId is TdApi.PushReceiverId) {
            callback(receiverId.id)
        }
    }
}

// 处理加密消息
fun TgApi.processPushNotification(payload: String) {
    client.send(TdApi.ProcessPushNotification(payload)) {}
}

// 获取归档会话
fun TgApi.getArchiveChats() {
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
fun TgApi.getMessageLink(
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
fun TgApi.downloadFile(
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
fun TgApi.cancelDownloadFile(fileId: Int, callback: () -> Unit) {
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
fun TgApi.downloadPhoto(file: TdApi.File, completion: (Boolean, String?) -> Unit) {
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

fun TgApi.searchPublicChat(username: String, callback: (TdApi.Chat?) -> Unit) {
    // 异步搜索公共聊天
    client.send(TdApi.SearchPublicChat(username)) { response ->
        if (response is TdApi.Chat) {
            callback(response)
        } else {
            callback(null)
        }
    }
}

// 发送加入聊天请求
fun TgApi.joinChat(chatId: Long, reInit: () -> Unit) {
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

// 获取用户名
fun TgApi.getUserName(userId: Long, onResult: (String) -> Unit) {
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

// 添加代理
fun TgApi.addProxy(server: String, port: Int, type: TdApi.ProxyType, enable: Boolean = true) {
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
suspend fun TgApi.getChat(chatId: Long): TdApi.Chat? {
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
fun TgApi.logOut() {
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

// 发送消息
fun TgApi.sendMessage(chatId: Long, message: InputMessageContent, replyTo: TdApi.InputMessageReplyTo? = null) {
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
fun TgApi.editMessageText(chatId: Long, messageId: Long, message: InputMessageContent) {
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

// FCM注册设备
fun TgApi.setFCMToken(token: String = "", callback: (Long) -> Unit = {}) {
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
