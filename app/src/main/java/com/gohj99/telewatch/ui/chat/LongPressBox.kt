/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import kotlinx.coroutines.launch

@Composable
fun LongPressBox(
    callBack: suspend (String) -> String,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var nowShow by remember { mutableStateOf("first") }
    var showText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = {
        nowShow = "first"
        onDismiss()
    }) {
        BackHandler(onBack = {
            nowShow = "first"
            onDismiss()
        })
        when (nowShow) {
            "first" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .verticalRotaryScroll(scrollState)
                    ) {
                        LPMainCard(
                            text = stringResource(id = R.string.Reply),
                            callback = {
                                coroutineScope.launch {
                                    callBack("Reply")
                                    onDismiss()
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.Forward),
                            callback = {
                                coroutineScope.launch {
                                    callBack("Forward")
                                    onDismiss()
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.DeleteMessage),
                            callback = {
                                coroutineScope.launch {
                                    callBack("DeleteMessage")
                                    onDismiss()
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.copy_link),
                            callback = {
                                coroutineScope.launch {
                                    callBack("CopyLink")
                                    onDismiss()
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.ReloadMessage),
                            callback = {
                                coroutineScope.launch {
                                    callBack("ReloadMessage")
                                    onDismiss()
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.GetMessage),
                            callback = {
                                coroutineScope.launch {
                                    showText = callBack("GetMessage")
                                    nowShow = "showText"
                                }
                            }
                        )
                        LPMainCard(
                            text = stringResource(id = R.string.Save),
                            callback = {
                                coroutineScope.launch {
                                    showText = callBack("Save")
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
            "showText" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = showText,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0xFF2C323A).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 10.sp),
                        singleLine = false
                    )
                }
            }
        }
    }
}

@Composable
fun LPMainCard(text: String, callback: () -> Unit) {
    MainCard(
        column = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    color = Color.White
                )
            }
        },
        item = text,
        callback = { callback() }
    )
}

@Preview(showBackground = true)
@Composable
fun LongPressBoxPreview() {
    TelewatchTheme {
        LongPressBox(
            callBack = { return@LongPressBox "OK" },
            onDismiss = { }
        )
    }
}
