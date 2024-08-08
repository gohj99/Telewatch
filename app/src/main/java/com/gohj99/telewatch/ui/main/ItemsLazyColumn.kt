package com.gohj99.telewatch.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 定义一个数据类
data class Chat(val id: Long, val title: String, val message: String)

fun <T> MutableState<List<T>>.add(item: T) {
    val updatedList = this.value.toMutableList()
    updatedList.add(item)
    this.value = updatedList
}

@Composable
fun ChatLazyColumn(itemsList: MutableState<List<Chat>>, callback: (Chat) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize() // 确保 LazyColumn 填满父容器
            .padding(horizontal = 16.dp) // 只在左右添加 padding
    ) {
        items(itemsList.value) { item ->
            ChatView(item, callback)
        }
        item {
            Spacer(modifier = Modifier.height(30.dp)) // 添加一个高度为 50dp 的 Spacer
        }
    }
}

@Composable
fun ChatView(item: Chat, callback: (Chat) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { callback(item) },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C323A) // 设置 Card 的背景颜色
        )
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 9.dp, end = 14.dp, bottom = 9.dp)) {
            Text(text = item.title, color = Color.White)
            if (item.message.isNotEmpty()) {
                Text(text = item.message, color = Color(0xFF728AA5))
            }
        }
    }
}
