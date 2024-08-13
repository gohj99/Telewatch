package com.gohj99.telewatch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.res.painterResource
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

    private fun initializeApp() {
        settingList.value = listOf(
            getString(R.string.About),
            getString(R.string.setting_all)
        )

        val sharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
        isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startWelcomeActivity()
        } else {
            setContent {
                TelewatchTheme {
                    SplashLoadingScreen()
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
                TgApiManager.tgApi = TgApi(
                    this@MainActivity,
                    chatsList = chatsList
                )
                TgApiManager.tgApi?.getChats(
                    limit = 10
                )
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
