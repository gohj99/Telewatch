/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.telegram

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.getAppVersion
import com.gohj99.telewatch.loadConfig
import com.gohj99.telewatch.model.NotificationMessage
import com.gohj99.telewatch.utils.getColorById
import com.google.common.reflect.TypeToken
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
    private val PREFS_NAME = "ChatNotificationHistory"
    private val MAX_HISTORY_SIZE = 10 // 保留最近的消息数量

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
                    var bmp = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
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
                                    val itChat = tgApi?.getChat(senderId.chatId)
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
                            messageId = message.id,
                            isGroupChat = isGroup,
                            chatIconBitmap = bmp // 这里可以传入群组图标的 Uri
                        )
                    }
                }
            } catch (e: Exception) {
                println("GetChat request failed (handleNewChat): ${e.message}")
            }
        }
    }

    fun Context.sendChatMessageNotification(
        title: String, // 会话标题
        message: String, // 消息内容
        senderName: String, // 发送者名称
        conversationId: String, // 用于区分不同的聊天会话
        messageId: Long, // 唯一的消息 ID
        isGroupChat: Boolean = false,
        chatIconBitmap: Bitmap // 会话的通知图标 Bitmap
    ) {
        val channelId = "chat_notifications"
        val channelName = "Chat Messages"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Used to display instant chat messages"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<NotificationMessage>>() {}.type

        // 从 SharedPreferences 加载历史消息
        val historyJson = prefs.getString(conversationId, null)
        val messageHistory = historyJson?.let { gson.fromJson<MutableList<NotificationMessage>>(it, type) } ?: mutableListOf()

        // 添加新消息到历史记录
        messageHistory.add(NotificationMessage(message, senderName, System.currentTimeMillis()))

        // 限制历史记录大小
        while (messageHistory.size > MAX_HISTORY_SIZE) {
            messageHistory.removeAt(0)
        }

        // 将更新后的历史记录保存到 SharedPreferences
        prefs.edit { putString(conversationId, gson.toJson(messageHistory)) }

        val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName(title).build())
            .setGroupConversation(isGroupChat)
            .setConversationTitle(if (isGroupChat) title else null)

        for (notificationMessage in messageHistory) {
            val person = Person.Builder().setName(notificationMessage.senderName).build()
            messagingStyle.addMessage(notificationMessage.text, notificationMessage.timestamp, person)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply {
                chatIconBitmap?.let { bitmap ->
                    setSmallIcon(IconCompat.createWithBitmap(bitmap))
                }
            }
            .setStyle(messagingStyle)
            .setContentTitle(if (isGroupChat) title else senderName)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(conversationId)
            .setSortKey(messageId.toString())
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

        if (ActivityCompat.checkSelfPermission(
                this@sendChatMessageNotification,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(conversationId.hashCode(), notificationBuilder.build())
        }
    }

    private fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
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

    /**
     * 根据聊天标题的第一个字母和颜色 ID 生成一个默认聊天图标的 Bitmap
     *
     * @param context 用于访问资源和显示指标
     * @param title 聊天标题，用于获取第一个字母
     * @param accentColorId 用于获取背景颜色
     * @return 生成的 Bitmap
     */
    fun generateChatTitleIconBitmap(
        context: Context,
        title: String,
        accentColorId: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density

        // 转换为像素
        val sizeDp = 35f // 从 35.dp 获取值
        val textSizeSp = 18f // 从 18.sp 获取值

        val sizePx = (sizeDp * density).toInt()
        val textSizePx = (textSizeSp * density).toInt()

        // 创建一个可变的 Bitmap
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888) // ARGB_8888 也可以
        val canvas = Canvas(bitmap)

        // --- 绘制圆形背景 ---
        val circlePaint = Paint().apply {
            color = getColorById(accentColorId).toArgb() // 使用 getColorById 获取颜色并转换为 ARGB
            style = Paint.Style.FILL
            isAntiAlias = true // 启用抗锯齿
        }

        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = sizePx / 2f // 半径就是一半的尺寸

        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // --- 绘制文本 ---
        val textPaint = Paint().apply {
            color = Color.White.toArgb() // 文字颜色为白色
            textSize = textSizePx.toFloat() // 文字大小
            textAlign = Paint.Align.CENTER // 设置文本对齐方式为中心
            isAntiAlias = true
            // 您可能还需要设置字体，Compose 默认使用系统字体
            // typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // 确定要显示的文本（第一个大写字母），处理空字符串情况
        val text = title
            .takeIf { it.isNotEmpty() } // 如果标题不为空
            ?.get(0) // 获取第一个字符
            ?.uppercaseChar() // 转换为大写字符
            ?.toString() // 转换为字符串
            ?: "?" // 如果标题为空，使用 "?" 作为默认文本

        // 计算文本的基线位置，使其在垂直方向上居中
        // 对于 Paint.Align.CENTER，drawText 的 x 坐标是水平中心，y 坐标是基线位置
        // 文本高度 = ascent + descent (ascent 是负数)
        // 文本的半高 = (descent - ascent) / 2
        // 垂直居中的 y 坐标（基于中心）= (文本的半高) - descent
        // 基线 y 坐标 = centerY + 垂直居中的 y 坐标（基于中心）
        val fontMetrics = textPaint.fontMetrics
        val textBaseLineY = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f


        canvas.drawText(text, centerX, textBaseLineY, textPaint)

        return bitmap
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