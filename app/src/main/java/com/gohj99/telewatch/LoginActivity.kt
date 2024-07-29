package com.gohj99.telewatch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.google.zxing.BarcodeFormat
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.IOException
import java.util.Properties
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    private lateinit var client: Client
    private var qrCodeLink by mutableStateOf<String?>(null)

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
                SplashScreen(qrCodeLink = qrCodeLink)
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
                        val api_Id = config.getProperty("api_id").toInt()
                        val api_Hash = config.getProperty("api_hash")
                        // 设置 TDLib 参数
                        val parameters = TdApi.TdlibParameters().apply {
                            databaseDirectory = applicationContext.filesDir.absolutePath + "/tdlib"
                            useMessageDatabase = true
                            useSecretChats = true
                            apiId = api_Id
                            apiHash = api_Hash
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
                            this.qrCodeLink = link
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
                        val passwordHint = passwordState.passwordHint

                        // 进入密码输入函数
                        InputPassword(passwordHint)
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

    // 输入密码的函数
    private fun InputPassword(passwordHint: String) {
        // 输入密码
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

// 显示二维码
@Composable
fun QrCodeDisplay(qrCodeLink: String?, sizeInDp: Int = 200) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 如果 qrCodeLink 不为空，生成二维码
    LaunchedEffect(qrCodeLink) {
        qrCodeLink?.let {
            withContext(Dispatchers.Default) {
                bitmap = generateQrCode(context, it, sizeInDp)
            }
        }
    }

    if (bitmap != null) {
        // 绘制二维码
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        // 使用占位符图像
        Image(
            painter = painterResource(id = R.drawable.qr_tg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SplashScreen(qrCodeLink: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.scan_qr),
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 7.dp, bottom = 4.dp)
                .fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            QrCodeDisplay(qrCodeLink = qrCodeLink, sizeInDp = 120)

            if (qrCodeLink == null) {
                Text(
                    text = stringResource(id = R.string.loading),
                    color = Color(0xFF757575),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(top = 45.dp, bottom = 4.dp)
                        .fillMaxSize(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    TelewatchTheme {
        SplashScreen(qrCodeLink = "https://www.gohj99.site/")
    }
}
