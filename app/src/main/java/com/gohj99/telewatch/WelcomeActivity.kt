package com.gohj99.telewatch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gohj99.telewatch.ui.SplashWelcomeScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme

class WelcomeActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            // 登录成功后销毁页面
            if (sharedPreferences == this.sharedPref && key == "isLoggedIn") {
                val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
                if (isLoggedIn) {
                    finish()
                }
            }
        }


    override fun onDestroy() {
        super.onDestroy()
        // 取消注册监听器
        getPreferences(MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // 获取共享偏好设置
        sharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
        // 注册监听器
        getPreferences(MODE_PRIVATE).registerOnSharedPreferenceChangeListener(
            preferenceChangeListener
        )

        setContent {
            TelewatchTheme {
                SplashWelcomeScreen(
                    onStartMessagingClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onSettingClick = {
                        startActivity(Intent(this, SettingActivity::class.java))
                    }
                )
            }
        }


    }
}
