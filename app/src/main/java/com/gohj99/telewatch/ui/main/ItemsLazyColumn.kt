/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.ui.SearchBar
import com.gohj99.telewatch.ui.verticalRotaryScroll
import org.drinkless.tdlib.TdApi

// 字符串匹配
fun matchingString(target: String, original: String): Boolean {
    return if (target != "")
        if (original != "") original.contains(target, ignoreCase = true)
        else false
    else true
}

// 定义一个数据类
@SuppressLint("ParcelCreator")
data class Chat(
    val id: Long,
    val title: String,
    val message: String = "",
    val isPinned: Boolean = false, // 是否在全部会话置顶
    val isRead: Boolean = false, // 聊天是否已读
    val isBot: Boolean = false, // 是否为机器人对话
    val isChannel: Boolean = false, // 是否为频道
    val isGroup: Boolean = false, // 是否为群组或者超级群组（supergroups??
    val isPrivateChat: Boolean = false // 是否为私人会话
) : Parcelable {
    override fun describeContents(): Int {
        return 0 // 通常返回0即可，除非有特殊情况需要返回其他值
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        dest.writeString(message)
    }

    companion object CREATOR : Parcelable.Creator<Chat> {
        override fun createFromParcel(parcel: Parcel): Chat {
            return Chat(
                parcel.readLong(),
                parcel.readString() ?: "",
                parcel.readString() ?: ""
            )
        }

        override fun newArray(size: Int): Array<Chat?> {
            return arrayOfNulls(size)
        }
    }
}

fun <T> MutableState<List<T>>.add(item: T) {
    val updatedList = this.value.toMutableList()
    updatedList.add(item)
    this.value = updatedList
}

@Composable
fun ChatLazyColumn(
    itemsList: MutableState<List<Chat>>,
    callback: (Chat) -> Unit,
    chatsFolder: TdApi.ChatFolder? = null,
    contactsList: List<Chat> = listOf()
) {
    val listState = rememberLazyListState()
    val searchText = rememberSaveable { mutableStateOf("") }

    // 延迟加载时检测到滚动接近底部时加载更多聊天项
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index >= itemsList.value.size - 5) {
                    TgApiManager.tgApi?.loadChats(itemsList.value.size + 1)
                }
            }
    }

    // 获取context
    val context = LocalContext.current
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val isHomePagePin = settingsSharedPref.getBoolean("is_home_page_pin", false)

    // 转换 contactsList 为 Set<Long>，确保类型统一
    val contactsSet by remember(contactsList) {
        derivedStateOf { contactsList.map { it.id }.toSet() }
    }

    var includedChatIdsSet: Set<Long> by remember { mutableStateOf(emptySet()) }
    var excludedChatIdsSet: Set<Long> by remember { mutableStateOf(emptySet()) }

    if (chatsFolder != null) {
        includedChatIdsSet = chatsFolder.includedChatIds.map { it }.toSet()
        excludedChatIdsSet = chatsFolder.excludedChatIds.map { it }.toSet()
    }

    // 只有首次加载时分区数据，之后直接更新 pinned 和 regular 数据
    val pinnedChats = remember { mutableStateOf<List<Chat>>(emptyList()) }
    val regularChats = remember { mutableStateOf<List<Chat>>(emptyList()) }

    // 每次itemsList更新时，增量更新 pinnedChats 和 regularChats
    LaunchedEffect(itemsList.value) {
        if (chatsFolder == null) {
            if (isHomePagePin) {
                val newPinnedChats = itemsList.value.filter { it.isPinned }
                val newRegularChats = itemsList.value.filter { it !in newPinnedChats }

                if (newPinnedChats != pinnedChats.value) {
                    pinnedChats.value = newPinnedChats
                }

                if (newRegularChats != regularChats.value) {
                    regularChats.value = newRegularChats
                }
            } else {
                // 如果 chatsFolder 为 null 且 isHomePagePin 为 false，不需要进行置顶消息分割
                pinnedChats.value = emptyList()
                regularChats.value = emptyList()
            }
        } else {
            val newPinnedChats = itemsList.value.filter { it.id in chatsFolder.pinnedChatIds } // 通过 pinnedChatIds 判断
            val newRegularChats = itemsList.value.filter { it !in newPinnedChats }

            if (newPinnedChats != pinnedChats.value) {
                pinnedChats.value = newPinnedChats
            }

            if (newRegularChats != regularChats.value) {
                regularChats.value = newRegularChats
            }
        }
    }

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
            // 搜索框
            SearchBar(
                query = searchText.value,
                onQueryChange = { searchText.value = it },
                placeholder = stringResource(id = R.string.Search),
                modifier = Modifier
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 渲染聊天列表，首先渲染置顶消息
        if (chatsFolder == null) {
            if (isHomePagePin) {
                // 渲染置顶消息
                items(pinnedChats.value, key = { "${it.id}_${it.isPinned}" }) { item ->
                    ChatView(item, callback, searchText, pinnedView = true)
                }
                // 渲染普通消息
                items(regularChats.value, key = { it.id }) { item ->
                    ChatView(item, callback, searchText, pinnedView = false)
                }
            } else {
                // 渲染所有消息（置顶和非置顶）
                items(itemsList.value, key = { it.id }) { item ->
                    ChatView(item, callback, searchText)
                }
            }
        } else {
            // 渲染置顶消息
            items(pinnedChats.value, key = { "${it.id}_${chatsFolder.title}_${it.isPinned}" }) { item ->
                ChatView(
                    chat = item,
                    callback = callback,
                    searchText = searchText,
                    pinnedView = true,
                    chatFolderInfo = chatsFolder,
                    contactsSet = contactsSet,
                    includedChatIdsSet = includedChatIdsSet,
                    excludedChatIdsSet = excludedChatIdsSet
                )
            }
            // 渲染普通消息
            items(regularChats.value, key = { "${it.id}_${chatsFolder.title}" }) { item ->
                ChatView(
                    chat = item,
                    callback = callback,
                    searchText = searchText,
                    pinnedView = false,
                    chatFolderInfo = chatsFolder,
                    contactsSet = contactsSet,
                    includedChatIdsSet = includedChatIdsSet,
                    excludedChatIdsSet = excludedChatIdsSet
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ContactsLazyColumn(itemsList: List<Chat>, callback: (Chat) -> Unit) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize() // 确保 LazyColumn 填满父容器
            .padding(horizontal = 16.dp) // 只在左右添加 padding
            .verticalRotaryScroll(listState)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp)) // 添加一个高度为 8dp 的 Spacer
        }
        items(itemsList) { item ->
            ChatView(item, callback)
        }
        item {
            Spacer(modifier = Modifier.height(50.dp)) // 添加一个高度为 50dp 的 Spacer
        }
    }
}

@Composable
fun ChatView(
    chat: Chat,
    callback: (Chat) -> Unit,
    searchText: MutableState<String> = mutableStateOf(""),
    pinnedView: Boolean? = null,
    chatFolderInfo: TdApi.ChatFolder? = null,
    contactsSet: Set<Long> = emptySet(),  // 确保类型为 Long
    includedChatIdsSet: Set<Long> = emptySet(),  // 确保类型为 Long
    excludedChatIdsSet: Set<Long> = emptySet(),  // 确保类型为 Long
    notJoin: Boolean = false
) {
    // 使用 derivedStateOf 来确保不必要的重渲染
    val isMatchingSearchText by remember(searchText.value) {
        derivedStateOf { matchingString(searchText.value, chat.title) }
    }

    if (isMatchingSearchText) {
        if (chatFolderInfo == null) {
            if (pinnedView == null) {
                MainCard(
                    column = {
                        Text(
                            text = chat.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (chat.message.isNotEmpty()) {
                            MessageView(message = chat.message)
                        }
                    },
                    item = chat,
                    callback = { callback(chat) }
                )
            } else {
                if (chat.isPinned == pinnedView) {
                    MainCard(
                        column = {
                            Text(
                                text = chat.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (chat.message.isNotEmpty()) {
                                MessageView(message = chat.message)
                            }
                        },
                        item = chat,
                        callback = { callback(chat) }
                    )
                }
            }
        } else {
            if ((chat.id in chatFolderInfo.pinnedChatIds.map { it } == pinnedView)) {

                // 基于过滤条件设置显示会话
                val isShow by remember(chat.id, includedChatIdsSet, excludedChatIdsSet, contactsSet) {
                    derivedStateOf {
                        when {
                            chat.id in excludedChatIdsSet -> false
                            chat.id in includedChatIdsSet -> true
                            chat.isChannel && chatFolderInfo.includeChannels -> true
                            chat.isGroup && chatFolderInfo.includeGroups -> true
                            chat.isPrivateChat -> when {
                                chat.isBot -> chatFolderInfo.includeBots
                                chat.id in contactsSet -> chatFolderInfo.includeContacts
                                else -> chatFolderInfo.includeNonContacts
                            }
                            else -> false
                        }
                    }
                }

                if (isShow) {
                    MainCard(
                        column = {
                            Text(
                                text = chat.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (chat.message.isNotEmpty()) {
                                MessageView(message = chat.message)
                            }
                        },
                        item = chat,
                        callback = { callback(chat) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageView(message: String) {
    var currentMessage by remember { mutableStateOf(message) }

    // 如果消息更新了，才重新设置状态
    if (currentMessage != message) {
        currentMessage = message
    }

    if (currentMessage.isNotEmpty()) {
        Text(
            text = currentMessage,
            color = Color(0xFF728AA5),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
