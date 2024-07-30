package com.gohj99.telewatch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.gohj99.telewatch.ui.login.*
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.zxing.BarcodeFormat
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException

class LoginActivity : ComponentActivity() {
    private lateinit var client: Client
    private var qrCodeLink by mutableStateOf<String?>(null)
    private var showPasswordScreen by mutableStateOf(false)
    private var passwordHint by mutableStateOf("")

    override fun onDestroy() {
        super.onDestroy()
        // 在这里释放 TDLib 资源
        client.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelewatchTheme {
                if (showPasswordScreen) {
                    SplashPasswordScreen(
                        onDoneClick = { password ->
                            client.send(TdApi.CheckAuthenticationPassword(password), { authRequestHandler(it) })
                        },
                        passwordHint = passwordHint
                    )
                } else {
                    SplashLoginScreen(qrCodeLink = qrCodeLink)
                }
            }
        }

        // 初始化 TDLib 客户端
        client = Client.create({ update -> handleUpdate(update) }, null, null)
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
                            systemLanguageCode = "en"
                            deviceModel = "Desktop"
                            systemVersion = "Unknown"
                            applicationVersion = "1.0"
                            enableStorageOptimizer = true
                        }
                        client.send(TdApi.SetTdlibParameters(parameters), { })
                    }
                    TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
                        // 提供加密密钥
                        client.send(TdApi.CheckDatabaseEncryptionKey(), { })
                    }
                    TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                        // 请求二维码认证
                        client.send(TdApi.RequestQrCodeAuthentication(LongArray(0)), { authRequestHandler(it) })
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
                        // 发送广播通知 WelcomeActivity 销毁自己
                        /*LocalBroadcastManager.getInstance(this).sendBroadcast(
                            Intent("ACTION_DESTROY_WELCOME_ACTIVITY")
                        )*/
                        // 存储登录成功信息
                        val sharedPref = getSharedPreferences("LoginPref", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("isLoggedIn", true)
                            apply()
                        }
                        runOnUiThread {
                            // 启动新的页面
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
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
                }
            }
            // 处理其他更新...
        }
    }

    // 处理认证请求的函数
    private fun authRequestHandler(result: TdApi.Object) {
        // 处理认证请求结果
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
