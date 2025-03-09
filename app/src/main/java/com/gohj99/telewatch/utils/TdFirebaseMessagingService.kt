/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TdFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // 没有收到消息吗？看看为什么这可能是： https://goo.gl/39bRNJ
        println("From: ${remoteMessage.from}")

        // 检查消息是否包含数据有效载荷。
        if (remoteMessage.data.isNotEmpty()) {
            println("Message data payload: ${remoteMessage.data}")

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

        // 检查消息是否包含通知有效载荷。
        remoteMessage.notification?.let {
            println("Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
}