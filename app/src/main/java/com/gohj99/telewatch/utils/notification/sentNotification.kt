/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.notification

import android.Manifest
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
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.NotificationMessage
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

private const val ACTION_CLEAR_CHAT_HISTORY = "com.gohj99.telewatch.ACTION_CLEAR_CHAT_HISTORY"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
private const val MAX_HISTORY_SIZE = 10 // 保留最近的消息数量
private const val PREFS_NAME = "chat_prefs"
private const val CHANNEL_ID = "chat_notifications"
private const val CHANNEL_NAME = "Chat Messages"

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

    // —— 1. 创建或确认 Channel ——
    val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Instant chat message notification"
        enableLights(true)
        enableVibration(true)
    }
    nm.createNotificationChannel(ch)

    // —— 2. 读写 SharedPreferences ——
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val type = object : TypeToken<MutableList<NotificationMessage>>() {}.type
    val historyJson = prefs.getString(conversationId, null)
    val messageHistory: MutableList<NotificationMessage> =
        historyJson?.let { gson.fromJson(it, type) } ?: mutableListOf()

    // 添加新消息、按时间排序、截断
    messageHistory.add(NotificationMessage(message, senderName, timestamp))
    messageHistory.sortBy { it.timestamp }
    while (messageHistory.size > MAX_HISTORY_SIZE) {
        messageHistory.removeAt(0)
    }
    prefs.edit { putString(conversationId, gson.toJson(messageHistory)) }

    // —— 3. 构造 MessagingStyle ——
    val style = NotificationCompat.MessagingStyle(
        Person.Builder().setName(title).build()
    ).also { s ->
        s.setGroupConversation(isGroupChat)
        if (isGroupChat) s.setConversationTitle(title)
        messageHistory.forEach { msg ->
            val person = Person.Builder().setName(msg.senderName).build()
            s.addMessage(msg.text, msg.timestamp, person)
        }
    }

    // —— 4. 构造 DeleteIntent ——
    val deleteIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
        action = ACTION_CLEAR_CHAT_HISTORY
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val deletePending = PendingIntent.getBroadcast(
        this,
        conversationId.hashCode(),
        deleteIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // —— 5. 发通知 ——
    val notif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setLargeIcon(chatIconBitmap)
        .setStyle(style)
        .setContentTitle(if (isGroupChat) title else senderName)
        .setContentText(messageHistory.last().text)
        .setGroup(conversationId)
        .setGroupSummary(false)
        .setAutoCancel(true)
        .setDeleteIntent(deletePending)           // ← 用户滑动删除时触发
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .build()

    nm.notify(conversationId.hashCode(), notif)
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun Context.sendCompatChatNotification(
    title: String,
    message: String,
    senderName: String,
    conversationId: String,
    timestamp: Long,
    isGroupChat: Boolean = false,
    chatIconBitmap: Bitmap? = null,
    fallbackIconRes: Int = R.mipmap.ic_launcher
) {
    // 1. 创建/确认 Channel
    val mgr = getSystemService(NotificationManager::class.java)!!
    mgr.createNotificationChannel(
        NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Compatibility chat message notifications"
            enableLights(true)
            enableVibration(true)
        }
    )

    // 2. 存取历史消息
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val listType = object : TypeToken<MutableList<NotificationMessage>>() {}.type
    val historyJson = prefs.getString(conversationId, null)
    val history: MutableList<NotificationMessage> =
        historyJson?.let { gson.fromJson(it, listType) } ?: mutableListOf()

    history.add(NotificationMessage(message, senderName, timestamp))
    history.sortBy { it.timestamp }
    while (history.size > MAX_HISTORY_SIZE) history.removeAt(0)
    prefs.edit { putString(conversationId, gson.toJson(history)) }

    // 3. 构造 MessagingStyle
    val style = NotificationCompat.MessagingStyle(
        Person.Builder().setName(title).build()
    ).also { s ->
        s.setGroupConversation(isGroupChat)
        if (isGroupChat) s.setConversationTitle(title)
        history.forEach { msg ->
            val person = Person.Builder().setName(msg.senderName).build()
            s.addMessage(msg.text, msg.timestamp, person)
        }
    }

    // 4. 构建滑动删除广播 Intent
    val delIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
        action = ACTION_CLEAR_CHAT_HISTORY
        putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    val delPI = PendingIntent.getBroadcast(
        this,
        conversationId.hashCode(),
        delIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 5. 构造并发通知
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(fallbackIconRes)
        .setContentTitle(if (isGroupChat) title else senderName)
        .setContentText(history.lastOrNull()?.text ?: message)
        .setAutoCancel(true)
        .setDeleteIntent(delPI)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setStyle(style)
        .setGroup(conversationId)
        .setGroupSummary(false)

    chatIconBitmap?.let { builder.setLargeIcon(it) }

    NotificationManagerCompat.from(this)
        .notify(conversationId.hashCode(), builder.build())
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
