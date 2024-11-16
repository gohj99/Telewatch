/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.gohj99.telewatch.ui.main.ErrorScreen
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.utils.telegram.TgApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileOutputStream

class ImgViewActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var exceptionState by mutableStateOf<Exception?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TelewatchTheme {
                SplashLoadingScreen(modifier = Modifier.fillMaxSize())
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

                when (content) {
                    is TdApi.MessagePhoto -> {
                        val photo = content.photo
                        val photoSizes = photo.sizes
                        println(photoSizes)
                        val highestResPhoto = photoSizes.maxByOrNull { it.width * it.height }

                        highestResPhoto?.let {
                            val file = it.photo
                            if (!file.local.isDownloadingCompleted) {
                                tgApi?.downloadPhoto(file) { isSuccess, path ->
                                    if (isSuccess) {
                                        if (path != null) {
                                            checkFileAndShowImage(path, showImage(path))
                                        } else {
                                            checkFileAndShowImage(file.local.path, showImage(file.local.path))
                                        }
                                    } else {
                                        showError()
                                    }
                                }
                            } else {
                                checkFileAndShowImage(file.local.path, showImage(file.local.path))
                            }
                        } ?: showError()
                    }

                    is TdApi.MessageSticker -> {
                        println("贴纸消息处理中")
                        val photo = content.sticker.sticker

                        if (!photo.local.isDownloadingCompleted) {
                            tgApi?.downloadPhoto(photo) { isSuccess, path ->
                                if (isSuccess) {
                                    if (path != null) {
                                        checkFileAndShowImage(path, showTgs(path))
                                    } else {
                                        checkFileAndShowImage(photo.local.path, showTgs(photo.local.path))
                                    }
                                } else {
                                    showError()
                                }
                            }
                        } else {
                            //println(photo.local.path)
                            checkFileAndShowImage(photo.local.path, showTgs(photo.local.path))
                        }
                    }

                    is TdApi.MessageAnimation -> {
                        println("GIF消息处理中")
                        val photo = content.animation.animation

                        if (content.animation.mimeType == "image/gif") {
                            if (!photo.local.isDownloadingCompleted) {
                                tgApi?.downloadPhoto(photo) { isSuccess, path ->
                                    if (isSuccess) {
                                        if (path != null) {
                                            checkFileAndShowImage(path, showImage(path))
                                        } else {
                                            checkFileAndShowImage(photo.local.path, showImage(photo.local.path))
                                        }
                                    } else {
                                        showError()
                                    }
                                }
                            } else {
                                //println(photo.local.path)
                                checkFileAndShowImage(photo.local.path, showImage(photo.local.path))
                            }
                        } else if (content.animation.mimeType == "video/mp4") {
                            if (!photo.local.isDownloadingCompleted) {
                                tgApi?.downloadPhoto(photo) { isSuccess, path ->
                                    if (isSuccess) {
                                        if (path != null) {
                                            checkFileAndShowImage(path, showMp4(path))
                                        } else {
                                            checkFileAndShowImage(photo.local.path, showMp4(photo.local.path))
                                        }
                                    } else {
                                        showError()
                                    }
                                }
                            } else {
                                //println(photo.local.path)
                                checkFileAndShowImage(photo.local.path, showMp4(photo.local.path))
                            }
                        }
                    }

                    else -> showError()
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
            } // 添加了缺失的闭合括号
        }
    }

    private fun retryInitialization() {
        exceptionState = null
        initSelf()
    }

    private fun checkFileAndShowImage(photoPath: String, callback: Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            var file = File(photoPath)
            while (!file.exists() || file.length() == 0L) {
                //("文件不存在或长度为0，正在等待...")
                //println("完整途径：" + photoPath + "结尾")
                delay(1000)  // 每 1000 毫秒检查一次
                file = File(photoPath)  // 重新获取文件状态
            }
            withContext(Dispatchers.Main) {
                callback
            }
        }
    }

    private fun showImage(photoPath: String) {
        setContent {
            TelewatchTheme {
                SplashImgView(photoPath, {
                    Toast.makeText(this, saveImageToExternalStorage(this, photoPath), Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    private fun showTgs(photoPath: String) {
        setContent {
            TelewatchTheme {
                SplashTgsView(photoPath)
            }
        }
    }

    private fun showMp4(photoPath: String) {
        setContent {
            TelewatchTheme {
                SplashMp4View(photoPath)
            }
        }
    }

    private fun showError() {
        println("Error: ${R.string.Error_ImgView}")
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

fun saveImageToExternalStorage(context: Context, photoPath: String): String {
    // 获取图片文件名
    val imageName = File(photoPath).nameWithoutExtension

    // 从内部存储读取图片
    val bitmap = BitmapFactory.decodeFile(photoPath) ?: return context.getString(R.string.Read_failed)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10及以上使用MediaStore
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("$imageName.jpg")
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(queryUri, null, selection, selectionArgs, null)

        if (cursor != null && cursor.count > 0) {
            cursor.close()
            return context.getString(R.string.Same_name_image_exists)
        }
        cursor?.close()

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Telewatch")
            put(MediaStore.Images.Media.IS_PENDING, 1) // 设置IS_PENDING状态
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return context.getString(R.string.failling)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
        }

        // 更新IS_PENDING状态
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, contentValues, null, null)

        return context.getString(R.string.Save_success)
    } else {
        // Android 8和9使用传统方式保存到外部存储
        val imagesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Telewatch")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val file = File(imagesDir, "$imageName.jpg")

        if (file.exists()) {
            return context.getString(R.string.Same_name_image_exists)
        }

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }

        // 通知系统图库更新
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))

        return context.getString(R.string.Save_success)
    }
}

@Composable
private fun SplashImgView(photoPath: String, saveImage: () -> Unit) {
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
            modifier = Modifier
                .zoomable(
                    zoomState = rememberZoomState(),
                    onLongPress = {
                        saveImage()
                    }
                ),
        )
    }
}

@Composable
private fun SplashTgsView(photoPath: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Load the Lottie composition from the file path
        val composition by rememberLottieComposition(LottieCompositionSpec.File(photoPath))

        // Create and control the animation
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever // Set to repeat forever
        )

        // Render the animation
        LottieAnimation(
            composition = composition,
            progress = progress,
        )
    }
}

@Composable
private fun SplashMp4View(photoPath: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LoopingVideoPlayer(Uri.parse(photoPath))
    }
}

@Composable
fun LoopingVideoPlayer(videoUri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(
        AndroidView(factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Disable the playback controls
            }
        })
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}
