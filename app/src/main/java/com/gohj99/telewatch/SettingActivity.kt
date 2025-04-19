/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.gohj99.telewatch.model.SettingItem
import com.gohj99.telewatch.ui.setting.SplashSettingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File


class SettingActivity : ComponentActivity() {
    private var settingsList = mutableStateOf(listOf<SettingItem>())
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(
                    this,
                    getString(R.string.Done),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.Request_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsSharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        //Log.d("SharedPreferences", "当前获取状态: " + settingsSharedPref.getBoolean("Data_Collection", false))

        val externalDir: File = getExternalFilesDir(null)
            ?: throw IllegalStateException("Failed to get external directory.")

        // 获取传入页面数
        val page: Int = intent.getIntExtra("page", 0)

        // 初始标题
        var title = getString(R.string.Settings)

        val gson = Gson()
        val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
        val userList = sharedPref.getString("userList", "")
        var chatId = ""
        if (userList != "") {
            val jsonObject: JsonObject = gson.fromJson(userList, JsonObject::class.java)
            chatId = "/" + jsonObject.keySet().first()
        }

        when (page) {
            0 -> {
                settingsList.value = listOf(
                    // 捐赠
                    SettingItem.Click(
                        itemName = getString(R.string.Donate),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    DonateActivity::class.java
                                )
                            )
                        }
                    ),
                    // 网络设置
                    /*
                    SettingItem.Click(
                        itemName = getString(R.string.Network_setting),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    NetworkSettingActivity::class.java
                                )
                            )
                        }
                    ),*/
                    // 通知设置
                    SettingItem.Click(
                        itemName = getString(R.string.Notification),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    SettingActivity::class.java
                                ).putExtra("page", 3)
                            )
                        }
                    ),
                    // 界面调节
                    SettingItem.Click(
                        itemName = getString(R.string.UI_Edit),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    SettingActivity::class.java
                                ).putExtra("page", 1)
                            )
                        }
                    ),
                    // 应用设置
                    SettingItem.Click(
                        itemName = getString(R.string.App_setting),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    SettingActivity::class.java
                                ).putExtra("page", 2)
                            )
                        }
                    ),
                    // 查看提示
                    SettingItem.Click(
                        itemName = getString(R.string.View_Tips),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    RemindActivity::class.java
                                )
                            )
                        }
                    ),
                    // 检查更新
                    SettingItem.Click(
                        itemName = getString(R.string.Check_Update),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    GoToCheckUpdateActivity::class.java
                                )
                            )
                        }
                    ),
                    // 公告
                    /*
                    SettingItem.Click(
                        itemName = getString(R.string.announcement_title),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    GoToAnnouncementActivity::class.java
                                )
                            )
                        }
                    ),
                     */
                    // 关于
                    SettingItem.Click(
                        itemName = getString(R.string.About),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    AboutActivity::class.java
                                )
                            )
                        }
                    ),
                )
            }

            1 -> {
                title = getString(R.string.UI_Edit)
                settingsList.value = listOf(
                    SettingItem.Click(
                        itemName = getString(R.string.ui_test),
                        onClick = {
                            startActivity(
                                Intent(
                                    this,
                                    SettingActivity::class.java
                                ).putExtra("page", 4)
                            )
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.global_scale_factor),
                        progress = settingsSharedPref.getFloat("global_scale_factor", 1.0f).toFloat(),
                        maxValue = 2.5f,
                        minValue = 0.5f,
                        base = 0.01f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putFloat("global_scale_factor", size)
                                apply()
                            }
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.Down_Button_Offset),
                        progress = settingsSharedPref.getInt("Down_Button_Offset", 25).toFloat(),
                        maxValue = 80f,
                        minValue = 0f,
                        base = 1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putInt("Down_Button_Offset", size.toInt())
                                apply()
                            }
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.Line_spacing),
                        progress = settingsSharedPref.getFloat("Line_spacing", 5.5f),
                        maxValue = 10f,
                        minValue = 1f,
                        base = 0.1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putFloat("Line_spacing", size)
                                apply()
                            }
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.title_medium_font_size),
                        progress = settingsSharedPref.getFloat("title_medium_font_size", 14.3f),
                        maxValue = 25f,
                        minValue = 5f,
                        base = 0.1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putFloat("title_medium_font_size", size)
                                apply()
                            }
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.body_small_font_size),
                        progress = settingsSharedPref.getFloat("body_small_font_size", 13.3f),
                        maxValue = 25f,
                        minValue = 5f,
                        base = 0.1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putFloat("body_small_font_size", size)
                                apply()
                            }
                        }
                    ),
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.body_medium_font_size),
                        progress = settingsSharedPref.getFloat("body_medium_font_size", 13.5f),
                        maxValue = 25f,
                        minValue = 5f,
                        base = 0.1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putFloat("body_medium_font_size", size)
                                apply()
                            }
                        }
                    )
                )
            }

            2 -> {
                title = getString(R.string.App_setting)
                settingsList.value = listOf(
                    SettingItem.ProgressBar(
                        itemName = getString(R.string.Message_preload_quantity),
                        progress = settingsSharedPref.getInt("Message_preload_quantity", 10).toFloat(),
                        maxValue = 50f,
                        minValue = 5f,
                        base = 1f,
                        onProgressChange = { size ->
                            with(settingsSharedPref.edit()) {
                                putInt("Message_preload_quantity", size.toInt())
                                apply()
                            }
                        }
                    ),
                    SettingItem.Switch(
                        itemName = getString(R.string.show_unknown_message_type),
                        isSelected = settingsSharedPref.getBoolean("show_unknown_message_type", false),
                        onSelect = { dataCollection ->
                            with(settingsSharedPref.edit()) {
                                putBoolean("show_unknown_message_type", dataCollection)
                                commit()
                            }
                        }
                    ),
                    /*
                    SettingItem.Switch(
                        itemName = getString(R.string.is_home_page_pin),
                        isSelected = settingsSharedPref.getBoolean("is_home_page_pin", false),
                        onSelect = { dataCollection ->
                            with(settingsSharedPref.edit()) {
                                putBoolean("is_home_page_pin", dataCollection)
                                commit()
                            }
                        }
                    ),
                    */
                    SettingItem.Switch(
                        itemName = getString(R.string.data_Collection),
                        isSelected = settingsSharedPref.getBoolean("Data_Collection", false),
                        onSelect = { dataCollection ->
                            with(settingsSharedPref.edit()) {
                                putBoolean("Data_Collection", dataCollection)
                                commit()
                            }
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clearing_cache),
                        onClick = {
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Restart),
                        onClick = {
                            // 重启软件
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 1000)
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clear_thumbnails),
                        onClick = {
                            val dir = File(externalDir.absolutePath + chatId + "/tdlib")
                            dir.listFiles()?.find { it.name == "thumbnails" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clear_photos),
                        onClick = {
                            val dir = File(externalDir.absolutePath + chatId + "/tdlib")
                            dir.listFiles()?.find { it.name == "photos" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clear_voice),
                        onClick = {
                            val dir = File(externalDir.absolutePath + chatId + "/tdlib")
                            dir.listFiles()?.find { it.name == "voice" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clear_videos),
                        onClick = {
                            val dir = File(externalDir.absolutePath + chatId + "/tdlib")
                            dir.listFiles()?.find { it.name == "videos" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Clear_cache),
                        onClick = {
                            val dir = File(externalDir.absolutePath + chatId + "/tdlib")
                            dir.listFiles()?.find { it.name == "temp" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Reset_libtd),
                        onClick = {
                            val dir = File(externalDir.absolutePath)
                            dir.listFiles()?.find { it.name == "tdlib" && it.isDirectory }
                                ?.deleteRecursively()
                            cacheDir.deleteRecursively()
                            Toast.makeText(
                                this,
                                getString(R.string.Successful),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Reset_self),
                        onClick = {
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
                            getSharedPreferences("LoginPref", Context.MODE_PRIVATE).edit().clear()
                                .apply()
                            getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                                .clear().apply()
                            // Toast提醒
                            Toast.makeText(this, getString(R.string.Successful), Toast.LENGTH_SHORT)
                                .show()
                            // 重启软件
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 1000)
                        }
                    )
                )
            }

            3 -> {
                title = "${getString(R.string.Notification)} (${getString(R.string.Beta)})"
                settingsList.value = listOf(
                    SettingItem.Switch(
                        itemName = getString(R.string.Notification),
                        isSelected = settingsSharedPref.getBoolean("Use_Notification", false),
                        onSelect = { dataCollection ->
                            with(settingsSharedPref.edit()) {
                                putBoolean("Use_Notification", dataCollection)
                                commit()
                            }
                            if (dataCollection) {
                                FirebaseMessaging.getInstance().token
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val token = task.result
                                            println(token)
                                            settingsSharedPref.edit(commit = false) {
                                                putString("Token_Notification", token)
                                            }
                                            TgApiManager.tgApi?.setFCMToken(token) { id ->
                                                settingsSharedPref.edit(commit = false) {
                                                    putLong("Id_Notification", id)
                                                }
                                                println(id)
                                            }
                                        }
                                    }
                            } else TgApiManager.tgApi?.setFCMToken()

                        }
                    ),
                    SettingItem.Click(
                        itemName = getString(R.string.Apply_notification_permissions),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(this, getString(R.string.already_agreed), Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    )
                )
            }

            4 -> {
                title = getString(R.string.ui_test)
                settingsList.value = listOf(
                    SettingItem.Click(
                        itemName = "test1",
                        onClick = {
                            Toast.makeText(
                                this,
                                "test1",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = "test2",
                        onClick = {
                            Toast.makeText(
                                this,
                                "test2",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = "test3",
                        onClick = {
                            Toast.makeText(
                                this,
                                "test3",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = "test4",
                        onClick = {
                            Toast.makeText(
                                this,
                                "test4",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ),
                    SettingItem.Click(
                        itemName = "test5",
                        onClick = {
                            Toast.makeText(
                                this,
                                "test5",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                )
            }
        }

        setContent {
            TelewatchTheme {
                SplashSettingScreen(
                    title = title,
                    settings = settingsList
                )
            }
        }
    }
}
