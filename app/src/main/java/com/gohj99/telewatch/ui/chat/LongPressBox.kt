package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.launch
import com.gohj99.telewatch.R

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
        when (nowShow) {
            "first" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
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
                    }
                }
            }
            "showText" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                        .padding(7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = showText,
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2C323A).copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
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
            onDismiss = { /*TODO*/ }
        )
    }
}
