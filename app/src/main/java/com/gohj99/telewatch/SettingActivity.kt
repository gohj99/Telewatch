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
import com.gohj99.telewatch.ui.SettingItem
import com.gohj99.telewatch.ui.SplashSettingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import java.io.File

class SettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val settingsList = listOf(
            SettingItem.NestedSetting(getString(R.string.Clearing_cache), onClick = {
                cacheDir.deleteRecursively()
                Toast.makeText(this, getString(R.string.Successful), Toast.LENGTH_SHORT).show()
            }),
            SettingItem.NestedSetting(getString(R.string.Reset_libtd), onClick = { reset_libtd() }),
            SettingItem.NestedSetting(getString(R.string.Reset_self), onClick = { reset_self() })
        )

        setContent {
            TelewatchTheme {
                SplashSettingScreen(
                    settings = settingsList
                )
            }
        }
    }

    private fun reset_libtd(){
        val dir = File(applicationContext.filesDir.absolutePath)
        dir.listFiles()?.find { it.name == "tdlib" && it.isDirectory }?.deleteRecursively()
        // 清除登录数据
        getSharedPreferences("LoginPref", Context.MODE_PRIVATE).edit().clear().apply()
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

    private fun reset_self(){
        // 清除缓存
        cacheDir.deleteRecursively()
        // 清空软件文件
        filesDir.deleteRecursively()
        // 清空 SharedPreferences
        getSharedPreferences("LoginPref", Context.MODE_PRIVATE).edit().clear().apply()
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
