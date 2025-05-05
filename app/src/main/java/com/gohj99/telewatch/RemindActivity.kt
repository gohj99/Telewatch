/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.ui.main.LinkText
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.urlHandle

class RemindActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsSharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        setContent {
            TelewatchTheme {
                SplashRemindScreen { donate ->
                    with(settingsSharedPref.edit()) {
                        putBoolean("Remind3_read", true)
                        commit()
                        finish()
                    }
                    if (donate) {
                        startActivity(
                            Intent(
                                this@RemindActivity,
                                DonateActivity::class.java
                            )
                        )
                    }
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashRemindScreen(done: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(80)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .verticalRotaryScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.Remind),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 86.dp)
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 3.dp)
        ) {
            // 主要说明部分
            LinkText(
                text = stringResource(id = R.string.Remind3),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(16.dp),
                onLinkClick = { url ->
                    urlHandle(url, context)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 捐赠按钮
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp, start = 16.dp, end = 16.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { done(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3A7FBE),  // 按钮背景颜色
                    contentColor = Color.White   // 按钮文字颜色
                )
            ) {
                Text(text = stringResource(id = R.string.Donate))
            }
        }

        // 继续按钮
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp, start = 16.dp, end = 16.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { done(false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3E4D58),  // 按钮背景颜色
                    contentColor = Color.White   // 按钮文字颜色
                )
            ) {
                Text(text = stringResource(id = R.string.OK))
            }
        }

        Spacer(modifier = Modifier.height(42.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SplashRemindScreenPreview() {
    TelewatchTheme {
        SplashRemindScreen {}
    }
}
