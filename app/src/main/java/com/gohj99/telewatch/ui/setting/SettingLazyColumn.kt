/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.model.SettingItem
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.verticalRotaryScroll
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SettingLazyColumn(
    itemsList: MutableState<List<SettingItem>>,
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
        item {
            Spacer(modifier = Modifier.height(8.dp)) // 添加一个高度为 8dp 的 Spacer
        }
        items(itemsList.value.size) { index ->
            val item = itemsList.value[index]
            when (item) {
                is SettingItem.Click -> {
                    SettingClickView(item.itemName, item.onClick, item.color)
                }

                is SettingItem.Switch -> {
                    SettingSwitchView(item.itemName, item.isSelected, item.onSelect, item.color)
                }

                is SettingItem.ProgressBar -> {
                    SettingProgressBarView(
                        item.itemName,
                        item.progress,
                        item.maxValue,
                        item.minValue,
                        item.base,
                        item.onProgressChange,
                        item.color
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun SettingClickView(item: String, callback: () -> Unit, colors: Color = Color(0xFF404953)) {
    Box(
        modifier = Modifier
    ) {
        MainCard(
            column = {
                Text(
                    text = item,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            item = item,
            callback = {
                callback()
            },
            color = colors
        )
    }
}

@Composable
fun SettingSwitchView(
    item: String,
    isSelected: Boolean,
    callback: (Boolean) -> Unit,
    colors: Color = Color(0xFF404953)
) {
    var isSwitchOn by rememberSaveable { mutableStateOf(isSelected) }
    Box(
        modifier = Modifier
    ) {
        MainCard(
            column = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,  // 使文字和按钮垂直居中
                    modifier = Modifier.fillMaxWidth()  // 占据整个宽度
                ) {
                    Text(
                        text = item,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // 自定义开关组件
                    Switch(
                        checked = isSwitchOn,
                        onCheckedChange = {
                            isSwitchOn = it
                            callback(isSwitchOn)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.Gray,
                            checkedTrackColor = Color(0xFF0480FD),
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .scale(0.81f)  // 缩小 Switch 的尺寸，比如缩小到 80%
                            .align(Alignment.CenterVertically)  // 确保开关与文字居中对齐
                            .height(33.dp)
                    )
                }
            },
            item = item,
            callback = {
                isSwitchOn = !isSwitchOn
                callback(isSwitchOn)
            },
            color = colors
        )
    }
}

@Composable
fun SettingProgressBarView(
    item: String,
    oldValue: Float,
    maxValue: Float,
    minValue: Float,
    base: Float,
    callback: (Float) -> Unit,
    colors: Color = Color(0xFF404953)
) {
    var parameterValue by rememberSaveable { mutableFloatStateOf(oldValue) }

    // 计算 base 的小数位数
    val decimalPlaces = base.toString().substringAfter('.').length

    Box(
        modifier = Modifier
    ) {
        MainCard(
            column = {
                Text(
                    text = item,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (parameterValue > minValue) {
                                parameterValue -= base
                                parameterValue = BigDecimal(parameterValue.toString())
                                    .setScale(decimalPlaces, RoundingMode.HALF_UP)
                                    .toFloat()
                                callback(parameterValue)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.remove),
                            contentDescription = "Decrease",
                            tint = Color(0xFF0480FD)
                        )
                    }

                    // 使用 Slider 并隐藏滑块的实现
                    Slider(
                        value = parameterValue,
                        onValueChange = {
                            // 这里根据 base 的小数位动态设置保留的小数位数
                            parameterValue = BigDecimal(it.toString())
                                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                                .toFloat()
                            callback(parameterValue)
                        },
                        valueRange = minValue..maxValue,
                        modifier = Modifier
                            .fillMaxWidth(1f)  // 控制Slider的宽度
                            .height(8.dp)
                            .weight(20f)
                            .padding(start = 6.dp, end = 4.25.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1C1F24),
                            activeTrackColor = Color(0xFF0480FD), // 进度条的颜色
                            inactiveTrackColor = Color(0xFF1C1F24) // 进度条背景色
                        )
                    )

                    IconButton(
                        onClick = {
                            if (parameterValue < maxValue) {
                                parameterValue += base
                                parameterValue = BigDecimal(parameterValue.toString())
                                    .setScale(decimalPlaces, RoundingMode.HALF_UP)
                                    .toFloat()
                                callback(parameterValue)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = "Increase",
                            tint = Color(0xFF0480FD)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), // 填满父布局宽度
                    horizontalArrangement = Arrangement.Center // 水平居中
                ) {
                    Text(
                        text = stringResource(R.string.size) + ": $parameterValue",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            item = item,
            color = colors
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingProgressBarViewPreview() {
    SettingProgressBarView("参数设置", 0f, 100f, 0f, 1f, {})
}
