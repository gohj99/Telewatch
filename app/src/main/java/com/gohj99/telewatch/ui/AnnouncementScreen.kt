/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.Announcement
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.utils.urlHandle
import com.google.gson.JsonObject

@Composable
fun SplashAnnouncementScreen(
    announcementList: List<Announcement> = listOf(),
    jsonObject: JsonObject? = null,
    callback: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val context = LocalContext.current

        if (jsonObject == null) {
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
                        text = stringResource(R.string.announcement_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // 添加间距

            AnnouncementLazyColumn(announcementList, callback)
        } else {
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
                        text = jsonObject["title"]?.asString ?: stringResource(R.string.announcement_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1, // 限制为一行
                        overflow = TextOverflow.Ellipsis // 超出部分省略
                    )
                }

                Spacer(modifier = Modifier.height(4.dp)) // 添加间距

                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                        .verticalRotaryScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(35.dp)) // 添加一个高度为 8dp 的 Spacer
                    MainCard(
                        column = {
                            SelectionContainer {
                                LinkText(
                                    text = jsonObject["content"]?.asString ?: "Error",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    onLinkClick = { url ->
                                        urlHandle(url, context)
                                    }
                                )
                            }
                        },
                        item = "content",
                        color = Color(0xFF2C323A)
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = jsonObject["created_at"]?.asString ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(top = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementLazyColumn(itemsList: List<Announcement>, callback: (String) -> Unit) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalRotaryScroll(listState)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp)) // 添加一个高度为 8dp 的 Spacer
        }
        items(itemsList.size, key = { it }) { index ->
            val item = itemsList[index]
            MainCard(
                column = {
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                item = item,
                callback = {
                    callback(it.id)
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashAnnouncementScreenPreview() {
    SplashAnnouncementScreen(
        announcementList = listOf(
            Announcement(id = "1", title = "公告1"),
            Announcement(id = "2", title = "公告2"),
            Announcement(id = "3", title = "公告3")
        ),
        callback = {}
    )
}
