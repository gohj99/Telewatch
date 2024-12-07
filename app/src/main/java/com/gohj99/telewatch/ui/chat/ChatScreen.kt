/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 反射机制获取MessageContent的类信息
fun getMessageContentTypeName(messageContent: TdApi.MessageContent): String {
    return messageContent::class.simpleName ?: "Unknown"
}

fun formatTimestampToTime(unixTimestamp: Int): String {
    // 将 Unix 时间戳从 Int 转换为 Long，并转换为毫秒
    val date = Date(unixTimestamp.toLong() * 1000)
    // 定义时间格式
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    // 返回格式化的时间字符串
    return format.format(date)
}

fun formatTimestampToDate(unixTimestamp: Int): String {
    // 将时间戳转换为 Date 对象
    val date = Date(unixTimestamp.toLong() * 1000)
    // 获取当前年份
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    // 获取时间戳对应的年份
    val calendar = Calendar.getInstance()
    calendar.time = date
    val timestampYear = calendar.get(Calendar.YEAR)
    // 获取用户的本地化日期格式
    val dateFormat: DateFormat = if (timestampYear == currentYear) {
        // 当年份相同时，仅显示月和日
        DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
    } else {
        // 当年份不同时，显示完整日期
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }
    // 返回格式化的日期字符串
    return dateFormat.format(date)
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SplashChatScreen(
    chatTitle: String,
    chatList: MutableState<List<TdApi.Message>>,
    chatId: Long,
    sendCallback: (String) -> Unit,
    goToChat: (Chat) -> Unit,
    press: (TdApi.Message) -> Unit,
    longPress: suspend (String, TdApi.Message) -> String,
    chatObject: TdApi.Chat,
    lastReadOutboxMessageId: MutableState<Long>,
    lastReadInboxMessageId: MutableState<Long>,
    listState: LazyListState = rememberLazyListState(),
    onLinkClick: (String) -> Unit,
    chatTitleClick: () -> Unit,
    reInit: (String) -> Unit
) {
    var isFloatingVisible by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isLongPressed by remember { mutableStateOf(false) }
    var selectMessage by remember { mutableStateOf(TdApi.Message()) }
    val senderNameMap by remember { mutableStateOf(mutableMapOf<Long, String?>()) }
    var notJoin = false

    // 获取context
    val context = LocalContext.current
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

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index >= chatList.value.size - 5) {
                    TgApiManager.tgApi?.fetchMessages()
                }
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
            //println("开始渲染")
            ClickableText(
                text = AnnotatedString(if (chatTitle.length > 15) chatTitle.take(15) + "..." else chatTitle),
                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFFEFEFE), fontWeight = FontWeight.Bold),
                onClick = { chatTitleClick() }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
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

                    TgApiManager.tgApi?.markMessagesAsRead(message.id)

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
                                                                    title = senderName,
                                                                    message = ""
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
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    LaunchedEffect(message.senderId) {
                                        if (it in senderNameMap) {
                                            senderName = senderNameMap[it]!!
                                        } else {
                                            TgApiManager.tgApi?.getUserName(it) { user ->
                                                senderName = user
                                                senderNameMap[it] = user
                                            }
                                        }
                                    }
                                }
                            } else if (senderId.constructor == TdApi.MessageSenderChat.CONSTRUCTOR) {
                                val senderChat = senderId as TdApi.MessageSenderChat
                                println("senderChat: $senderChat")
                                senderChat.chatId.let { itChatId ->
                                    Text(
                                        text = senderName,
                                        modifier = Modifier
                                            .padding(start = 10.dp, end = 5.dp),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    LaunchedEffect(message.senderId) {
                                        val itChat = TgApiManager.tgApi?.getChat(itChatId)
                                        itChat.let {
                                            senderName = it!!.title
                                        }
                                    }
                                }
                            }
                        }

                        // 回复
                        if (message.replyTo != null) {
                            val replyTo = message.replyTo
                            if (replyTo is TdApi.MessageReplyToMessage) {
                                var content: TdApi.MessageContent = (
                                    TdApi.MessageText(
                                        TdApi.FormattedText(
                                            context.getString(R.string.loading),
                                            emptyArray()
                                        ),
                                        null,
                                        null
                                    )
                                )
                                if (replyTo.content != null) {
                                    content = replyTo.content!!
                                } else {
                                    LaunchedEffect(replyTo.chatId) {
                                        val replyMessage = TgApiManager.tgApi?.getMessageTypeById(replyTo.messageId)
                                        replyMessage?.let { content = it.content }
                                    }
                                }
                                var parentHeight by remember { mutableIntStateOf(0) }
                                Row(
                                    modifier = Modifier
                                        .padding(start = 5.dp, end = 5.dp, top = 5.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = alignment
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFF3A4048), shape = RoundedCornerShape(8.dp))
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
                                                    parentHeight = size.height // 获取父容器的高度
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(5.dp)
                                            ) {
//                                                Text(
//                                                    text = "你好",
//                                                    color = Color(0xFFFEFEFE),
//                                                    style = MaterialTheme.typography.bodyMedium
//                                                )
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

                        // 正文
                        Row(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = alignment
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(backgroundColor, shape = RoundedCornerShape(8.dp))
                                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 1.dp)
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
                                                            TgApiManager.tgApi!!.downloadFile(
                                                                file = videoFile,
                                                                schedule = { schedule ->
                                                                    println("下载进度: $schedule")
                                                                },
                                                                completion = { boolean, path ->
                                                                    println("下载完成情况: $boolean")
                                                                    println("下载路径: $path")
                                                                    videoDownload.value = false
                                                                    videoDownloadDone.value = true
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

        // 长按处理
        if (isLongPressed) {
            LongPressBox(
                callBack = { select ->
                    return@LongPressBox longPress(select, selectMessage)
                },
                onDismiss = { isLongPressed = false }
            )
        }

        // 隐藏的 TextField 用于触发输入法
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
                    // TODO: Handle the input text (e.g., send message)
                }
            )
        )

        // 消息发送部分
        var showKeyboard by remember { mutableStateOf(false) }
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
                                TgApiManager.tgApi?.joinChat(
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
                                textFieldFocusRequester.requestFocus() // 将焦点移动到隐藏的 TextField
                                keyboardController?.show() // 显示输入法
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

                        IconButton(
                            onClick = {
                                sendCallback(inputText.text)
                                inputText = TextFieldValue("")
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

@Composable
fun messageDrawer(
    onLinkClick: (String) -> Unit,
    content: TdApi.MessageContent,
    videoDownload: MutableState<Boolean>,
    videoDownloadDone: MutableState<Boolean>,
    textColor: Color,
    showUnknownMessageType: Boolean,
) {
    when (content) {
        is TdApi.MessageText -> {
            SelectionContainer {
                LinkText(
                    text = content.text.text,
                    color = Color(0xFFFEFEFE),
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = onLinkClick
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
                    textColor = Color(0xFFFEFEFE)
                )
            } else {
                // 处理没有缩略图的情况
                Text(
                    text = stringResource(id = R.string.No_thumbnail_available),
                    color = Color(0xFFFEFEFE),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // 图片文字
            content.caption?.text?.let {
                SelectionContainer {
                    LinkText(
                        text = it,
                        color = Color(0xFFFEFEFE),
                        modifier = Modifier.padding(top = 4.dp),
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
                        textColor = Color(0xFFFEFEFE)
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
                        style = MaterialTheme.typography.bodyMedium
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp) // 设置图标大小为 24dp
                    )
                } else {
                    if (!videoDownload.value) {
                        Image(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier
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
                        modifier = Modifier.padding(top = 4.dp),
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
                    textColor = textColor
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
                    loadingText = emoji
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
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
                    loadingText = emoji
                )
            } else {
                SelectionContainer {
                    Text(
                        text = emoji,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        else -> {
            SelectionContainer {
                Text(
                    text = stringResource(id = R.string.Unknown_Message) + if (showUnknownMessageType) "\nType: TdApi." + getMessageContentTypeName(content) else "",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
            sendCallback = { text ->
                println(text)
            },
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
            reInit = {}
        )
    }
}
