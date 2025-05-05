/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.utils.telegram.markMessagesAsRead
import com.gohj99.telewatch.utils.telegram.sendMessage
import org.drinkless.tdlib.TdApi

private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        if (conversationId == null) {
            Log.e("NotificationAction", "Missing conversation ID in intent")
            return
        }
        val notificationId = conversationId.hashCode()
        val nm = NotificationManagerCompat.from(context)

        when (intent.action) {
            ACTION_MARK_AS_READ -> {
                Log.d("NotificationAction", "Marking conversation $conversationId as read")
                // 在这里实现将对话标记为已读的逻辑
                if (TgApiForPushNotificationManager.tgApi != null) {
                    TgApiForPushNotificationManager.tgApi?.markMessagesAsRead(conversationId.toLongOrNull()?: -1L)
                } else {
                    if (TgApiManager.tgApi != null) {
                        TgApiManager.tgApi?.markMessagesAsRead(conversationId.toLongOrNull()?: -1L)
                    } else {
                        val tgApi = TgApiForPushNotification(context)
                        TgApiForPushNotificationManager.tgApi = tgApi
                        tgApi.markMessagesAsRead(conversationId.toLongOrNull()?: -1L)
                        Thread.sleep(10 * 1000)
                        TgApiForPushNotificationManager.tgApi = null
                        tgApi.close()
                    }

                }

                // 清除此对话的 SharedPreferences 历史
                val convId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
                if (!convId.isNullOrEmpty()) {
                    val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove(convId).apply()
                }

                // 取消通知
                nm.cancel(notificationId)
            }

            ACTION_REPLY -> {
                val replyText = getMessageText(intent) // 获取用户输入的文本
                if (replyText.isNullOrBlank()) {
                    Log.w("NotificationAction", "Reply text is empty for $conversationId")
                    // 可以选择不处理空回复，或者给用户提示
                    // 注意：如果用户点了 Reply 但没输入就发送，这里会是空
                    // 可能需要更新通知告知用户（但现在先直接返回）
                    return
                }

                Log.d("NotificationAction", "Replying to $conversationId with: $replyText")
                // 在这里实现发送回复消息的逻辑

                if (TgApiForPushNotificationManager.tgApi != null) {
                    TgApiForPushNotificationManager.tgApi?.sendMessage(
                        chatId = conversationId.toLongOrNull()?: -1L,
                        message = TdApi.InputMessageText().apply {
                            text = TdApi.FormattedText().apply {
                                this.text = replyText.toString()
                            }
                        }
                    )
                } else {
                    if (TgApiManager.tgApi != null) {
                        TgApiManager.tgApi?.sendMessage(
                            chatId = conversationId.toLongOrNull()?: -1L,
                            message = TdApi.InputMessageText().apply {
                                text = TdApi.FormattedText().apply {
                                    this.text = replyText.toString()
                                }
                            }
                        )
                    } else {
                        val tgApi = TgApiForPushNotification(context)
                        TgApiForPushNotificationManager.tgApi = tgApi
                        tgApi.sendMessage(
                            chatId = conversationId.toLongOrNull()?: -1L,
                            message = TdApi.InputMessageText().apply {
                                text = TdApi.FormattedText().apply {
                                    this.text = replyText.toString()
                                }
                            }
                        )
                        Thread.sleep(10 * 1000)
                        TgApiForPushNotificationManager.tgApi = null
                        tgApi.close()
                    }
                }

                // 发送回复后，通常也应该取消原通知
                // 或者，更好的做法是更新通知，显示“正在发送...”或确认“已发送”
                // 这里为了简单，我们先直接取消
                val convId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
                if (!convId.isNullOrEmpty()) {
                    val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                    prefs.edit().remove(convId).apply()
                }
                nm.cancel(notificationId)

                // (可选) 更新通知状态为 "正在发送..."
                // 这需要重新构建一个类似的通知，可能移除回复按钮，添加一个指示器
                // updateSendingNotification(context, conversationId, notificationId, "Sending: $replyText")

            }
            else -> {
                Log.w("NotificationAction", "Unknown action: ${intent.action}")
            }
        }
    }

    // Helper function to extract reply text from RemoteInput
    private fun getMessageText(intent: Intent): CharSequence? {
        return RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
    }
}
