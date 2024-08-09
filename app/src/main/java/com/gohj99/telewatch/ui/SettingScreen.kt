/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme

sealed class SettingItem {
    data class SwitchSetting(val title: String, val isChecked: Boolean) : SettingItem()
    data class RadioSetting(val title: String, val options: List<String>, val selectedOption: String) : SettingItem()
    data class TextSetting(val title: String, val text: String) : SettingItem()
    data class ClickableSetting(val title: String, val onClick: () -> Unit) : SettingItem()
    data class NestedSetting(val title: String, val onClick: () -> Unit) : SettingItem()
}

@Composable
fun SplashSettingScreen(settings: List<SettingItem>) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.Settings),
            color = Color.White,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        settings.forEach { setting ->
            when (setting) {
                is SettingItem.SwitchSetting -> {
                    val isChecked = remember { mutableStateOf(setting.isChecked) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = setting.title, color = Color.White)
                        Switch(checked = isChecked.value, onCheckedChange = { isChecked.value = it })
                    }
                }
                is SettingItem.RadioSetting -> {
                    val selectedOption = remember { mutableStateOf(setting.selectedOption) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = setting.title, color = Color.White)
                        setting.options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedOption.value == option,
                                    onClick = { selectedOption.value = option }
                                )
                                Text(text = option, color = Color.White)
                            }
                        }
                    }
                }
                is SettingItem.TextSetting -> {
                    val text = remember { mutableStateOf(setting.text) }
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        label = { Text(text = setting.title) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is SettingItem.ClickableSetting -> {
                    Button(onClick = setting.onClick, modifier = Modifier.fillMaxWidth()) {
                        Text(text = setting.title)
                    }
                }
                is SettingItem.NestedSetting -> {
                    Button(
                        onClick = setting.onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = setting.title)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SplashSettingScreenPreview() {
    TelewatchTheme {
        SplashSettingScreen(
            settings = listOf(
                SettingItem.SwitchSetting("Enable Notifications", true),
                SettingItem.RadioSetting("Select Theme", listOf("Light", "Dark"), "Light"),
                SettingItem.TextSetting("Username", "John Doe"),
                SettingItem.ClickableSetting("Click Me", onClick = { /* 执行一些代码 */ }),
                SettingItem.NestedSetting("Clearing cache", onClick = { /* 执行一些代码 */ }),
                SettingItem.NestedSetting("Reset libtd", onClick = { /* 执行一些代码 */ }),
                SettingItem.NestedSetting("Reset self", onClick = { /* 执行一些代码 */ })
            ),
        )
    }
}
