/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.login

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.generateQrCode
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.InputRoundBar
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 手机号+验证码登录页面
@Composable
fun SplashLoginScreen(
    showSendCode: Boolean,
    onLoginWithQR: () -> Unit,
    onSendVerifyCode: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    var phoneNumber by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scrollState.scrollTo(60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .verticalRotaryScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.Login),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp, top = 60.dp)
        )

        Text(
            text = stringResource(id = R.string.Login_With_QR),
            color = colorResource(id = R.color.blue_dark),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clickable{
                    onLoginWithQR()
                }
        )

        // 手机号输入框
        InputRoundBar(
            query = phoneNumber,
            onQueryChange = {
                phoneNumber = it
            },
            placeholder = "${stringResource(id = R.string.Phone_Number)} (+)"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 验证码输入框
        InputRoundBar(
            query = verifyCode,
            onQueryChange = {
                verifyCode = it
            },
            placeholder = stringResource(id = R.string.Verify_Code)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 发送验证码按钮
        CustomButton(
            onClick = {
                onSendVerifyCode(phoneNumber)
            },
            text = stringResource(id = R.string.Send_Verify_Code),
            textColor = if (showSendCode) Color.White else Color.Gray,
            color = if (showSendCode) Color(0xff2c323a) else Color(0xFF0D0F11),
            enabled = showSendCode
        )

        Spacer(modifier = Modifier.height(12.dp))

        IconButton(
            onClick = {
                onDone(verifyCode)
            },
            modifier = Modifier
                .size(45.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.next_icon),
                contentDescription = null,
                modifier = Modifier.size(45.dp)
            )
        }

        Spacer(modifier = Modifier.height(42.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SplashLoginScreenPreview() {
    TelewatchTheme {
        SplashLoginScreen(true, {}, {}, {})
    }
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
        // 使用占位符图像和文字
        Image(
            painter = painterResource(id = R.drawable.qr_tg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentScale = ContentScale.Crop
        )
        Text(
            text = stringResource(id = R.string.loading),
            color = Color(0xFF757575),
            fontSize = 16.sp,
            modifier = Modifier
                .padding(top = 45.dp, bottom = 4.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center
        )
    }
}

// 二维码登录页面
@Composable
fun SplashLoginQRScreen(qrCodeLink: String?) {
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
            fontSize = 16.sp,
            modifier = Modifier
                .padding(top = 6.dp, bottom = 4.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
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
            QrCodeDisplay(qrCodeLink = qrCodeLink, sizeInDp = 200)
        }
    }
}

// 密码输入页面
@Composable
fun SplashPasswordScreen(
    onDoneClick: (String) -> Unit,
    passwordHint: String = "",
    doneStr: MutableState<String>
) {
    val loading = stringResource(id = R.string.loading)
    val passwordError = stringResource(id = R.string.Password_Error)
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.Password),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            textAlign = TextAlign.Center
        )

        // 密码输入框
        InputRoundBar(
            query = password,
            isPassword = true,
            onQueryChange = {
                password = it
            },
            placeholder = passwordHint
        )

        Spacer(modifier = Modifier.height(12.dp))

        CustomButton(
            onClick = {
                if (password != "") {
                    doneStr.value = loading
                    onDoneClick(password)
                } else doneStr.value = passwordError
                password = ""
            },
            text = doneStr.value
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashLoginScreenQRPreview() {
    TelewatchTheme {
        SplashLoginQRScreen(qrCodeLink = "")
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPasswordScreenPreview() {
    TelewatchTheme {
        SplashPasswordScreen(
            onDoneClick = { password ->
                println(password)
            },
            doneStr = remember { mutableStateOf("") }
        )
    }
}
