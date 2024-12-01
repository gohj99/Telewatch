/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.model

import androidx.compose.ui.graphics.Color

sealed class SettingItem(val name: String) {
    data class Click(
        val itemName: String,
        val onClick: () -> Unit,
        val color: Color = Color(0xFF404953)
    ) : SettingItem(itemName)

    data class Switch(
        val itemName: String,
        var isSelected: Boolean,
        val onSelect: (Boolean) -> Unit,
        val color: Color = Color(0xFF404953)
    ) : SettingItem(itemName)

    data class ProgressBar(
        val itemName: String,
        var progress: Float,
        val maxValue: Float,
        val minValue: Float,
        val base: Float,
        val onProgressChange: (Float) -> Unit,
        val color: Color = Color(0xFF404953)
    ) : SettingItem(itemName)
}
