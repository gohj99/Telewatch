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
import android.content.SharedPreferences
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.utils.telegram.TgApiForPushNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class TdFirebaseMessagingService : FirebaseMessagingService() {
    private val settingsSharedPref: SharedPreferences by lazy {
        getSharedPreferences("app_settings", MODE_PRIVATE)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("Refreshed token: $token")
        // 异步提交
        settingsSharedPref.edit(commit = false) {
            putString("Token_Notification", token)
        }
        if (settingsSharedPref.getBoolean("Use_Notification", false)) {
            TgApiManager.tgApi?.setFCMToken(token) { id ->
                settingsSharedPref.edit(commit = false) {
                    putLong("Id_Notification", id)
                }
                //println(id)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        println("collapseKey: ${remoteMessage.collapseKey}")
        println("From: ${remoteMessage.from}")
        println("messageId: ${remoteMessage.messageId}")
        println("messageType: ${remoteMessage.messageType}")
        println("senderId: ${remoteMessage.senderId}")
        println("remoteMessage.notification: ${remoteMessage.notification}")

        //sendNotification("测试通知", "这是一条测试通知")

        if (settingsSharedPref.getBoolean("Use_Notification", false)) {
            // 检查消息是否包含数据有效载荷。
            if (remoteMessage.data.isNotEmpty()) {
                println("Message data payload: ${remoteMessage.data}")
                //val payloadJson = JSONObject(remoteMessage.data)
                //sendNotification("测试通知1", payloadJson.getString("p"))
                settingsSharedPref.edit(commit = true) {
                    putBoolean("FCM_state", true)
                }

                if (TgApiManager.tgApi == null) {
                    //println("执行测试1")
                    try {
                        val tgApi = TgApiForPushNotification(this)
                        tgApi.getPushReceiverId(JSONObject(remoteMessage.data).toString()) { id->
                            if (id == settingsSharedPref.getLong("userPushReceiverId", 0L)) {
                                tgApi.processPushNotification(JSONObject(remoteMessage.data).toString())
                            }
                            //sendNotification("测试通知2", id.toString())
                        }
                        Thread.sleep(10 * 1000)
                        tgApi.close()
                    } catch (e: Exception) {
                        println(e)
                        e.printStackTrace()
                    }
                }

                // 检查数据是否需要通过长期运行的工作处理
                /*
                if (needsToBeScheduled()) {
                    // 对于长期运行的任务（10秒或更长时间），请使用工人。
                    scheduleJob()
                } else {
                    // 在10秒内处理消息
                    handleNow()
                }
                */
            }
        }


        // 检查消息是否包含通知有效载荷。
        //remoteMessage.notification?.let {
        //    println("Message Notification Body: ${it.body}")
        //}

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    fun sendNotification(title: String, message: String) {
        // 定义通知渠道的唯一标识符（用于 Android Oreo 及以上版本）
        val channelId = "default_channel_id"

        // 获取系统默认的通知声音 URI
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 创建 NotificationCompat.Builder 构建器
        // 传入当前上下文（this）和通知渠道ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // 设置通知图标，确保该图标资源存在
            .setContentTitle(title)                     // 设置通知标题
            .setContentText(message)                    // 设置通知内容
            .setAutoCancel(true)                        // 设置点击后自动取消通知
            .setSound(defaultSoundUri)                  // 设置通知声音

        // 获取系统的 NotificationManager 服务，用于管理通知
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 针对 Android Oreo（API 26）及以上版本创建通知渠道
        // 设置通知渠道的名称
        val channelName = "默认通知渠道"
        // 创建一个 NotificationChannel 对象，传入渠道ID、渠道名称和重要性等级
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        // 可选：为通知渠道设置描述信息
        channel.description = "这是默认通知渠道，用于展示推送通知"
        // 将通知渠道注册到系统 NotificationManager 中
        notificationManager.createNotificationChannel(channel)

        // 使用 NotificationManager 发送通知
        // 第一个参数为通知的唯一ID，通知ID可以用来更新或取消通知（此处使用 0，实际开发中可使用随机数或自定义逻辑生成唯一ID）
        notificationManager.notify(0, notificationBuilder.build())
    }
}