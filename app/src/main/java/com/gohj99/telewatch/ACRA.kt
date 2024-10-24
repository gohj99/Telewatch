/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.app.Application
import android.content.Context
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.HttpSenderConfigurationBuilder
import org.acra.data.StringFormat
import org.acra.sender.HttpSender

class ACRA : Application() {

    override fun onCreate() {
        super.onCreate()

        // 获取是否同意获取数据
        val settingsSharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val dataCollection = settingsSharedPref.getBoolean("Data_Collection", true)

        if (dataCollection) {
            // 初始化 com.gohj99.telewatch.ACRA 配置
            val config = CoreConfigurationBuilder()
                .withReportFormat(StringFormat.JSON) // 设置报告格式为 JSON
                .withPluginConfigurations(
                    HttpSenderConfigurationBuilder()
                        .withUri("https://acra.gohj99.site/report") // 设置服务器 URI
                        .withHttpMethod(HttpSender.Method.POST)     // 设置 HTTP 方法
                        .withBasicAuthLogin("xxpsMCujsVtf3Sxu")     // 基本认证用户名
                        .withBasicAuthPassword("1Ch23ThusrJhxVWD")  // 基本认证密码
                        .build()
                )

            // 初始化 com.gohj99.telewatch.ACRA
            ACRA.init(this, config)
        }
    }
}
