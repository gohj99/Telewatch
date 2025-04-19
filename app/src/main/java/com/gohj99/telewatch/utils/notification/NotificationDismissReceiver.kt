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

private const val ACTION_CLEAR_CHAT_HISTORY = "com.gohj99.telewatch.ACTION_CLEAR_CHAT_HISTORY"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLEAR_CHAT_HISTORY) {
            val convId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
            val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove(convId).apply()
        }
    }
}