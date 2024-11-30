/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.SettingItem
import com.gohj99.telewatch.ui.theme.TelewatchTheme

@Composable
fun SplashSettingScreen(
    title: String,
    settings: MutableState<List<SettingItem>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 包含 Row 的 Box
        Box(
            modifier = Modifier
                .fillMaxWidth() // 只填充宽度
                .padding(top = 14.dp) // 添加顶部填充
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter) // 将 Row 对齐到顶部中央
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp)) // 添加间距

        SettingLazyColumn(settings)
    }
}

@Preview(showBackground = true)
@Composable
fun SplashSettingScreenPreview() {
    TelewatchTheme {
        val settings = remember {
            mutableStateOf(
                listOf(
                    SettingItem.Click(
                        itemName = "设置1",
                        onClick = {}
                    ),
                    SettingItem.Switch(
                        itemName = "设置2",
                        isSelected = true,
                        onSelect = {}
                    ),
                    SettingItem.Switch(
                        itemName = "设置3",
                        isSelected = true,
                        onSelect = {}
                    ),
                    SettingItem.ProgressBar(
                        itemName = "设置4",
                        progress = 0f,
                        maxValue = 100f,
                        minValue = 0f,
                        base = 1f,
                        onProgressChange = {}
                    )
                )
            )
        }
        SplashSettingScreen(
            title = stringResource(id = R.string.Settings),
            settings = settings
        )
    }
}
