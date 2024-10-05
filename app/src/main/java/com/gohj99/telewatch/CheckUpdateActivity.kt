/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("assets") val assets: List<Asset>
)

data class Asset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

class CheckUpdateActivity : ComponentActivity() {
    private var downloadId: Long = -1
    private var fileName = ""

    companion object {
        private const val REQUEST_CODE_UNKNOWN_APP = 1234
    }

    private lateinit var installApkLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TelewatchTheme {
                SplashLoadingScreen(modifier = Modifier.fillMaxSize())
            }
        }

        // 检查 Android 版本，如果是 Android 10 及以上，注册 ActivityResultLauncher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerInstallApkLauncher()
        }

        init()
    }

    private fun registerInstallApkLauncher() {
        installApkLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // 用户已授予安装未知来源应用的权限
                installApk(fileName)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.unknown_apps_install_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun init() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/gohj99/Telewatch/releases/latest")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(
                            "com.gohj99.telewatch.CheckUpdateActivity",
                            "Request failed: ${e.message}"
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")

                            val responseData = response.body?.string()
                            if (responseData != null) {
                                val releaseInfo =
                                    Gson().fromJson(responseData, ReleaseInfo::class.java)

                                val armAsset =
                                    releaseInfo.assets.find { it.name.contains("arm.apk") }
                                val armDownloadUrl = armAsset?.browserDownloadUrl ?: ""

                                fileName = armDownloadUrl.substringAfterLast('/')

                                val needsUpdate = compareVersions(
                                    releaseInfo.tagName.substring(1, 6),
                                    packageManager.getPackageInfo(packageName, 0).versionName
                                )

                                if (needsUpdate) {
                                    setContent {
                                        TelewatchTheme {
                                            SplashUpdateView(
                                                contentText = generateUpdateInfo(releaseInfo),
                                                onDownloadClick = { startDownload(armDownloadUrl) },
                                                downloadProgress = 0f,
                                                onInstallClick = { installApk(fileName) },
                                                isDownloadComplete = false
                                            )
                                        }
                                    }
                                } else {
                                    setContent {
                                        TelewatchTheme {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.already_latest),
                                                    color = Color.White,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("com.gohj99.telewatch.CheckUpdateActivity", "Error: ${e.message}")
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            ErrorScreen(onRetry = { init() })
                        }
                    }
                }
            }
        }
    }

    private fun startDownload(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading update")
            .setDescription("Downloading the latest version of the app")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
    }

    private fun installApk(fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // 请求用户授予安装未知来源应用的权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    installApkLauncher.launch(intent)
                } else {
                    // Android 7.1 及以下版本使用 onActivityResult
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP)
                }
                return
            }
        }

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) {
            Log.e("CheckUpdateActivity", "APK file does not exist: ${file.absolutePath}")
            return
        }

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    @Deprecated("Deprecated in Android 14", ReplaceWith("ActivityResult API"))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UNKNOWN_APP) {
            if (resultCode == RESULT_OK) {
                installApk(fileName)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.unknown_apps_install_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun compareVersions(version1: String, version2: String): Boolean {
        val parts1 = version1.split(".")
        val parts2 = version2.split(".")
        for (i in 0..2) {
            val num1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 > num2) return true
            if (num1 < num2) return false
        }
        return false
    }

    private fun generateUpdateInfo(releaseInfo: ReleaseInfo): String {
        val releaseType =
            if (releaseInfo.prerelease) getString(R.string.pre_release) else getString(R.string.release)
        val tagName = releaseInfo.tagName
        val version = tagName.substring(1, 6)
        val publishedAt = releaseInfo.publishedAt
        val zonedDateTime = ZonedDateTime.parse(publishedAt)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedPublishedAt = zonedDateTime.format(formatter)

        return """
            ${getString(R.string.version)}: $version
            ${getString(R.string.Release_type)}: $releaseType
            ${getString(R.string.Release_time)}: $formattedPublishedAt
        """.trimIndent()
    }
}

@Composable
fun SplashUpdateView(
    contentText: String,
    onDownloadClick: () -> Unit,
    downloadProgress: Float,
    onInstallClick: () -> Unit,
    isDownloadComplete: Boolean
) {
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
            text = stringResource(id = R.string.New_version),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 64.dp)
        )

        // 主要说明部分
        Text(
            text = contentText,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 下载按钮或进度条
        if (!isDownloadComplete) {
            var isShowDownloadButton by rememberSaveable { mutableStateOf(true) }

            if (isShowDownloadButton) {
                CustomButton(
                    onClick = {
                        isShowDownloadButton = false
                        onDownloadClick()
                    },
                    text = stringResource(id = R.string.downloads)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
        } else {
            // 安装按钮
            CustomButton(
                onClick = onInstallClick,
                text = stringResource(id = R.string.install)
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
