/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * gohj99调教了114514小时的表冠转动函数
 * 震动和线性滑动一应俱全
 * 包舒服的，跟德芙一样（
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun <T> Modifier.verticalRotaryScroll(
    state: T,
    reverse: Boolean = false,
    pagerState: PagerState? = null,
    pageCurrent: Int = -1
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val focusRequester = rememberActiveFocusRequester()

    if (pagerState != null) {

        LaunchedEffect(pagerState.isScrollInProgress) {
            if (!pagerState.isScrollInProgress && pagerState.currentPage == pageCurrent) focusRequester.requestFocus()
        }
    }

    return then(
        Modifier
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    // 震动反馈
                    if (vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    17, // 振动时长，单位为毫秒
                                    155   // 振幅，越低越弱，范围为1到255
                                )
                            )
                        } else {
                            vibrator.vibrate(40) // 振动时长为 50 毫秒
                        }
                    }

                    // 转动页面
                    val totalScroll = if (reverse) {
                        event.verticalScrollPixels * -1f
                    } else {
                        event.verticalScrollPixels * 1f
                    }
                    val stepCount = 30 // 分30步执行，更加丝滑
                    val stepSize = totalScroll / stepCount

                    // 分步滚动
                    for (i in 1..stepCount) {
                        when (state) {
                            is LazyListState -> state.scrollBy(stepSize)
                            is ScrollState -> state.scrollBy(stepSize)
                            else -> throw IllegalArgumentException("Unsupported state type")
                        }
                        delay(1L) // 延迟1ms
                    }
                }
                true
            }
            .focusRequester(focusRequester) // 请求焦点
            .focusable() // 聚焦
    )
}

