package com.minipai.article.core.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

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
    val info = currentWindowAdaptiveInfo()
    val size = info.windowSizeClass
    return when {
        size.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED -> AppLayoutType.EXPANDED
        size.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> AppLayoutType.MEDIUM
        size.windowHeightSizeClass == WindowHeightSizeClass.COMPACT -> AppLayoutType.MEDIUM // 横屏手机按 MEDIUM 处理
        else -> AppLayoutType.COMPACT
    }
}
