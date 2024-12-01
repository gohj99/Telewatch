/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.model

import android.os.Parcel
import android.os.Parcelable
import org.drinkless.tdlib.TdApi

data class tgFile(
    val file: TdApi.File
) : Parcelable {
    constructor(parcel: Parcel) : this(
        TdApi.File().apply {
            id = parcel.readInt() // 修改这里
            local = TdApi.LocalFile().apply {
                path = parcel.readString()
            }
            remote = TdApi.RemoteFile().apply {
                id = parcel.readString()
            }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(file.id) // 修改这里
        parcel.writeString(file.local?.path)
        parcel.writeString(file.remote?.id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<tgFile> {
        override fun createFromParcel(parcel: Parcel): tgFile {
            return tgFile(parcel)
        }

        override fun newArray(size: Int): Array<tgFile?> {
            return arrayOfNulls(size)
        }
    }
}
