/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.notification

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.getAppVersion
import com.gohj99.telewatch.loadConfig
import com.gohj99.telewatch.utils.generateChatTitleIconBitmap
import com.gohj99.telewatch.utils.telegram.getChat
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch

class TgApiForPushNotification(private val context: Context) {
    private val sharedPref = context.getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
    private val client: Client = Client.create({ update -> handleUpdate(update) }, null, null)
    private val userList = sharedPref.getString("userList", "")
    private val gson = Gson()
    @Volatile private var isAuthorized: Boolean = false
    private val authLatch = CountDownLatch(1)
    private var currentUser: List<String> = emptyList()
    private var userId = ""

    init {
        // 获取用户ID
        userId = if (userList != null && userList.isNotEmpty()) {
            gson.fromJson(userList, JsonObject::class.java)
                .keySet().firstOrNull().toString()
        } else throw IllegalStateException("User list is empty or null")
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
            else -> {
                Log.d("TdApiUpdate","Received update: $update")
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

    // 处理获取到的新消息
    private fun handleNewMessage(update: TdApi.UpdateNewMessage) {
        val message = update.message
        val chatId = message.chatId

        if ((message.senderId as TdApi.MessageSenderUser).userId == userId.toLong()) {
            return // 如果消息是自己发送的，则不处理
        }
        // 异步获取聊天标题和聊天信息
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatResult = sendRequest(TdApi.GetChat(chatId))
                if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {

                    // 判断是否是群组
                    var isGroup = false
                    when (chatResult.type) {
                        is TdApi.ChatTypeSupergroup -> {
                            isGroup = true
                        }
                        is TdApi.ChatTypeBasicGroup -> {
                            isGroup = true
                        }
                    }

                    // 获取聊天图片
                    var bmp = drawableToBitmap(context, R.mipmap.ic_launcher)!!
                    val photoFile = chatResult.photo?.small
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
                            chatResult.title,
                            chatResult.accentColorId
                        )
                    }

                    //val accentColorId = chatResult.accentColorId
                    val needNotification = chatResult.notificationSettings.muteFor == 0
                    val chatTitle = chatResult.title

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
                                    val itChat = TgApiManager.tgApi?.getChat(senderId.chatId)
                                    itChat.let {
                                        senderName = it!!.title
                                    }
                                }
                            }
                        }
                    }

                    if (needNotification) {
                        context.sendChatMessageNotification(
                            title = chatTitle,
                            message = handleAllMessages(message),
                            senderName = senderName,
                            conversationId = chatId.toString(),
                            timestamp = message.date * 1000L,
                            isGroupChat = isGroup,
                            chatIconBitmap = bmp // 这里可以传入群组图标的 Uri
                        )
                    }
                }
            } catch (e: Exception) {
                println("HandleNewChat failed: ${e.message}")
            }
        }
    }

    private fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
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

    // 标记已读
    fun markMessagesAsRead(chatId: Long, forceRead: Boolean = true) {
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

    // 发送消息
    fun sendMessage(chatId: Long, message: TdApi.InputMessageContent, replyTo: TdApi.InputMessageReplyTo? = null) {
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

    // 处理和简化消息
    fun handleAllMessages(
        message: TdApi.Message? = null,
        messageContext: TdApi.MessageContent? = null,
        maxText: Int = 64
    ): String {
        val content: TdApi.MessageContent = messageContext ?: message?.content
        ?: return context.getString(R.string.Unknown_Message)

        return when (content) {
            is TdApi.MessageText -> {
                val text = content.text.text.replace('\n', ' ')
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessagePhoto -> {
                val caption = content.caption.text.replace('\n', ' ')
                val text = context.getString(R.string.Photo) + " " + caption
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessageVideo -> {
                val caption = content.caption.text.replace('\n', ' ')
                val text = context.getString(R.string.Video) + " " + caption
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessageVoiceNote -> {
                val caption = content.caption.text.replace('\n', ' ')
                val text = context.getString(R.string.Voice) + " " + caption
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessageAnimation -> {
                val caption = content.caption.text.replace('\n', ' ')
                val text = context.getString(R.string.Animation) + " " + caption
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessageDocument -> {
                val caption = content.document.fileName.replace('\n', ' ') + content.caption.text.replace('\n', ' ')
                val text = context.getString(R.string.File) + " " + caption
                if (text.length > maxText) text.take(maxText) + "..." else text
            }
            is TdApi.MessageAnimatedEmoji -> {
                if (content.emoji.isEmpty()) context.getString(R.string.Unknown_Message)
                else content.emoji
            }
            is TdApi.MessageSticker -> {
                if (content.sticker.emoji.isEmpty()) context.getString(R.string.Unknown_Message)
                else content.sticker.emoji
            }
            else -> context.getString(R.string.Unknown_Message)
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

    // 关闭连接
    fun close() {
        println("Closing client")
        runBlocking {
            sendRequest(TdApi.Close())
        }
    }
}