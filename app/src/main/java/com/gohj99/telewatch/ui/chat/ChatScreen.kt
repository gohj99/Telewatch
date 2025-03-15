/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.InputBar
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.main.MessageView
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.formatDuration
import com.gohj99.telewatch.utils.formatTimestampToDate
import com.gohj99.telewatch.utils.formatTimestampToTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.IOException

// 反射机制获取MessageContent的类信息
fun getMessageContentTypeName(messageContent: TdApi.MessageContent): String {
    return messageContent::class.simpleName ?: "Unknown"
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
    listState: LazyListState = rememberLazyListState(),
    onLinkClick: (String) -> Unit,
    chatTitleClick: () -> Unit,
    currentUserId: MutableState<Long>
) {
    // 获取context
    val context = LocalContext.current

    var isFloatingVisible by remember { mutableStateOf(true) }
    var inputText by rememberSaveable { mutableStateOf(("")) }
    var isLongPressed by remember { mutableStateOf(false) }
    var selectMessage by remember { mutableStateOf(TdApi.Message()) }
    val senderNameMap by remember { mutableStateOf(mutableMapOf<Long, String?>()) }
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
    var notJoin = false
    val coroutineScope = rememberCoroutineScope()
    var planReplyMessage by remember { mutableStateOf(tgApi!!.replyMessage.value) }
    var planReplyMessageSenderName by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // 保存和恢复MessageContent
    val MessageContentSaver = Saver<TdApi.MessageContent, Any>(
        save = { messageContent ->
            when (messageContent) {
                is TdApi.MessageText -> mapOf(
                    "type" to "MessageText",
                    "text" to messageContent.text.text,
                    "entities" to messageContent.text.entities.toList(),
                    "linkPreview" to messageContent.linkPreview,
                    "linkPreviewOptions" to messageContent.linkPreviewOptions
                )
                else -> throw IllegalStateException("Unsupported type")
            }
        },
        restore = { value ->
            when ((value as Map<String, *>)["type"]) {
                "MessageText" -> TdApi.MessageText(
                    TdApi.FormattedText(
                        value["text"] as String,
                        (value["entities"] as List<TdApi.TextEntity>).toTypedArray()
                    ),
                    value["linkPreview"] as? TdApi.LinkPreview,
                    value["linkPreviewOptions"] as? TdApi.LinkPreviewOptions
                )
                else -> throw IllegalStateException("Unsupported type")
            }
        }
    )

    // 获取show_unknown_message_type值
    val settingsSharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val showUnknownMessageType = settingsSharedPref.getBoolean("show_unknown_message_type", false)

    //println(chatsListManager.chatsList.value)
    val chatPermissions: TdApi.ChatPermissions? = chatObject.permissions
    if (chatPermissions != null) {
        if (!chatPermissions.canSendBasicMessages) {
            if (chatObject.positions.isEmpty()) notJoin = true
        }
    } else {
        if (chatObject.positions.isEmpty()) notJoin = true
    }

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
    LaunchedEffect(planReplyMessage) {
        if (planReplyMessage != null) {
            when (val sender = planReplyMessage!!.senderId) {
                is TdApi.MessageSenderUser -> {
                    if (sender.userId in senderNameMap) {
                        planReplyMessageSenderName = senderNameMap[sender.userId]!!
                    } else {
                        tgApi?.getUserName(sender.userId) { user ->
                            planReplyMessageSenderName = user
                            senderNameMap[sender.userId] = user
                        }
                    }
                    val replyChatId = planReplyMessage!!.chatId
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
                    val replyChatId = planReplyMessage!!.chatId
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

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index >= chatList.value.size - 5) {
                    tgApi?.fetchMessages()
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
            //println("开始渲染")
            Box(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp) // 调整垂直填充
            ) {
                ClickableText(
                    text = AnnotatedString(if (chatTitle.length > 15) chatTitle.take(15) + "..." else chatTitle),
                    style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFFEFEFE), fontWeight = FontWeight.Bold),
                    onClick = { chatTitleClick() }
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
                                .verticalRotaryScroll(listState, true)
                                .weight(1f),
                            reverseLayout = true, // 反转布局
                            verticalArrangement = Arrangement.Top
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(70.dp)) // 添加一个高度为 70dp 的 Spacer
                            }
                            itemsIndexed(
                                chatList.value,
                                key = { _, message -> message.id.toString() + message.date.toString() }) { index, message ->
                                val isCurrentUser = message.isOutgoing
                                val backgroundColor =
                                    if (isCurrentUser) Color(0xFF003C68) else Color(0xFF2C323A)
                                val textColor = if (isCurrentUser) Color(0xFF66D3FE) else Color(0xFFFEFEFE)
                                val alignment = if (isCurrentUser) Arrangement.End else Arrangement.Start
                                val modifier = if (isCurrentUser) Modifier.align(Alignment.End) else Modifier
                                var videoDownloadDone = rememberSaveable { mutableStateOf(false) }
                                var videoDownload = rememberSaveable { mutableStateOf(false) }

                                tgApi?.markMessagesAsRead(message.id)

                                Column {
                                    // 绘制日期
                                    val nextItem = chatList.value.getOrNull(index + 1)
                                    if (nextItem == null){
                                        DateText(formatTimestampToDate(message.date))
                                    } else {
                                        val currentDate = formatTimestampToDate(message.date)
                                        if (formatTimestampToDate(nextItem.date) != currentDate) {
                                            DateText(currentDate)
                                        }
                                    }

                                    // 渲染用户名字
                                    if (!isCurrentUser) {
                                        var senderName by rememberSaveable { mutableStateOf("") }
                                        val senderId = message.senderId
                                        //println("senderId: $senderId")
                                        if (senderId.constructor == TdApi.MessageSenderUser.CONSTRUCTOR){
                                            val senderUser = senderId as TdApi.MessageSenderUser
                                            //println("senderUser: $senderUser")
                                            senderUser.userId.let {
                                                Text(
                                                    text = senderName,
                                                    modifier = Modifier
                                                        .pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onTap = {
                                                                    if (senderUser.userId != chatId) {
                                                                        goToChat(
                                                                            Chat(
                                                                                id = senderUser.userId,
                                                                                title = senderName
                                                                            )
                                                                        )
                                                                    }
                                                                },
                                                                onLongPress = {
                                                                    selectMessage = message
                                                                    isLongPressed = true
                                                                }
                                                            )
                                                        }
                                                        .padding(start = 10.dp, end = 5.dp),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                LaunchedEffect(message.senderId) {
                                                    if (it in senderNameMap) {
                                                        senderName = senderNameMap[it]!!
                                                    } else {
                                                        tgApi?.getUserName(it) { user ->
                                                            senderName = user
                                                            senderNameMap[it] = user
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (senderId.constructor == TdApi.MessageSenderChat.CONSTRUCTOR) {
                                            val senderChat = senderId as TdApi.MessageSenderChat
                                            //println("senderChat: $senderChat")
                                            senderChat.chatId.let { itChatId ->
                                                Text(
                                                    text = senderName,
                                                    modifier = Modifier
                                                        .padding(start = 10.dp, end = 5.dp)
                                                        .pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onLongPress = {
                                                                    selectMessage = message
                                                                    isLongPressed = true
                                                                }
                                                            )
                                                        },
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                LaunchedEffect(message.senderId) {
                                                    if (senderId.chatId == chatId) {
                                                        senderName = chatTitle
                                                    } else {
                                                        val itChat = tgApi?.getChat(itChatId)
                                                        itChat.let {
                                                            senderName = it!!.title
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 回复
                                    if (message.replyTo != null) {
                                        var senderName by rememberSaveable { mutableStateOf("") }
                                        var messagePosition by remember { mutableStateOf(-1) }
                                        val replyTo = message.replyTo
                                        if (replyTo is TdApi.MessageReplyToMessage) {
                                            var content by rememberSaveable(stateSaver = MessageContentSaver) { mutableStateOf<TdApi.MessageContent>(
                                                TdApi.MessageText(
                                                    TdApi.FormattedText(
                                                        context.getString(R.string.loading),
                                                        emptyArray()
                                                    ),
                                                    null,
                                                    null
                                                )
                                            )}
                                            LaunchedEffect(replyTo.chatId) {
                                                if (replyTo.origin != null) {
                                                    //println(replyTo.origin)
                                                    when (val origin = replyTo.origin) {
                                                        is TdApi.MessageOriginChannel -> {
                                                            if (origin.authorSignature != "") senderName = origin.authorSignature
                                                            else {
                                                                val chat = tgApi?.getChat(origin.chatId)
                                                                chat?.let {
                                                                    senderName = it.title
                                                                }
                                                            }
                                                        }
                                                        is TdApi.MessageOriginChat -> {
                                                            if (origin.authorSignature != "") senderName = origin.authorSignature
                                                            else {
                                                                val chat = tgApi?.getChat(origin.senderChatId)
                                                                chat?.let {
                                                                    senderName = it.title
                                                                }
                                                            }
                                                        }
                                                        is TdApi.MessageOriginHiddenUser -> {
                                                            senderName = origin.senderName
                                                        }
                                                        is TdApi.MessageOriginUser -> {
                                                            val chat = tgApi?.createPrivateChat(origin.senderUserId)
                                                            chat?.let {
                                                                senderName = it.title
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            //println(replyTo.content)
                                            LaunchedEffect(replyTo.content) {
                                                if (replyTo.content != null) {
                                                    //println("replyTo.content: ${replyTo.content}")
                                                    content = replyTo.content!!
                                                } else {
                                                    if (replyTo.chatId == 0L) {
                                                        if (replyTo.quote != null) {
                                                            if (replyTo.quote!!.text != null) {
                                                                if (replyTo.quote!!.text.text != "") {
                                                                    content = TdApi.MessageText(
                                                                        TdApi.FormattedText(
                                                                            replyTo.quote!!.text.text,
                                                                            emptyArray()
                                                                        ),
                                                                        null,
                                                                        null
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            content = TdApi.MessageText(
                                                                TdApi.FormattedText(
                                                                    context.getString(R.string.empty_message),
                                                                    emptyArray()
                                                                ),
                                                                null,
                                                                null
                                                            )
                                                        }
                                                    } else if (replyTo.chatId == chatId) {
                                                        var replyMessage = chatList.value.find { it.id == replyTo.messageId }
                                                        if (replyMessage == null) replyMessage = tgApi?.getMessageTypeById(replyTo.messageId)
                                                        else messagePosition = chatList.value.indexOfFirst {
                                                            it.id == replyTo.messageId
                                                        }
                                                        if (replyMessage != null) {
                                                            chatList.value.find { it.id == replyTo.messageId }?.let {
                                                                content = it.content
                                                            }

                                                            content = replyMessage.content

                                                            // 用户名称
                                                            //println(replyMessage.senderId)
                                                            if (replyMessage.senderId != null) {
                                                                val senderId = replyMessage.senderId
                                                                if (senderId is TdApi.MessageSenderUser){
                                                                    senderId.userId.let { senderUserId ->
                                                                        if (senderUserId in senderNameMap) {
                                                                            senderName = senderNameMap[senderUserId]!!
                                                                        } else {
                                                                            tgApi?.getUserName(senderUserId) { user ->
                                                                                senderName = user
                                                                                senderNameMap[senderUserId] = user
                                                                            }
                                                                        }
                                                                    }
                                                                } else if (senderId is TdApi.MessageSenderChat) {
                                                                    if (senderId.chatId == chatId) {
                                                                        senderName = chatTitle
                                                                    } else {
                                                                        val chat = tgApi?.getChat(senderId.chatId)
                                                                        chat?.let {
                                                                            senderName = it.title
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            content = TdApi.MessageText(
                                                                TdApi.FormattedText(
                                                                    context.getString(R.string.Deleted_message),
                                                                    emptyArray()
                                                                ),
                                                                null,
                                                                null
                                                            )
                                                        }
                                                    } else {
                                                        val chat = tgApi?.getChat(replyTo.chatId)
                                                        if (chat != null) {
                                                            val replyMessage = tgApi?.getMessageTypeById(replyTo.messageId, replyTo.chatId)
                                                            replyMessage?.let { content = it.content }
                                                        } else {
                                                            content = TdApi.MessageText(
                                                                TdApi.FormattedText(
                                                                    context.getString(R.string.empty_message),
                                                                    emptyArray()
                                                                ),
                                                                null,
                                                                null
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            var parentHeight by remember { mutableIntStateOf(0) }

                                            Box (
                                                modifier = Modifier.clickable(
                                                    onClick = {
                                                        if (messagePosition != -1) {
                                                            coroutineScope.launch {
                                                                listState.animateScrollToItem(messagePosition)
                                                            }
                                                        }
                                                    }
                                                )
                                            ) {
                                                if (isCurrentUser) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clickable(
                                                                onClick = {
                                                                    if (messagePosition != -1) {
                                                                        coroutineScope.launch {
                                                                            listState.animateScrollToItem(
                                                                                messagePosition
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                            .padding(
                                                                start = 5.dp,
                                                                end = 5.dp,
                                                                top = 5.dp
                                                            )
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = alignment
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .clickable(
                                                                    onClick = {
                                                                        if (messagePosition != -1) {
                                                                            coroutineScope.launch {
                                                                                listState.animateScrollToItem(
                                                                                    messagePosition
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                )
                                                                .background(
                                                                    Color(0xFF3A4048),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .clip(RoundedCornerShape(8.dp))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clickable(
                                                                        onClick = {
                                                                            if (messagePosition != -1) {
                                                                                coroutineScope.launch {
                                                                                    listState.animateScrollToItem(
                                                                                        messagePosition
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                    .weight(1f, fill = false)
                                                                    .fillMaxHeight()
                                                                    .onSizeChanged { size ->
                                                                        parentHeight =
                                                                            size.height // 获取父容器的高度
                                                                    },
                                                            ) {
                                                                if (senderName != "") {
                                                                    Column(
                                                                        modifier = Modifier
                                                                            .clickable(
                                                                                onClick = {
                                                                                    if (messagePosition != -1) {
                                                                                        coroutineScope.launch {
                                                                                            listState.animateScrollToItem(
                                                                                                messagePosition
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                            .padding(
                                                                                bottom = 5.dp,
                                                                                start = 5.dp,
                                                                                end = 5.dp
                                                                            ),
                                                                        horizontalAlignment = Alignment.End // 文字右对齐
                                                                    ) {
                                                                        Text(
                                                                            text = senderName,
                                                                            color = Color(0xFF66D3FE),
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                        )
                                                                        messageDrawer(
                                                                            content = content,
                                                                            onLinkClick = onLinkClick,
                                                                            textColor = textColor,
                                                                            videoDownload = videoDownload,
                                                                            videoDownloadDone = videoDownloadDone,
                                                                            showUnknownMessageType = showUnknownMessageType
                                                                        )
                                                                    }
                                                                } else {
                                                                    Column(
                                                                        modifier = Modifier
                                                                            .clickable(
                                                                                onClick = {
                                                                                    if (messagePosition != -1) {
                                                                                        coroutineScope.launch {
                                                                                            listState.animateScrollToItem(
                                                                                                messagePosition
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                            .padding(5.dp),
                                                                        horizontalAlignment = Alignment.End // 文字右对齐
                                                                    ) {
                                                                        messageDrawer(
                                                                            content = content,
                                                                            onLinkClick = onLinkClick,
                                                                            textColor = textColor,
                                                                            videoDownload = videoDownload,
                                                                            videoDownloadDone = videoDownloadDone,
                                                                            showUnknownMessageType = showUnknownMessageType
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clickable(
                                                                        onClick = {
                                                                            if (messagePosition != -1) {
                                                                                coroutineScope.launch {
                                                                                    listState.animateScrollToItem(
                                                                                        messagePosition
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                    .background(Color(0xFF397DBC))
                                                                    .width(8.dp)
                                                                    .fillMaxHeight()
                                                            ) {
                                                                Spacer(Modifier.height((parentHeight/2).dp)) // 保持Spacer，虽然在这里作用不大
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Row(
                                                        modifier = Modifier
                                                            .clickable(
                                                                onClick = {
                                                                    if (messagePosition != -1) {
                                                                        coroutineScope.launch {
                                                                            listState.animateScrollToItem(
                                                                                messagePosition
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                            .padding(
                                                                start = 5.dp,
                                                                end = 5.dp,
                                                                top = 5.dp
                                                            )
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = alignment
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .clickable(
                                                                    onClick = {
                                                                        if (messagePosition != -1) {
                                                                            coroutineScope.launch {
                                                                                listState.animateScrollToItem(
                                                                                    messagePosition
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                )
                                                                .background(
                                                                    Color(0xFF3A4048),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .clip(RoundedCornerShape(8.dp))
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clickable(
                                                                        onClick = {
                                                                            if (messagePosition != -1) {
                                                                                coroutineScope.launch {
                                                                                    listState.animateScrollToItem(
                                                                                        messagePosition
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                    .background(Color(0xFF397DBC))
                                                                    .width(8.dp) // 指定左边颜色宽度为 10.dp
                                                            ) {
                                                                Spacer(Modifier.height((parentHeight/2).dp))
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clickable(
                                                                        onClick = {
                                                                            if (messagePosition != -1) {
                                                                                coroutineScope.launch {
                                                                                    listState.animateScrollToItem(
                                                                                        messagePosition
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                    .fillMaxHeight()
                                                                    .onSizeChanged { size ->
                                                                        parentHeight =
                                                                            size.height // 获取父容器的高度
                                                                    }
                                                            ) {
                                                                if (senderName != "") {
                                                                    Column(
                                                                        modifier = Modifier
                                                                            .clickable(
                                                                                onClick = {
                                                                                    if (messagePosition != -1) {
                                                                                        coroutineScope.launch {
                                                                                            listState.animateScrollToItem(
                                                                                                messagePosition
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                            .padding(
                                                                                bottom = 5.dp,
                                                                                start = 5.dp,
                                                                                end = 5.dp
                                                                            )
                                                                    ) {
                                                                        Text(
                                                                            text = senderName,
                                                                            color = Color(0xFF66D3FE),
                                                                            fontSize = 10.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                        )
                                                                        messageDrawer(
                                                                            content = content,
                                                                            onLinkClick = onLinkClick,
                                                                            textColor = textColor,
                                                                            videoDownload = videoDownload,
                                                                            videoDownloadDone = videoDownloadDone,
                                                                            showUnknownMessageType = showUnknownMessageType
                                                                        )
                                                                    }
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clickable(
                                                                                onClick = {
                                                                                    if (messagePosition != -1) {
                                                                                        coroutineScope.launch {
                                                                                            listState.animateScrollToItem(
                                                                                                messagePosition
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                            )
                                                                            .padding(5.dp)
                                                                    ) {
                                                                        messageDrawer(
                                                                            content = content,
                                                                            onLinkClick = onLinkClick,
                                                                            textColor = textColor,
                                                                            videoDownload = videoDownload,
                                                                            videoDownloadDone = videoDownloadDone,
                                                                            showUnknownMessageType = showUnknownMessageType
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 正文
                                    Row(
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = alignment
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    backgroundColor,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(
                                                    start = 8.dp,
                                                    end = 8.dp,
                                                    top = 6.dp,
                                                    bottom = 1.dp
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            selectMessage = message
                                                            isLongPressed = true
                                                        },
                                                        onTap = {
                                                            if (!videoDownload.value) {
                                                                if (message.content is TdApi.MessageVideo) {
                                                                    val videoFile =
                                                                        (message.content as TdApi.MessageVideo).video.video
                                                                    if (!videoFile.local.isDownloadingCompleted) {
                                                                        tgApi!!.downloadFile(
                                                                            file = videoFile,
                                                                            schedule = { schedule ->
                                                                                println("下载进度: $schedule")
                                                                            },
                                                                            completion = { boolean, path ->
                                                                                println("下载完成情况: $boolean")
                                                                                println("下载路径: $path")
                                                                                videoDownload.value =
                                                                                    false
                                                                                videoDownloadDone.value =
                                                                                    true
                                                                            }
                                                                        )
                                                                        videoDownload.value = true
                                                                    }
                                                                }
                                                                press(message)
                                                            }
                                                        }
                                                    )
                                                }
                                        ) {
                                            Column {
                                                val content = message.content
                                                messageDrawer(
                                                    content = content,
                                                    onLinkClick = onLinkClick,
                                                    textColor = textColor,
                                                    videoDownload = videoDownload,
                                                    videoDownloadDone = videoDownloadDone,
                                                    showUnknownMessageType = showUnknownMessageType
                                                )

                                                Row(
                                                    modifier = modifier
                                                        .padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween // 两端对齐
                                                ) {
                                                    // 时间
                                                    Text(
                                                        text = if (message.editDate == 0) formatTimestampToTime(message.date)
                                                        else stringResource(id = R.string.edit) + " " + formatTimestampToTime(message.editDate),
                                                        modifier = modifier,
                                                        color = Color(0xFF6A86A3),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )

                                                    // 已读未读标识
                                                    // 确定消息是否为自己发的
                                                    if (message.isOutgoing) {
                                                        //println("read.message.id: ${chatObject.lastReadInboxMessageId}")
                                                        if (message.id <= lastReadOutboxMessageId.value) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.outgoing_read),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(19.4.dp, 12.dp) // 设置 Image 的大小
                                                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                                                    .padding(start = 3.8.dp)
                                                            )
                                                        } else if (message.id <= lastReadInboxMessageId.value) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.outgoing),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(16.2.dp, 11.dp) // 设置 Image 的大小
                                                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                                                    .padding(start = 3.5.dp)
                                                            )
                                                        } else {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.sending),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(15.8.dp, 12.dp) // 设置 Image 的大小
                                                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                                                                    .padding(start = 3.5.dp)
                                                            )
                                                        }
                                                    }

                                                    if (!isCurrentUser) {
                                                        val forwardInfo = message.forwardInfo
                                                        forwardInfo?.origin?.let { origin ->
                                                            if (origin is TdApi.MessageOriginChannel) {
                                                                // 署名
                                                                Text(
                                                                    text = origin.authorSignature,
                                                                    color = Color(0xFF6A86A3),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    modifier = Modifier
                                                                        .align(Alignment.CenterVertically)
                                                                        .weight(1f)
                                                                        .wrapContentWidth(Alignment.End) // 向右对齐
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                .verticalRotaryScroll(scrollState),
                            verticalArrangement = Arrangement.Top
                        ) {
                            if (planReplyMessage != null) {
                                // 将回复消息显示
                                Box (
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            onClick = {
                                                planReplyMessage = null
                                                tgApi!!.replyMessage.value = null
                                            }
                                        )
                                )
                                Text(
                                    text = stringResource(R.string.Reply),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .padding(
                                            start = 10.dp,
                                            end = 5.dp,
                                            top = 5.dp
                                        )
                                        .fillMaxWidth()
                                )
                                var parentHeight by remember { mutableIntStateOf(0) }
                                var videoDownloadDone = rememberSaveable { mutableStateOf(false) }
                                var videoDownload = rememberSaveable { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .padding(
                                            start = 5.dp,
                                            end = 5.dp,
                                            top = 5.dp
                                        )
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF3A4048),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF397DBC))
                                                .width(8.dp) // 指定左边颜色宽度为 10.dp
                                        ) {
                                            Spacer(Modifier.height((parentHeight/2).dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .onSizeChanged { size ->
                                                    parentHeight =
                                                        size.height // 获取父容器的高度
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(
                                                        bottom = 5.dp,
                                                        start = 5.dp,
                                                        end = 5.dp
                                                    )
                                            ) {
                                                if (planReplyMessageSenderName == "") {
                                                    messageDrawer(
                                                        content = planReplyMessage!!.content,
                                                        onLinkClick = onLinkClick,
                                                        textColor = Color(0xFFFEFEFE),
                                                        videoDownload = videoDownload,
                                                        videoDownloadDone = videoDownloadDone,
                                                        showUnknownMessageType = showUnknownMessageType
                                                    )
                                                } else {
                                                    Text(
                                                        text = planReplyMessageSenderName,
                                                        color = Color(0xFF66D3FE),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    messageDrawer(
                                                        content = planReplyMessage!!.content,
                                                        onLinkClick = onLinkClick,
                                                        textColor = Color(0xFFFEFEFE),
                                                        videoDownload = videoDownload,
                                                        videoDownloadDone = videoDownloadDone,
                                                        showUnknownMessageType = showUnknownMessageType
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            InputBar(
                                query = inputText,
                                onQueryChange = { inputText = it },
                                placeholder = stringResource(id = R.string.Write_message),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // 发送消息按钮
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = {
                                        if (planReplyMessage == null) {
                                            tgApi?.sendMessage(
                                                chatId = chatId,
                                                message = TdApi.InputMessageText().apply {  // 参数名改为message
                                                    text = TdApi.FormattedText().apply {
                                                        this.text = inputText  // 正确设置text字段
                                                    }
                                                }
                                            )
                                        } else {
                                            if (planReplyMessage!!.chatId != chatId) {
                                                tgApi?.sendMessage(
                                                    chatId = chatId,
                                                    message = TdApi.InputMessageText().apply {
                                                        text = TdApi.FormattedText().apply {
                                                            this.text = inputText
                                                        }
                                                    },
                                                    replyTo = TdApi.InputMessageReplyToExternalMessage(
                                                        planReplyMessage!!.chatId,
                                                        planReplyMessage!!.id, null)
                                                )
                                            } else {
                                                tgApi?.sendMessage(
                                                    chatId = chatId,
                                                    message = TdApi.InputMessageText().apply {
                                                        text = TdApi.FormattedText().apply {
                                                            this.text = inputText
                                                        }
                                                    },
                                                    replyTo = TdApi.InputMessageReplyToMessage(
                                                        planReplyMessage!!.id, null)
                                                )
                                            }
                                            planReplyMessage = null
                                            tgApi!!.replyMessage.value = null
                                        }
                                        inputText = ""
                                    },
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
                            Spacer(modifier = Modifier.height(12.dp))
                            val forwardMessage = tgApi!!.forwardMessage
                            if (forwardMessage.value != null) {
                                val messageText =
                                    tgApi!!.handleAllMessages(message = forwardMessage.value, maxText = 100)
                                val targetTitle =
                                    if (forwardMessage.value!!.chatId == currentUserId.value) stringResource(R.string.Saved_Messages) else
                                        tgApi!!.chatsList.value
                                            .find { it.id == forwardMessage.value!!.chatId }
                                            ?.title ?: stringResource(R.string.Unknown_chat) // 找不到时返回默认值

                                Text(
                                    text = stringResource(R.string.Forward),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                MainCard(
                                    column = {
                                        Text(
                                            text = targetTitle,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        MessageView(message = messageText)
                                    },
                                    item = forwardMessage.value
                                )
                                // 转发消息部分发送按钮
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            tgApi?.sendMessage(
                                                chatId = chatId,
                                                message = TdApi.InputMessageForwarded().apply {  // 参数名改为message
                                                    copyOptions = null
                                                    fromChatId = forwardMessage.value!!.chatId
                                                    inGameShare = false
                                                    messageId = forwardMessage.value!!.id
                                                }
                                            )
                                            inputText = ""
                                        },
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
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // 长按处理
        if (isLongPressed) {
            LongPressBox(
                callBack = { select ->
                    when (select) {
                        "Reply" -> {
                            // 返回空字符串同时执行操作
                            planReplyMessage = selectMessage
                            tgApi!!.replyMessage.value = selectMessage
                            ""
                        }
                        else -> return@LongPressBox longPress(select, selectMessage)
                    }
                },
                onDismiss = { isLongPressed = false }
            )
        }

        // 隐藏的 TextField 用于触发输入法
        /*
        val textFieldFocusRequester by remember { mutableStateOf(FocusRequester()) }

        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(textFieldFocusRequester),
            maxLines = 1,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            )
        )
         */

        // 消息发送部分
        if (pagerState.currentPage == 0) {
            var showKeyboard by remember { mutableStateOf(true) }
            /*
            if (notJoin) {
                showKeyboard = true
            } else {
                if (chatPermissions == null) {
                    showKeyboard = true
                } else {
                    if (chatPermissions.canSendBasicMessages) {
                        showKeyboard = true
                    }
                }
            }
             */
            if (showKeyboard) {
                if (isFloatingVisible) {
                    if (notJoin) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                onClick = {
                                    tgApi?.joinChat(
                                        chatId = chatId,
                                        reInit = {
                                            notJoin = false
                                            //reInit("joined")
                                        }
                                    )
                                },
                                text = stringResource(id = R.string.join_in)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                                .alpha(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier
                                    .size(84.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_custom_keyboard),
                                    contentDescription = null,
                                    modifier = Modifier.size(82.dp)
                                )
                            }

                            // 滑动最下面按钮
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .size(45.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.bottom),
                                    contentDescription = null,
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight() // 使 Box 填满整个屏幕高度
                            .fillMaxWidth(), // 使 Box 填满整个屏幕宽度
                        contentAlignment = Alignment.BottomCenter // 将内容对齐到 Box 的底部中心
                    ) {
                        IconButton(
                            onClick = {
                                isFloatingVisible = true
                            },
                            modifier = Modifier
                                .padding(3.dp) // 可选的内边距
                                .size(20.dp) // 设置 IconButton 的大小
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.up), // 替换为你自己的向上箭头图标资源ID
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp) // 设置 Image 的大小
                                    .graphicsLayer(alpha = 0.5f) // 设置 Image 的不透明度
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun messageDrawer(
    onLinkClick: (String) -> Unit,
    content: TdApi.MessageContent,
    videoDownload: MutableState<Boolean>,
    videoDownloadDone: MutableState<Boolean>,
    textColor: Color,
    showUnknownMessageType: Boolean,
    modifier: Modifier = Modifier
) {
    when (content) {
        is TdApi.MessageText -> {
            SelectionContainer {
                LinkText(
                    text = content.text.text,
                    color = Color(0xFFFEFEFE),
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = onLinkClick,
                    modifier = modifier
                )
            }
        }
        is TdApi.MessagePhoto -> {
            val thumbnail = content.photo.sizes.minByOrNull { it.width * it.height }
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.photo,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = Color(0xFFFEFEFE),
                    modifier = modifier
                )
            } else {
                // 处理没有缩略图的情况
                Text(
                    text = stringResource(id = R.string.No_thumbnail_available),
                    color = Color(0xFFFEFEFE),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier
                )
            }
            // 图片文字
            content.caption?.text?.let {
                SelectionContainer {
                    LinkText(
                        text = it,
                        color = Color(0xFFFEFEFE),
                        modifier = modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        onLinkClick = onLinkClick
                    )
                }
            }
        }
        is TdApi.MessageVideo -> {
            val thumbnail = content.video.thumbnail
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    ThumbnailImage(
                        thumbnail = thumbnail.file,
                        imageWidth = thumbnail.width,
                        imageHeight = thumbnail.height,
                        textColor = Color(0xFFFEFEFE),
                        modifier = modifier
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize() // 覆盖层与图片大小一致
                            .background(Color.Black.copy(alpha = 0.5f)) // 设置半透明黑色背景
                    )
                } else {
                    // 处理没有缩略图的情况
                    Text(
                        text = stringResource(id = R.string.No_thumbnail_available),
                        color = Color(0xFFFEFEFE),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }

                if (videoDownload.value) SplashLoadingScreen()
                val videoFile = content.video.video
                if (videoFile.local.isDownloadingCompleted) {
                    videoDownloadDone.value = true
                }
                if (videoDownloadDone.value) {
                    Image(
                        painter = painterResource(id = R.drawable.play),
                        contentDescription = null,
                        modifier = modifier
                            .align(Alignment.Center)
                            .size(36.dp) // 设置图标大小为 24dp
                    )
                } else {
                    if (!videoDownload.value) {
                        Image(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = null,
                            modifier = modifier
                                .align(Alignment.Center)
                                .size(36.dp) // 设置图标大小为 24dp
                        )
                    }
                }
            }

            // 视频文字
            content.caption?.text?.let {
                SelectionContainer {
                    LinkText(
                        text = it,
                        color = Color(0xFFFEFEFE),
                        modifier = modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        onLinkClick = onLinkClick
                    )
                }
            }
        }
        // GIF信息
        is TdApi.MessageAnimation -> {
            val thumbnail = content.animation.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    modifier = modifier
                )
            }
        }
        // 表情消息
        is TdApi.MessageAnimatedEmoji -> {
            val emoji = content.emoji
            val thumbnail = content.animatedEmoji.sticker?.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    loadingText = emoji,
                    modifier = modifier
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }
            }
        }
        // 贴纸表情消息
        is TdApi.MessageSticker -> {
            val emoji = content.sticker.emoji
            val thumbnail = content.sticker.thumbnail
            if (thumbnail != null) {
                ThumbnailImage(
                    thumbnail = thumbnail.file,
                    imageWidth = thumbnail.width,
                    imageHeight = thumbnail.height,
                    textColor = textColor,
                    loadingText = emoji,
                    modifier = modifier
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = modifier
                    )
                }
            }
        }
        // 语音消息
        is TdApi.MessageVoiceNote -> {
            MessageVideoNote(
                messageVideoNote = content,
                modifier = modifier
            )
        }
        else -> {
            SelectionContainer {
                Text(
                    text = stringResource(id = R.string.Unknown_Message) + if (showUnknownMessageType) "\nType: TdApi." + getMessageContentTypeName(content) else "",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun MessageVideoNote(
    messageVideoNote: TdApi.MessageVoiceNote,
    modifier: Modifier = Modifier
) {
    val videoNote = messageVideoNote.voiceNote
    val voiceFile = videoNote.voice
    val context = LocalContext.current
    var playTime by remember { mutableStateOf(0) }
    var playingShow by remember { mutableStateOf(false) }
    var isDownload by remember { mutableStateOf(voiceFile.local.isDownloadingCompleted) }
    var downloading by remember { mutableStateOf(false) }
    var fileUrl = remember { mutableStateOf("") }
    if (voiceFile.local.isDownloadingCompleted) fileUrl.value = voiceFile.local.path

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !isPlaying) {
                        // 播放器已准备好但未播放
                        playTime = (currentPosition / 1000).toInt()
                    } else if (state == Player.STATE_ENDED) {
                        playingShow = false
                        playTime = 0
                        seekTo(0) // 将播放位置重置到起点
                        pause()
                    }
                }
            })
        }
    }

    // 播放时间更新任务
    LaunchedEffect(playingShow) {
        if (playingShow) {
            while (playingShow) {
                playTime = (exoPlayer.currentPosition / 1000).toInt()
                delay(500) // 每 500ms 更新一次
                //println(exoPlayer.currentPosition)
                //println(playTime)
            }
        }
    }

    // 播放器初始化
    LaunchedEffect(isDownload) {
        //println("值改变isDownload: $isDownload")
        if (isDownload) {
            //println("初始化")
            try {
                var file = File(fileUrl.value)
                while (!file.exists() || file.length() == 0L) {
                    //("文件不存在或长度为0，正在等待...")
                    //println("完整途径：" + photoPath + "结尾")
                    delay(1000)  // 每 1000 毫秒检查一次
                    file = File(fileUrl.value)  // 重新获取文件状态
                    //println(fileUrl.value)
                    //println("文件大小：" + file.length())
                }
                //println("文件存在")
                exoPlayer.setMediaItem(MediaItem.fromUri(file.toUri()))
                exoPlayer.prepare()
                //println("初始化完成")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 资源释放
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDownload) {
            if (playingShow) {
                Image(
                    painter = painterResource(id = R.drawable.playing_audio),
                    contentDescription = "playing_audio",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            exoPlayer.pause()
                            playingShow = false
                        }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.play_audio),
                    contentDescription = "play_audio",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            exoPlayer.play()
                            playingShow = true
                        }
                )
            }
        } else {
            if (videoNote.mimeType == "audio/ogg") {
                if (downloading) {
                    SplashLoadingScreen()
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.download_audio),
                        contentDescription = "download_audio",
                        modifier = Modifier
                            .size(width = 32.dp, height = 32.dp)
                            .clip(CircleShape)
                            .clickable {
                                downloading = true
                                tgApi!!.downloadFile(
                                    file = voiceFile,
                                    schedule = { schedule -> },
                                    completion = { success, tdFleUrl ->
                                        if (success) {
                                            //println(tdFleUrl)
                                            if (tdFleUrl != null) fileUrl.value = tdFleUrl
                                            //println(fileUrl)
                                            isDownload = true
                                            downloading = false
                                        }
                                    }
                                )
                            }
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.Audio_Error),
                    color = Color(0xFF6985A2),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = modifier
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.width(42.dp))
            Image(
                painter = painterResource(id = R.drawable.video_ripple),
                contentDescription = "video_ripple",
                modifier = Modifier
                    .size(32.dp)
                    .scale(1.65f)
            )
            Text(
                text = "${formatDuration(playTime)} | ${formatDuration(videoNote.duration)}",
                color = Color(0xFF6985A2),
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier
            )
        }
    }
}

@Composable
fun ThumbnailImage(
    thumbnail: TdApi.File,
    imageWidth: Int,
    imageHeight: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    loadingText: String = stringResource(id = R.string.loading)
) {
    val heightDp = with(LocalDensity.current) { imageHeight.toDp() }
    val widthDp = with(LocalDensity.current) { imageWidth.toDp() }
    var imagePath by remember { mutableStateOf<String?>(null) }
    val painter = rememberAsyncImagePainter(model = imagePath) // 在Composable作用域

    LaunchedEffect(thumbnail.id) {
        //println("执行")
        while (true) {
            if (thumbnail.local.isDownloadingCompleted) {
                try {
                    imagePath = thumbnail.local.path // 更新状态触发重组
                    println("下载完成")
                    println(imagePath)
                    break
                } catch (e: IOException) {
                    println("Image load failed: ${e.message}")
                    // 处理错误，例如显示占位符
                    break
                }
            } else {
                //println("本地没图片，正在下载图片")
                try {
                    tgApi!!.downloadPhoto(thumbnail) { success, path ->
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

    if (imagePath != null) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.5f)) // 设置半透明黑色背景
        )
        Image(
            painter = painter,
            contentDescription = "Thumbnail",
            modifier = Modifier.size(width = widthDp, height = heightDp)
        )
    } else {
        Box(
            modifier = Modifier.size(width = widthDp, height = heightDp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = loadingText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
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
            onLinkClick = {},
            chatTitleClick = {},
            currentUserId = mutableStateOf(-1L)
        )
    }
}
