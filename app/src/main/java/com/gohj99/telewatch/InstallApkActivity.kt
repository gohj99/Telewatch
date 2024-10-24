/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File

class InstallApkActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_UNKNOWN_APP = 1234
        const val EXTRA_FILE_NAME = "extra_file_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return

        installApk(fileName)
    }

    private fun installApk(fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // 请求用户授予安装未知来源应用的权限
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP)
                return
            }
        }

        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        if (!file.exists()) {
            Log.e("InstallApkActivity", "APK file does not exist: ${file.absolutePath}")
            return
        }

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_UNKNOWN_APP) {
            val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return
            installApk(fileName)
        }
    }
}
