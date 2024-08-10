/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun MainScreen(chats: MutableState<List<Chat>>, chatPage: (Chat) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val allPages = listOf(
        stringResource(id = R.string.HOME),
        stringResource(id = R.string.Contacts),
        stringResource(id = R.string.Settings),
    )
    var nowPage by remember { mutableStateOf(allPages[0]) }

    // 使用 Column 包裹 Box 和 ChatLazyColumn
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 包含 Row 的 Box
        Box(
            modifier = Modifier
                .fillMaxWidth() // 只填充宽度
                .padding(top = 16.dp) // 添加顶部填充
                .clickable { showMenu = !showMenu } // 点击时切换显示状态
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter) // 将 Row 对齐到顶部中央
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
            ) {
                Image(
                    painter = if (showMenu) {
                        painterResource(id = R.drawable.up)
                    } else {
                        painterResource(id = R.drawable.down)
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.114514.dp) // 设置图片大小
                )
                Spacer(modifier = Modifier.width(8.dp)) // 添加间距
                Text(
                    text = stringResource(id = R.string.HOME),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp)) // 添加间距

        // ChatLazyColumn
        if (showMenu) {
            MenuLazyColumn(
                allPages = allPages,
                nowPage = { page ->
                    nowPage = page
                    showMenu = false
                }
            )
        } else {
            when (nowPage) {
                stringResource(id = R.string.HOME) -> {
                    ChatLazyColumn(
                        itemsList = chats,
                        callback = chatPage
                    )
                }

                stringResource(id = R.string.Contacts) -> {}
                stringResource(id = R.string.Settings) -> {}
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleChats = mutableStateOf(
        listOf(
            Chat(id = 1, title = "钱显康", message = "我是傻逼"),
            Chat(id = 2, title = "Rechrd", message = "我父亲是钱明"),
            Chat(id = 3, title = "将军", message = "我母亲是康庆莉")
        )
    )

    TelewatchTheme {
        MainScreen(sampleChats) {/*TODO*/ }
    }
}
