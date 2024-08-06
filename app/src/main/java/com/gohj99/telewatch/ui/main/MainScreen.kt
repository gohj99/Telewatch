package com.gohj99.telewatch.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import org.drinkless.td.libcore.telegram.TdApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(chats: MutableState<MutableList<TdApi.Chat>>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp)
        ) {
            items(chats.value) { chat ->
                ChatItem(chat)
            }
        }
    }
}

@Composable
fun ChatItem(chat: TdApi.Chat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // 处理点击事件，例如导航到聊天详情页面
                println("Clicked on chat: ${chat.id}")
            },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chat.title, color = Color.White)
            Text(text = "ID: ${chat.id}", color = Color.White)
        }
    }
}

@SuppressLint("UnrememberedMutableState", "MutableCollectionMutableState")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TelewatchTheme {
        MainScreen(mutableStateOf(mutableListOf()))
    }
}
