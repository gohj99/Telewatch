/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.gohj99.telewatch.ui.setting.SplashSettingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import java.io.File

class SettingActivity : ComponentActivity() {
    private var settingsList = mutableStateOf(listOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val externalDir: File = getExternalFilesDir(null)
            ?: throw IllegalStateException("Failed to get external directory.")

        settingsList.value = listOf(
            getString(R.string.Clearing_cache),
            getString(R.string.Restart),
            getString(R.string.Clear_thumbnails),
            getString(R.string.Clear_photos),
            getString(R.string.Clear_videos),
            getString(R.string.Clear_cache),
            getString(R.string.Reset_self),
            getString(R.string.About)
        )

        setContent {
            TelewatchTheme {
                SplashSettingScreen(
                    settings = settingsList,
                    callback = { item ->
                        when (item) {
                            getString(R.string.Clearing_cache) -> {
                                cacheDir.deleteRecursively()
                                Toast.makeText(
                                    this,
                                    getString(R.string.Successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            getString(R.string.Restart) -> {
                                // 重启软件
                                Handler(Looper.getMainLooper()).postDelayed({
                                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                }, 1000)
                            }

                            getString(R.string.Clear_thumbnails) -> {
                                val dir = File(externalDir.absolutePath + "/tdlib")
                                dir.listFiles()?.find { it.name == "thumbnails" && it.isDirectory }
                                    ?.deleteRecursively()
                                cacheDir.deleteRecursively()
                                Toast.makeText(
                                    this,
                                    getString(R.string.Successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            getString(R.string.Clear_photos) -> {
                                val dir = File(externalDir.absolutePath + "/tdlib")
                                dir.listFiles()?.find { it.name == "photos" && it.isDirectory }
                                    ?.deleteRecursively()
                                cacheDir.deleteRecursively()
                                Toast.makeText(
                                    this,
                                    getString(R.string.Successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            getString(R.string.Clear_videos) -> {
                                val dir = File(externalDir.absolutePath + "/tdlib")
                                dir.listFiles()?.find { it.name == "videos" && it.isDirectory }
                                    ?.deleteRecursively()
                                cacheDir.deleteRecursively()
                                Toast.makeText(
                                    this,
                                    getString(R.string.Successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            getString(R.string.Clear_cache) -> {
                                val dir = File(externalDir.absolutePath + "/tdlib")
                                dir.listFiles()?.find { it.name == "temp" && it.isDirectory }
                                    ?.deleteRecursively()
                                cacheDir.deleteRecursively()
                                Toast.makeText(
                                    this,
                                    getString(R.string.Successful),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            getString(R.string.Reset_self) -> {
                                resetSelf()
                            }

                            getString(R.string.About) -> {
                                val intent = Intent(this, AboutActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun resetSelf() {
        // 清除缓存
        cacheDir.deleteRecursively()
        // 清空软件文件
        filesDir.deleteRecursively()
        // 清空 SharedPreferences
        getSharedPreferences("LoginPref", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().apply()
        // Toast提醒
        Toast.makeText(this, getString(R.string.Successful), Toast.LENGTH_SHORT).show()
        // 重启软件
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 1000)
    }
}
