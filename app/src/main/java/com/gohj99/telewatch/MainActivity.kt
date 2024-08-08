package com.gohj99.telewatch

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
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var isLoggedIn: Boolean = false
    private var exceptionState by mutableStateOf<Exception?>(null)
    private var chatsList = mutableStateOf(listOf<Chat>())

    override fun onDestroy() {
        super.onDestroy()
        tgApi?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

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
                tgApi = TgApi(this@MainActivity)
                tgApi?.getChats(
                    limit = 10,
                    chatsList = chatsList
                )
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            MainScreen(chatsList)
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
                                        Intent(
                                            this@MainActivity,
                                            SettingActivity::class.java
                                        )
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
