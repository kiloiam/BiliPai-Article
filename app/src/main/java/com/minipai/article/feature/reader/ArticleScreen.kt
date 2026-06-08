package com.minipai.article.feature.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minipai.article.R
import com.minipai.article.core.ui.theme.BiliPink

/**
 * 文章阅读页。
 * - WebView 加载 B 站专栏页（净化 CSS 注入）
 * - 自定义 toolbar：返回 / 标题 / 字号 - / 字号 + / 分享 / 浏览器打开 / 刷新
 * - 顶部进度条跟随 WebView 加载进度
 * - 出错时显示重试 + 浏览器打开
 */
@Composable
fun ArticleScreen(
    cvId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var fontSize by remember { mutableStateOf(17) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewKey by remember { mutableStateOf(0) }
    var showFontMenu by remember { mutableStateOf(false) }

    val articleUrl = "https://www.bilibili.com/read/cv$cvId"

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // WebView
            if (errorMessage == null) {
                key(webViewKey) {  // 重试时重新创建
                    ArticleWebView(
                        cvId = cvId,
                        onProgress = { progress = it },
                        onTitleChange = { title = it },
                        onError = { errorMessage = it },
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // 错误态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "😵", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.reader_failed),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            errorMessage = null
                            webViewKey++
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.reader_retry))
                        }
                        TextButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.reader_open_browser))
                        }
                    }
                }
            }

            // 自定义 toolbar（悬浮在顶部）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = title.ifBlank { "专栏 $cvId" },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(12) }) {
                            Icon(
                                imageVector = Icons.Outlined.TextDecrease,
                                contentDescription = "缩小字号",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(24) }) {
                            Icon(
                                imageVector = Icons.Outlined.TextIncrease,
                                contentDescription = "放大字号",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, articleUrl)
                                }
                                context.startActivity(Intent.createChooser(intent, "分享到"))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.reader_share),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = stringResource(R.string.reader_open_browser),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                // 进度条
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = BiliPink,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}
