/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gohj99.telewatch.R
import com.gohj99.telewatch.getAppVersion
import com.gohj99.telewatch.loadConfig
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

        // 异步获取聊天标题
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatResult = sendRequest(TdApi.GetChat(chatId))
                if (chatResult.constructor == TdApi.Chat.CONSTRUCTOR) {
                    //val accentColorId = chatResult.accentColorId
                    val needNotification = chatResult.notificationSettings.muteFor == 0
                    val chatTitle = chatResult.title

                    if (needNotification) {
                        sendNotification(chatTitle, handleAllMessages(message))
                    }
                }
            } catch (e: Exception) {
                println("GetChat request failed (handleNewChat): ${e.message}")
            }
        }
    }

    fun sendNotification(title: String, message: String) {
        // 定义通知渠道的唯一标识符（用于 Android Oreo 及以上版本）
        val channelId = "default_channel_id"

        // 获取系统默认的通知声音 URI
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 创建 NotificationCompat.Builder 构建器
        // 传入当前上下文（this）和通知渠道ID
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // 设置通知图标，确保该图标资源存在
            .setContentTitle(title)                     // 设置通知标题
            .setContentText(message)                    // 设置通知内容
            .setAutoCancel(true)                        // 设置点击后自动取消通知
            .setSound(defaultSoundUri)                  // 设置通知声音

        // 获取系统的 NotificationManager 服务，用于管理通知
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 针对 Android Oreo（API 26）及以上版本创建通知渠道
        // 设置通知渠道的名称
        val channelName = "默认通知渠道"
        // 创建一个 NotificationChannel 对象，传入渠道ID、渠道名称和重要性等级
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        // 可选：为通知渠道设置描述信息
        channel.description = "这是默认通知渠道，用于展示推送通知"
        // 将通知渠道注册到系统 NotificationManager 中
        notificationManager.createNotificationChannel(channel)

        // 使用 NotificationManager 发送通知
        // 第一个参数为通知的唯一ID，通知ID可以用来更新或取消通知（此处使用 0，实际开发中可使用随机数或自定义逻辑生成唯一ID）
        notificationManager.notify(0, notificationBuilder.build())
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