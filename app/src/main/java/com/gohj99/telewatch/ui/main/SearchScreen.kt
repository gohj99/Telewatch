/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager
import com.gohj99.telewatch.model.Chat
import com.gohj99.telewatch.ui.SearchBar
import com.gohj99.telewatch.ui.verticalRotaryScroll

@Composable
fun SearchLazyColumn(callback: (Chat) -> Unit) {
    val listState = rememberLazyListState()
    val searchText = rememberSaveable { mutableStateOf("") }
    val searchList = rememberSaveable { mutableStateOf(listOf<Chat>()) }

    LaunchedEffect(searchText.value) {
        if (searchText.value != ""){
            TgApiManager.tgApi!!.searchPublicChats(
                query = searchText.value,
                searchList = searchList
            )
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

        items(searchList.value, key = { it.id }) { item ->
            ChatView(item, callback, searchText)
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}
