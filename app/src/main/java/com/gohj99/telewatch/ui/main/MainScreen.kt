/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import android.annotation.SuppressLint
import androidx.compose.animation.core.MutableTransitionState
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
import androidx.compose.runtime.LaunchedEffect
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
import com.gohj99.telewatch.SettingItem
import com.gohj99.telewatch.ui.setting.SettingLazyColumn
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import org.drinkless.tdlib.TdApi

@Composable
fun MainScreen(
    chats: MutableState<List<Chat>>,
    chatPage: (Chat) -> Unit,
    settingList: MutableState<List<SettingItem>>,
    contacts: MutableState<List<Chat>>,
    topTitle: MutableState<String>,
    chatsFoldersList: MutableState<List<TdApi.ChatFolder>>
) {
    val contact = stringResource(id = R.string.Contacts)
    val home = stringResource(id = R.string.HOME)
    val setting = stringResource(id = R.string.Settings)
    var showMenu by remember { mutableStateOf(false) }
    val lastPages = listOf(
        contact,
        setting,
    )
    var allPages by remember {
        mutableStateOf(listOf(home) + lastPages)  // 直接合并两个列表
    }
    var nowPage by remember { mutableStateOf(allPages[0]) }

    LaunchedEffect(chatsFoldersList.value) {
        allPages = mutableListOf<String>().apply {
            add(home)
            addAll(chatsFoldersList.value.map { it.title })
            addAll(lastPages.toList())
        }
        if (nowPage !in allPages) {
            nowPage = home
        }
    }

    // 使用 Column 包裹 Box 和 ChatLazyColumn
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 包含 Row 的 Box
        Box(
            modifier = Modifier
                .fillMaxWidth() // 只填充宽度
                .padding(top = 14.dp) // 添加顶部填充
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
                    modifier = Modifier.size(19.9114514.dp) // 设置图片大小
                )
                Spacer(modifier = Modifier.width(8.dp)) // 添加间距
                Text(
                    text = if (nowPage !in lastPages)
                        if (topTitle.value == "") nowPage
                        else topTitle.value
                    else nowPage,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp)) // 添加间距

        val transitionState = remember { MutableTransitionState(false) }
        transitionState.targetState = showMenu

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
                home -> {
                    ChatLazyColumn(
                        itemsList = chats,
                        callback = chatPage
                    )
                }

                contact -> {
                    ContactsLazyColumn(
                        itemsList = contacts.value,
                        callback = chatPage
                    )
                }

                setting -> {
                    SettingLazyColumn(settingList)
                }
                else -> {
                    if (nowPage in chatsFoldersList.value.map { it.title }) {
                        ChatLazyColumn(
                            itemsList = chats,
                            callback = chatPage,
                            chatsFolder = chatsFoldersList.value.find { it.title == nowPage },
                            contactsList = contacts.value
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    // 傻逼钱显康
    // 户籍：湖北省随州市曾都区 西城办事处沿江大道22号(地图查验为宾馆)
    // 住址: 湖北省随州市曾都区汉东名居(无精确门牌)
    // 学校：湖北省随州市二中
    // 申必代码：421302200801060033
    // “个人”网站： https://www.rechrd.top/

    val sampleChats = mutableStateOf(
        listOf(
            Chat(id = 1, title = "钱显康", message = "我是傻逼"),
            Chat(id = 2, title = "Rechrd", message = "我父亲是钱明"),
            Chat(id = 3, title = "将军", message = "我母亲是康庆莉")
        )
    )

    val settings = remember {
        mutableStateOf(
            listOf<SettingItem>(
                SettingItem.Click(
                    itemName = "设置1",
                    onClick = {}
                ),
                SettingItem.Click(
                    itemName = "设置2",
                    onClick = {}
                ), SettingItem.Click(
                    itemName = "设置3",
                    onClick = {}
                ), SettingItem.Click(
                    itemName = "设置4",
                    onClick = {}
                ), SettingItem.Click(
                    itemName = "设置5",
                    onClick = {}
                )
            )
        )
    }

    TelewatchTheme {
        MainScreen(
            chats = sampleChats,
            chatPage = {},
            settingList = settings,
            contacts = remember { mutableStateOf(listOf()) },
            topTitle = remember { mutableStateOf("Home") },
            chatsFoldersList = remember { mutableStateOf(listOf()) }
        )
    }
}
