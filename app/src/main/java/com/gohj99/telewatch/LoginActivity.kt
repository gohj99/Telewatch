/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gohj99.telewatch.ui.login.SplashLoginScreen
import com.gohj99.telewatch.ui.login.SplashPasswordScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties

class LoginActivity : ComponentActivity() {
    private lateinit var client: Client
    private lateinit var languageCode: String
    private lateinit var appVersion: String
    private var qrCodeLink by mutableStateOf<String?>(null)
    private var showPasswordScreen by mutableStateOf(false)
    private var passwordHint by mutableStateOf("")
    private var doneStr = mutableStateOf("")

    override fun onDestroy() {
        super.onDestroy()
        // 在这里释放 TDLib 资源
        client.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        languageCode = this.resources.configuration.locales[0].language
        appVersion = getAppVersion(this)
        setContent {
            TelewatchTheme {
                if (showPasswordScreen) {
                    SplashPasswordScreen(
                        onDoneClick = { password ->
                            client.send(TdApi.CheckAuthenticationPassword(password)) {
                                authRequestHandler(
                                    it
                                )
                            }
                        },
                        passwordHint = passwordHint,
                        doneStr = doneStr
                    )
                } else {
                    SplashLoginScreen(qrCodeLink = qrCodeLink)
                }
            }
        }

        client = Client.create({ update -> handleUpdate(update) }, null, null)
        doneStr.value = getString(R.string.Done) // 初始化 doneStr
    }

    // 加载配置文件
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

    // 处理 TDLib 更新的函数
    private fun handleUpdate(update: TdApi.Object) {
        when (update.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val authorizationState = (update as TdApi.UpdateAuthorizationState).authorizationState
                when (authorizationState.constructor) {
                    TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                        //获取API ID和API Hash
                        val config = loadConfig(this) // 传递 context
                        val tdapiId = config.getProperty("api_id").toInt()
                        val tdapiHash = config.getProperty("api_hash")
                        // 设置 TDLib 参数
                        val parameters = TdApi.TdlibParameters().apply {
                            databaseDirectory = applicationContext.filesDir.absolutePath + "/tdlib"
                            useMessageDatabase = true
                            useSecretChats = true
                            apiId = tdapiId
                            apiHash = tdapiHash
                            systemLanguageCode = languageCode
                            deviceModel = Build.MODEL
                            systemVersion = Build.VERSION.RELEASE
                            applicationVersion = appVersion
                            enableStorageOptimizer = true
                        }
                        client.send(TdApi.SetTdlibParameters(parameters)) { }
                    }
                    TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                        // 检查本地是否有加密密钥
                        val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
                        val encryptionKeyString = sharedPref.getString("encryption_key", null)
                        val encryptionKey: TdApi.CheckDatabaseEncryptionKey = if (encryptionKeyString != null) {
                            val keyBytes = encryptionKeyString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            TdApi.CheckDatabaseEncryptionKey(keyBytes)
                        } else {
                            // 生成一个新的加密密钥并保存
                            val newKeyBytes = ByteArray(32).apply { (0..31).forEach { this[it] = (it * 7).toByte() } }
                            val newKeyString = newKeyBytes.joinToString("") { "%02x".format(it) }
                            with(sharedPref.edit()) {
                                putString("encryption_key", newKeyString)
                                apply()
                            }
                            TdApi.CheckDatabaseEncryptionKey(newKeyBytes)
                        }

                        client.send(encryptionKey) { }
                    }
                    TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                        // 请求二维码认证
                        client.send(TdApi.RequestQrCodeAuthentication(LongArray(0))) {
                            authRequestHandler(
                                it
                            )
                        }
                    }
                    TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                        val link = (authorizationState as TdApi.AuthorizationStateWaitOtherDeviceConfirmation).link
                        println(link)
                        // 展示二维码
                        runOnUiThread {
                            qrCodeLink = link
                        }
                    }
                    TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                        // 登录成功
                        println("Login Successful")
                        doneStr.value = getString(R.string.Login_Successful)
                        // 发送广播通知 WelcomeActivity 销毁自己
                        /*LocalBroadcastManager.getInstance(this).sendBroadcast(
                            Intent("ACTION_DESTROY_WELCOME_ACTIVITY")
                        )*/
                        // 存储登录成功信息
                        val sharedPref = getSharedPreferences("LoginPref", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("isLoggedIn", true)
                            apply()
                        }
                        runOnUiThread {
                            // 重启软件
                            resetSelf()
                            //finish()
                        }
                    }
                    TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                        //当需要输入密码时执行
                        val passwordState = authorizationState as TdApi.AuthorizationStateWaitPassword
                        passwordHint = passwordState.passwordHint

                        // 进入密码输入处理
                        runOnUiThread {
                            showPasswordScreen = true
                        }
                    }
                    // 处理其他授权状态...
                    TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                        TODO()
                    }

                    TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                        TODO()
                    }

                    TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                        TODO()
                    }

                    TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                        TODO()
                    }

                    TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR -> {
                        TODO()
                    }
                }
            }
            // 处理其他更新...
        }
    }

    // 处理认证请求的函数
    private fun authRequestHandler(result: TdApi.Object) {
        // 处理认证请求结果
        when (result.constructor) {
            TdApi.Error.CONSTRUCTOR -> {
                val error = result as TdApi.Error
                println("${getString(R.string.Request_error)} : ${error.message}")
                when (error.message) {
                    "PASSWORD_HASH_INVALID" -> {
                        doneStr.value = getString(R.string.Password_Error)
                    }

                    else -> runOnUiThread {
                        doneStr.value = getString(R.string.Done)
                        AlertDialog.Builder(this)
                            .setMessage("${getString(R.string.Request_error)}\ncode:${error.code}\n${error.message}")
                            .setPositiveButton(getString(R.string.OK)) { dialog, which -> }
                            .show()
                    }
                }
            }
            // 处理其他结果...
        }
    }

    // 重启软件
    private fun resetSelf(){
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 1000)
    }
}

// 创建一个函数来生成二维码的 Bitmap
fun generateQrCode(context: Context, qrCodeLink: String, sizeInDp: Int): Bitmap? {
    val sizeInPx = context.dpToPx(sizeInDp)
    val hints = hashMapOf<EncodeHintType, Any>()
    hints[EncodeHintType.MARGIN] = 0 // 设置边距为0

    return try {
        val bitMatrix = MultiFormatWriter().encode(qrCodeLink, BarcodeFormat.QR_CODE, sizeInPx, sizeInPx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: WriterException) {
        e.printStackTrace()
        null
    }
}

//dp值转换为像素值
fun Context.dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics
    ).toInt()
}
