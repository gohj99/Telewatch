/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import kotlinx.coroutines.launch

@Composable
fun MessageBottomFunctionalCompose(
    listState: androidx.compose.foundation.lazy.LazyListState,
    chatId: Long
) {
    val coroutineScope = rememberCoroutineScope()
    var chatReadList = tgApi?.chatReadList!!

    if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset > 10240) {
        // 滑动最下面
        IconButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier

        ) {
            Box(modifier = Modifier.size(60.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.bottom),
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
        // 未读消息指示器
        chatReadList[chatId]?.takeIf { it > 0 }?.let { unreadCount ->
            Box(
                modifier = Modifier.offset(x = (15.7).dp, y = (-3).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF3F81BB),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = unreadCount.toString(),
                        modifier = Modifier
                            .padding(horizontal = 4.6.dp, vertical = 1.3.dp),
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