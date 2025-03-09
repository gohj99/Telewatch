/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.ui.CustomButton
import com.gohj99.telewatch.ui.InputBar
import com.gohj99.telewatch.ui.InputIntBar
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.telegram.TgApi
import org.drinkless.tdlib.TdApi

fun parseProxyUrl(url: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val queryParams = url.split("?")[1].split("&")

    for (param in queryParams) {
        val keyValue = param.split("=")
        if (keyValue.size == 2) {
            result[keyValue[0]] = keyValue[1]
        }
    }

    return result
}

class AddProxyActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var proxyUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tgApi = TgApiManager.tgApi
        if (tgApi == null) {
            finish()
            return
        }


        // 获取传参
        var parseServer = ""
        var parsePort: Int? = null
        var parsePassword = ""
        var useProxy = ""
        proxyUrl = intent.getStringExtra("proxyUrl").toString()
        try {
            proxyUrl?.let { url ->
                val parsedParams = parseProxyUrl(url)
                parseServer = parsedParams["server"].toString()
                parsePort = parsedParams["port"]?.toInt()
                parsePassword = parsedParams["secret"].toString()
                useProxy = "MTPROTO"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            TelewatchTheme {
                SplashAddProxyScreen(
                    add = { server, port, type ->
                        tgApi?.addProxy(server, port, type)
                    },
                    parseServer = parseServer,
                    parsePort = parsePort,
                    parsePassword = parsePassword,
                    useProxy = useProxy
                )
            }
        }
    }
}

@Composable
fun SplashAddProxyScreen(add: (String, Int, TdApi.ProxyType) -> Unit, parseServer: String = "", parsePort: Int? = null, parsePassword:String, useProxy: String = "") {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        var server by remember { mutableStateOf(parseServer) }
        var port by remember { mutableStateOf<Int?>(parsePort) }
        var useProxy by remember { mutableStateOf(useProxy) }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf(parsePassword) }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 包含 Row 的 Box
            Box(
                modifier = Modifier
                    .fillMaxWidth() // 只填充宽度
                    .padding(top = 14.dp) // 添加顶部填充
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter) // 将 Row 对齐到顶部中央
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically // 垂直方向居中对齐
                ) {
                    Text(
                        text = stringResource(id = R.string.Proxy),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // 添加间距

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalRotaryScroll(listState)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    /*Text(
                        text = stringResource(id = R.string.Server),
                        color = Color.White,
                        modifier = Modifier.padding(start = 18.dp),
                        style = MaterialTheme.typography.titleMedium
                    )*/
                    InputBar(
                        query = server,
                        onQueryChange = { server = it },
                        placeholder = stringResource(id = R.string.Server)
                    )
                }
                item {
                    /*Text(
                        text = stringResource(id = R.string.Port),
                        color = Color.White,
                        modifier = Modifier.padding(start = 18.dp),
                        style = MaterialTheme.typography.titleMedium
                    )*/
                    InputIntBar(
                        query = port,
                        onQueryChange = { port = it },
                        placeholder = stringResource(id = R.string.Port),
                        maxQuery = 65535,
                        minQuery = 1
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    // 选择SOCKS5代理
                    MainCard(
                        column = {
                            Text(
                                text = "SOCKS5",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        item = "SOCKS5",
                        color = if (useProxy == "SOCKS5") Color(0xFF2C323A) else Color(0xFF404953),
                        callback = {
                            useProxy = "SOCKS5"
                        }
                    )
                }
                item {
                    // 选择HTTP代理
                    MainCard(
                        column = {
                            Text(
                                text = "HTTP",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        item = "HTTP",
                        color = if (useProxy == "HTTP") Color(0xFF2C323A) else Color(0xFF404953),
                        callback = {
                            useProxy = "HTTP"
                        }
                    )
                }
                item {
                    // 选择MTPROTO代理
                    MainCard(
                        column = {
                            Text(
                                text = "MTPROTO",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        item = "MTPROTO",
                        color = if (useProxy == "MTPROTO") Color(0xFF2C323A) else Color(0xFF404953),
                        callback = {
                            useProxy = "MTPROTO"
                        }
                    )
                }
                item {
                    if (useProxy == "SOCKS5" || useProxy == "HTTP") {
                        Spacer(modifier = Modifier.height(12.dp))
                        InputBar(
                            query = username,
                            onQueryChange = { username = it },
                            modifier = Modifier,
                            placeholder = stringResource(id = R.string.username) + "(${stringResource(id = R.string.Optional)})"
                        )
                        InputBar(
                            query = password,
                            onQueryChange = { password = it },
                            placeholder = stringResource(id = R.string.password) + "(${stringResource(id = R.string.Optional)})"
                        )
                    } else if (useProxy == "MTPROTO") {
                        Spacer(modifier = Modifier.height(12.dp))
                        InputBar(
                            query = password,
                            onQueryChange = { password = it },
                            placeholder = stringResource(id = R.string.Key)
                        )
                    }
                }
                item {
                    // 完成添加
                    if (server.isNotEmpty() &&  port != null &&  useProxy != "") {
                        if (!(useProxy == "MTPROTO" && !password.startsWith("ee") && (password.length != 22 && password.length != 32 && password.length != 44 && password.length != 64))) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomButton(
                                    onClick = {
                                        if (port != null) {
                                            when(useProxy) {
                                                "SOCKS5" -> add(server, port!!, TdApi.ProxyTypeSocks5(username, password))
                                                "HTTP" -> add(server, port!!, TdApi.ProxyTypeHttp(username, password, false))
                                                "MTPROTO" -> add(server, port!!, TdApi.ProxyTypeMtproto(password))
                                            }
                                        }
                                    },
                                    text = stringResource(id = R.string.Add),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}
