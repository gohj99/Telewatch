/*
 * Copyright (c) 2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.gohj99.telewatch.ui.main.MainCard
import com.gohj99.telewatch.ui.theme.TelewatchTheme
import com.gohj99.telewatch.ui.verticalRotaryScroll
import com.gohj99.telewatch.utils.telegram.TgApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

class SetProxyActivity : ComponentActivity() {
    private var tgApi: TgApi? = null
    private var tgProxies = mutableStateOf(TdApi.Proxies())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tgApi = TgApiManager.tgApi
        if (tgApi == null) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            updateProxies()
        }

        setContent {
            TelewatchTheme {
                SplashSetProxyScreen(tgProxies, ::updateProxies)
            }
        }
    }

    suspend fun updateProxies() {
        tgApi?.getProxy()?.let{
            tgProxies.value = it
        }
    }
}

@Composable
fun SplashSetProxyScreen(tgProxies: MutableState<TdApi.Proxies>, updateProxies: suspend() -> Unit) {
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

        ProxyLazyColumn(tgProxies, updateProxies)
    }
}

@Composable
fun ProxyLazyColumn(tgProxies: MutableState<TdApi.Proxies>, updateProxies: suspend() -> Unit) {
    val listState = rememberLazyListState()

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
        if (tgProxies.value.proxies != null) {
            items(tgProxies.value.proxies) { item ->
                ProxyItem(item, updateProxies)
            }
        }
        item {
            AddProxy()
        }
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ProxyItem(proxyItem: TdApi.Proxy, updateProxies: suspend () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    MainCard(
        column = {
            Text(
                text = proxyItem.server,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        },
        item = "AddProxy",
        color = if (proxyItem.isEnabled) Color(0xFF2C323A) else Color(0xFF404953),
        callback = {
            coroutineScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    if (proxyItem.isEnabled) {
                        TgApiManager.tgApi?.disableProxy()
                    } else {
                        TgApiManager.tgApi?.enableProxy(proxyItem.id)
                    }
                    updateProxies()
                }
            }
        },
        onLongClick = {
            coroutineScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    TgApiManager.tgApi?.removeProxy(proxyItem.id)
                    updateProxies()
                }
            }
        }
    )
}

@Composable
fun AddProxy() {
    val context = LocalContext.current

    MainCard(
        column = {
            Text(
                text = stringResource(id = R.string.AddProxy),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        },
        item = "AddProxy",
        callback = {
            context.startActivity(
                Intent(
                    context,
                    AddProxyActivity::class.java
                )
            )
        }
    )
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun SplashSetProxyScreenPreview() {
    TelewatchTheme {
        SplashSetProxyScreen(mutableStateOf(TdApi.Proxies())) {}
    }
}
