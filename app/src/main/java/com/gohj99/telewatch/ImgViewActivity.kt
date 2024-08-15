/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.gohj99.telewatch.telegram.TgApi
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

class ImgViewActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var exceptionState by mutableStateOf<Exception?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TelewatchTheme {
                SplashLoadingScreen()
            }
        }

        initSelf()
    }

    private fun initSelf() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val chatId = intent.getLongExtra("messageId", -1L)
                tgApi = TgApiManager.tgApi
                val message = tgApi?.getMessageTypeById(chatId)
                val content = message?.content

                if (content is TdApi.MessagePhoto) {
                    val photo = content.photo
                    val photoSizes = photo.sizes
                    val highestResPhoto = photoSizes.maxByOrNull { it.width * it.height }

                    highestResPhoto?.let {
                        val file = it.photo
                        if (!file.local.isDownloadingCompleted) {
                            tgApi?.downloadPhoto(file) { isSuccess, path ->
                                if (isSuccess) {
                                    if (path != null) {
                                        checkFileAndShowImage(path)
                                    } else {
                                        checkFileAndShowImage(file.local.path)
                                    }
                                } else {
                                    showError()
                                }
                            }
                        } else {
                            checkFileAndShowImage(file.local.path)
                        }
                    } ?: showError()
                } else {
                    showError()
                }
            } catch (e: Exception) {
                exceptionState = e
                launch(Dispatchers.Main) {
                    setContent {
                        TelewatchTheme {
                            ErrorScreen(
                                onRetry = { retryInitialization() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun retryInitialization() {
        exceptionState = null
        initSelf()
    }

    private fun checkFileAndShowImage(photoPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var file = File(photoPath)
            while (!file.exists() || file.length() == 0L) {
                //println("文件不存在或长度为0，正在等待...")
                //println("完整途径：" + photoPath + "结尾")
                delay(1000)  // 每 1000 毫秒检查一次
                file = File(photoPath)  // 重新获取文件状态
            }
            withContext(Dispatchers.Main) {
                showImage(photoPath)
            }
        }
    }

    private fun showImage(photoPath: String) {
        setContent {
            TelewatchTheme {
                SplashImgView(photoPath)
            }
        }
    }

    private fun showError() {
        setContent {
            TelewatchTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.Error_ImgView),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashImgView(photoPath: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = photoPath),
            contentDescription = null,
            modifier = Modifier.zoomable(rememberZoomState()),
        )
    }
}
