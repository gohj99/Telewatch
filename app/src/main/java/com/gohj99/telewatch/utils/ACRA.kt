/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

import android.app.Application
import android.content.Context
import com.gohj99.telewatch.R
import org.acra.BuildConfig
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

class ACRA : Application() {

    override fun onCreate() {
        super.onCreate()

        // 获取是否同意获取数据
        val settingsSharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val dataCollection = settingsSharedPref.getBoolean("Data_Collection", false)

        if (dataCollection) {
            // 初始化 com.gohj99.telewatch.utils.ACRA 配置

            initAcra {
                // 核心配置
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.JSON

                // 配置 HTTP 发送器
                httpSender {
                    uri = "https://acra.gohj99.site/report" // 设置服务器 URI
                    httpMethod = HttpSender.Method.POST     // 设置 HTTP 方法
                    basicAuthLogin = "xxpsMCujsVtf3Sxu"     // 基本认证用户名
                    basicAuthPassword = "1Ch23ThusrJhxVWD"  // 基本认证密码
                    // 打开这个块自动启用插件
                }

                // 配置 Toast 插件
                toast {
                    text = getString(R.string.acra_toast_text)
                    // 打开这个块自动启用插件
                }
            }
        }
    }
}
