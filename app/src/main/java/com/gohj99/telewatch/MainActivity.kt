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
import androidx.compose.runtime.*
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.MainScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import org.drinkless.td.libcore.telegram.TdApi

class MainActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var isLoggedIn: Boolean = false
    private var exceptionState by mutableStateOf<Exception?>(null)
    @SuppressLint("MutableCollectionMutableState")
    private var chatsList = mutableStateOf(mutableListOf<TdApi.Chat>())

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
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }, 0)
        } else {
            try {
                initMain()
            } catch (e: Exception) {
                exceptionState = e
                setContent {
                    TelewatchTheme {
                        ErrorScreen(
                            onRetry = { retryInitialization() },
                            onSetting = {
                                startActivity(Intent(this, SettingActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun initMain() {
        //println("start")
        tgApi = TgApi(this)
        tgApi?.getChats(
            limit = 10,
            chatsList = chatsList
        )
        setContent {
            TelewatchTheme {
                MainScreen(chatsList)
            }
        }
    }

    private fun retryInitialization() {
        exceptionState = null
        try {
            initMain()
        } catch (e: Exception) {
            exceptionState = e
        }
    }
}
