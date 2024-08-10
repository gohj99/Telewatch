/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun SplashPasswordScreen(
    onDoneClick: (String) -> Unit,
    passwordHint: String = "",
    doneStr: MutableState<String>
) {
    val loading = stringResource(id = R.string.loading)
    val done = stringResource(id = R.string.Done)
    val passwordError = stringResource(id = R.string.Password_Error)
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.Password),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .height(40.dp)
                .padding(start = 30.dp, end = 30.dp)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
        ) {
            BasicTextField(
                value = password,
                onValueChange = {
                    password = it
                    doneStr.value = done
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (password.isEmpty()) {
                        Text(
                            text = passwordHint,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomButton(
            onClick = {
                if (password != "") {
                    doneStr.value = loading
                    onDoneClick(password)
                } else doneStr.value = passwordError
                password = ""
            },
            text = doneStr.value
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPasswordScreenPreview() {
    TelewatchTheme {
        SplashPasswordScreen(
            onDoneClick = { password ->
                println(password)
            },
            doneStr = mutableStateOf("")
        )
    }
}
