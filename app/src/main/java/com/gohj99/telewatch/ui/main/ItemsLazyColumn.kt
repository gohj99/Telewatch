/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.ui.verticalRotaryScroll

// 字符串匹配
fun matchingString(target: String, original: String): Boolean {
    if (original != "") return original.contains(target, ignoreCase = true)
    else return true
}

// 定义一个数据类
@SuppressLint("ParcelCreator")
data class Chat(
    val id: Long,
    val title: String,
    val message: String,
    val isPinned: Boolean = false
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
fun ChatLazyColumn(itemsList: MutableState<List<Chat>>, callback: (Chat) -> Unit) {
    val listState = rememberLazyListState()
    var searchText = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index >= itemsList.value.size - 5) {  // 检测是否到倒数第五项
                    TgApiManager.tgApi?.loadChats(itemsList.value.size + 1)
                }
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
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .padding(start = 30.dp, end = 30.dp, bottom = 6.dp)
                    .background(
                        color = Color(0xFF474B57),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clip(RoundedCornerShape(6.dp))
            ) {
                BasicTextField(
                    value = searchText.value,
                    onValueChange = {
                        searchText.value = it
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                ) { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchText.value.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.Search),
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
        items(itemsList.value) { item ->
            ChatView(item, callback, searchText, true)
        }
        items(itemsList.value) { item ->
            ChatView(item, callback, searchText, false)
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
    pinnedView: Boolean = false
) {
    if (chat.isPinned == pinnedView) {
        if (matchingString(searchText.value, chat.title)) {
            MainCard(
                column = {
                    Text(
                        text = chat.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (chat.message.isNotEmpty()) {
                        Text(
                            text = chat.message,
                            color = Color(0xFF728AA5),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                item = chat,
                callback = {
                    callback(chat)
                }
            )
        }
    }
}
