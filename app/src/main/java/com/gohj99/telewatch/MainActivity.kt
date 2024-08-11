/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.MainScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TgApiManager {
    @SuppressLint("StaticFieldLeak")
    var tgApi: TgApi? = null
}

class MainActivity : ComponentActivity() {
    private var isLoggedIn: Boolean = false
    private var exceptionState by mutableStateOf<Exception?>(null)
    private var chatsList = mutableStateOf(listOf<Chat>())
    private var settingList = mutableStateOf(listOf<String>())

    override fun onDestroy() {
        super.onDestroy()
        TgApiManager.tgApi?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelewatchTheme {
                SplashLoadingScreen()
            }
        }

        settingList.value = listOf(
            getString(R.string.About),
            getString(R.string.setting_all)
        )

        val sharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
        isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startWelcomeActivity()
        } else {
            initMain()
        }
    }

    private fun startWelcomeActivity() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    private fun initMain() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //println("开始启动Main")
                TgApiManager.tgApi = TgApi(
                    this@MainActivity,
                    chatsList = chatsList
                )
                //println("实例化TgApi")
                TgApiManager.tgApi?.getChats(
                    limit = 10
                )
                //println("获取消息列表")
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            MainScreen(
                                chats = chatsList,
                                chatPage = { chat ->
                                    startActivity(
                                        Intent(this@MainActivity, ChatActivity::class.java).apply {
                                            putExtra("chat", chat)
                                        }
                                    )
                                },
                                settingList = settingList,
                                settingCallback = {
                                    when (it) {
                                        getString(R.string.setting_all) -> {
                                            startActivity(
                                                Intent(
                                                    this@MainActivity,
                                                    SettingActivity::class.java
                                                )
                                            )
                                        }

                                        getString(R.string.About) -> {
                                            startActivity(
                                                Intent(this@MainActivity, AboutActivity::class.java)
                                            )
                                        }
                                    }
                                },
                                getContacts = { contacts ->
                                    TgApiManager.tgApi!!.getContacts(contacts)
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                exceptionState = e
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            ErrorScreen(
                                onRetry = { retryInitialization() },
                                onSetting = {
                                    startActivity(
                                        Intent(this@MainActivity, SettingActivity::class.java)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun retryInitialization() {
        exceptionState = null
        initMain()
    }
}
