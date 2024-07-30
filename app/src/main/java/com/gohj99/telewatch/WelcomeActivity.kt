package com.gohj99.telewatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gohj99.telewatch.ui.SplashWelcomeScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
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
