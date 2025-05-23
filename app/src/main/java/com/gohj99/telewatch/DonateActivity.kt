/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.urlHandle

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelewatchTheme {
                SplashDonateScreen()
            }
        }
    }
}

@Composable
fun SplashDonateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 包含 Row 的 Box
        Box(
            modifier = Modifier
                .fillMaxWidth() // 只填充宽度
                .padding(top = 14.dp) // 添加顶部填充
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter) // 将 Row 对齐到顶部中央
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
            ) {
                Text(
                    text = stringResource(id = R.string.Donate),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp)) // 添加间距

        DonateLazyColumn()
    }
}

@Composable
fun DonateLazyColumn() {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var donate1 by remember { mutableStateOf(context.getString(R.string.DonateL)) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalRotaryScroll(listState)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        donate1 = context.getString(R.string.Donate1)
                    },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)) {
                    Text (
                        text = donate1,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionContainer {
                        LinkText(
                            text = "Liberapay: https://liberapay.com/gohj99",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            onLinkClick = { url ->
                                urlHandle(url, context)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.liberapay_qr),
                        contentDescription = "Liberapay_qr",
                        modifier = Modifier
                            .size(120.dp)
                            .fillMaxSize(), // 确保 Image 填充整个 Box
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Alipay",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.alipay),
                        contentDescription = "Alipay",
                        modifier = Modifier
                            .size(120.dp)
                            .fillMaxSize(), // 确保 Image 填充整个 Box
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WeChat pay",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.wechat_pay),
                        contentDescription = "WeChat pay",
                        modifier = Modifier
                            .size(120.dp)
                            .fillMaxSize(), // 确保 Image 填充整个 Box
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashDonateScreenPreview() {
    TelewatchTheme {
        SplashDonateScreen()
    }
}
