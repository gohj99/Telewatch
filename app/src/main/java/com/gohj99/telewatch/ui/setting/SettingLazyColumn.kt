/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.verticalRotaryScroll

@Composable
fun SettingLazyColumn(
    itemsList: MutableState<List<String>>,
    callback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalRotaryScroll(listState)
    ) {
        items(itemsList.value) { item ->
            SettingView(item, callback)
        }
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun SettingView(item: String, callback: (String) -> Unit) {
    Box(
        modifier = Modifier
    ) {
        MainCard(
            column = {
                Text(text = item, color = Color.White)
            },
            item = item,
            callback = {
                callback(item)
            }
        )
    }
}
