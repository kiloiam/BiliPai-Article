package com.minipai.article.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BiliPink,
    onPrimary = Color.White,
    primaryContainer = BiliPinkLight,
    onPrimaryContainer = BiliPinkDark,
    secondary = BiliBlue,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral700,
    outline = Neutral200,
    outlineVariant = Neutral80,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BiliPink,
    onPrimary = Color.White,
    primaryContainer = BiliPinkDark,
    onPrimaryContainer = BiliPinkLight,
    secondary = BiliBlue,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOnSurfaceVariant,
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

/**
 * BiliPai 专栏 App 主题。
 * - 主色固定为 B 站粉 #FB7299
 * - 跟随系统暗色模式
 */
@Composable
fun BiliPaiArticleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColors
        else -> LightColors
    }

    // 状态栏颜色透明 + 图标明暗适配
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.value.toInt().let {
                // 让 WindowCompat 处理；这里用透明即可
                android.graphics.Color.TRANSPARENT
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightNavigationBars = colorScheme.background.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BiliTypography,
        content = content
    )
}
