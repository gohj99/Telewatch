/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun Modifier.verticalRotaryScroll(
    state: LazyListState
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    return then(
        Modifier
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    // 表冠顺时针往下滚动，逆时针往上滚动
                    state.scrollBy(event.verticalScrollPixels)

                    // 震动反馈
                    if (vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // API 26~28 使用 VibrationEffect
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    17, // 振动时长，单位为毫秒
                                    155   // 振幅，越低越弱，范围为1到255
                                )
                            )
                        } else {
                            // API 26 以下使用旧版的 vibrate 方法
                            vibrator.vibrate(50) // 振动时长为 50 毫秒
                        }
                    }
                }
                true
            }
            .focusRequester(rememberActiveFocusRequester()) // 请求焦点
            .focusable() // 聚焦
    )
}
