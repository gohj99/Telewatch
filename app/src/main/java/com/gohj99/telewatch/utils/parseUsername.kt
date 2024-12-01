/*
 * Copyright (c) 2024 gohj99. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.gohj99.telewatch.utils

fun parseUsername(input: String): String? {
    // Regex to match Telegram username formats
    val regex = Regex(
        pattern = """(?:https?://)?(?:www\.)?t\.me/(?:@)?([a-zA-Z0-9_]{4,32})(?:@.*)?""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    return regex.find(input)?.groupValues?.get(1)
}