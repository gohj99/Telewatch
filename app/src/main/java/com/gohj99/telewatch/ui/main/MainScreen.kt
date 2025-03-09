/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.model.SettingItem
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
    chatsFoldersList: MutableState<List<TdApi.ChatFolder>>,
    currentUserId: MutableState<Long>
) {
    val search = stringResource(id = R.string.Global_Search)
    val contact = stringResource(id = R.string.Contacts)
    val home = stringResource(id = R.string.HOME)
    val setting = stringResource(id = R.string.Settings)
    var showMenu by remember { mutableStateOf(false) }
    val lastPages = listOf(
        search,
        contact,
        setting,
    )
    var allPages by remember {
        mutableStateOf(listOf(home) + lastPages)  // 直接合并两个列表
    }
    var nowPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(chatsFoldersList.value) {
        allPages = mutableListOf<String>().apply {
            add(home)
            addAll(chatsFoldersList.value.map { it.title })
            addAll(lastPages.toList())
        }
        if (nowPage > allPages.size) {
            nowPage = 0
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
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clickable { showMenu = !showMenu }
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
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

                // 关键修改点：用 Row 替代 Box 作为滚动容器
                Row(
                    modifier = Modifier
                        .widthIn(max = 100.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text =
                            if (showMenu) if (nowPage <= allPages.size) allPages[nowPage] else "error$nowPage"
                            else
                                if (nowPage <= allPages.size)
                                    if (nowPage < allPages.size - lastPages.size)
                                        if (topTitle.value == "") allPages[nowPage]
                                        else topTitle.value
                                    else allPages[nowPage]
                                else "error",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .wrapContentWidth(unbounded = true) // 允许内容无限扩展
                    )
                }
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
            if (nowPage < (allPages.size - lastPages.size) && nowPage != 0) {
                if (allPages[nowPage] in chatsFoldersList.value.map { it.title }) {
                    ChatLazyColumn(
                        itemsList = chats,
                        callback = chatPage,
                        chatsFolder = chatsFoldersList.value.find { it.title == allPages[nowPage] },
                        currentUserId = currentUserId,
                        contactsList = contacts.value
                    )
                }
            } else {
                when (if (nowPage <= allPages.size) allPages[nowPage] else "error") {
                    home -> {
                        ChatLazyColumn(
                            itemsList = chats,
                            callback = chatPage,
                            currentUserId = currentUserId,
                        )
                    }

                    search -> {
                        SearchLazyColumn(
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
                }
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
            Chat(id = 1, title = "钱*康", message = buildAnnotatedString { append("我是**") }),
            Chat(id = 2, title = "Rechrd", message = buildAnnotatedString { append("我**是*明") }),
            Chat(id = 3, title = "将军", message = buildAnnotatedString { append("我**是**莉") })
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
            currentUserId = remember { mutableStateOf(-1) },
            chatsFoldersList = remember { mutableStateOf(listOf()) }
        )
    }
}
