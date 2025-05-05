/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.utils.telegram.downloadPhoto
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi
import java.io.IOException

@Composable
fun ThumbnailImage(
    thumbnail: TdApi.File,
    imageWidth: Int,
    imageHeight: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    loadingText: String = stringResource(id = R.string.loading)
) {
    val heightDp = with(LocalDensity.current) { imageHeight.toDp() }
    val widthDp = with(LocalDensity.current) { imageWidth.toDp() }
    var imagePath by remember { mutableStateOf<String?>(null) }
    val painter = rememberAsyncImagePainter(model = imagePath) // 在Composable作用域

    LaunchedEffect(thumbnail.id) {
        //println("执行")
        while (true) {
            if (thumbnail.local.isDownloadingCompleted) {
                try {
                    imagePath = thumbnail.local.path // 更新状态触发重组
                    println("下载完成")
                    println(imagePath)
                    break
                } catch (e: IOException) {
                    println("Image load failed: ${e.message}")
                    // 处理错误，例如显示占位符
                    break
                }
            } else {
                //println("本地没图片，正在下载图片")
                try {
                    tgApi!!.downloadPhoto(thumbnail) { success, path ->
                        if (success) {
                            thumbnail.local.isDownloadingCompleted = true
                            thumbnail.local.path = path
                        } else {
                            println("Download failed")

                        }
                    }
                } catch (e: Exception) {
                    println("Download error: ${e.message}")

                }
                delay(1000)
            }
        }
    }

    if (imagePath != null) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.5f)) // 设置半透明黑色背景
        )
        Image(
            painter = painter,
            contentDescription = "Thumbnail",
            modifier = Modifier.size(width = widthDp, height = heightDp)
        )
    } else {
        Box(
            modifier = Modifier.size(width = widthDp, height = heightDp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = loadingText,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
