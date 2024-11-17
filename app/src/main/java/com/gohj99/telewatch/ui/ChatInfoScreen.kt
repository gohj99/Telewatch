/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.formatJson
import com.gohj99.telewatch.ui.main.MainCard
import com.google.gson.Gson
import org.drinkless.tdlib.TdApi

@Composable
fun SplashChatInfoScreen(chatObject: TdApi.Chat) {
    var subtitle by remember { mutableStateOf("") }
    val gson = Gson()
    val messageJson = formatJson(gson.toJson(chatObject))
    val context = LocalContext.current

    val chatType = chatObject.type
    if (chatType is TdApi.ChatTypePrivate) {
        TgApiManager.tgApi!!.getUser(chatType.userId) { result ->
            when (result!!.status) {
                is TdApi.UserStatusOnline ->
                    subtitle = context.getString(R.string.Online)
                is TdApi.UserStatusEmpty ->
                    subtitle = context.getString(R.string.Unknown)
                is TdApi.UserStatusRecently ->
                    subtitle = context.getString(R.string.Lately)
                is TdApi.UserStatusLastWeek ->
                    subtitle = context.getString(R.string.Last_week)
                is TdApi.UserStatusLastMonth ->
                    subtitle = context.getString(R.string.Last_month)
                is TdApi.UserStatusOffline ->
                    subtitle = context.getString(R.string.Offline)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 标题
        Box(
            modifier = Modifier
                .fillMaxWidth() // 只填充宽度
                .padding(top = 14.dp), // 添加顶部填充
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.Chat_Info),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalRotaryScroll(listState)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // 只填充宽度
                ) {
                    MainCard(
                        column = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Top //  或 Alignment.Bottom，或者移除此行使用默认的 Top 对齐
                            ) {
                                ThumbnailChatPhoto(chatObject.photo!!.small)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Spacer(Modifier.height(1.5.dp))
                                    Text(
                                        text = chatObject.title,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        text = subtitle,
                                        color = Color(0xFF596B7F),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        },
                        item = "chat",
                        callback = { }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = messageJson,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0xFF2C323A).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 10.sp),
                        singleLine = false
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun ThumbnailChatPhoto(
    thumbnail: TdApi.File
) {
    val isDownloaded = remember { mutableStateOf(thumbnail.local.isDownloadingCompleted) }

    if (!isDownloaded.value) {
        LaunchedEffect(thumbnail) {
            println("本地没图片，正在下载图片")
            TgApiManager.tgApi!!.downloadPhoto(thumbnail) { success, path ->
                if (success) {
                    isDownloaded.value = true
                } else {
                    // 处理下载失败
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = thumbnail.local.path),
                contentDescription = "Thumbnail",
                modifier = Modifier
                    .clip(CircleShape)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashChatInfoScreenPreview() {
    SplashChatInfoScreen(chatObject = TdApi.Chat())
}
