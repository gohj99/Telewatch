/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.model.SettingItem
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.MainScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.utils.telegram.TgApi
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


object TgApiManager {
    @SuppressLint("StaticFieldLeak")
    var tgApi: TgApi? = null
}

object ChatsListManager {
    @SuppressLint("StaticFieldLeak")
    var chatsList: MutableState<List<Chat>> = mutableStateOf(listOf())
}

class MainActivity : ComponentActivity() {
    private var isLoggedIn: Boolean = false
    private var exceptionState by mutableStateOf<Exception?>(null)
    private var chatsList = mutableStateOf(listOf<Chat>())
    private var chatsFoldersList = mutableStateOf(listOf<TdApi.ChatFolder>())
    private var settingList = mutableStateOf(listOf<SettingItem>())
    private var topTitle = mutableStateOf("")
    private val contacts = mutableStateOf(listOf<Chat>())
    private val currentUserId = mutableStateOf(-1L)
    private val settingsSharedPref: SharedPreferences by lazy {
        getSharedPreferences("app_settings", MODE_PRIVATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        TgApiManager.tgApi?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 获取数据收集配置
        if (!settingsSharedPref.contains("Data_Collection")) {
            startActivity(
                Intent(
                    this,
                    AllowDataCollectionActivity::class.java
                )
            )
            finish()
        } else {
            initializeApp()
            /*
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
            */
        }
    }

    private fun initializeApp() {
        topTitle.value = getString(R.string.HOME)

        val loginSharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
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

            CoroutineScope(Dispatchers.IO).launch {
                checkAndUpdateConfiguration(this)
            }

            if (!settingsSharedPref.getBoolean("Remind1_read", false)) {
                startActivity(
                    Intent(
                        this,
                        RemindActivity::class.java
                    )
                )
            }

            initMain()
        }
    }

    private fun startWelcomeActivity() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    private fun initMain() {
        val config = loadConfig(this)
        if (config.getProperty("BETA") == "true") {
            startActivity(
                Intent(
                    this,
                    IsBetaActivity::class.java
                )
            )
        }
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
                        topTitle = topTitle,
                        chatsFoldersList = chatsFoldersList
                    )

                    // 调用重试机制来获取用户信息
                    var currentUser = fetchCurrentUserWithRetries(
                        tgApi = tempTgApi,
                        repeatTimes = 10  // 最多重试10次
                        // 每次失败后等待1秒
                    )

                    while (currentUser == null) {
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
                    // 捐赠
                    SettingItem.Click(
                        itemName = getString(R.string.Donate),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    DonateActivity::class.java
                                )
                            )
                        }
                    ),
                    // 设置代理
                    SettingItem.Click(
                        itemName = getString(R.string.Proxy),
                        onClick = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    SetProxyActivity::class.java
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
                                    GoToCheckUpdateActivity::class.java
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
                    userId = jsonObject.keySet().firstOrNull().toString(),
                    topTitle = topTitle,
                    chatsFoldersList = chatsFoldersList
                )
                ChatsListManager.chatsList = chatsList
                TgApiManager.tgApi?.loadChats(15)
                TgApiManager.tgApi?.getContacts(contacts)
                // 异步获取当前用户 ID
                lifecycleScope.launch {
                    while (currentUserId.value == -1L) {
                        TgApiManager.tgApi?.getCurrentUser() ?.let {
                            currentUserId.value = it[0].toLong()
                        }
                    }
                    TgApiManager.tgApi?.getArchiveChats()
                }
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
                                contacts = contacts,
                                topTitle = topTitle,
                                chatsFoldersList = chatsFoldersList,
                                currentUserId = currentUserId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                exceptionState = e
                Log.e("MainActivity", "Error initializing app: ${e.message}")
                if (e.message == "Failed to authorize") {
                    val externalDir: File = getExternalFilesDir(null)
                        ?: throw IllegalStateException("Failed to get external directory.")

                    val gson = Gson()
                    val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
                    var userList = sharedPref.getString("userList", "")
                    if (userList == "") throw Exception("No user data found")
                    val jsonObject: JsonObject = gson.fromJson(userList, JsonObject::class.java)
                    if (jsonObject.entrySet().size <= 1) {
                        // 清除缓存
                        cacheDir.deleteRecursively()
                        // 清空软件文件
                        filesDir.deleteRecursively()
                        val dir = externalDir.listFiles()
                        dir?.forEach { file ->
                            if (!file.deleteRecursively()) {
                                // 如果某个文件或文件夹无法删除，可以记录日志或采取其他处理方式
                                println("Failed to delete: ${file.absolutePath}")
                            }
                        }
                        cacheDir.deleteRecursively()
                        // 清空 SharedPreferences
                        getSharedPreferences("LoginPref", MODE_PRIVATE).edit().clear()
                            .apply()
                        // 重启软件
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }, 1000)
                    } else {
                        val account = jsonObject.keySet().firstOrNull().toString()
                        jsonObject.remove(account)
                        userList = jsonObject.toString()
                        val dir = File(externalDir.absolutePath + "/" + account)
                        dir.listFiles()?.find { it.name == "tdlib" && it.isDirectory }
                            ?.deleteRecursively()
                        cacheDir.deleteRecursively()
                        with(sharedPref.edit()) {
                            putString("userList", userList)
                            commit()
                        }
                        retryInitialization()
                    }
                } else {
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

private fun Context.checkAndUpdateConfiguration(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    scope.launch {
        val settingsSharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val dataCollection = settingsSharedPref.getBoolean("Data_Collection", false)
        if (dataCollection) {
            val appConfig = getSharedPreferences("app_config", Context.MODE_PRIVATE)
            val configStatus = getSharedPreferences("config_status", Context.MODE_PRIVATE)

            if (!configStatus.getBoolean("is_configured", false)) {
                var uniqueId = appConfig.getString("unique_identifier", null)
                if (uniqueId == null) {
                    uniqueId = UUID.randomUUID().toString()
                    appConfig.edit().putString("unique_identifier", uniqueId).apply()
                }

                try {
                    val domain = getDomain(this@checkAndUpdateConfiguration)
                    val url = URL("https://$domain/config/?data=$uniqueId")

                    with(url.openConnection() as HttpURLConnection) {
                        requestMethod = "GET"

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val response = inputStream.bufferedReader().use { it.readText() }
                            val configData = JSONObject(response)
                            val configCode = configData.optInt("status_code", -1) // Use optInt to avoid exceptions
                            if (configCode == 200) {
                                configStatus.edit().putBoolean("is_configured", true).apply()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handle or log the exception as needed
                }
            }
        }
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
