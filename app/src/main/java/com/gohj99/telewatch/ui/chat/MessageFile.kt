/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui.chat

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.TgApiManager.tgApi
import com.gohj99.telewatch.utils.formatSize
import org.drinkless.tdlib.TdApi

@Composable
fun MessageFile(
    content: TdApi.MessageDocument,
    stateDownload: MutableState<Boolean>,
    stateDownloadDone: MutableState<Boolean>,
    modifier: Modifier = Modifier
){
    val context = LocalContext.current
    val document = content.document
    val file = document.document
    var fileUrl by remember { mutableStateOf(file.local.path) }
    val fileName = document.fileName
    val fileSize = file.size
    var downloadSchedule by remember { mutableStateOf(file.local.downloadedSize) }

    // 检查文件状态
    LaunchedEffect(document) {
        if (file.local.isDownloadingCompleted) {
            stateDownloadDone.value = true
        } else {
            stateDownloadDone.value = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (stateDownloadDone.value) {
            Image(
                painter = painterResource(id = R.drawable.file_icon),
                contentDescription = "downloaded_file",
                modifier = Modifier
                    .size(width = 32.dp, height = 32.dp)
                    .clip(CircleShape)
                    .clickable {
                        Toast.makeText(context, fileUrl, Toast.LENGTH_SHORT).show()
                    }
            )
        } else {
            if (stateDownload.value) {
                Image(
                    painter = painterResource(id = R.drawable.remove_icon),
                    contentDescription = "remove",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            tgApi!!.cancelDownloadFile(file.id) {
                                stateDownload.value = false
                                stateDownloadDone.value = false
                            }
                        }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.download_file),
                    contentDescription = "download_file",
                    modifier = Modifier
                        .size(width = 32.dp, height = 32.dp)
                        .clip(CircleShape)
                        .clickable {
                            stateDownload.value = true
                            tgApi!!.downloadFile(
                                file = file,
                                schedule = { schedule ->
                                    downloadSchedule = schedule.local.downloadedSize
                                },
                                completion = { success, tdFleUrl ->
                                    if (success) {
                                        //println(tdFleUrl)
                                        if (tdFleUrl != null) fileUrl = tdFleUrl
                                        //println(fileUrl)
                                        stateDownloadDone.value = true
                                        stateDownload.value = false
                                    }
                                }
                            )
                        }
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fileName,
                color = Color(0xFFFEFEFE),
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text =
                    if (downloadSchedule == 0L || downloadSchedule == fileSize) formatSize(fileSize)
                    else "${formatSize(downloadSchedule)} | ${formatSize(fileSize)}",
                color = Color(0xFF6985A2),
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier
            )
        }
    }
}