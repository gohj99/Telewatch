/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatchlite

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatchlite.telegram.TgApi
import com.gohj99.telewatchlite.ui.chat.SplashChatScreen
import com.gohj99.telewatchlite.ui.main.Chat
import com.gohj99.telewatchlite.ui.main.SplashLoadingScreen
import com.gohj99.telewatchlite.ui.theme.telewatchliteTheme
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class ChatActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var chat: Chat? = null
    private var chatList = mutableStateOf(emptyList<TdApi.Message>())
    private var lastReadOutboxMessageId = mutableStateOf(0L)
    private var lastReadInboxMessageId = mutableStateOf(0L)

    @SuppressLint("AutoboxingStateCreation")
    private var currentUserId = mutableStateOf(-1L) // 使用 MutableState 来持有当前用户 ID

    override fun onDestroy() {
        super.onDestroy()
        TgApiManager.tgApi?.exitChatPage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 显示加载页面
        setContent {
            telewatchliteTheme {
                SplashLoadingScreen(modifier = Modifier.fillMaxSize())
            }
        }

        tgApi = TgApiManager.tgApi

        // 接收传递的 Chat 对象
        chat = intent.getParcelableExtra("chat")

        // 如果 chat 为 null，直接退出页面
        if (chat == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        // 已读未读消息id传参
        lastReadOutboxMessageId = tgApi!!.getLastReadOutboxMessageId()
        lastReadInboxMessageId = tgApi!!.getLastReadInboxMessageId()

        // 清空旧的聊天消息
        chatList.value = emptyList()

        // 异步获取当前用户 ID 和聊天记录
        lifecycleScope.launch {
            currentUserId.value = tgApi!!.getCurrentUser()[0].toLong()
            tgApi!!.getChatMessages(chat!!.id, chatList) // 异步加载全部聊天消息
        }

        // 异步获取当前用户聊天对象
        chat?.let { safeChat ->
            var chatObject: TdApi.Chat?  // 在外部声明变量

            runBlocking {
                chatObject = tgApi!!.getChat(safeChat.id)  // 在 runBlocking 中赋值
            }

            // 这里可以使用 chatObject，因为它在 runBlocking 块外声明了
            chatObject?.let { itChatObject ->
                lastReadOutboxMessageId.value = itChatObject.lastReadOutboxMessageId
                lastReadInboxMessageId.value = itChatObject.lastReadInboxMessageId

                setContent {
                    telewatchliteTheme {
                        SplashChatScreen(
                            chatTitle = chat!!.title,
                            chatList = chatList,
                            chatId = chat!!.id,
                            currentUserId = currentUserId.value,
                            goToChat = { chat ->
                                startActivity(
                                    Intent(this@ChatActivity, ChatActivity::class.java).apply {
                                        putExtra("chat", chat)
                                    }
                                )
                            },
                            sendCallback = { messageText ->
                                tgApi?.sendMessage(
                                    chatId = chat!!.id,
                                    messageText = messageText
                                )
                            },
                            press = { message ->
                                println("点击触发")
                                println(message.id)
                                when (message.content) {
                                    is TdApi.MessageText -> {
                                        println("文本消息")
                                    }

                                    is TdApi.MessagePhoto -> {
                                        println("图片消息")
                                        val intent = Intent(this, ImgViewActivity::class.java)
                                        intent.putExtra("messageId", message.id)
                                        startActivity(intent)
                                    }

                                    is TdApi.MessageVideo -> {
                                        println("视频消息")
                                        lifecycleScope.launch {
                                            tgApi!!.getMessageTypeById(message.id)?.let {
                                                val videoFile =
                                                    (it.content as TdApi.MessageVideo).video.video
                                                getUriFromFilePath(
                                                    this@ChatActivity,
                                                    videoFile.local.path
                                                )?.let { uri ->
                                                    playVideo(uri)
                                                }
                                            }
                                        }
                                    }

                                    is TdApi.MessageVoiceNote -> {
                                        println("语音消息")
                                    }

                                    is TdApi.MessageAnimation -> {
                                        println("动画消息")
                                    }
                                }
                            },
                            longPress = { select, message ->
                                println("长按触发")
                                println(message)
                                when (select) {
                                    "ReloadMessage" -> {
                                        tgApi!!.reloadMessageById(message.id)
                                        Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show()
                                        return@SplashChatScreen "OK"
                                    }

                                    "GetMessage" -> {
                                        return@SplashChatScreen tgApi!!.getMessageTypeById(message.id)
                                            ?.let { messageType ->
                                                val gson = Gson()
                                                val messageJson = gson.toJson(messageType)
                                                formatJson(messageJson)
                                            } ?: "error"
                                        /*val gson = Gson()
                                        val messageJson = gson.toJson(message)
                                        return@SplashChatScreen formatJson(messageJson)*/
                                    }

                                    "Save" -> {
                                        when (message.content) {
                                            is TdApi.MessagePhoto -> {
                                                tgApi!!.getMessageTypeById(message.id)?.let {
                                                    val content = it.content as TdApi.MessagePhoto
                                                    val photo = content.photo
                                                    val photoSizes = photo.sizes
                                                    val highestResPhoto =
                                                        photoSizes.maxByOrNull { it.width * it.height }
                                                    highestResPhoto?.let { itPhoto ->
                                                        val file = itPhoto.photo
                                                        if (file.local.isDownloadingCompleted) {
                                                            Toast.makeText(
                                                                this,
                                                                saveImageToExternalStorage(
                                                                    this,
                                                                    file.local.path
                                                                ),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@SplashChatScreen "OK"
                                                        } else {
                                                            Toast.makeText(
                                                                this,
                                                                getString(R.string.Download_first),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@SplashChatScreen "OK"
                                                        }
                                                    }
                                                }
                                            }

                                            is TdApi.MessageVideo -> {
                                                tgApi!!.getMessageTypeById(message.id)?.let {
                                                    val content = it.content as TdApi.MessageVideo
                                                    val video = content.video
                                                    video.video.let { videoIt ->
                                                        val videoFile: TdApi.File = videoIt
                                                        if (videoFile.local.isDownloadingCompleted) {
                                                            Toast.makeText(
                                                                this,
                                                                saveVideoToExternalStorage(
                                                                    this,
                                                                    videoFile.local.path
                                                                ),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@SplashChatScreen "OK"
                                                        } else {
                                                            Toast.makeText(
                                                                this,
                                                                getString(R.string.Download_first),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            return@SplashChatScreen "OK"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Toast.makeText(
                                            this,
                                            getString(R.string.No_need_to_save),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@SplashChatScreen "OK"
                                    }

                                    else -> return@SplashChatScreen "NotFind"
                                }
                            },
                            chatObject = itChatObject,
                            lastReadOutboxMessageId = lastReadOutboxMessageId,
                            lastReadInboxMessageId = lastReadInboxMessageId
                        )
                    }
                }
            }
        }
    }

    fun saveVideoToExternalStorage(context: Context, videoPath: String): String {
        // 获取视频文件名
        val videoName = File(videoPath).nameWithoutExtension

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
            val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("$videoName.mp4")
            val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val cursor =
                context.contentResolver.query(queryUri, null, selection, selectionArgs, null)

            if (cursor != null && cursor.count > 0) {
                cursor.close()
                return context.getString(R.string.Same_name_video_exists)
            }
            cursor?.close()

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "$videoName.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/telewatchlite"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1) // 设置IS_PENDING状态
            }

            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
                ?: return context.getString(R.string.failling)

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    File(videoPath).inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return context.getString(R.string.failling)
            }

            // 更新IS_PENDING状态
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            return context.getString(R.string.Save_success)
        } else {
            // Android 8和9使用传统方式保存到外部存储
            val videosDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "telewatchlite"
            )
            if (!videosDir.exists()) {
                videosDir.mkdirs()
            }

            val file = File(videosDir, "$videoName.mp4")

            if (file.exists()) {
                return context.getString(R.string.Same_name_video_exists)
            }

            FileInputStream(File(videoPath)).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 通知系统图库更新
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))

            return context.getString(R.string.Save_success)
        }
    }

    fun saveImageToExternalStorage(context: Context, photoPath: String): String {
        // 获取图片文件名
        val imageName = File(photoPath).nameWithoutExtension

        // 从内部存储读取图片
        val bitmap =
            BitmapFactory.decodeFile(photoPath) ?: return context.getString(R.string.Read_failed)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("$imageName.jpg")
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor =
                context.contentResolver.query(queryUri, null, selection, selectionArgs, null)

            if (cursor != null && cursor.count > 0) {
                cursor.close()
                return context.getString(R.string.Same_name_image_exists)
            }
            cursor?.close()

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$imageName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/telewatchlite"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1) // 设置IS_PENDING状态
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
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
            val imagesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "telewatchlite"
            )
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

    @SuppressLint("QueryPermissionsNeeded")
    fun playVideo(videoUri: Uri) {
        // 创建一个 Intent 来播放视频
        val intent = Intent(Intent.ACTION_VIEW).apply {
            // 设置数据和 MIME 类型
            setDataAndType(videoUri, "video/*")
            // 确保系统能够通过选择器显示可用的播放器应用
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 检查是否有应用能够处理这个 Intent
        if (intent.resolveActivity(packageManager) != null) {
            // 启动系统的选择器来选择一个播放器
            startActivity(Intent.createChooser(intent, getString(R.string.Select_player)))
        } else {
            // 处理没有应用能够播放视频的情况
            Toast.makeText(this, getString(R.string.No_player), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUriFromFilePath(context: Context, filePath: String): Uri? {
        // 创建File对象
        val file = File(filePath)

        // 确保文件存在
        if (!file.exists()) {
            return null
        }

        // 返回Uri
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun formatJson(jsonString: String): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonElement = gson.fromJson(jsonString, Any::class.java)
        return gson.toJson(jsonElement)
    }
}
