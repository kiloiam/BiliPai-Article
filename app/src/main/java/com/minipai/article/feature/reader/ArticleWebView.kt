package com.minipai.article.feature.reader

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.BufferedReader

/**
 * 嵌入 B 站专栏页的 WebView。
 * - 加载 https://www.bilibili.com/read/cv{cvId}
 * - 注入 assets/reader.css 净化 B 站 chrome
 * - 进度回调给 Composable 用于顶部进度条
 * - 自定义 User-Agent（避免被识别为移动 WebView 触发简化版）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleWebView(
    cvId: Long,
    onProgress: (Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onError: (String?) -> Unit,
    fontSize: Int = 17,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val css = remember { loadCssFromAssets(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    // 伪装成桌面 Chrome
                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    allowFileAccess = false
                    allowContentAccess = false
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                    }
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (title != null) onTitleChange(title)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // 站内链接允许在 WebView 内打开；外链跳到系统浏览器
                        val url = request?.url?.toString() ?: return false
                        return if (url.contains("bilibili.com")) {
                            false
                        } else {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            runCatching { ctx.startActivity(intent) }
                            true
                        }
                    }
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        onError(description)
                    }
                }
                loadUrl("https://www.bilibili.com/read/cv$cvId")
            }
        },
        update = { webView ->
            // 字号变化时重新注入 CSS
            val cssWithFont = css.replace("/*FONT_SIZE_TOKEN*/", "${fontSize}px")
            webView.evaluateJavascript(
                """
                (function() {
                    var style = document.getElementById('bili-reader-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'bili-reader-style';
                        document.head.appendChild(style);
                    }
                    style.innerHTML = ${cssToJsString(cssWithFont)};
                })();
                """.trimIndent(),
                null
            )
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            // 不在这里 destroy WebView，让 ViewModel 持有，避免快速返回时重建
        }
    }
}

private fun loadCssFromAssets(context: android.content.Context): String {
    return runCatching {
        context.assets.open("reader.css")
            .bufferedReader()
            .use(BufferedReader::readText)
    }.getOrDefault("")
}

private fun cssToJsString(css: String): String {
    // 简单的 JSON 字符串化
    return buildString {
        append('"')
        for (ch in css) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
}
