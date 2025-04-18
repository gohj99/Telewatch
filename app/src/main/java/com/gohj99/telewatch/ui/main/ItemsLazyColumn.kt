/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.SearchBar
import com.gohj99.telewatch.ui.ThumbnailChatPhoto
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.formatTimestampToDateAndTime
import com.gohj99.telewatch.utils.getColorById
import org.drinkless.tdlib.TdApi

// 字符串匹配
fun matchingString(target: String, original: String): Boolean {
    return if (target != "")
        if (original != "") original.contains(target, ignoreCase = true)
        else false
    else true
}

@Composable
fun ChatLazyColumn(
    itemsList: MutableState<List<Chat>>,
    callback: (Chat) -> Unit,
    chatsFolder: TdApi.ChatFolder? = null,
    contactsList: List<Chat> = listOf(),
    currentUserId: MutableState<Long>
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

    /*
    // 获取context
    val context = LocalContext.current
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val isHomePagePin = settingsSharedPref.getBoolean("is_home_page_pin", false)
    */

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
        // 处理可能存在的 order 字段缺失问题（使用安全调用和默认值）
        val sortedList = itemsList.value
            .filter {
                //println("Filtering chat ${it.id}: order=${it.order}, isArchive=${it.isArchiveChatPin}")
                it.order !in listOf(-1L, 0L) && it.isArchiveChatPin == null
            }
            .sortedWith(
                // 第一排序条件：order不为null时降序排列
                compareByDescending<Chat> { it.order }
                    // 第二排序条件：当order相同时按id降序排列
                    .thenByDescending { it.id }
            )

        if (chatsFolder == null) {
            // 分离置顶消息时需要保持原有置顶顺序
            val newPinnedChats = sortedList
                .filter { it.isPinned }
                .sortedWith(compareByDescending { it.order ?: Long.MIN_VALUE }) // 置顶组内单独排序

            val newRegularChats = sortedList
                .filter { it !in newPinnedChats }
                .sortedWith(compareByDescending { it.order ?: Long.MIN_VALUE }) // 非置顶组内单独排序


            if (newPinnedChats != pinnedChats.value) {
                pinnedChats.value = newPinnedChats
            }

            if (newRegularChats != regularChats.value) {
                regularChats.value = newRegularChats
            }
        } else {
            // 使用 sortedList 替代原始 itemsList.value
            val newPinnedChats = sortedList.filter { it.id in chatsFolder.pinnedChatIds }
            val newRegularChats = sortedList.filter { it !in newPinnedChats }

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
        verticalArrangement = Arrangement.spacedBy(0.dp),
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
            // 渲染置顶消息
            items(pinnedChats.value, key = { "${it.id}_${it.isPinned}" }) { item ->
                ChatView(
                    chat = item,
                    callback = callback,
                    searchText = searchText,
                    currentUserId = currentUserId,
                    pinnedView = true
                )
            }
            // 渲染普通消息
            items(regularChats.value, key = { it.id }) { item ->
                ChatView(
                    chat = item,
                    callback = callback,
                    searchText = searchText,
                    currentUserId = currentUserId,
                    pinnedView = false
                )
            }
        } else {
            // 渲染置顶消息
            items(pinnedChats.value, key = { "${it.id}_${chatsFolder.title}_${it.isPinned}" }) { item ->
                ChatView(
                    chat = item,
                    callback = callback,
                    searchText = searchText,
                    currentUserId = currentUserId,
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
                    currentUserId = currentUserId,
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
fun ArchivedChatsLazyColumn(itemsList: MutableState<List<Chat>>, callback: (Chat) -> Unit) {
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

    // 只有首次加载时分区数据，之后直接更新 pinned 和 regular 数据
    val pinnedChats = remember { mutableStateOf<List<Chat>>(emptyList()) }
    val regularChats = remember { mutableStateOf<List<Chat>>(emptyList()) }

    // 每次itemsList更新时，增量更新 pinnedChats 和 regularChats
    LaunchedEffect(itemsList.value) {
        // 处理可能存在的 order 字段缺失问题（使用安全调用和默认值）
        val sortedList = itemsList.value
            .filter {
                it.isArchiveChatPin != null  // 仅接受归档会话
            }
            .sortedWith(
                // 第一排序条件：order不为null时降序排列
                compareByDescending<Chat> { it.order }
                    // 第二排序条件：当order相同时按id降序排列
                    .thenByDescending { it.id }
            )

        // 分离置顶消息时需要保持原有置顶顺序
        val newPinnedChats = sortedList
            .filter { it.isPinned }
            .sortedWith(compareByDescending { it.order ?: Long.MIN_VALUE }) // 置顶组内单独排序

        val newRegularChats = sortedList
            .filter { it !in newPinnedChats }
            .sortedWith(compareByDescending { it.order ?: Long.MIN_VALUE }) // 非置顶组内单独排序


        if (newPinnedChats != pinnedChats.value) {
            pinnedChats.value = newPinnedChats
        }

        if (newRegularChats != regularChats.value) {
            regularChats.value = newRegularChats
        }
    }

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
        // 渲染置顶消息
        items(pinnedChats.value, key = { "${it.id}_${it.isPinned}" }) { item ->
            ChatView(
                chat = item,
                callback = callback,
                searchText = searchText,
                pinnedView = true
            )
        }
        // 渲染普通消息
        items(regularChats.value, key = { it.id }) { item ->
            ChatView(
                chat = item,
                callback = callback,
                searchText = searchText,
                pinnedView = false
            )
        }
        item {
            Spacer(modifier = Modifier.height(50.dp)) // 添加一个高度为 50dp 的 Spacer
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
    currentUserId: MutableState<Long> = mutableStateOf(-1),
    pinnedView: Boolean? = null,
    chatFolderInfo: TdApi.ChatFolder? = null,
    contactsSet: Set<Long> = emptySet(),  // 确保类型为 Long
    includedChatIdsSet: Set<Long> = emptySet(),  // 确保类型为 Long
    excludedChatIdsSet: Set<Long> = emptySet(),  // 确保类型为 Long
) {
    // 使用 derivedStateOf 来确保不必要的重渲染
    val isMatchingSearchText by remember(searchText.value) {
        derivedStateOf { matchingString(searchText.value, chat.title) }
    }

    if (isMatchingSearchText) {
        if (chatFolderInfo == null) {
            if (pinnedView == null) {
                ChatViewMainCard(chat, callback, currentUserId)
            } else {
                if (chat.isPinned == pinnedView) {
                    ChatViewMainCard(chat, callback, currentUserId)
                }
            }
        } else {
            if ((chat.id in chatFolderInfo.pinnedChatIds.map { it } == pinnedView)) {

                // 基于过滤条件设置显示会话
                //println(chat.isBot)
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
                    ChatViewMainCard(chat, callback, currentUserId)
                }
            }
        }
    }
}

@Composable
fun ChatViewMainCard(
    chat: Chat,
    callback: (Chat) -> Unit,
    currentUserId: MutableState<Long> = mutableStateOf(-1)
) {
    MainCard(
        column = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (chat.id == currentUserId.value) {
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .clip(CircleShape)
                            .clickable {
                                // 在这里处理点击事件
                                //println("Box clicked!")
                                callback(chat)
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.saved_messages_icon),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                } else {
                    if (chat.chatPhoto != null) {
                        ThumbnailChatPhoto(chat.chatPhoto, 35, if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title) { callback(chat) }
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(35.dp), // 固定宽高为35dp
                            color = getColorById(chat.accentColorId),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) { // 居中显示文本
                                Text(
                                    text = (if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title)[0].toString().uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Column {
                    Spacer(Modifier.height(1.5.dp))
                    Text(
                        text = if (chat.id == currentUserId.value) stringResource(R.string.Saved_Messages) else chat.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis // 过长省略号
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 时间：使用 weight 占据剩余空间，允许换行显示
                        Text(
                            text = formatTimestampToDateAndTime(chat.lastMessageTime),
                            color = Color(0xFF728AA5),
                            fontSize = 10.5.sp,
                            lineHeight = 10.5.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        // 未读指示器：始终完整显示，不受时间文本影响
                        if (chat.unreadCount > 0) {
                            Surface(
                                modifier = Modifier.wrapContentSize(),
                                color = if (chat.needNotification) Color(0xFF3F81BB) else Color(0xFF49617A),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = chat.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 4.6.dp, vertical = 1.3.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            if (chat.lastMessage.isNotEmpty()) {
                MessageView(message = chat.lastMessage)
            }

        },
        item = chat,
        callback = { callback(chat) },
        modifier = Modifier.padding(start = 10.dp, top = 6.dp, end = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun MessageView(message: androidx.compose.ui.text.AnnotatedString) {
    var currentMessage by remember { mutableStateOf(message) }

    // 如果消息更新了，才重新设置状态
    if (currentMessage != message) {
        currentMessage = message
    }

    if (currentMessage.isNotEmpty()) {
        Text(
            text = currentMessage,
            color = Color(0xFF728AA5),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // 过长省略号
        )
    }
}
