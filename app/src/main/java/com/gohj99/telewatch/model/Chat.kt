/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.text.buildAnnotatedString

@SuppressLint("ParcelCreator")
data class Chat(
    val id: Long,
    val title: String,
    val message: androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {},
    val order: Long = -1, // 会话排序
    val isPinned: Boolean = false, // 是否在全部会话置顶
    val isRead: Boolean = false, // 聊天是否已读
    val isBot: Boolean = false, // 是否为机器人对话
    val isChannel: Boolean = false, // 是否为频道
    val isGroup: Boolean = false, // 是否为群组或者超级群组（supergroups??
    val isPrivateChat: Boolean = false, // 是否为私人会话
    val isArchiveChatPin: Boolean? = null // 归档会话是否置顶（非归档对话为null）
) : Parcelable {
    override fun describeContents(): Int {
        return 0 // 通常返回0即可，除非有特殊情况需要返回其他值
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
    }

    companion object CREATOR : Parcelable.Creator<Chat> {
        override fun createFromParcel(parcel: Parcel): Chat {
            return Chat(
                parcel.readLong(),
                parcel.readString() ?: "",
            )
        }

        override fun newArray(size: Int): Array<Chat?> {
            return arrayOfNulls(size)
        }
    }
}
