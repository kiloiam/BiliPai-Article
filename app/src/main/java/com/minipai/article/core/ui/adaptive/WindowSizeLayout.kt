package com.minipai.article.core.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 应用布局类型。
 * - COMPACT：手机竖屏（<600dp 宽）。单栏布局。
 * - MEDIUM：手机横屏 / 小平板（600-840dp）。左右两栏。
 * - EXPANDED：平板 / 折叠屏展开（>=840dp）。三栏 Master-Detail-Detail。
 */
@Immutable
enum class AppLayoutType {
    COMPACT, MEDIUM, EXPANDED;

    val isMultiColumn: Boolean get() = this != COMPACT
}

@Composable
fun rememberAppLayoutType(): AppLayoutType {
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp >= 840 -> AppLayoutType.EXPANDED
        config.screenWidthDp >= 600 || config.screenHeightDp < 480 -> AppLayoutType.MEDIUM
        else -> AppLayoutType.COMPACT
    }
}
