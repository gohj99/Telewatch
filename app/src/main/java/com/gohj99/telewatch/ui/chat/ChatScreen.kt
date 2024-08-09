/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import org.drinkless.td.libcore.telegram.TdApi

@Composable
fun SplashChatScreen(
    chatTitle: String,
    chatList: MutableState<List<TdApi.Message>>,
    currentUserId: Long
) {
    val listState = rememberLazyListState()
    var isFloatingVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, scrollOffset) ->
                if (index != previousIndex) {
                    if (index < previousIndex - 2) {
                        isFloatingVisible = false
                    } else {
                        isFloatingVisible = true
                    }
                } else {
                    if (scrollOffset < previousScrollOffset - 2) {
                        isFloatingVisible = false
                    } else if (scrollOffset > previousScrollOffset) {
                        isFloatingVisible = true
                    }
                }
                previousIndex = index
                previousScrollOffset = scrollOffset
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp) // 调整垂直填充
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = chatTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                items(chatList.value) { message ->
                    val isCurrentUser =
                        (message.senderId as? TdApi.MessageSenderUser)?.userId == currentUserId
                    val backgroundColor =
                        if (isCurrentUser) Color(0xFF003C68) else Color(0xFF2C323A)
                    val textColor = if (isCurrentUser) Color(0xFF66D3FE) else Color(0xFFFEFEFE)
                    val alignment = if (isCurrentUser) Arrangement.End else Arrangement.Start

                    Row(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = (message.content as? TdApi.MessageText)?.text?.text
                                    ?: stringResource(id = R.string.Unknown_Message),
                                color = textColor,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(70.dp)) // 添加一个高度为 70dp 的 Spacer
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // 将按钮置于底部中央
                .padding(bottom = 4.dp) // 添加底部填充以将按钮稍微往下移动
                .alpha(if (isFloatingVisible) 1f else 0f), // 根据 isFloatingVisible 控制透明度
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically // 使内容在垂直方向上居中对齐
        ) {
            IconButton(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .size(84.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_custom_keyboard),
                    contentDescription = null,
                    modifier = Modifier.size(82.dp)
                )
            }

            IconButton(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .size(45.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_custom_send),
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashChatScreenPreview() {
    val sampleMessages = remember {
        mutableStateOf(
            listOf(
                TdApi.Message().apply {
                    id = 1
                    senderId = TdApi.MessageSenderUser(1) // 当前用户
                    content = TdApi.MessageText(
                        TdApi.FormattedText(
                            "我去，大佬你用qt开发的吗，太美了",
                            emptyArray()
                        ), null
                    )
                },
                TdApi.Message().apply {
                    id = 2
                    senderId = TdApi.MessageSenderUser(2) // 对方用户
                    content = TdApi.MessageText(TdApi.FormattedText("你再骂！", emptyArray()), null)
                },
                TdApi.Message().apply {
                    id = 3
                    senderId = TdApi.MessageSenderUser(2) // 对方用户
                    content = TdApi.MessageText(
                        TdApi.FormattedText(
                            "这可是用高贵的jetpack compose写的。\n原生啊，原生懂吗？",
                            emptyArray()
                        ), null
                    )
                }
            )
        )
    }

    TelewatchTheme {
        SplashChatScreen(chatTitle = "gohj99", chatList = sampleMessages, currentUserId = 1L)
    }
}
