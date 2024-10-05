/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class ConfirmLogoutActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var deletaAll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tgApi = TgApiManager.tgApi

        val externalDir: File = getExternalFilesDir(null)
            ?: throw IllegalStateException("Failed to get external directory.")

        val gson = Gson()
        val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
        var userList = sharedPref.getString("userList", "")
        if (userList == "") throw Exception("No user data found")
        val jsonObject: JsonObject = gson.fromJson(userList, JsonObject::class.java)
        var account = ""
        if (jsonObject.entrySet().size <= 1) {
            deletaAll = true
        } else {
            account = jsonObject.keySet().firstOrNull().toString()
            jsonObject.remove(account)
            userList = jsonObject.toString()
        }

        setContent {
            TelewatchTheme {
                SplashConfirmLogoutActivityScreen { isSwitch ->
                    if (isSwitch) {
                        tgApi?.logOut()
                        if (deletaAll) {
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
                            val dir = File(externalDir.absolutePath + "/" + account + "/tdlib")
                            dir.listFiles()?.find { it.name == "temp" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            with(sharedPref.edit()) {
                                putString("userList", userList)
                                commit()
                                // 重启软件
                                Handler(Looper.getMainLooper()).postDelayed({
                                    val intent =
                                        packageManager.getLaunchIntentForPackage(packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                }, 1000)
                            }
                        }
                    } else {
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashConfirmLogoutActivityScreen(set: (Boolean) -> Unit) {
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(80)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.Confirm_Logout),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 86.dp)
        )

        // 主要说明部分
        Text(
            text = stringResource(id = R.string.Confirm_logout_sure),
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 不同意和同意按钮
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp, start = 16.dp, end = 16.dp)
        ) {
            Button(
                onClick = { set(false) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3E4D58),  // 按钮背景颜色
                    contentColor = Color.White   // 按钮文字颜色
                )
            ) {
                Text(text = stringResource(id = R.string.disagree))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { set(true) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3A7FBE),  // 按钮背景颜色
                    contentColor = Color.White   // 按钮文字颜色
                )
            ) {
                Text(text = stringResource(id = R.string.agree))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashConfirmLogoutActivityScreenPreview() {
    TelewatchTheme {
        SplashConfirmLogoutActivityScreen { /*TODO*/ }
    }
}
