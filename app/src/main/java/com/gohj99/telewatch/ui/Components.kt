/*
 * Copyright (c) 2024-2025 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gohj99.telewatch.R
import kotlinx.coroutines.flow.first

// 蓝底按钮
@Composable
fun CustomButton(
    onClick: () -> Unit,
    text: String,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    color: Color = Color(0xFF2397D3),
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed && enabled) 0.9f else 1f, label = "")

    Box(
        modifier = modifier
            .scale(scale)  // 应用缩放到整个Box
            .size(width = 148.dp, height = 33.dp)
            .clip(shape)
            .background(color)
            .pointerInput(Unit) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        },
                        onTap = {
                            onClick()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor
        )
    }
}

@Composable
fun InputIntBar(
    query: Int?,
    onQueryChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
        .padding(horizontal = 16.dp)
        .height(40.dp),
    placeholder: String = "",
    maxQuery: Int? = null,
    minQuery: Int? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                val cornerRadius = strokeWidth / 2 // 圆角半径，使用线宽的一半
                drawRoundRect(
                    color = Color(0xFF2C323A),
                    topLeft = Offset(0f, size.height - strokeWidth),
                    size = Size(size.width, strokeWidth),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .clip(RoundedCornerShape(28.dp))
    ) {
        BasicTextField(
            value = query?.toString() ?: "",  // 显示数字，但转换为字符串显示，允许为空
            onValueChange = { newText ->
                // 只允许数字输入，使用正则检查
                if (newText.isEmpty() || newText.all { it.isDigit() }) {
                    var int = newText.toIntOrNull()
                    if (maxQuery != null && int != null && int >= maxQuery) {
                        int = maxQuery
                    }
                    if (minQuery != null && int != null && int <= minQuery) {
                        int = minQuery
                    }
                    onQueryChange(int)  // 处理空字符串或非数字
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp) // 调整内边距，使文本框和图标之间有空间
                .align(Alignment.CenterStart), // 文本垂直居中
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            ),
            cursorBrush = SolidColor(Color(0.0f, 0.0f, 0.0f, 0.0f)),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),  // 设置输入法只允许数字
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (query == null || query == 0) {  // 如果为空或为0，显示占位符
                            Text(
                                text = placeholder,
                                color = Color(0xFF4E5C67),
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        }
                        innerTextField() // 输入框内容
                    }
                }
            }
        )
    }
}

@Composable
fun InputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .defaultMinSize(minHeight = 40.dp) // 设置最小高度，允许根据内容自动增高
            .drawBehind {
                val strokeWidth = 4.dp.toPx()
                val cornerRadius = strokeWidth / 2 // 使用线宽的一半作为圆角半径
                drawRoundRect(
                    color = Color(0xFF2C323A),
                    topLeft = Offset(0f, size.height - strokeWidth),
                    size = Size(size.width, strokeWidth),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .clip(RoundedCornerShape(28.dp))
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp), // 添加垂直内边距适配多行文本
            singleLine = false,
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            cursorBrush = SolidColor(Color(0.0f, 0.0f, 0.0f, 0.0f)),
            decorationBox = { innerTextField ->
                // 使用Box使内容从上方开始显示，适合多行输入
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF4E5C67),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Bold,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    widthInMax: Dp = 10000.dp,
    spacing: Dp = 50.dp
) {
    Text(
        text = text,
        modifier = modifier
            .widthIn(max = widthInMax)
            .basicMarquee(
                iterations = Int.MAX_VALUE, // 无限次滚动
                spacing = MarqueeSpacing(spacing), // 间隔
                velocity = 30.dp, // 滚动速度
                animationMode = MarqueeAnimationMode.Immediately // 立即开始动画
            ),
        color = color,
        fontWeight = fontWeight,
        style = style
    )
}

/**
 * 将指定的列表项滚动到屏幕中心
 */
suspend fun LazyListState.animateScrollToItemCentered(itemIndex: Int) {
    // 先滚动到目标项（不设置偏移），确保该项被测量
    animateScrollToItem(itemIndex)

    // 等待目标项进入可见范围
    snapshotFlow { layoutInfo.visibleItemsInfo.any { it.index == itemIndex } }
        .first { it }

    // 计算视口（LazyColumn可见区域）的高度
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

    // 从布局信息中找到目标项的信息
    val targetItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
    if (targetItemInfo != null) {
        // 计算目标项中心点位置
        val itemCenter = targetItemInfo.offset + targetItemInfo.size / 2
        // 视口中心
        val viewportCenter = viewportHeight / 2
        // 计算需要滚动的偏移量（正值：向下滚动，负值：向上滚动）
        val scrollAdjustment = itemCenter - viewportCenter
        animateScrollBy(scrollAdjustment.toFloat())
    }
}

@Preview(showBackground = true)
@Composable
fun InputBarPreview() {
    InputBar(query = "", onQueryChange = {}, placeholder = "InputBar")
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 4.dp,
                color = Color(0xFF2C323A), // 灰色边框
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .height(40.dp) // 固定高度
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp) // 调整内边距，使文本框和图标之间有空间
                .align(Alignment.CenterStart), // 文本垂直居中
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            ),
            cursorBrush = SolidColor(Color(0.0f, 0.0f, 0.0f, 0.0f)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFF4E5C67),
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        }
                        innerTextField() // 输入框内容
                    }
                    Image(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp) // 搜索图标
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    SearchBar(query = "", onQueryChange = {})
}

@Composable
fun InputRoundBar(
    query: String,
    isPassword: Boolean = false,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(
                width = 2.dp,
                color = Color(0xFF2C323A), // 灰色边框
                shape = RoundedCornerShape(15.5.dp)
            )
            .clip(RoundedCornerShape(15.5.dp))
            .height(40.dp) // 固定高度
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp) // 调整内边距，使文本框和图标之间有空间
                .align(Alignment.CenterStart), // 文本垂直居中
            singleLine = true,
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            ),
            cursorBrush = SolidColor(Color(0.0f, 0.0f, 0.0f, 0.0f)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFF4E5C67),
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        }
                        innerTextField() // 输入框内容
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InputRoundBarPreview() {
    InputRoundBar(query = "", onQueryChange = {}, placeholder = "InputRoundBar")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDropdown(
    options: Map<Long, String>,
    onItemSelected: (Long) -> Unit,
    select: MutableState<Long>,
    title: String = "",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember {
        mutableStateOf(options[select.value])
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            readOnly = true,
            value = selectedOptionText?: "",
            onValueChange = { /* 不允许手动输入 */ },
            label = { Text(title, color = Color.White) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor(),
            colors = TextFieldDefaults.colors(
                // 对于文本颜色
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White, // 你原来的 textColor
                disabledTextColor = Color.Gray,
                // errorTextColor = Color.Red, // 通常错误状态下文本颜色也会改变

                // 对于背景/容器颜色
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black, // 你原来的 containerColor
                disabledContainerColor = Color.Black, // 通常禁用状态颜色会更灰暗
                // errorContainerColor = ...,

                cursorColor = Color.Black,
                errorCursorColor = Color.Black,

                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.Gray,
                disabledIndicatorColor = Color.Gray, // 通常禁用状态颜色会更灰暗
                errorIndicatorColor = Color.Red,

                focusedLeadingIconColor = Color.Black,
                unfocusedLeadingIconColor = Color.DarkGray,
                disabledLeadingIconColor = Color.DarkGray, // 通常禁用状态颜色会更灰暗
                errorLeadingIconColor = Color.Red,

                focusedTrailingIconColor = Color.DarkGray,
                unfocusedTrailingIconColor = Color.DarkGray,

                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,

                // 对于占位符颜色
                focusedPlaceholderColor = Color.LightGray,
                unfocusedPlaceholderColor = Color.LightGray,
                disabledPlaceholderColor = Color.LightGray
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.value) },
                    onClick = {
                        selectedOptionText = entry.value
                        onItemSelected(entry.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextDropdownPreview() {
    val options = mapOf(1L to "123", 2L to "456")

    TextDropdown(
        options = options,
        onItemSelected = { option ->
            println("选择了: $option")
        },
        select = remember { mutableLongStateOf(1L) }
    )
}
