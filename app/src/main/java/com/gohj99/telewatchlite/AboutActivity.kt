/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatchlite

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gohj99.telewatchlite.ui.SplashAboutScreen
import com.gohj99.telewatchlite.ui.theme.telewatchliteTheme
import java.io.IOException
import java.util.Properties

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val config = loadConfig(this)
        val buildDate = config.getProperty("BUILD_DATE")
        setContent {
            telewatchliteTheme {
                SplashAboutScreen(getAppVersion(this), buildDate)
            }
        }
    }
}

fun loadConfig(context: Context): Properties {
    val properties = Properties()
    try {
        val inputStream = context.assets.open("config.properties")
        inputStream.use { properties.load(it) }
    } catch (e: IOException) {
        e.printStackTrace()
        // 处理异常，例如返回默认配置或通知用户
    }
    return properties
}

private fun getAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName
    } catch (e: Exception) {
        "1.0.0"
    }
}
