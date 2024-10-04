/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import java.io.IOException
import java.util.Properties

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Firebase Analytics
        initFirebaseAnalytics(this)

        val config = loadConfig(this)
        val buildDate = config.getProperty("BUILD_DATE")
        setContent {
            TelewatchTheme {
                SplashAboutScreen(getAppVersion(this), buildDate)
            }
        }
    }
}

private fun loadConfig(context: Context): Properties {
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

@Composable
fun SplashAboutScreen(appVersion: String, buildDate: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.4f)
                    .clip(CircleShape)
                    .align(Alignment.Center)
            )
            Image(
                painter = painterResource(id = R.drawable.circle),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Text(
            text = stringResource(id = R.string.app_name),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "gohj99",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.little_jelly),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.Build_Date) + " $buildDate",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.Version) + " $appVersion",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashAboutScreenPreview() {
    TelewatchTheme {
        SplashAboutScreen("1.0", "2024")
    }
}
