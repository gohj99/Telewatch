package com.gohj99.telewatch.ui.login

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.generateQrCode
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        // 使用占位符图像和文字s
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SplashLoginScreen(qrCodeLink: String?) {
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
            QrCodeDisplay(qrCodeLink = qrCodeLink, sizeInDp = 200)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashLoginScreenPreview() {
    TelewatchTheme {
        SplashLoginScreen(qrCodeLink = "")
    }
}
