/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.gohj99.telewatch.ChatActivity
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.model.Chat

fun urlHandle(url: String, context: Context, callback: ((Boolean) -> Unit)? = null) {
    val username = parseUsername(url)
    if (username != null) {
        TgApiManager.tgApi!!.searchPublicChat(username) { tdChat ->
            //println(tdChat)
            if (tdChat != null) {
                callback?.invoke(true)
                context.startActivity(
                    Intent(context, ChatActivity::class.java).apply {
                        putExtra("chat", Chat(
                            id = tdChat.id,
                            title = tdChat.title
                            )
                        )
                    }
                )
            } else {
                println("Unable to get username: $username")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, context.getString(R.string.Unable_get_username), Toast.LENGTH_SHORT).show()
                }
            }
        }
    } else {
        callback?.invoke(false)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val packageManager: PackageManager = context.packageManager
        val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        if (activities.isNotEmpty()) {
            context.startActivity(intent)
        } else {
            // 处理没有可用浏览器的情况
            Toast.makeText(context, context.getString(R.string.No_app_to_handle_this_url), Toast.LENGTH_SHORT).show()
        }
    }
}
