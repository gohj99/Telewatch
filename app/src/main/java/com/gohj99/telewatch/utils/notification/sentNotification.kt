/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.gohj99.telewatch.MainActivity
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.NotificationMessage
import com.google.gson.Gson

private const val ACTION_CLEAR_CHAT_HISTORY = "com.gohj99.telewatch.ACTION_CLEAR_CHAT_HISTORY"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
private const val MAX_HISTORY_SIZE = 10 // 保留最近的消息数量
private const val PREFS_NAME = "chat_prefs"
private const val CHANNEL_ID = "chat_notifications"
private const val CHANNEL_NAME = "Chat Messages"

// 用于 Intent Action 和 Extra
const val ACTION_MARK_AS_READ = "com.gohj99.telewatch.ACTION_MARK_AS_READ"
const val ACTION_REPLY = "com.gohj99.telewatch.ACTION_REPLY"
const val KEY_TEXT_REPLY = "key_text_reply" // RemoteInput 的 Key

fun Context.sendChatMessageNotification(
    title: String,
    message: String,
    senderName: String,
    conversationId: String,
    timestamp: Long,
    isGroupChat: Boolean = false,
    chatIconBitmap: Bitmap
) {
    val nm = getSystemService(NotificationManager::class.java)!!
    val notificationId = conversationId.hashCode() // 使用 conversationId 的哈希值作为通知 ID

    // —— 1. 创建或确认 Channel ——
    val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Instant chat message notification"
        enableLights(true)
        enableVibration(true)
    }
    nm.createNotificationChannel(ch)


    // —— 2. 读写 SharedPreferences (消息历史) ——
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val historyJson = prefs.getString(conversationId, null)
    val array = gson.fromJson(historyJson, Array<NotificationMessage>::class.java)
    val messageHistory = array?.toMutableList() ?: mutableListOf()

    messageHistory.add(NotificationMessage(message, senderName, timestamp))
    messageHistory.sortBy { it.timestamp }
    while (messageHistory.size > MAX_HISTORY_SIZE) {
        messageHistory.removeAt(0)
    }
    prefs.edit { putString(conversationId, gson.toJson(messageHistory)) }


    // —— 3. 构造 MessagingStyle ——
    // 注意: Person 的 Key 最好是唯一的标识符，如果 senderName 可能重复，考虑使用 senderId
    val userPerson = Person.Builder().setName("You").setKey("user_self").build() // 定义当前用户
    val style = NotificationCompat.MessagingStyle(userPerson).also { s -> // 使用当前用户作为 MessagingStyle 的 user
        s.setGroupConversation(isGroupChat)
        if (isGroupChat) {
            s.setConversationTitle(title)
        }
        messageHistory.forEach { msg ->
            // 假设有一个方法可以根据 senderName 获取唯一的 senderId
            val senderId = msg.senderName // 理想情况下这里用 senderId
            val person = Person.Builder().setName(msg.senderName).setKey(senderId).build()
            s.addMessage(msg.text, msg.timestamp, person)
        }
    }

    // --- 4. 创建 PendingIntents for Actions ---

    // 标记已读 Intent
    val markReadIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = ACTION_MARK_AS_READ
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    // 使用不同的 request code 区分 PendingIntent
    val markReadPendingIntent = PendingIntent.getBroadcast(
        this,
        notificationId + 1, // Request Code 1
        markReadIntent,
        // FLAG_IMMUTABLE 是推荐的，如果不需要修改 Intent 中的 extra
        // FLAG_UPDATE_CURRENT 确保如果通知更新，PendingIntent 也更新
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 回复 Intent
    val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = ACTION_REPLY
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val replyPendingIntent = PendingIntent.getBroadcast(
        this,
        notificationId + 2, // Request Code 2
        replyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // 需要 Mutable 因为 RemoteInput 会添加数据
        // 或者 PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE (如果目标 SDK >= 31 且 Receiver 不需要修改 Intent)
        // 对于 RemoteInput, 通常需要 FLAG_MUTABLE 或者在 Receiver 中处理
    )

    // --- 5. 创建 RemoteInput for Reply Action ---
    val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
        setLabel("Reply") // 输入框提示文字
        build()
    }

    // --- 6. 创建 Notification Actions ---
    val markReadAction = NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_view,
        getString(R.string.mark_as_read),
        markReadPendingIntent
    ).build()

    val replyAction = NotificationCompat.Action.Builder(
        // R.drawable.ic_reply, // 替换为你的回复图标
        android.R.drawable.ic_menu_send,
        getString(R.string.Reply),
        replyPendingIntent
    )
        .addRemoteInput(remoteInput) // 添加 RemoteInput 到 Action
        .setAllowGeneratedReplies(true) // 允许系统建议快速回复 (可选)
        .build()


    // —— 7. 构造 DeleteIntent (滑动清除时) ——
    val deleteIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
        action = ACTION_CLEAR_CHAT_HISTORY // 使用 Receiver 中的常量
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val deletePending = PendingIntent.getBroadcast(
        this,
        notificationId, // Request Code 0 (与 action 的区分开)
        deleteIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // —— 8. 发通知 (添加了 Actions) ——
    // 点击通知主体的 Intent (通常是打开对应的聊天界面)
    val openChatIntent = Intent(this, MainActivity::class.java).apply {
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val openChatPendingIntent = PendingIntent.getActivity(this, notificationId + 3, openChatIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notifBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的应用小图标
        .setLargeIcon(chatIconBitmap)
        .setStyle(style)
        .setWhen(timestamp) // 显示消息时间戳
        // .setContentTitle(if (isGroupChat) title else senderName) // MessagingStyle 会处理标题，这里可以不设置或用作备用
        // .setContentText(messageHistory.lastOrNull()?.text ?: "") // MessagingStyle 会处理内容，这里可以不设置或用作备用
        .setGroup(conversationId) // 用于通知分组
        .setGroupSummary(false) // 此通知不是分组摘要
        .setAutoCancel(true) // 点击通知主体时自动取消
        .setDeleteIntent(deletePending) // 用户滑动删除时触发
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setColor(getColor(android.R.color.holo_blue_bright)) // 设置强调颜色 (可选)
        .setOnlyAlertOnce(true) // 同一个会话的新消息只响铃/震动一次 (直到通知被取消)
        .setContentIntent(openChatPendingIntent) // 设置点击通知的操作

        // 添加 Actions
        .addAction(markReadAction)
        .addAction(replyAction)


    // 没有设置 ContentTitle/ContentText 时，需要设置 Ticker 或确保 Style 不为 null
    notifBuilder.setContentTitle(if (isGroupChat) title else senderName)
    notifBuilder.setContentText(messageHistory.lastOrNull()?.text ?: "")
    notifBuilder.setTicker("$senderName: ${messageHistory.lastOrNull()?.text ?: ""}") // 旧版本状态栏滚动提示

    // 发送通知
    val notificationManager = NotificationManagerCompat.from(this)
    if (notificationManager.areNotificationsEnabled()) {
        notificationManager.notify(notificationId, notifBuilder.build())
    }
}

fun drawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    return when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is AdaptiveIconDrawable -> {
            // 创建一个空 Bitmap，把 AdaptiveIconDrawable 绘制上去
            val width  = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        else -> {
            // 其他类型的 drawable（VectorDrawable 之类）
            val width  = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
}
