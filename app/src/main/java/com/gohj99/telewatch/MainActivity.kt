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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.main.Chat
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.MainScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.JsonObject
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
    private var settingList = mutableStateOf(listOf<SettingItem>())
    private var topTitle = mutableStateOf("")

    override fun onDestroy() {
        super.onDestroy()
        TgApiManager.tgApi?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Firebase Analytics
        val settingsSharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!settingsSharedPref.contains("Data_Collection")) {
            startActivity(
                Intent(
                    this,
                    AllowDataCollectionActivity::class.java
                )
            )
            finish()
        } else {
            val dataCollection = settingsSharedPref.getBoolean("Data_Collection", false)
            val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            firebaseAnalytics.setAnalyticsCollectionEnabled(dataCollection)

            // 显示启动页面
            setContent {
                TelewatchTheme {
                    SplashScreen()
                }
            }

            // 使用 Handler 延迟启动主逻辑
            Handler(Looper.getMainLooper()).postDelayed({
                initializeApp()
            }, 600) // 延迟
        }
    }

    private fun initializeApp() {
        topTitle.value = getString(R.string.HOME)

        val loginSharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
        isLoggedIn = loginSharedPref.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startWelcomeActivity()
        } else {
            // 显示加载页面
            setContent {
                TelewatchTheme {
                    SplashLoadingScreen(modifier = Modifier.fillMaxSize())
                }
            }
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
                val gson = Gson()
                val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
                var userList = sharedPref.getString("userList", "")
                if (userList == "") {
                    val tempChatsList = mutableStateOf(listOf<Chat>())
                    val tempTgApi = TgApi(
                        this@MainActivity,
                        chatsList = tempChatsList,
                        topTitle = topTitle
                    )

                    // 调用重试机制来获取用户信息
                    var currentUser = fetchCurrentUserWithRetries(
                        tgApi = tempTgApi,
                        repeatTimes = 10  // 最多重试10次
                        // 每次失败后等待1秒
                    )

                    if (currentUser == null) {
                        currentUser = tempTgApi.getCurrentUser()
                    }

                    val jsonObject = JsonObject()
                    jsonObject.addProperty(
                        currentUser[0],
                        currentUser[1]
                    )
                    userList = jsonObject.toString()
                    with(sharedPref.edit()) {
                        putString("userList", userList)
                        apply()
                    }
                    tempTgApi.close()
                }

                val jsonObject: JsonObject = gson.fromJson(userList, JsonObject::class.java)
                val accounts = mutableListOf<SettingItem>()
                var a = 0
                for (account in jsonObject.keySet()) {
                    if (a == 0) {
                        accounts.add(
                            SettingItem.Click(
                                itemName = jsonObject.get(account.toString()).asString,
                                onClick = {},
                                color = Color(0xFF2C323A)
                            )
                        )
                    } else {
                        accounts.add(
                            SettingItem.Click(
                                itemName = jsonObject.get(account.toString()).asString,
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            SwitchAccountActivity::class.java
                                        ).putExtra("account", account)
                                    )
                                }
                            )
                        )
                    }
                    a += 1
                }
                accounts.addAll(listOf<SettingItem>(
                    SettingItem.Click(
                        itemName = getString(R.string.Add_Account),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Log_out),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    ConfirmLogoutActivity::class.java
                                )
                            )
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Check_Update),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    CheckUpdateActivity::class.java
                                )
                            )
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.About),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    AboutActivity::class.java
                                )
                            )
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.setting_all),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    SettingActivity::class.java
                                )
                            )
                        }
                    )
                )
                )
                settingList.value = accounts

                TgApiManager.tgApi = TgApi(
                    this@MainActivity,
                    chatsList = chatsList,
                    UserId = jsonObject.keySet().firstOrNull().toString(),
                    topTitle = topTitle
                )
                TgApiManager.tgApi?.loadChats(15)
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
                                getContacts = { contacts ->
                                    TgApiManager.tgApi!!.getContacts(contacts)
                                },
                                topTitle = topTitle
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                exceptionState = e
                Log.e("MainActivity", "Error initializing app: ${e.message}")
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            ErrorScreen(
                                onRetry = { retryInitialization() },
                                onSetting = {
                                    startActivity(
                                        Intent(this@MainActivity, SettingActivity::class.java)
                                    )
                                },
                                cause = e.message ?: ""
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchCurrentUserWithRetries(
        tgApi: TgApi,
        repeatTimes: Int
    ): List<String>? {
        repeat(repeatTimes) {
            val currentUser = tgApi.getCurrentUser()  // 假设这个是挂起函数
            // 成功获取到用户
            //println("成功获取用户: $currentUser，提前退出循环。")
            return currentUser
        }
        return null // 尝试多次失败，返回 null
    }

    private fun retryInitialization() {
        exceptionState = null
        initMain()
    }
}

// 扩展函数，用于在 JsonObject 前添加新的键值对
fun JsonObject.firstAdd(key: String, value: String) {
    // 创建临时的 JsonObject 来保存当前的数据
    val tempJsonObject = JsonObject()

    // 将当前 JsonObject 的键值对复制到临时对象中
    for (entry in this.entrySet()) {
        tempJsonObject.add(entry.key, entry.value)
    }

    // 清空当前 JsonObject
    this.entrySet().clear()

    // 先添加新的键值对到当前对象
    this.addProperty(key, value)

    // 将临时对象的数据重新添加到当前对象
    for (entry in tempJsonObject.entrySet()) {
        this.add(entry.key, entry.value)
    }
}

// 初始化 Firebase Analytics
fun initFirebaseAnalytics(context: Context) {
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    if (!settingsSharedPref.contains("Data_Collection")) {
        context.startActivity(
            Intent(
                context,
                AllowDataCollectionActivity::class.java
            )
        )
    } else {
        val dataCollection = settingsSharedPref.getBoolean("Data_Collection", false)
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        firebaseAnalytics.setAnalyticsCollectionEnabled(dataCollection)
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // 居中对齐
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Splash Icon"
        )
    }
}
