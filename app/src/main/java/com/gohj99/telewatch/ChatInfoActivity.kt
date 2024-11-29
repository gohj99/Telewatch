/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.SplashChatInfoScreen
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.utils.telegram.TgApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi

class ChatInfoActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var chat: Chat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 显示加载页面
        setContent {
            TelewatchTheme {
                SplashLoadingScreen(modifier = Modifier.fillMaxSize())
            }
        }

        tgApi = TgApiManager.tgApi

        // 接收传递的 Chat 对象
        chat = intent.getParcelableExtra("chat")

        // 如果 chat 为 null，直接退出页面
        if (chat == null) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                init()
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            ErrorScreen(
                                onRetry = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        init()
                                    }
                                },
                                cause = e.message ?: ""
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun init() {
        chat?.let { safeChat ->
            val chatObject = tgApi!!.getChat(safeChat.id)

            //println("获取到的chatObject")
            //println(chatObject)
            // 副标题
            var subtitle = getString(R.string.Unknown)
            // 详细信息
            var info = ""
            val chatType = chatObject?.type
            when(chatType) {
                // 私人聊天
                is TdApi.ChatTypePrivate -> {
                    subtitle = getString(R.string.Private_Chat)
                    val userInfo = runBlocking {
                        tgApi!!.getUser(chatType.userId)
                    }
                    if (userInfo != null) {
                        when(userInfo.status) {
                            is TdApi.UserStatusOnline ->
                                subtitle = getString(R.string.Online)
                            is TdApi.UserStatusEmpty ->
                                subtitle = getString(R.string.Unknown)
                            is TdApi.UserStatusRecently ->
                                subtitle = getString(R.string.Lately)
                            is TdApi.UserStatusLastWeek ->
                                subtitle = getString(R.string.Last_week)
                            is TdApi.UserStatusLastMonth ->
                                subtitle = getString(R.string.Last_month)
                            is TdApi.UserStatusOffline ->
                                subtitle = getString(R.string.Offline)
                        }
                        if (userInfo.phoneNumber != "") {
                            info += "\n#### ${getString(R.string.phoneNumber)}: +${userInfo.phoneNumber}"
                        }
                        if (userInfo.usernames != null) {
                            info += "\n#### ${getString(R.string.username)}: @${userInfo.usernames!!.activeUsernames[0]}"
                        }
                    }
                }

                // 普通群组
                is TdApi.ChatTypeBasicGroup -> {
                    //println("普通群组")
                    subtitle = getString(R.string.Group_Chat)
                    val groupInfo = runBlocking {
                        tgApi!!.getBasicGroup(chatType.basicGroupId)
                    }
                    //println(groupInfo)
                    if (groupInfo != null) {
                        subtitle = "${groupInfo.memberCount} ${getString(R.string.Member)}"
                    }
                    val groupFullInfo = runBlocking {
                        tgApi!!.getBasicGroupFullInfo(chatType.basicGroupId)
                    }
                    //println(groupFullInfo)
                    if (groupFullInfo != null) {
                        if (groupFullInfo.inviteLink != null) {
                            if (groupFullInfo.inviteLink!!.inviteLink != "") {
                                info += "\n#### ${getString(R.string.Invite_Link)}: ${groupFullInfo.inviteLink!!.inviteLink}"
                            }
                        }
                        if (groupFullInfo.description != "") {
                            info += "\n#### ${getString(R.string.Description)}: ${groupFullInfo.description}"
                        }
                    }
                }

                // 超级群组
                is TdApi.ChatTypeSupergroup -> {
                    println("超级群组")
                    subtitle = getString(R.string.Supergroup_Chat)
                    val supergroupInfo = runBlocking {
                        tgApi!!.getSupergroup(chatType.supergroupId)
                    }
                    if (chatType.isChannel) {
                        // 频道
                        subtitle = getString(R.string.Channel)
                        if (supergroupInfo != null) {
                            subtitle =
                                "${supergroupInfo.memberCount} ${getString(R.string.Subscribers)}"
                        }
                    } else {
                        if (supergroupInfo != null) {
                            subtitle =
                                "${supergroupInfo.memberCount} ${getString(R.string.Member)}"
                        }
                    }

                    if (supergroupInfo != null) {
                        // 公开链接
                        if (supergroupInfo.usernames != null) {
                            info += "\n#### ${getString(R.string.url)}\nhttps://t.me/${supergroupInfo.usernames!!.activeUsernames[0]}"
                        }
                        // 获取超级组详细信息
                        val supergroupFullInfo = runBlocking {
                            tgApi!!.getSupergroupFullInfo(chatType.supergroupId)
                        }
                        if (supergroupFullInfo != null) {
                            // 超级组简介
                            if (supergroupFullInfo.description != "") {
                                info += "\n#### ${getString(R.string.Description)}\n${supergroupFullInfo.description}"
                            }
                        }
                    }
                }

            }
            runOnUiThread {
                setContent {
                    TelewatchTheme {
                        SplashChatInfoScreen(
                            chatObject = chatObject!!,
                            subtitle = subtitle,
                            info = info
                        )
                    }
                }
            }
            }
    }
}
