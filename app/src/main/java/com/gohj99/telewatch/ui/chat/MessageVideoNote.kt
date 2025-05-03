/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.ui.main.SplashLoadingScreen
import com.gohj99.telewatch.utils.formatDuration
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi
import java.io.File

@Composable
fun MessageVideoNote(
    messageVideoNote: TdApi.MessageVoiceNote,
    modifier: Modifier = Modifier
) {
    val videoNote = messageVideoNote.voiceNote
    val voiceFile = videoNote.voice
    val context = LocalContext.current
    var playTime by remember { mutableStateOf(0) }
    var playingShow by remember { mutableStateOf(false) }
    var isDownload by remember { mutableStateOf(voiceFile.local.isDownloadingCompleted) }
    var downloading by remember { mutableStateOf(false) }
    var fileUrl = remember { mutableStateOf("") }
    if (voiceFile.local.isDownloadingCompleted) fileUrl.value = voiceFile.local.path

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !isPlaying) {
                        // 播放器已准备好但未播放
                        playTime = (currentPosition / 1000).toInt()
                    } else if (state == Player.STATE_ENDED) {
                        playingShow = false
                        playTime = 0
                        seekTo(0) // 将播放位置重置到起点
                        pause()
                    }
                }
            })
        }
    }

    // 播放时间更新任务
    LaunchedEffect(playingShow) {
        if (playingShow) {
            while (playingShow) {
                playTime = (exoPlayer.currentPosition / 1000).toInt()
                delay(500) // 每 500ms 更新一次
                //println(exoPlayer.currentPosition)
                //println(playTime)
            }
        }
    }

    // 播放器初始化
    LaunchedEffect(isDownload) {
        //println("值改变isDownload: $isDownload")
        if (isDownload) {
            //println("初始化")
            try {
                var file = File(fileUrl.value)
                while (!file.exists() || file.length() == 0L) {
                    //("文件不存在或长度为0，正在等待...")
                    //println("完整途径：" + photoPath + "结尾")
                    delay(1000)  // 每 1000 毫秒检查一次
                    file = File(fileUrl.value)  // 重新获取文件状态
                    //println(fileUrl.value)
                    //println("文件大小：" + file.length())
                }
                //println("文件存在")
                exoPlayer.setMediaItem(MediaItem.fromUri(file.toUri()))
                exoPlayer.prepare()
                //println("初始化完成")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 资源释放
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDownload) {
            if (playingShow) {
                Image(
                    painter = painterResource(id = R.drawable.playing_audio),
                    contentDescription = "playing_audio",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            exoPlayer.pause()
                            playingShow = false
                        }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.play_audio),
                    contentDescription = "play_audio",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            exoPlayer.play()
                            playingShow = true
                        }
                )
            }
        } else {
            if (videoNote.mimeType == "audio/ogg") {
                if (downloading) {
                    SplashLoadingScreen()
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.download_audio),
                        contentDescription = "download_audio",
                        modifier = Modifier
                            .size(width = 32.dp, height = 32.dp)
                            .clip(CircleShape)
                            .clickable {
                                downloading = true
                                tgApi!!.downloadFile(
                                    file = voiceFile,
                                    schedule = { schedule -> },
                                    completion = { success, tdFleUrl ->
                                        if (success) {
                                            //println(tdFleUrl)
                                            if (tdFleUrl != null) fileUrl.value = tdFleUrl
                                            //println(fileUrl)
                                            isDownload = true
                                            downloading = false
                                        }
                                    }
                                )
                            }
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.Audio_Error),
                    color = Color(0xFF6985A2),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = modifier
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.width(42.dp))
            Image(
                painter = painterResource(id = R.drawable.video_ripple),
                contentDescription = "video_ripple",
                modifier = Modifier
                    .size(32.dp)
                    .scale(1.65f)
            )
            Text(
                text = "${formatDuration(playTime)} | ${formatDuration(videoNote.duration)}",
                color = Color(0xFF6985A2),
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier
            )
        }
    }
}
