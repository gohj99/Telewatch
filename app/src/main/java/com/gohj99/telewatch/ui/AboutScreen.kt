/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gohj99.telewatch.R
import com.gohj99.telewatch.ui.theme.TelewatchTheme


@Composable
fun SplashAboutScreen(appVersion: String, buildDate: String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.4f)
                    .clip(CircleShape)
                    .align(Alignment.Center)
            )
            Image(
                painter = painterResource(id = R.drawable.circle),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        Text(
            text = stringResource(id = R.string.app_name),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.Build_Date) + " $buildDate",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.Version) + " $appVersion",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp) // 只在左右添加 padding
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)) {
                    Text(
                        text = stringResource(R.string.author),
                        color = Color.White,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape) // 将头像裁剪为圆形
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_gohj99),
                                contentDescription = "Avatar_gohj99",
                                modifier = Modifier
                                    .fillMaxSize(), // 确保 Image 填充整个 Box
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp)) // 调整头像和文字之间的间距
                        Text(
                            text = "gohj99",
                            color = Color.White,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            modifier = Modifier
                                .weight(1f) // 使用 weight 确保文字占据剩余空间
                                .height(25.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis // 如果文字过长，使用省略号表示
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp)) // 作者与作者的间隙

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape) // 将头像裁剪为圆形
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_little_jelly),
                                contentDescription = "Avatar_little_jelly",
                                modifier = Modifier
                                    .fillMaxSize(), // 确保 Image 填充整个 Box
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp)) // 调整头像和文字之间的间距

                        Text(
                            text = stringResource(id = R.string.little_jelly),
                            color = Color.White,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            modifier = Modifier
                                .weight(1f) // 使用 weight 确保文字占据剩余空间
                                .height(25.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis // 如果文字过长，使用省略号表示
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp) // 只在左右添加 padding
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF404953) // 设置 Card 的背景颜色
                )
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 9.dp)) {
                    Text(
                        text = stringResource(R.string.Famous_Quotes),
                        color = Color.White,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "\nLife is dear, love is dearer. Both can be given up for freedom.\n\n宁鸣而死，不默而生\n\n己所不欲，勿施予人\n\nLiberté, Égalité, Fraternité\n\nWo man singt, da lass dich ruhig nieder, böse Menschen haben keine Lieder.\n\nСвобода лучше, чем несвобода.",
                        color = Color.White,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SplashAboutScreenPreview() {
    TelewatchTheme {
        SplashAboutScreen("1.0", "2024")
    }
}
