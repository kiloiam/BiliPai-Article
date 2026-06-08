package com.minipai.article.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
 * - Android 12+ 启用 dynamicColor 但保留 B 站粉主色覆盖
 */
@Composable
fun BiliPaiArticleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Android 12+ 优先用 dynamicColor（用户壁纸色），但 primary 仍固定为 B 站粉
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context)
                         else dynamicLightColorScheme(context)
            dynamic.copy(
                primary = BiliPink,
                onPrimary = Color.White,
                primaryContainer = if (darkTheme) BiliPinkDark else BiliPinkLight,
                onPrimaryContainer = if (darkTheme) BiliPinkLight else BiliPinkDark,
            )
        }
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
