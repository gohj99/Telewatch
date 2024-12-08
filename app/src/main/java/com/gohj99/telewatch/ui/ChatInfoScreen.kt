/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.gohj99.telewatch.ImgViewActivity
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.formatJson
import com.gohj99.telewatch.model.tgFile
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.utils.urlHandle
import com.google.gson.Gson
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi
import java.io.IOException

@Composable
fun SplashChatInfoScreen(
chatObject: TdApi.Chat,
subtitle: String,
info: String
) {
    val gson = Gson()
    var messageJson = ""
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

//    val markwon = remember { Markwon.create(context) }
//    val infoSpanned: Spanned = remember(info) { markwon.toMarkdown(info) }
//    val infoAnnotatedString = remember(infoSpanned) {
//        androidx.compose.ui.text.AnnotatedString(
//            infoSpanned.toString()
//        )
//    }

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
                // 头像与标题
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
                                if (chatObject.photo != null) ThumbnailChatPhoto(chatObject.photo!!.small) {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            ImgViewActivity::class.java
                                        ).apply {
                                            val bigPhotoFile = tgFile(chatObject.photo!!.big)
                                            putExtra("file", bigPhotoFile)
                                        }
                                    )
                                }
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
                // 详细信息
                if (info != "") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() // 只填充宽度
                    ) {
                        MainCard(
                            column = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.Information),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    SelectionContainer {
                                        LinkText(
                                            text = info,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            onLinkClick = { url ->
                                                urlHandle(url, context)
                                            }
                                        )
                                    }
                                }
                            },
                            item = "chat",
                            callback = { }
                        )
                    }
                }
            }
            item {
                // 其他json信息
                Spacer(modifier = Modifier.height(35.dp))
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
                    if (isExpanded) {
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
                    } else {
                        /*
                        Text(
                            text = stringResource(R.string.Show_more),
                            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        */
                        CustomButton(
                            text = stringResource(R.string.Show_more),
                            onClick = {
                                messageJson = formatJson(gson.toJson(chatObject))
                                println(messageJson)
                                isExpanded = true
                            }
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun ThumbnailChatPhoto(thumbnail: TdApi.File, callback: () -> Unit) {
    var imagePath by remember { mutableStateOf<String?>(null) }
    val painter = rememberAsyncImagePainter(model = imagePath) // 在Composable作用域

    LaunchedEffect(thumbnail.id) {
        while (true) {
            if (thumbnail.local.isDownloadingCompleted) {
                try {
                    imagePath = thumbnail.local.path // 更新状态触发重组
                    break
                } catch (e: IOException) {
                    println("Image load failed: ${e.message}")
                    // 处理错误，例如显示占位符
                    break
                }
            } else {
                //println("本地没图片，正在下载图片")
                try {
                    TgApiManager.tgApi!!.downloadPhoto(thumbnail) { success, path ->
                        if (success) {
                            thumbnail.local.isDownloadingCompleted = true
                            thumbnail.local.path = path
                        } else {
                            println("Download failed")

                        }
                    }
                } catch (e: Exception) {
                    println("Download error: ${e.message}")

                }
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(CircleShape)
            .clickable {
                // 在这里处理点击事件
                //println("Box clicked!")
                callback()
            }
    ) {
        if (imagePath != null) { // imagePath 非空才显示图片
            Image(
                painter = painter,
                contentDescription = "Thumbnail",
                modifier = Modifier.clip(CircleShape)
            )
        }
        /*else {
            // 根据 painterResource 的状态显示占位符
            when (painterResource) {
                is AsyncImagePainter.State.Loading -> {/* 显示加载指示器 */}
                is AsyncImagePainter.State.Error -> {/* 显示错误指示器 */}
                else -> {/* 显示默认占位符 */}
            }
        }*/
    }
}

@Preview(showBackground = true)
@Composable
fun SplashChatInfoScreenPreview() {
    SplashChatInfoScreen(chatObject = TdApi.Chat(), "群组", "电话号码")
}
