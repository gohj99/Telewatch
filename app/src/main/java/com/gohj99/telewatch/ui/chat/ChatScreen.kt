/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.AutoScrollingText
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.telegram.getChat
import com.gohj99.telewatch.utils.telegram.getUserName
import com.gohj99.telewatch.utils.telegram.joinChat
import com.gohj99.telewatch.utils.telegram.markMessagesAsRead
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object MessageCache {
    private val cache = mutableMapOf<Int, TdApi.MessageContent>()
    private var cacheSize = 0

    fun put(content: TdApi.MessageContent) : Int {
        cacheSize += 1
        cache[cacheSize] = content
        return cacheSize
    }

    fun get(cacheSize: Int): TdApi.MessageContent? = cache[cacheSize]
}

// 反射机制获取MessageContent的类信息
fun getMessageContentTypeName(messageContent: TdApi.MessageContent): String {
    return messageContent::class.simpleName ?: "Unknown"
}

// 复制对应内容
fun copyText(text: String, context: Context) {
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = android.content.ClipData.newPlainText("Copied Text", text)
    clipboardManager.setPrimaryClip(clipData)
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SplashChatScreen(
    chatTitle: String,
    chatList: MutableState<List<TdApi.Message>>,
    chatId: Long,
    goToChat: (Chat) -> Unit,
    press: (TdApi.Message) -> Unit,
    longPress: suspend (String, TdApi.Message) -> String,
    chatObject: TdApi.Chat,
    lastReadOutboxMessageId: MutableState<Long>,
    lastReadInboxMessageId: MutableState<Long>,
    inputText: MutableState<String>,
    listState: LazyListState = rememberLazyListState(),
    onLinkClick: (String) -> Unit,
    chatTitleClick: () -> Unit,
    currentUserId: MutableState<Long>
) {
    // 获取context
    val context = LocalContext.current

    var isFloatingVisible by remember { mutableStateOf(true) }
    var isLongPressed = remember { mutableStateOf(false) }
    val selectMessage = remember {
        mutableStateOf<TdApi.Message>(TdApi.Message())
    }
    val senderNameMap = remember { mutableStateOf(mutableMapOf<Long, String?>()) }
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
    var notJoin by remember { mutableStateOf(chatObject.positions.isEmpty()) }
    val coroutineScope = rememberCoroutineScope()
    var planReplyMessage = remember { mutableStateOf(tgApi!!.replyMessage.value) }
    var planReplyMessageSenderName by rememberSaveable { mutableStateOf("") }
    var planEditMessage = remember { mutableStateOf<TdApi.Message?>((null)) }
    var planEditMessageText = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    //var chatReadList = tgApi?.chatReadList!!

    // 获取show_unknown_message_type值
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val showUnknownMessageType = settingsSharedPref.getBoolean("show_unknown_message_type", false)

    // 获取加载内容数量
    val messagePreloadQuantity = settingsSharedPref.getInt("Message_preload_quantity", 10)

    // 获取下滑按钮显示偏移量
    val downButtonOffset = settingsSharedPref.getInt("Down_Button_Offset", 25)

    //println(chatsListManager.chatsList.value)
    //val chatPermissions: TdApi.ChatPermissions? = chatObject.permissions

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, scrollOffset) ->
                if (index != previousIndex) {
                    if (index > previousIndex + 2) {
                        isFloatingVisible = false
                    }
                } else {
                    if (scrollOffset > previousScrollOffset) {
                        isFloatingVisible = false
                    }
                }
                previousIndex = index
                previousScrollOffset = scrollOffset
            }
    }

    // 更新将回复消息的发送者
    LaunchedEffect(planReplyMessage.value) {
        if (planReplyMessage.value != null) {
            when (val sender = planReplyMessage.value!!.senderId) {
                is TdApi.MessageSenderUser -> {
                    if (sender.userId in senderNameMap.value) {
                        planReplyMessageSenderName = senderNameMap.value[sender.userId]!!
                    } else {
                        tgApi?.getUserName(sender.userId) { user ->
                            planReplyMessageSenderName = user
                            senderNameMap.value[sender.userId] = user
                        }
                    }
                    val replyChatId = planReplyMessage.value!!.chatId
                    if (replyChatId != chatId && replyChatId != sender.userId) {
                        val itChat = tgApi?.getChat(replyChatId)
                        itChat.let {
                            planReplyMessageSenderName += " -> ${it!!.title}"
                        }
                    }
                }
                is TdApi.MessageSenderChat -> {
                    if (sender.chatId == chatId) {
                        planReplyMessageSenderName = chatTitle
                    } else {
                        val itChat = tgApi?.getChat(sender.chatId)
                        itChat.let {
                            planReplyMessageSenderName = it!!.title
                        }
                    }
                    val replyChatId = planReplyMessage.value!!.chatId
                    if (replyChatId != chatId && replyChatId != sender.chatId) {
                        val itChat = tgApi?.getChat(replyChatId)
                        itChat.let {
                            planReplyMessageSenderName += " -> ${it!!.title}"
                        }
                    }
                }
                else -> "" // 处理未知类型
            }
        }
    }

    // 向后加载消息
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index >= chatList.value.size - messagePreloadQuantity) {
                    tgApi?.fetchMessages(fromMessageId =  chatList.value.lastOrNull()?.id ?: -1L,nowChatId = chatId)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 标题
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp) // 调整垂直填充
                    .clickable(
                        onClick = { chatTitleClick() }
                    )
            ) {
                AutoScrollingText(
                    text = chatTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // 消息部分
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 0.dp)
                                .verticalRotaryScroll(listState, true, pagerState, 0)
                                .weight(1f),
                            reverseLayout = true, // 反转布局
                            verticalArrangement = Arrangement.Top,
                        ) {
                            item {
                                // 未加入会话
                                if (notJoin) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 28.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CustomButton(
                                            onClick = {
                                                println("按钮点击")
                                                tgApi?.joinChat(
                                                    chatId = chatId,
                                                    reInit = { }
                                                )
                                                notJoin = false
                                            },
                                            text = stringResource(id = R.string.join_in)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(55.dp))
                                }
                            }
                            // 消息正文
                            itemsIndexed(
                                chatList.value,
                                key = { _, message -> "${message.id}-${message.date}-${message.editDate}" }
                            ) { index, message ->
                                val isCurrentUser = message.isOutgoing
                                val backgroundColor =
                                    if (isCurrentUser) Color(0xFF003C68) else Color(0xFF2C323A)
                                val textColor = if (isCurrentUser) Color(0xFF66D3FE) else Color(0xFFFEFEFE)
                                val alignment = if (isCurrentUser) Arrangement.End else Arrangement.Start
                                val modifier = if (isCurrentUser) Modifier.align(Alignment.End) else Modifier
                                var stateDownloadDone = rememberSaveable { mutableStateOf(false) }
                                var stateDownload = rememberSaveable { mutableStateOf(false) }

                                tgApi?.markMessagesAsRead(messageId = message.id, chatId = chatId)

                                MessageHandleCompose(
                                    message = message,
                                    chatList = chatList,
                                    index = index,
                                    chatId = chatId,
                                    chatTitle = chatTitle,
                                    isCurrentUser = isCurrentUser,
                                    modifier = modifier,
                                    alignment = alignment,
                                    textColor = textColor,
                                    backgroundColor = backgroundColor,
                                    stateDownload = stateDownload,
                                    stateDownloadDone = stateDownloadDone,
                                    showUnknownMessageType = showUnknownMessageType,
                                    selectMessage = selectMessage,
                                    isLongPressed = isLongPressed,
                                    senderNameMap = senderNameMap,
                                    listState = listState,
                                    press = press,
                                    lastReadOutboxMessageId = lastReadOutboxMessageId,
                                    lastReadInboxMessageId = lastReadInboxMessageId,
                                    onLinkClick = onLinkClick,
                                    goToChat = goToChat,
                                )
                            }
                        }
                    }
                    1 -> {
                        // 发送消息页面部分
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 0.dp)
                                .verticalScroll(scrollState)
                                .verticalRotaryScroll(
                                    state = scrollState,
                                    pagerState = pagerState,
                                    pageCurrent = 1
                                ),
                            verticalArrangement = Arrangement.Top
                        ) {
                            SendMessageCompose(
                                chatId = chatId,
                                inputText = inputText,
                                currentUserId = currentUserId,
                                planReplyMessage = planReplyMessage,
                                planReplyMessageSenderName = planReplyMessageSenderName,
                                planEditMessage = planEditMessage,
                                planEditMessageText = planEditMessageText,
                                listState = listState,
                                pagerState = pagerState,
                                showUnknownMessageType = showUnknownMessageType,
                                onLinkClick = onLinkClick,
                            )
                        }
                    }
                }
            }
        }

        // 长按处理
        if (isLongPressed.value) {
            LongPressBox(
                callBack = { select ->
                    when (select) {
                        // 特殊处理
                        "Edit" -> {
                            val content = selectMessage.value.content
                            if (content is TdApi.MessageText) {
                                planEditMessage.value = selectMessage.value
                                planEditMessageText.value = content.text.text
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            } else {
                                longPress(select, selectMessage.value)
                            }
                            ""
                        }
                        "Reply" -> {
                            // 返回空字符串同时执行操作
                            planReplyMessage.value = selectMessage.value
                            tgApi!!.replyMessage.value = selectMessage.value
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                            ""
                        }
                        else -> return@LongPressBox longPress(select, selectMessage.value)
                    }
                },
                onDismiss = { isLongPressed.value = false }
            )
        }

        // 0页下方功能区
        if (pagerState.currentPage == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd) // 定位到右下角
                    .offset(x = (-downButtonOffset).dp, y = (-downButtonOffset).dp) // 向左上偏移，避免紧贴屏幕边缘
            ) {
                MessageBottomFunctionalCompose(
                    listState = listState,
                    chatId = chatId
                )
            }
        }
    }
}

@Composable
fun DateText(date: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            modifier = Modifier,
            color = Color(0xFF3A80BF)
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun SplashChatScreenPreview() {
    val sampleMessages = remember {
        mutableStateOf(
            listOf(
                TdApi.Message().apply {
                    date = 1692127800
                    editDate = 283848839
                    id = 1
                    senderId = TdApi.MessageSenderUser(2) // 对方用户
                    content = TdApi.MessageText(
                        TdApi.FormattedText(
                            "这可是用高贵的jetpack compose写的。\n原生啊，原生懂吗？",
                            emptyArray()
                        ),
                        null,
                        null
                    )
                },
                TdApi.Message().apply {
                    date = 1692127800
                    id = 2
                    senderId = TdApi.MessageSenderUser(2) // 对方用户
                    content = TdApi.MessageText(
                        TdApi.FormattedText("你再骂！", emptyArray()),
                        null,
                        null
                    )
                },
                TdApi.Message().apply {
                    date = 1692127800
                    id = 3
                    senderId = TdApi.MessageSenderUser(1) // 当前用户
                    content = TdApi.MessageText(
                        TdApi.FormattedText(
                            "我去，大佬你用qt开发的吗，太美了",
                            emptyArray()
                        ),
                        null,
                        null
                    )
                },
            )
        )
    }

    TelewatchTheme {
        SplashChatScreen(
            chatTitle = "XCちゃん",
            chatList = sampleMessages,
            chatId = 1L,
            goToChat = { },
            press = {
                println("点击触发")
            },
            longPress = { select, message ->
                println("长按触发")
                println(message)
                return@SplashChatScreen select
            },
            chatObject = TdApi.Chat(),
            lastReadOutboxMessageId = mutableLongStateOf(0L),
            lastReadInboxMessageId = mutableLongStateOf(0L),
            inputText = mutableStateOf(""),
            onLinkClick = {},
            chatTitleClick = {},
            currentUserId = mutableLongStateOf(-1L)
        )
    }
}
